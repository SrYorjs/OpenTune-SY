@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.arturo254.opentune.ui.player.v5

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.PlayerHorizontalPadding
import com.arturo254.opentune.ui.component.ResizableIconButton
import com.arturo254.opentune.ui.player.controls.playerFavoriteIconRes
import com.arturo254.opentune.ui.player.controls.playerPlaybackIconRes

/**
 * Stage 4.14: the legacy V5 top actions, extracted unchanged visually from
 * the shared V3/V5 branch. Playback commands are supplied by PlayerActions.
 */
@Composable
fun V5PlayerTopActions(
    liked: Boolean,
    textBackgroundColor: Color,
    onShare: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onShare()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.share),
                        contentDescription = null,
                        tint = textBackgroundColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onToggleFavorite() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            playerFavoriteIconRes(liked)
                        ),
                        contentDescription = null,
                        tint = if (liked)
                            MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                        else textBackgroundColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        
}

/**
 * Stage 4.14: original V1/V5 five-button row rendered for V5 only.
 * Pressed animation, 72/76dp primary button, 32dp icons, colors and
 * repeat/like semantics are unchanged. The shared Player owns all actions.
 */
@Composable
fun V5PlayerTransport(
    playbackState: Int,
    isPlaying: Boolean,
    isLoading: Boolean,
    repeatMode: Int,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    textButtonColor: Color,
    iconButtonColor: Color,
    textBackgroundColor: Color,
    playPauseRoundness: Dp,
    liked: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
            val repeatInteraction = remember { MutableInteractionSource() }
            val backInteraction = remember { MutableInteractionSource() }
            val playInteraction = remember { MutableInteractionSource() }
            val nextInteraction = remember { MutableInteractionSource() }
            val likeInteraction = remember { MutableInteractionSource() }

            val isRepeatPressed by repeatInteraction.collectIsPressedAsState()
            val isBackPressed by backInteraction.collectIsPressedAsState()
            val isPlayPressed by playInteraction.collectIsPressedAsState()
            val isNextPressed by nextInteraction.collectIsPressedAsState()
            val isLikePressed by likeInteraction.collectIsPressedAsState()

            val playSize by animateDpAsState(
                targetValue = if (isPlayPressed) 76.dp else 72.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "playSizeV1V5"
            )

            val playAlpha by animateFloatAsState(
                targetValue = if (isPlayPressed) 0.9f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "playAlphaV1V5"
            )

            val sideScale by animateFloatAsState(
                targetValue = if (isBackPressed || isNextPressed) 0.9f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "sideScaleV1V5"
            )

            val extraScale by animateFloatAsState(
                targetValue = if (isRepeatPressed || isLikePressed) 0.9f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "extraScaleV1V5"
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PlayerHorizontalPadding),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ResizableIconButton(
                        icon = when (repeatMode) {
                            Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL -> R.drawable.repeat
                            Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                            else -> throw IllegalStateException()
                        },
                        color = textBackgroundColor,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(4.dp)
                            .align(Alignment.Center)
                            .alpha(if (repeatMode == Player.REPEAT_MODE_OFF) 0.5f else 1f)
                            .graphicsLayer(scaleX = extraScale, scaleY = extraScale),
                        onClick = {
                            onToggleRepeat()
                        },
                        interactionSource = repeatInteraction
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    ResizableIconButton(
                        icon = R.drawable.skip_previous,
                        enabled = canSkipPrevious,
                        color = textBackgroundColor,
                        modifier =
                            Modifier
                                .size(32.dp)
                                .align(Alignment.Center)
                                .graphicsLayer(scaleX = sideScale, scaleY = sideScale),
                        onClick = onPrevious,
                        interactionSource = backInteraction
                    )
                }

                Spacer(Modifier.width(8.dp))

                Box(
                    modifier =
                        Modifier
                            .size(playSize)
                            .clip(RoundedCornerShape(playPauseRoundness))
                            .alpha(playAlpha)
                            .background(textButtonColor)
                            .clickable(
                                interactionSource = playInteraction,
                                indication = null
                            ) {
                                onPlayPause()
                            },
                ) {
                    if (isLoading) {
                        CircularWavyProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(36.dp),
                            color = iconButtonColor,
                        )
                    } else {
                        Image(
                            painter =
                                painterResource(
                                    playerPlaybackIconRes(playbackState, isPlaying),
                                ),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(iconButtonColor),
                            modifier =
                                Modifier
                                    .align(Alignment.Center)
                                    .size(36.dp),
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Box(modifier = Modifier.weight(1f)) {
                    ResizableIconButton(
                        icon = R.drawable.skip_next,
                        enabled = canSkipNext,
                        color = textBackgroundColor,
                        modifier =
                            Modifier
                                .size(32.dp)
                                .align(Alignment.Center)
                                .graphicsLayer(scaleX = sideScale, scaleY = sideScale),
                        onClick = onNext,
                        interactionSource = nextInteraction
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    ResizableIconButton(
                        icon = playerFavoriteIconRes(liked),
                        color = if (liked) MaterialTheme.colorScheme.error else textBackgroundColor,
                        modifier =
                            Modifier
                                .size(32.dp)
                                .padding(4.dp)
                                .align(Alignment.Center)
                                .graphicsLayer(scaleX = extraScale, scaleY = extraScale),
                        onClick = onToggleFavorite,
                        interactionSource = likeInteraction
                    )
                }
            }
        
}
