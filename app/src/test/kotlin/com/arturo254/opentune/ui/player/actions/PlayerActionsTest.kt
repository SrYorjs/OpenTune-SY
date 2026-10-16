package com.arturo254.opentune.ui.player.actions

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerActionsTest {
    private class FakeBackend(
        override var playbackState: Int = Player.STATE_READY,
        override var activeMediaId: String? = "current",
    ) : PlayerActionBackend {
        val commands = mutableListOf<String>()
        override fun togglePlayback() { commands += "toggle" }
        override fun replayEnded() { commands += "replay" }
        override fun seekTo(positionMs: Long) { commands += "seek:$positionMs" }
        override fun advanceCrossfadeTarget() { commands += "crossfadeNext" }
        override fun next() { commands += "next" }
        override fun previous() { commands += "previous" }
        override fun toggleFavorite() { commands += "favorite" }
    }

    @Test fun normalPlayPauseDelegatesToTheOriginalTransport() {
        val fake = FakeBackend()
        val actions = PlayerActions(fake)
        actions.playPause()
        fake.playbackState = Player.STATE_IDLE
        actions.playPause()
        assertEquals(listOf("toggle", "toggle"), fake.commands)
    }

    @Test fun endedPlaybackReplaysFromStart() {
        val fake = FakeBackend(playbackState = Player.STATE_ENDED)
        PlayerActions(fake).playPause()
        assertEquals(listOf("replay"), fake.commands)
    }

    @Test fun normalSeeksNeverSkip() {
        val fake = FakeBackend()
        val actions = PlayerActions(fake)
        actions.seekTo(25_000)
        actions.seekTo(42_000, displayedTrackId = "current")
        assertEquals(listOf("seek:25000", "seek:42000"), fake.commands)
    }

    @Test fun crossfadeSeeksAdvanceBeforeApplyingPosition() {
        val fake = FakeBackend(activeMediaId = "old")
        PlayerActions(fake).seekTo(23_000, displayedTrackId = "incoming")
        assertEquals(listOf("crossfadeNext", "seek:23000"), fake.commands)
    }

    @Test fun transportAndFavoriteForwardExactlyOnce() {
        val fake = FakeBackend()
        val actions = PlayerActions(fake)
        actions.previous()
        actions.next()
        actions.toggleFavorite()
        assertEquals(listOf("previous", "next", "favorite"), fake.commands)
    }

    @Test fun replayAndCrossfadeRemainSeparateActions() {
        val fake = FakeBackend(playbackState = Player.STATE_ENDED)
        val actions = PlayerActions(fake)
        actions.playPause()
        actions.seekTo(5000L)
        assertEquals(listOf("replay", "seek:5000"), fake.commands)
        assertTrue(fake.commands.none { it == "crossfadeNext" })
    }
}
