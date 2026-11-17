package com.arturo254.opentune.playback

import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

class PlaybackResolutionTest {
    @Test fun unavailableInnertubeDoesNotCancelPendingResolver() = runTest {
        var completed = false
        val result = resolvePreferredPlayback(
            primary = { delay(12_000); completed = true; "resolver" },
            fallback = { delay(1_000); throw IOException("Unavailable") },
        )
        assertEquals("resolver", result)
        assertTrue(completed)
        assertEquals(12_000L, testScheduler.currentTime)
    }

    @Test fun resolverThatFinishesAfterEightSecondsCanStillBeatUnavailableInnertube() = runTest {
        var requests = 0
        var alternativeCancelled = false
        assertEquals("resolver", resolvePreferredPlayback(
            primary = { requests++; delay(9_000); "resolver" },
            fallback = { pending ->
                assertTrue(pending)
                try { delay(7_000); throw IOException("Unavailable") }
                finally { alternativeCancelled = true }
            },
        ))
        assertEquals(9_000L, testScheduler.currentTime)
        assertEquals(1, requests)
        assertTrue(alternativeCancelled)
    }

    @Test fun failedFallbackKeepsResolverAliveOnlyUntilTheTwentySecondDeadline() = runTest {
        var cancelled = false
        try {
            resolvePreferredPlayback(primary = { try { awaitCancellation() } finally { cancelled = true } },
                fallback = { delay(7_000); throw IOException("Unavailable") })
            fail("failure lost")
        } catch (_: PlaybackResolutionTimeoutException) { assertEquals(20_000L, testScheduler.currentTime) }
        assertTrue(cancelled)
        val started = testScheduler.currentTime
        try {
            resolvePreferredPlayback(primary = { delay(30_000); "late" }, fallback = { delay(30_000); "also late" })
            fail("deadline ignored")
        } catch (_: PlaybackResolutionTimeoutException) { assertEquals(20_000L, testScheduler.currentTime - started) }
    }

    @Test fun fastResolverSkipsFallbackAndParentCancellationCancelsBothRequests() = runTest {
        var alternatives = 0
        assertEquals("ready", resolvePreferredPlayback(primary = { "ready" }, fallback = { alternatives++; "wrong" }))
        assertEquals(0, alternatives)
        val primaryStopped = CompletableDeferred<Unit>()
        val fallbackStarted = CompletableDeferred<Unit>()
        val fallbackStopped = CompletableDeferred<Unit>()
        val job = launch {
            resolvePreferredPlayback<String>(
                primary = { try { awaitCancellation() } finally { primaryStopped.complete(Unit) } },
                fallback = { try { fallbackStarted.complete(Unit); awaitCancellation() } finally { fallbackStopped.complete(Unit) } },
            )
        }
        fallbackStarted.await()
        job.cancelAndJoin()
        assertTrue(primaryStopped.isCompleted)
        assertTrue(fallbackStopped.isCompleted)
        assertTrue(job.isCancelled)
    }

    @Test fun slowResolverLeavesTimeForFallbackAndRecordsStageTimes() = runTest {
        val observed = mutableListOf<ResolutionObservation>()
        val result = resolvePlaybackStages(listOf(
            ResolutionStage("resolver_v2", 8_000) { delay(30_000); "late" },
            ResolutionStage("innertube", 12_000) { delay(500); "playable" },
        ), nowMs = { testScheduler.currentTime }, observe = observed::add)
        assertEquals("playable", result)
        assertEquals(8500L, testScheduler.currentTime)
        assertEquals(listOf("timeout", "success"), observed.map { it.outcome })
        assertEquals(8000L, observed.first().elapsedMs)
    }

    @Test fun totalDeadlineBeyondTwentySecondsIsAnIoTimeout() = runTest {
        try {
            resolvePlaybackStages(listOf(ResolutionStage("innertube", 60_000) { delay(30_000); "late" }))
            fail("deadline ignored")
        } catch (_: PlaybackResolutionTimeoutException) {
            assertEquals(20_000L, testScheduler.currentTime)
        }
    }

    @Test fun parentCancellationDoesNotStartFallbackOrBecomeTimeout() = runTest {
        var fallback = false
        val started = CompletableDeferred<Unit>()
        val job = launch {
            resolvePlaybackStages(listOf(
                ResolutionStage<String>("resolver_v2", 8000) { started.complete(Unit); awaitCancellation() },
                ResolutionStage("legacy", 3000) { fallback = true; "wrong" },
            ))
        }
        started.await()
        job.cancelAndJoin()
        assertTrue(job.isCancelled)
        assertFalse(fallback)
        assertEquals(0L, testScheduler.currentTime)
    }

    @Test fun failedRequestDoesNotCancelServiceOrFollowingTrack() {
        val parent = Job()
        val requests = PlaybackResolutionRequests()
        try {
            requests.resolve("-9l5t3MWss8", parent) { throw IOException("fixture") }
            fail("failure lost")
        } catch (_: IOException) { }
        assertTrue(parent.isActive)
        assertEquals("first", requests.resolve("-9l5t3MWss8", parent) { "first" })
        assertEquals("second", requests.resolve("Mcj75l2gJcY", parent) { "second" })
        parent.cancel()
    }

    @Test fun rapidTrackChangeCancelsOnlyOldLoadAndUsesIoCancellation() = runBlocking {
        val requests = PlaybackResolutionRequests()
        val parent = Job()
        val started = CompletableDeferred<Unit>()
        val old = async(Dispatchers.IO) {
            try {
                requests.resolve("-9l5t3MWss8", parent) { started.complete(Unit); awaitCancellation() }
                null
            } catch (error: PlaybackLoadCancelledException) { error }
        }
        withTimeout(2000) { started.await() }
        requests.cancelExcept("Mcj75l2gJcY")
        assertEquals("-9l5t3MWss8", withTimeout(2000) { old.await() }?.mediaId)
        assertTrue(parent.isActive)
        assertEquals("next", requests.resolve("Mcj75l2gJcY", parent) { "next" })
        parent.cancel()
    }

    @Test fun recoveryIsBoundedAndNewTrackGetsItsOwnAllowance() {
        var now = 100L
        val attempts = TrackRecoveryAttempts { now }
        assertTrue(attempts.allow("-9l5t3MWss8"))
        assertTrue(attempts.allow("-9l5t3MWss8"))
        assertFalse(attempts.allow("-9l5t3MWss8"))
        attempts.transition("Mcj75l2gJcY")
        assertTrue(attempts.allow("Mcj75l2gJcY"))
        assertTrue(attempts.allow("Mcj75l2gJcY"))
        assertFalse(attempts.allow("Mcj75l2gJcY"))
        now += 45_001
        assertTrue(attempts.allow("Mcj75l2gJcY"))
        assertTrue(ErrorRecoveryPolicy.shouldAttemptStreamRefresh(StreamRefreshSignals(resolutionTimeoutError = true)))
    }
}
