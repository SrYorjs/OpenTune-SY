package com.arturo254.opentune.ui.player.visual

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.skydoves.cloudy.cloudy

private const val V7BackdropBlurHeightFraction = 0.54f

/** Existing V7 layered gradient artwork, moved unchanged to the visual engine. */
@Composable
internal fun V7PlayerBackdrop(
    thumbnailUrl: String?,
    disableBlur: Boolean,
    label: String,
    modifier: Modifier = Modifier,
) {
    val cloudyRadius = 100
    val blurMaskStart = (1f - V7BackdropBlurHeightFraction).coerceIn(0f, 0.85f)
    val blurMaskMid = (blurMaskStart + 0.12f).coerceIn(blurMaskStart, 0.95f)
    val blurMaskSolid = (blurMaskStart + 0.22f).coerceIn(blurMaskMid, 1f)
    val baseArtworkScale = if (disableBlur) 1.03f else 1.06f
    val baseArtworkAlpha = if (disableBlur) 0.72f else 0.82f
    val surfaceTint = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        AnimatedContent(
            targetState = thumbnailUrl,
            transitionSpec = {
                fadeIn(tween(900)) togetherWith fadeOut(tween(900))
            },
            label = label,
        ) { artworkUrl ->
            if (artworkUrl != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = baseArtworkScale
                                scaleY = baseArtworkScale
                                alpha = baseArtworkAlpha
                            }
                    )

                    if (!disableBlur) {
                        AsyncImage(
                            model = artworkUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .cloudy(radius = cloudyRadius)
                                .graphicsLayer {
                                    compositingStrategy = CompositingStrategy.Offscreen
                                }
                                .drawWithCache {
                                    val blurMask = Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0f to Color.Transparent,
                                            blurMaskStart to Color.Transparent,
                                            blurMaskMid to Color.Black.copy(alpha = 0.6f),
                                            blurMaskSolid to Color.Black,
                                            1f to Color.Black,
                                        )
                                    )

                                    onDrawWithContent {
                                        drawContent()
                                        drawRect(
                                            brush = blurMask,
                                            blendMode = BlendMode.DstIn,
                                        )
                                    }
                                }
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.08f))
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.18f),
                            0.34f to Color.Transparent,
                            0.64f to Color.Black.copy(alpha = 0.22f),
                            1f to Color.Black.copy(alpha = 0.82f),
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.56f to Color.Transparent,
                            0.8f to surfaceTint.copy(alpha = 0.16f),
                            1f to surfaceTint.copy(alpha = 0.3f),
                        )
                    )
                )
        )
    }
}

