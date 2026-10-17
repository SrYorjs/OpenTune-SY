@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.arturo254.opentune.ui.player.v2

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.R
import com.arturo254.opentune.ui.player.controls.PlayerSharedPlaybackContent
import com.arturo254.opentune.ui.player.controls.PlayerSharedSkipIcon
import com.arturo254.opentune.ui.player.controls.PlayerSkipDirection
import com.arturo254.opentune.ui.player.controls.playerFavoriteIconRes

/**
 * V2 visual skin only. Actions, current playback state, progress and lifecycle
 * stay with PlayerActions / PlayerState / PlayerHost.
 *
 * Keep the asymmetric 42dp share/favorite pills from the original V2 design.
 */
@Composable
fun V2PlayerTopActions(
    liked: Boolean,
    textButtonColor: Color,
    iconButtonColor: Color,
    onShare: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val shareShape = RoundedCornerShape(
        topStart = 50.dp, bottomStart = 50.dp,
        topEnd = 10.dp, bottomEnd = 10.dp
    )
    val favShape = RoundedCornerShape(
        topStart = 10.dp, bottomStart = 10.dp,
        topEnd = 50.dp, bottomEnd = 50.dp
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(shareShape)
                .background(textButtonColor)
                .clickable(onClick = onShare)
        ) {
            Image(
                painter = painterResource(R.drawable.share),
                contentDescription = stringResource(R.string.share),
                colorFilter = ColorFilter.tint(iconButtonColor),
                modifier = Modifier.align(Alignment.Center).size(24.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(favShape)
                .background(textButtonColor)
                .clickable(onClick = onToggleFavorite)
        ) {
            Image(
                painter = painterResource(playerFavoriteIconRes(liked)),
                contentDescription = stringResource(if (liked) R.string.player_a11y_unlike else R.string.player_a11y_like),
                colorFilter = ColorFilter.tint(iconButtonColor),
                modifier = Modifier.align(Alignment.Center).size(24.dp)
            )
        }
    }
}

/**
 * Same V2 geometry, pressed-weight springs, icon sizes and button types as the
 * legacy branch. Icons are shared by 4.8 and all commands are caller-provided.
 */
@Composable
fun V2PlayerTransport(
    playbackState: Int,
    isPlaying: Boolean,
    isLoading: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    textButtonColor: Color,
    iconButtonColor: Color,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxW = maxWidth
        val playButtonHeight = maxW / 6f
        val playButtonWidth = playButtonHeight * 1.6f
        val sideButtonHeight = playButtonHeight * 0.8f
        val sideButtonWidth = sideButtonHeight * 1.3f

        val backInteraction = remember { MutableInteractionSource() }
        val playInteraction = remember { MutableInteractionSource() }
        val nextInteraction = remember { MutableInteractionSource() }

        val isBackPressed by backInteraction.collectIsPressedAsState()
        val isPlayPressed by playInteraction.collectIsPressedAsState()
        val isNextPressed by nextInteraction.collectIsPressedAsState()

        val playWeight by animateFloatAsState(
            targetValue = if (isPlayPressed) 1.6f else 1.2f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "playWeightV2"
        )

        val sideWeight by animateFloatAsState(
            targetValue = if (isBackPressed || isNextPressed) 0.9f else 0.7f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "sideWeightV2"
        )

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            FilledTonalIconButton(
                onClick = onPrevious,
                enabled = canSkipPrevious,
                interactionSource = backInteraction,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = textButtonColor,
                    contentColor = iconButtonColor
                ),
                modifier = Modifier
                    .weight(sideWeight)
                    .size(width = sideButtonWidth, height = sideButtonHeight)
                    .clip(RoundedCornerShape(32.dp))
            ) {
                PlayerSharedSkipIcon(
                    direction = PlayerSkipDirection.PREVIOUS,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            FilledIconButton(
                onClick = onPlayPause,
                interactionSource = playInteraction,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = textButtonColor,
                    contentColor = iconButtonColor
                ),
                modifier = Modifier
                    .weight(playWeight)
                    .size(width = playButtonWidth, height = playButtonHeight)
                    .clip(RoundedCornerShape(32.dp))
            ) {
                PlayerSharedPlaybackContent(
                    playbackState = playbackState,
                    isPlaying = isPlaying,
                    isLoading = isLoading,
                    iconSize = 42.dp,
                    loadingSize = 42.dp,
                    loadingTint = iconButtonColor,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            FilledTonalIconButton(
                onClick = onNext,
                enabled = canSkipNext,
                interactionSource = nextInteraction,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = textButtonColor,
                    contentColor = iconButtonColor
                ),
                modifier = Modifier
                    .weight(sideWeight)
                    .size(width = sideButtonWidth, height = sideButtonHeight)
                    .clip(RoundedCornerShape(32.dp))
            ) {
                PlayerSharedSkipIcon(
                    direction = PlayerSkipDirection.NEXT,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
