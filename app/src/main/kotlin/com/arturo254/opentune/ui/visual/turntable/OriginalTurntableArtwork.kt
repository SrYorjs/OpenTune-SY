package com.arturo254.opentune.ui.visual.turntable

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arturo254.opentune.ui.player.actions.PlayerActions
import kotlinx.coroutines.isActive

@Composable
fun OriginalTurntableArtwork(
    mediaId: String,
    title: String,
    artworkUrl: String?,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    active: Boolean,
    actions: PlayerActions,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
) {
    val angle = remember(mediaId) { mutableFloatStateOf(0f) }
    val lastFrameNanos = remember(mediaId) { mutableLongStateOf(0L) }
    LaunchedEffect(isPlaying, active, mediaId) {
        if (!isPlaying || !active) {
            lastFrameNanos.longValue = 0L
            return@LaunchedEffect
        }
        while (isActive) {
            withInfiniteAnimationFrameNanos { frameTimeNanos ->
                val previous = lastFrameNanos.longValue
                lastFrameNanos.longValue = frameTimeNanos
                if (previous != 0L) {
                    val delta = (frameTimeNanos - previous) / 1_000_000_000f
                    angle.floatValue = (angle.floatValue + 26f * delta).mod(360f)
                }
            }
        }
    }
    Box(modifier) {
        TurntableBase(Modifier.fillMaxSize())
        DiscCover(
            style = PlayerCoverStyle.TURNTABLE,
            artworkUrl = artworkUrl,
            discName = title,
            realistic = true,
            editMode = !interactive,
            rotationProvider = { angle.floatValue },
            modifier = Modifier.fillMaxSize(),
        )
        TurntableArm(
            active = active,
            interactive = interactive,
            positionMs = positionMs,
            durationMs = durationMs,
            isPlaying = isPlaying,
            mediaId = mediaId,
            onSeek = { actions.seekTo(it, displayedTrackId = mediaId) },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
