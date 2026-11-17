package com.arturo254.opentune.playback

import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select

internal const val PLAYBACK_RESOLUTION_BUDGET_MS = 20_000L
internal const val PLAYBACK_STREAM_CONNECT_TIMEOUT_MS = 10_000L

internal data class ResolutionStage<T>(val name: String, val timeoutMs: Long, val resolve: suspend () -> T?)
internal data class ResolutionObservation(val stage: String, val elapsedMs: Long, val outcome: String, val rootType: String?)
private data class ResolutionValue<T>(val value: T?)

internal suspend fun <T : Any> resolvePreferredPlayback(
    primary: suspend () -> T?,
    fallback: suspend (Boolean) -> T,
    observe: (ResolutionObservation) -> Unit = {},
    hedgeAfterMs: Long = 8_000L,
    timeoutMs: Long = PLAYBACK_RESOLUTION_BUDGET_MS,
    nowMs: () -> Long = { System.nanoTime() / 1_000_000L },
): T = withTimeoutOrNull(timeoutMs) {
    coroutineScope {
        val preferred = async {
            val started = nowMs()
            var outcome = "unavailable"
            var root: String? = null
            try {
                Result.success(primary().also { if (it != null) outcome = "success" })
            } catch (cancelled: CancellationException) {
                outcome = "cancelled"
                throw cancelled
            } catch (error: Exception) {
                outcome = "failed"
                root = error.javaClass.simpleName
                Result.failure<T?>(error)
            } finally { observe(ResolutionObservation("resolver_v2", nowMs() - started, outcome, root)) }
        }
        try {
            val early = withTimeoutOrNull(hedgeAfterMs) { preferred.await() }
            early?.getOrNull()?.let { return@coroutineScope it }
            val pending = early == null
            if (pending) observe(ResolutionObservation("resolver_v2_wait", hedgeAfterMs, "fallback_started", null))
            val alternative = async {
                try { Result.success(fallback(pending)) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { Result.failure<T>(error) }
            }
            try {
                if (!pending) return@coroutineScope alternative.await().getOrThrow()
                val first = select<Pair<Boolean, Result<T?>>> {
                    preferred.onAwait { true to it }
                    alternative.onAwait { false to it }
                }
                if (!first.first) return@coroutineScope first.second.getOrThrow() ?: throw java.io.IOException("No playable fallback")
                first.second.getOrNull() ?: alternative.await().getOrThrow()
            } finally { alternative.cancel() }
        } finally { preferred.cancel() }
    }
} ?: throw PlaybackResolutionTimeoutException()

internal class PlaybackResolutionTimeoutException : SocketTimeoutException("Playback resolution deadline exceeded")
internal class PlaybackLoadCancelledException(val mediaId: String) : InterruptedIOException("Playback load cancelled")

internal suspend fun <T : Any> resolvePlaybackStages(
    stages: List<ResolutionStage<T>>,
    timeoutMs: Long = PLAYBACK_RESOLUTION_BUDGET_MS,
    nowMs: () -> Long = { System.nanoTime() / 1_000_000L },
    observe: (ResolutionObservation) -> Unit = {},
): T {
    val value = withTimeoutOrNull(timeoutMs) {
        var lastError: Exception? = null
        var timedOut = false
        for (stage in stages) {
            val started = nowMs()
            var outcome = "unavailable"
            var rootType: String? = null
            try {
                val result = withTimeoutOrNull(stage.timeoutMs) { ResolutionValue(stage.resolve()) }
                if (result == null) {
                    timedOut = true
                    outcome = "timeout"
                } else if (result.value != null) {
                    outcome = "success"
                    return@withTimeoutOrNull result.value
                }
            } catch (cancelled: CancellationException) {
                outcome = if (cancelled is kotlinx.coroutines.TimeoutCancellationException) "timeout" else "cancelled"
                rootType = cancelled.javaClass.simpleName
                throw cancelled
            } catch (error: Exception) {
                lastError = error
                outcome = "failed"
                rootType = error.javaClass.simpleName
            } finally {
                observe(ResolutionObservation(stage.name, nowMs() - started, outcome, rootType))
            }
        }
        throw lastError ?: if (timedOut) PlaybackResolutionTimeoutException() else java.io.IOException("No playable source")
    }
    return value ?: throw PlaybackResolutionTimeoutException()
}

internal class PlaybackResolutionRequests {
    private val pending = ConcurrentHashMap<Any, Pair<String, Job>>()

    fun <T> resolve(mediaId: String, parent: Job?, block: suspend () -> T): T {
        val token = Any()
        val job = SupervisorJob(parent)
        pending[token] = mediaId to job
        try {
            return runBlocking(Dispatchers.IO + job) { block() }
        } catch (cancelled: CancellationException) {
            throw PlaybackLoadCancelledException(mediaId).apply { initCause(cancelled) }
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
            throw PlaybackLoadCancelledException(mediaId).apply { initCause(interrupted) }
        } finally {
            pending.remove(token)
            job.cancel()
        }
    }

    fun cancelExcept(mediaId: String?) {
        pending.values.filter { it.first != mediaId }.forEach { it.second.cancel() }
    }
}
