package com.arturo254.opentune.ui.player.v7

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.PlayerHorizontalPadding
import com.arturo254.opentune.ui.player.controls.PlayerSharedPlaybackContent
import com.arturo254.opentune.ui.player.controls.PlayerSharedSkipIcon
import com.arturo254.opentune.ui.player.controls.PlayerSkipDirection
import com.arturo254.opentune.ui.player.controls.playerFavoriteIconRes

/** V7 top actions. Preserve circular 42dp hit surfaces; callbacks are shared. */
@Composable
fun V7PlayerTopActions(
    liked: Boolean,
    textBackgroundColor: Color,
    onToggleFavorite: () -> Unit,
    onOpenMenu: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    if (liked) textBackgroundColor.copy(alpha = 0.2f)
                    else Color.Transparent
                )
                .clickable(onClick = onToggleFavorite),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(playerFavoriteIconRes(liked)),
                contentDescription = null,
                tint = textBackgroundColor.copy(alpha = if (liked) 1f else 0.7f),
                modifier = Modifier.size(22.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .clickable(onClick = onOpenMenu),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.more_horiz),
                contentDescription = null,
                tint = textBackgroundColor.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * V7 is deliberately NOT spring-animated (unlike V5).
 * Exact legacy 64/72/64dp transparent circular controls and 44/52dp
 * glyph sizes; skip tint fades to 40% when disabled.
 */
@Composable
fun V7PlayerTransport(
    playbackState: Int,
    isPlaying: Boolean,
    isLoading: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    textBackgroundColor: Color,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PlayerHorizontalPadding)
    ) {
        Surface(
            onClick = onPrevious,
            enabled = canSkipPrevious,
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier.size(64.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PlayerSharedSkipIcon(
                    direction = PlayerSkipDirection.PREVIOUS,
                    contentDescription = null,
                    tint = textBackgroundColor.copy(alpha = if (canSkipPrevious) 1f else 0.4f),
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Surface(
            onClick = onPlayPause,
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier.size(72.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PlayerSharedPlaybackContent(
                    playbackState = playbackState,
                    isPlaying = isPlaying,
                    isLoading = isLoading,
                    iconSize = 52.dp,
                    loadingSize = 44.dp,
                    loadingTint = textBackgroundColor,
                    iconTint = textBackgroundColor,
                )
            }
        }

        Surface(
            onClick = onNext,
            enabled = canSkipNext,
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier.size(64.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PlayerSharedSkipIcon(
                    direction = PlayerSkipDirection.NEXT,
                    contentDescription = null,
                    tint = textBackgroundColor.copy(alpha = if (canSkipNext) 1f else 0.4f),
                    modifier = Modifier.size(44.dp)
                )
            }
        }
    }
}
