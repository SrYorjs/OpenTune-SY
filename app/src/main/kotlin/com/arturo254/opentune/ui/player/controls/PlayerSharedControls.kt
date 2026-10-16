@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.arturo254.opentune.ui.player.controls

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.size
import androidx.media3.common.Player
import com.arturo254.opentune.R

/**
 * Common playback glyphs and content for V1–V8.
 *
 * Each VisualSkin/PlayerLayout still owns button shapes, click indication,
 * pressed animation, enabled state, size, Glass and placement.
 */
enum class PlayerSkipDirection { PREVIOUS, NEXT }

@DrawableRes
fun playerPlaybackIconRes(playbackState: Int, isPlaying: Boolean): Int =
    when {
        playbackState == Player.STATE_ENDED -> R.drawable.replay
        isPlaying -> R.drawable.pause
        else -> R.drawable.play
    }

@DrawableRes
fun playerSkipIconRes(direction: PlayerSkipDirection): Int =
    when (direction) {
        PlayerSkipDirection.PREVIOUS -> R.drawable.skip_previous
        PlayerSkipDirection.NEXT -> R.drawable.skip_next
    }

@DrawableRes
fun playerFavoriteIconRes(liked: Boolean, v8: Boolean = false): Int =
    when {
        v8 && liked -> R.drawable.favorite_filled
        v8 -> R.drawable.favorite_outline
        liked -> R.drawable.favorite
        else -> R.drawable.favorite_border
    }

@DrawableRes
fun playerRepeatIconRes(mode: Int): Int =
    if (mode == Player.REPEAT_MODE_ONE) R.drawable.repeat_one else R.drawable.repeat

/** Original sharing URL, now defined once rather than copied across skins. */
fun playerShareUrl(trackId: String): String =
    "https://music.youtube.com/watch?v=$trackId"

fun sharePlayerTrack(context: Context, trackId: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, playerShareUrl(trackId))
    }
    context.startActivity(Intent.createChooser(intent, null))
}

/** Replaces repeated play/pause/replay + loading branches without changing UI layout. */
@Composable
fun PlayerSharedPlaybackContent(
    playbackState: Int,
    isPlaying: Boolean,
    isLoading: Boolean,
    iconSize: Dp,
    loadingSize: Dp,
    loadingTint: Color,
    iconTint: Color = LocalContentColor.current,
) {
    if (isLoading) {
        CircularWavyProgressIndicator(
            modifier = Modifier.size(loadingSize),
            color = loadingTint,
        )
    } else {
        Icon(
            painter = painterResource(playerPlaybackIconRes(playbackState, isPlaying)),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(iconSize),
        )
    }
}

/** Skip glyph with the exact caller-supplied alpha, size and tint. */
@Composable
fun PlayerSharedSkipIcon(
    direction: PlayerSkipDirection,
    contentDescription: String? = null,
    tint: Color = LocalContentColor.current,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(playerSkipIconRes(direction)),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier,
    )
}
