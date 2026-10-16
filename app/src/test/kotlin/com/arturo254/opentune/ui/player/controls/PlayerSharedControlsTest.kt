package com.arturo254.opentune.ui.player.controls

import androidx.media3.common.Player
import com.arturo254.opentune.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerSharedControlsTest {
    @Test fun playPauseAndEndedStateChooseExactlyTheLegacyIcons() {
        assertEquals(R.drawable.replay, playerPlaybackIconRes(Player.STATE_ENDED, true))
        assertEquals(R.drawable.replay, playerPlaybackIconRes(Player.STATE_ENDED, false))
        assertEquals(R.drawable.pause, playerPlaybackIconRes(Player.STATE_READY, true))
        assertEquals(R.drawable.play, playerPlaybackIconRes(Player.STATE_READY, false))
        assertEquals(R.drawable.play, playerPlaybackIconRes(Player.STATE_IDLE, false))
    }

    @Test fun skipIconsAreDirectionSpecific() {
        assertEquals(R.drawable.skip_previous, playerSkipIconRes(PlayerSkipDirection.PREVIOUS))
        assertEquals(R.drawable.skip_next, playerSkipIconRes(PlayerSkipDirection.NEXT))
        assertNotEquals(playerSkipIconRes(PlayerSkipDirection.PREVIOUS),
            playerSkipIconRes(PlayerSkipDirection.NEXT))
    }

    @Test fun favoriteRespectsLegacyAndV8Assets() {
        assertEquals(R.drawable.favorite, playerFavoriteIconRes(liked = true))
        assertEquals(R.drawable.favorite_border, playerFavoriteIconRes(liked = false))
        assertEquals(R.drawable.favorite_filled, playerFavoriteIconRes(liked = true, v8 = true))
        assertEquals(R.drawable.favorite_outline, playerFavoriteIconRes(liked = false, v8 = true))
    }

    @Test fun repeatModeOneHasItsDedicatedGlyph() {
        assertEquals(R.drawable.repeat_one, playerRepeatIconRes(Player.REPEAT_MODE_ONE))
        assertEquals(R.drawable.repeat, playerRepeatIconRes(Player.REPEAT_MODE_ALL))
        assertEquals(R.drawable.repeat, playerRepeatIconRes(Player.REPEAT_MODE_OFF))
    }

    @Test fun shareTextRetainsExistingUrlForAllTrackIds() {
        assertEquals("https://music.youtube.com/watch?v=my-song", playerShareUrl("my-song"))
        assertTrue(playerShareUrl("abc123").endsWith("abc123"))
    }
}
