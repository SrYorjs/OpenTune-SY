package com.arturo254.opentune.ui.player.actions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.media3.common.Player
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.extensions.toggleRepeatMode
import com.arturo254.opentune.playback.PlayerConnection

/**
 * Single user-intent entry point for all current/future Player layouts.
 * It does not own Media3, a ticker, a StateFlow or playback lifecycle.
 */
class PlayerActions internal constructor(private val backend: PlayerActionBackend) {
    /** Preserve the existing Player replay behavior when a track has ended. */
    fun playPause() {
        if (backend.playbackState == Player.STATE_ENDED) backend.replayEnded()
        else backend.togglePlayback()
    }

    /**
     * Preserve the old seek-on-crossfade sequence: the visible metadata can
     * identify the NEXT track before the engine has switched the current item.
     */
    fun seekTo(positionMs: Long, displayedTrackId: String? = null) {
        if (displayedTrackId != null && backend.activeMediaId != displayedTrackId) {
            backend.advanceCrossfadeTarget()
        }
        backend.seekTo(positionMs)
    }

    /** Preserve Together guest + Discord Presence routing from PlayerConnection. */
    fun next() = backend.next()
    fun previous() = backend.previous()

    /** Likes are still owned by MusicService via PlayerConnection. */
    fun toggleFavorite() = backend.toggleFavorite()

    /** Shared secondary controls; use the same Media3 commands as legacy UI. */
    fun toggleShuffle() = backend.toggleShuffle()
    fun toggleRepeat() = backend.toggleRepeat()
}

internal interface PlayerActionBackend {
    val playbackState: Int
    val activeMediaId: String?
    fun togglePlayback()
    fun replayEnded()
    fun seekTo(positionMs: Long)
    fun advanceCrossfadeTarget()
    fun next()
    fun previous()
    fun toggleFavorite()
    fun toggleShuffle()
    fun toggleRepeat()
}

private class PlayerConnectionActionBackend(private val connection: PlayerConnection) : PlayerActionBackend {
    private val player get() = connection.player
    override val playbackState: Int get() = player.playbackState
    override val activeMediaId: String? get() = player.currentMediaItem?.mediaId

    override fun togglePlayback() { player.togglePlayPause() }
    override fun replayEnded() {
        player.seekTo(0, 0)
        player.playWhenReady = true
    }
    override fun seekTo(positionMs: Long) { player.seekTo(positionMs) }
    override fun advanceCrossfadeTarget() {
        // Internal crossfade realignment, not a user skip.
        player.seekToNext()
    }
    override fun next() { connection.seekToNext() }
    override fun previous() { connection.seekToPrevious() }
    override fun toggleFavorite() { connection.toggleLike() }
    override fun toggleShuffle() { player.shuffleModeEnabled = !player.shuffleModeEnabled }
    override fun toggleRepeat() { player.toggleRepeatMode() }
}

@Composable
fun rememberPlayerActions(connection: PlayerConnection): PlayerActions =
    remember(connection) { PlayerActions(PlayerConnectionActionBackend(connection)) }
