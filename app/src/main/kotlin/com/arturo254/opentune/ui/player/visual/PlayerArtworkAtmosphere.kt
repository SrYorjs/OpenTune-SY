package com.arturo254.opentune.ui.player.visual

import androidx.compose.animation.AnimatedContent
import com.arturo254.opentune.core.designsystem.motion.galaxMotion
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * Apple Music-inspired immersive player atmosphere — artwork is the ONLY
 * background source and always belongs to the currently playing track.
 *
 * An unblurred square cover occupies the top width and fades into its OWN
 * heavily blurred extension. Lower controls have contrast without an opaque
 * dark sheet. This source is independently captured by the expanded player,
 * never by the NavHost. Playback, gesture handling and media session are
 * deliberately untouched.
 */
@Composable
fun PlayerArtworkAtmosphere(
    artworkUrl: String?,
    gradientColors: List<Color>,
    isPlaying: Boolean,
    disableBlur: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = gradientColors.firstOrNull() ?: MaterialTheme.colorScheme.primary
    val secondary = gradientColors.getOrNull(1) ?: accent
    val floor = lerp(accent, Color(0xFF11131A), .77f)
    val middle = lerp(secondary, Color(0xFF16171D), .66f)
    val reducedMotion = OpenTuneDesignSystem.galaxMotion.reducedMotion
    val breathing by animateFloatAsState(
        targetValue = if (isPlaying && !reducedMotion) 1.035f else 1f,
        animationSpec = spring(dampingRatio = .93f, stiffness = 90f),
        label = "immersiveArtworkBreathing",
    )

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(middle, floor)),
        ),
    ) {
        val squareHeight = maxWidth.coerceAtMost(maxHeight * .59f)
        AnimatedContent(
            targetState = artworkUrl,
            transitionSpec = {
                fadeIn(tween(if (reducedMotion) 0 else 240)) togetherWith fadeOut(tween(if (reducedMotion) 0 else 180))
            },
            label = "immersiveAlbumChange",
            modifier = Modifier.fillMaxSize(),
        ) { cover ->
            Box(Modifier.fillMaxSize()) {
                if (!cover.isNullOrBlank()) {
                    // Cover remains proportional (1:1 width), unblurred and
                    // gently dissolves into the lower atmospheric background.
                    Box(
                        Modifier.align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(squareHeight)
                            .graphicsLayer {
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                            .drawWithCache {
                                val mask = Brush.verticalGradient(
                                    0f to Color.Black,
                                    .55f to Color.Black,
                                    .80f to Color.Black.copy(alpha = .86f),
                                    1f to Color.Transparent,
                                )
                                onDrawWithContent {
                                    drawContent()
                                    drawRect(mask, blendMode = BlendMode.DstIn)
                                }
                            },
                    ) {
                        AsyncImage(
                            model = cover,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                                .graphicsLayer {
                                    scaleX = breathing
                                    scaleY = breathing
                                },
                        )
                    }
                }
            }
        }
        // A subtle floor makes progress, title and text legible without
        // producing a harsh straight line across the album artwork.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    .40f to Color.Transparent,
                    .68f to Color.Black.copy(alpha = .14f),
                    1f to Color.Black.copy(alpha = .30f),
                ),
            )
        )
    }
}
