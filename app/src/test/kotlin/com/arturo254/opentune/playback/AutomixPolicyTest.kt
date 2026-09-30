package com.arturo254.opentune.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomixPolicyTest {
    @Test
    fun normalActiveAutomix_doesNotRefreshSameSeed() {
        assertFalse(
            AutomixPolicy.shouldRefreshCurrentSeed(
                force = false,
                sameSeed = true,
                hasItems = true,
                jobActive = true,
            ),
        )
    }

    @Test
    fun forcedRefresh_alwaysRefreshes() {
        assertTrue(
            AutomixPolicy.shouldRefreshCurrentSeed(
                force = true,
                sameSeed = true,
                hasItems = true,
                jobActive = true,
            ),
        )
    }

    @Test
    fun missingPool_orInactiveJob_refreshes() {
        assertTrue(
            AutomixPolicy.shouldRefreshCurrentSeed(
                force = false,
                sameSeed = true,
                hasItems = false,
                jobActive = true,
            ),
        )
        assertTrue(
            AutomixPolicy.shouldRefreshCurrentSeed(
                force = false,
                sameSeed = true,
                hasItems = true,
                jobActive = false,
            ),
        )
    }

    @Test
    fun automixRun_requiresEnabledRepeatOffAndActiveQueue() {
        assertTrue(
            AutomixPolicy.shouldRun(
                autoLoadMoreEnabled = true,
                repeatModeOff = true,
                suppressAutoPlayback = false,
                mediaItemCount = 3,
            ),
        )
        assertFalse(
            AutomixPolicy.shouldRun(
                autoLoadMoreEnabled = false,
                repeatModeOff = true,
                suppressAutoPlayback = false,
                mediaItemCount = 3,
            ),
        )
        assertFalse(
            AutomixPolicy.shouldRun(
                autoLoadMoreEnabled = true,
                repeatModeOff = false,
                suppressAutoPlayback = false,
                mediaItemCount = 3,
            ),
        )
        assertFalse(
            AutomixPolicy.shouldRun(
                autoLoadMoreEnabled = true,
                repeatModeOff = true,
                suppressAutoPlayback = true,
                mediaItemCount = 3,
            ),
        )
        assertFalse(
            AutomixPolicy.shouldRun(
                autoLoadMoreEnabled = true,
                repeatModeOff = true,
                suppressAutoPlayback = false,
                mediaItemCount = 0,
            ),
        )
    }
}
