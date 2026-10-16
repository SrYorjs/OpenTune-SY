package com.arturo254.opentune.ui.player.state

import androidx.media3.common.C
import androidx.media3.common.Player
import com.arturo254.opentune.models.MediaMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerStateTest {
    private fun meta(
        id: String = "test-track",
        artwork: String? = "https://example.org/cover.jpg",
    ) = MediaMetadata(
        id = id,
        title = "Track",
        artists = listOf(MediaMetadata.Artist(id = "artist1", name = "Artist")),
        duration = 180,
        thumbnailUrl = artwork,
    )

    private fun input(
        metadata: MediaMetadata? = meta(),
        playback: Int = Player.STATE_READY,
        playing: Boolean = true,
        position: Long = 30_000L,
        duration: Long = 180_000L,
        seek: Long? = null,
        artwork: String? = null,
    ) = PlayerStateInput(
        mediaMetadata = metadata,
        playbackState = playback,
        isPlaying = playing,
        positionMs = position,
        durationMs = duration,
        pendingSeekMs = seek,
        offlineArtworkUrl = artwork,
        canSkipPrevious = true,
        canSkipNext = false,
        isLiked = true,
        shuffleModeEnabled = true,
        repeatMode = Player.REPEAT_MODE_ONE,
    )

    @Test
    fun trackAndArtworkReflectProvidedMetadata() {
        val state = input().toPlayerState()
        assertEquals("test-track", state.trackId)
        assertEquals("Track", state.title)
        assertTrue(state.hasTrack)
        assertEquals("https://example.org/cover.jpg", state.artworkUrl)
        assertEquals(30_000L, state.positionMs)
        assertEquals(180_000L, state.durationMs)
        assertEquals(1f / 6f, state.progressFraction, 0.00001f)
    }

    @Test
    fun offlineArtworkWinsButBlankOverrideKeepsOnlineArtwork() {
        assertEquals("content://local/cover", input(artwork = "content://local/cover").toPlayerState().artworkUrl)
        assertEquals("https://example.org/cover.jpg", input(artwork = " ").toPlayerState().artworkUrl)
        assertNull(input(metadata = null).toPlayerState().artworkUrl)
        assertFalse(input(metadata = null).toPlayerState().hasTrack)
    }

    @Test
    fun seekPreviewChangesDisplayWithoutMutatingPlaybackPosition() {
        val state = input(seek = 65_000L).toPlayerState()
        assertEquals(30_000L, state.positionMs)
        assertEquals(65_000L, state.displayPositionMs)
        assertEquals(65f / 180f, state.progressFraction, 0.00001f)
        assertTrue(state.isLoading)
        assertTrue(state.canSeek)
    }

    @Test
    fun bufferingAndSeekAreSeparateFromPlayingStatus() {
        val buffering = input(playback = Player.STATE_BUFFERING, playing = false).toPlayerState()
        assertTrue(buffering.isBuffering)
        assertTrue(buffering.isLoading)
        assertFalse(buffering.isPlaying)
        val paused = input(playing = false).toPlayerState()
        assertFalse(paused.isBuffering)
        assertFalse(paused.isLoading)
        assertFalse(paused.isPlaying)
        val seeking = input(seek = 10L).toPlayerState()
        assertFalse(seeking.isBuffering)
        assertTrue(seeking.isLoading)
        assertTrue(seeking.isPlaying)
    }

    @Test
    fun unknownAndInvalidDurationsCannotProduceInvalidProgress() {
        for (duration in listOf(0L, -1L, C.TIME_UNSET)) {
            val state = input(duration = duration, position = 100L).toPlayerState()
            assertFalse(state.canSeek)
            assertEquals(0L, state.durationMs)
            assertEquals(0f, state.progressFraction, 0f)
        }
    }

    @Test
    fun seekPreviewAndPositionsRemainWithinKnownDuration() {
        val state = input(position = -40L, duration = 100L, seek = 500L).toPlayerState()
        assertEquals(0L, state.positionMs)
        assertEquals(100L, state.displayPositionMs)
        assertEquals(1f, state.progressFraction, 0f)
        val exceeded = input(position = 500L, duration = 100L).toPlayerState()
        assertEquals(100L, exceeded.positionMs)
        assertEquals(1f, exceeded.progressFraction, 0f)
    }

    @Test
    fun everyControlStateRemainsSourcedFromConnectionInputs() {
        val state = input().toPlayerState()
        assertTrue(state.canSkipPrevious)
        assertFalse(state.canSkipNext)
        assertTrue(state.isLiked)
        assertTrue(state.shuffleModeEnabled)
        assertEquals(Player.REPEAT_MODE_ONE, state.repeatMode)
        assertEquals(Player.STATE_READY, state.playbackState)
        assertEquals("next-track", input(metadata = meta("next-track")).toPlayerState().trackId)
    }
}
