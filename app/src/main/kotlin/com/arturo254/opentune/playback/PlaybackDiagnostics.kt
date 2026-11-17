package com.arturo254.opentune.playback

import androidx.media3.common.ParserException
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.cache.Cache
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException

internal class PlaybackResolutionDataSourceException(val selectedId: String, error: Throwable, spec: DataSpec, val elapsedMs: Long = 0L) :
    HttpDataSource.HttpDataSourceException(
        error as? IOException ?: IOException("Playback resolution failed", error), spec,
        when (error) {
            is SocketTimeoutException -> PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
            is java.net.ConnectException, is java.net.UnknownHostException -> PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
            else -> PlaybackException.ERROR_CODE_IO_UNSPECIFIED
        }, TYPE_OPEN,
    )

internal data class PlaybackFailureDiagnostic(
    val mediaId: String, val provider: String, val stage: String, val rootType: String,
    val code: Int, val httpCode: Int?, val category: String, val cancelled: Boolean, val stale: Boolean, val elapsedMs: Long?,
)

internal fun playbackFailureDiagnostic(selectedId: String?, error: Throwable, code: Int = -1): PlaybackFailureDiagnostic {
    val chain = generateSequence(error) { it.cause }.take(32).toList()
    val resolution = chain.filterIsInstance<PlaybackResolutionDataSourceException>().firstOrNull()
    val cancelledLoad = chain.filterIsInstance<PlaybackLoadCancelledException>().firstOrNull()
    val io = chain.flatMap { it.suppressed.toList() }.filterIsInstance<PlaybackIoContext>().firstOrNull()
    val rawId = resolution?.selectedId ?: cancelledLoad?.mediaId ?: io?.mediaId ?: selectedId.orEmpty()
    val provider = when {
        rawId.startsWith("audius:") -> "audius"
        rawId.startsWith("peertube:") || rawId.startsWith("video:peertube:") -> "peertube"
        rawId.matches(Regex("[A-Za-z0-9_-]{11}")) -> "youtube"
        rawId.startsWith("local:") || rawId.startsWith("offline:") -> "local"
        else -> "unknown"
    }
    val id = when (provider) {
        "youtube" -> rawId
        "audius" -> rawId.substringAfter("audius:").takeIf { it.matches(Regex("[A-Za-z0-9_-]{1,80}")) }
        "peertube" -> rawId.substringAfterLast(':').takeIf { it.matches(Regex("[A-Fa-f0-9-]{36}")) }
        else -> null
    } ?: "redacted"
    val http = chain.filterIsInstance<HttpDataSource.InvalidResponseCodeException>().firstOrNull()
    val network = chain.filterIsInstance<HttpDataSource.HttpDataSourceException>().firstOrNull()
    val timeout = chain.any { it is SocketTimeoutException || it is TimeoutCancellationException }
    val cancelled = !timeout && chain.any { it is PlaybackLoadCancelledException || it is CancellationException }
    val category = when {
        timeout -> "timeout"
        cancelled -> "cancelled"
        chain.any { it is Cache.CacheException } -> "cache"
        chain.any { it is ParserException } -> "extractor"
        http != null -> "http"
        chain.any { it is javax.net.ssl.SSLException } -> "tls"
        chain.any { it is java.net.UnknownHostException || it is java.net.ConnectException || it is java.net.SocketException } -> "network"
        resolution != null -> "resolution"
        else -> "read"
    }
    val stage = when {
        resolution != null || chain.any { it is TimeoutCancellationException || it is PlaybackLoadCancelledException } -> "resolver"
        category == "cache" -> "cache"
        category == "extractor" -> "extractor"
        network?.type == HttpDataSource.HttpDataSourceException.TYPE_OPEN -> "http_connection"
        else -> "data_source"
    }
    val sourceId = resolution?.selectedId ?: cancelledLoad?.mediaId ?: io?.mediaId
    return PlaybackFailureDiagnostic(id, provider, stage, chain.last().javaClass.simpleName, code, http?.responseCode, category, cancelled,
        sourceId != null && sourceId != selectedId, resolution?.elapsedMs ?: io?.elapsedMs)
}

private class PlaybackIoContext(val mediaId: String?, val elapsedMs: Long) : IOException("Playback I/O context")

internal class PlaybackDiagnosticDataSource(private val source: DataSource, private val openStage: String = "data_source_open", private val traceReads: Boolean = true) : DataSource by source {
    private var mediaId: String? = null
    private var openedAt = 0L
    private var firstByte = false

    override fun open(spec: DataSpec): Long {
        mediaId = spec.key
        openedAt = System.nanoTime()
        firstByte = false
        return measured(spec.key) { source.open(spec) }.also {
            PlaybackStartupTrace.record(mediaId, openStage, (System.nanoTime() - openedAt) / 1_000_000L)
            if (traceReads) PlaybackStartupTrace.mark(mediaId, "data_source_open_total")
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int = measured(mediaId) { source.read(buffer, offset, length) }.also {
        if (it > 0 && !firstByte && traceReads) {
            firstByte = true
            PlaybackStartupTrace.record(mediaId, "first_audio_bytes", (System.nanoTime() - openedAt) / 1_000_000L)
            PlaybackStartupTrace.mark(mediaId, "first_audio_bytes_total")
        }
    }

    private inline fun <T> measured(id: String?, block: () -> T): T = try {
        block()
    } catch (error: IOException) {
        error.addSuppressed(PlaybackIoContext(id, (System.nanoTime() - openedAt) / 1_000_000L))
        throw error
    }

    override fun close() {
        try { source.close() } finally { mediaId = null }
    }
}
