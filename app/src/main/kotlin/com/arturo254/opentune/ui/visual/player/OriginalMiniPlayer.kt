package com.arturo254.opentune.ui.visual.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.arturo254.opentune.R
import com.arturo254.opentune.ui.player.state.PlayerState
import com.arturo254.opentune.ui.visual.component.ChromeHighlightColor
import com.arturo254.opentune.ui.visual.component.ChromeShadow
import com.arturo254.opentune.ui.visual.component.FrostedChromeGlass
import com.arturo254.opentune.ui.visual.component.GlassState
import com.arturo254.opentune.ui.visual.component.LocalOriginalGlassState
import com.arturo254.opentune.ui.visual.component.RoundedHighlight
import com.arturo254.opentune.ui.visual.component.drawChromeOutline
import com.arturo254.opentune.ui.visual.component.pressBounce
import com.arturo254.opentune.ui.visual.component.rememberChromeAccentWash
import kotlinx.coroutines.launch

private val PillShape = RoundedCornerShape(20.dp)
private val MiniButtonHighlight = RoundedHighlight(
    cornerRadius = 22.dp,
    spillX = 4.dp,
    spillY = 4.dp,
    color = ChromeHighlightColor,
)
private val MiniPlayHeight = 19.dp
private val MiniSkipWidth = 19.dp
private val MiniIconSwap = (fadeIn(tween(160)) + scaleIn(tween(220, easing = FastOutSlowInEasing), initialScale = 0.6f)) togetherWith
    (fadeOut(tween(120)) + scaleOut(tween(160), targetScale = 0.6f))

@Composable
fun OriginalMiniPlayer(
    playerState: PlayerState,
    pureBlack: Boolean,
    horizontalOffset: Float,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    val glassState = LocalOriginalGlassState.current ?: remember { GlassState() }
    val accentWash = rememberChromeAccentWash()
    val scope = rememberCoroutineScope()
    val skipCycle = remember { SkipCycleState() }
    val metadata = playerState.mediaMetadata

    Box(modifier = modifier.fillMaxWidth().height(64.dp).padding(horizontal = 24.dp)) {
        Box(
            Modifier.fillMaxSize()
                .dropShadow(PillShape, ChromeShadow)
                .clip(PillShape)
                .drawWithContent {
                    drawContent()
                    drawChromeOutline(Offset.Zero, size, cornerRadius = size.height * 20f / 64f)
                }
        ) {
            FrostedChromeGlass(
                state = glassState,
                pureBlack = pureBlack,
                accentWash = accentWash,
                modifier = Modifier.fillMaxSize(),
            )
            Row(
                modifier = Modifier.fillMaxSize()
                    .graphicsLayer { translationX = horizontalOffset }
                    .padding(start = 8.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier.weight(1f).height(64.dp).clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = onOpen,
                    ),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = playerState.artworkUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)),
                        )
                        Spacer(Modifier.width(12.dp))
                        Box(
                            modifier = Modifier.weight(1f)
                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                .drawWithCache {
                                    val fade = Brush.horizontalGradient(
                                        0f to Color.Black,
                                        0.82f to Color.Black,
                                        1f to Color.Transparent,
                                    )
                                    onDrawWithContent {
                                        drawContent()
                                        drawRect(brush = fade, blendMode = BlendMode.DstIn)
                                    }
                                },
                        ) {
                            MiniSongLines(
                                title = metadata?.title.orEmpty(),
                                artist = metadata?.artists?.joinToString(", ") { it.name }.orEmpty(),
                                album = metadata?.album?.title.orEmpty(),
                                modifier = Modifier,
                            )
                        }
                    }
                }
                Box(Modifier.size(36.dp)) {
                    MiniPlayerButton(onClick = onPlayPause) {
                        val tint = MaterialTheme.colorScheme.onSurface
                        AnimatedContent(
                            targetState = playerState.playbackState == Player.STATE_ENDED,
                            transitionSpec = { MiniIconSwap },
                            contentAlignment = Alignment.Center,
                            label = "miniPlayerIcon",
                        ) { ended ->
                            if (ended) {
                                Icon(
                                    painter = painterResource(R.drawable.replay),
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = Modifier.size(20.dp),
                                )
                            } else {
                                AnimatedPlayGlyph(
                                    playing = playerState.isPlaying,
                                    color = tint,
                                    height = MiniPlayHeight,
                                )
                            }
                        }
                    }
                }
                Box(Modifier.size(36.dp)) {
                    MiniPlayerButton(
                        onClick = {
                            scope.launch { skipCycle.step() }
                            onNext()
                        }
                    ) {
                        AnimatedSkipGlyph(
                            cycle = skipCycle,
                            backwards = false,
                            color = MaterialTheme.colorScheme.onSurface,
                            width = MiniSkipWidth,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerButton(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Box(
        modifier = Modifier.fillMaxSize()
            .pressBounce(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = MiniButtonHighlight,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}

@Composable
private fun MiniSongLines(title: String, artist: String, album: String, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
        )
        val subtitle = listOf(artist, album)
            .filter { it.isNotBlank() }
            .joinToString(" — ")
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}
