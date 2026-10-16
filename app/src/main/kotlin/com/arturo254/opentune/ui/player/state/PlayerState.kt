package com.arturo254.opentune.ui.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.media3.common.C
import androidx.media3.common.Player
import com.arturo254.opentune.models.MediaMetadata

/**
 * One immutable VIEW of the currently playing track.
 *
 * Playback stays with PlayerConnection / MusicService. Position is supplied by
 * the existing Player.kt 100-ms ticker: this model NEVER starts a second timer
 * or seeks the Media3 player.
 */
@Immutable
data class PlayerState(
    val mediaMetadata: MediaMetadata?,
    val artworkUrl: String?,
    val positionMs: Long,
    val durationMs: Long,
    val displayPositionMs: Long,
    val progressFraction: Float,
    val playbackState: Int,
    val isPlaying: Boolean,
    val isBuffering: Boolean,
    val isLoading: Boolean,
    val canSeek: Boolean,
    val canSkipPrevious: Boolean,
    val canSkipNext: Boolean,
    val isLiked: Boolean,
    val shuffleModeEnabled: Boolean,
    val repeatMode: Int,
) {
    val trackId: String? get() = mediaMetadata?.id
    val title: String? get() = mediaMetadata?.title
    val hasTrack: Boolean get() = mediaMetadata != null
}

/** Immutable inputs from the *existing* connection and Player UI state. */
@Immutable
data class PlayerStateInput(
    val mediaMetadata: MediaMetadata?,
    val playbackState: Int,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val pendingSeekMs: Long? = null,
    val offlineArtworkUrl: String? = null,
    val canSkipPrevious: Boolean,
    val canSkipNext: Boolean,
    val isLiked: Boolean,
    val shuffleModeEnabled: Boolean,
    val repeatMode: Int,
)

/**
 * Pure mapping shared by full Player and MiniPlayer. No Flow subscriptions,
 * Media3 listener, preference write, playback action or additional tick.
 */
fun PlayerStateInput.toPlayerState(): PlayerState {
    val canSeek = durationMs > 0L && durationMs != C.TIME_UNSET
    val safeDuration = if (canSeek) durationMs else 0L
    val safePosition =
        positionMs.coerceAtLeast(0L).let { if (canSeek) it.coerceAtMost(safeDuration) else it }
    val displayPosition =
        (pendingSeekMs ?: positionMs).coerceAtLeast(0L)
            .let { if (canSeek) it.coerceAtMost(safeDuration) else it }
    val progress =
        if (canSeek) (displayPosition.toDouble() / safeDuration.toDouble()).toFloat().coerceIn(0f, 1f)
        else 0f
    val offline = offlineArtworkUrl?.takeIf { it.isNotBlank() }
    val remote = mediaMetadata?.thumbnailUrl?.takeIf { it.isNotBlank() }
    val buffering = playbackState == Player.STATE_BUFFERING
    return PlayerState(
        mediaMetadata = mediaMetadata,
        artworkUrl = offline ?: remote,
        positionMs = safePosition,
        durationMs = safeDuration,
        displayPositionMs = displayPosition,
        progressFraction = progress,
        playbackState = playbackState,
        isPlaying = isPlaying,
        isBuffering = buffering,
        isLoading = buffering || pendingSeekMs != null,
        canSeek = canSeek,
        canSkipPrevious = canSkipPrevious,
        canSkipNext = canSkipNext,
        isLiked = isLiked,
        shuffleModeEnabled = shuffleModeEnabled,
        repeatMode = repeatMode,
    )
}

/** Compose presentation adapter; inputs remain owned by the old consumers. */
@Composable
fun rememberPlayerState(input: PlayerStateInput): PlayerState =
    remember(input) { input.toPlayerState() }
