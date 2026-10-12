package com.arturo254.opentune.extensions

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerTransportPolicyTest {
    @Test
    fun idleAndPausedPrepareBeforeTogglingIntoPlay() {
        assertTrue(PlayerTransportPolicy.shouldPrepareBeforeToggle(
            playWhenReady = false,
            playbackState = Player.STATE_IDLE,
        ))
    }

    @Test
    fun activeOrReadyPlayersNeverPrepareAgainOnToggle() {
        for (playbackState in listOf(
            Player.STATE_IDLE,
            Player.STATE_BUFFERING,
            Player.STATE_READY,
            Player.STATE_ENDED,
        )) {
            assertFalse(PlayerTransportPolicy.shouldPrepareBeforeToggle(
                playWhenReady = true,
                playbackState = playbackState,
            ))
        }
        for (state in listOf(Player.STATE_BUFFERING, Player.STATE_READY, Player.STATE_ENDED)) {
            assertFalse(PlayerTransportPolicy.shouldPrepareBeforeToggle(false, state))
        }
    }

    @Test
    fun repeatModeCyclesOffAllOneOffWithoutChangingValues() {
        val off = Player.REPEAT_MODE_OFF
        val all = Player.REPEAT_MODE_ALL
        val one = Player.REPEAT_MODE_ONE
        assertEquals(all, PlayerTransportPolicy.nextRepeatMode(off))
        assertEquals(one, PlayerTransportPolicy.nextRepeatMode(all))
        assertEquals(off, PlayerTransportPolicy.nextRepeatMode(one))
    }

    @Test(expected = IllegalStateException::class)
    fun unsupportedRepeatModesAreStillRejected() {
        PlayerTransportPolicy.nextRepeatMode(-42)
    }
}
