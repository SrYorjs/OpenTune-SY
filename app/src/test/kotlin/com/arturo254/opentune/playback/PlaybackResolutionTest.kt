package com.arturo254.opentune.playback

import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

class PlaybackResolutionTest {
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
