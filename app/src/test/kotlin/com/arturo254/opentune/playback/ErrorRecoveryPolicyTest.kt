package com.arturo254.opentune.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorRecoveryPolicyTest {
    @Test
    fun streamRefresh_isRequestedForExistingRecoverableSignals() {
        assertTrue(
            ErrorRecoveryPolicy.shouldAttemptStreamRefresh(
                StreamRefreshSignals(badHttpStatusError = true),
            ),
        )
        assertTrue(
            ErrorRecoveryPolicy.shouldAttemptStreamRefresh(
                StreamRefreshSignals(fileNotFoundError = true),
            ),
        )
        assertTrue(
            ErrorRecoveryPolicy.shouldAttemptStreamRefresh(
                StreamRefreshSignals(readPositionOutOfRangeError = true),
            ),
        )

        listOf(403, 404, 410, 416, 429, 500, 502, 503).forEach { code ->
            assertTrue(
                ErrorRecoveryPolicy.shouldAttemptStreamRefresh(
                    StreamRefreshSignals(httpStatusCode = code),
                ),
            )
        }
    }

    @Test
    fun streamRefresh_ignoresNonRecoverableHttpStatuses() {
        listOf(200, 400, 401, 418, 501).forEach { code ->
            assertFalse(
                ErrorRecoveryPolicy.shouldAttemptStreamRefresh(
                    StreamRefreshSignals(httpStatusCode = code),
                ),
            )
        }
    }

    @Test
    fun silenceRecovery_requiresSettingAndMatchingCause() {
        assertTrue(
            ErrorRecoveryPolicy.looksLikeSilenceProcessorFailure(
                skipSilenceEnabled = true,
                causeText = "SilenceSkippingAudioProcessor failed",
            ),
        )
        assertTrue(
            ErrorRecoveryPolicy.looksLikeSilenceProcessorFailure(
                skipSilenceEnabled = true,
                causeText = "decoder error: silence processor",
            ),
        )
        assertFalse(
            ErrorRecoveryPolicy.looksLikeSilenceProcessorFailure(
                skipSilenceEnabled = false,
                causeText = "SilenceSkippingAudioProcessor failed",
            ),
        )
        assertFalse(
            ErrorRecoveryPolicy.looksLikeSilenceProcessorFailure(
                skipSilenceEnabled = true,
                causeText = "unrelated decoder error",
            ),
        )
    }

    @Test
    fun consecutiveErrorPolicy_preservesLegacyTwoPointIncrementAndThreshold() {
        val first = ErrorRecoveryPolicy.nextConsecutiveErrorCount(0)
        val second = ErrorRecoveryPolicy.nextConsecutiveErrorCount(first)
        val third = ErrorRecoveryPolicy.nextConsecutiveErrorCount(second)

        assertEquals(2, first)
        assertEquals(4, second)
        assertEquals(6, third)

        assertTrue(
            ErrorRecoveryPolicy.shouldSkipToNext(
                nextConsecutiveErrorCount = first,
                hasNextItem = true,
            ),
        )
        assertTrue(
            ErrorRecoveryPolicy.shouldSkipToNext(
                nextConsecutiveErrorCount = second,
                hasNextItem = true,
            ),
        )
        assertFalse(
            ErrorRecoveryPolicy.shouldSkipToNext(
                nextConsecutiveErrorCount = third,
                hasNextItem = true,
            ),
        )
    }

    @Test
    fun autoSkip_neverRunsWithoutAnotherQueueItem() {
        assertFalse(
            ErrorRecoveryPolicy.shouldSkipToNext(
                nextConsecutiveErrorCount = 2,
                hasNextItem = false,
            ),
        )
    }
}
