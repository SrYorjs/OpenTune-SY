package com.arturo254.opentune.ui.player.v8

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.media3.ui.AspectRatioFrameLayout
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import com.arturo254.opentune.canvas.models.CanvasArtwork
import com.arturo254.opentune.ui.player.CanvasArtworkPlayer
import com.arturo254.opentune.ui.utils.highRes
import com.skydoves.cloudy.cloudy

@Composable
fun MainV8PlayerBackdrop(
    thumbnailUrl: String?,
    canvasArtwork: CanvasArtwork? = null,
    isPlaying: Boolean = false,
    disableBlur: Boolean,
    label: String,
    modifier: Modifier = Modifier,
) {
    val cloudyRadius = 100
    val blurMaskStart = 0.42f
    val blurMaskMid = 0.55f
    val blurMaskSolid = 0.72f
    val baseArtworkScale = if (disableBlur) 1.02f else 1.05f
    val baseArtworkAlpha = if (disableBlur) 0.65f else 0.75f

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AnimatedContent(
            targetState = thumbnailUrl,
            transitionSpec = {
                fadeIn(tween(800)) togetherWith fadeOut(tween(800))
            },
            label = label,
        ) { artworkUrl ->
            if (artworkUrl != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (canvasArtwork != null && !canvasArtwork.preferredAnimationUrl.isNullOrBlank()) {
                        CanvasArtworkPlayer(
                            primaryUrl = canvasArtwork.animated,
                            fallbackUrl = canvasArtwork.videoUrl,
                            isPlaying = isPlaying,
                            modifier = Modifier.fillMaxSize(),
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0f to Color.Transparent,
                                            0.15f to Color.Black.copy(alpha = 0.05f),
                                            0.45f to Color.Black.copy(alpha = 0.25f),
                                            0.70f to Color.Black.copy(alpha = 0.50f),
                                            1f to Color.Black.copy(alpha = 0.85f),
                                        )
                                    )
                                )
                        )
                    } else {
                        AsyncImage(
                            model = artworkUrl.highRes(),
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

                        val painter = rememberAsyncImagePainter(
                            model = artworkUrl.highRes()
                        )

                        val imageSize = painter.intrinsicSize

                        val contentScale = remember(imageSize) {
                            if (imageSize.width <= 0 || imageSize.height <= 0) {
                                ContentScale.Crop
                            } else {
                                val aspectRatio = imageSize.width / imageSize.height

                                when {
                                    aspectRatio > 1.35f -> ContentScale.FillHeight
                                    aspectRatio < 0.75f -> ContentScale.FillWidth
                                    else -> ContentScale.Crop
                                }
                            }
                        }

                        if (!disableBlur) {
                            Image(
                                painter = painter,
                                contentDescription = null,
                                contentScale = contentScale,
                                alignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .cloudy(radius = cloudyRadius)
                                    .drawWithCache {
                                        val blurMask = Brush.verticalGradient(
                                            colorStops = arrayOf(
                                                0f to Color.Transparent,
                                                blurMaskStart to Color.Transparent,
                                                blurMaskMid to Color.Black.copy(alpha = 0.5f),
                                                blurMaskSolid to Color.Black,
                                                1f to Color.Black
                                            )
                                        )

                                        onDrawWithContent {
                                            drawContent()

                                            drawRect(
                                                brush = blurMask,
                                                blendMode = BlendMode.DstIn
                                            )
                                        }
                                    }
                            )
                        }
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
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.20f to Color.Transparent,
                            0.50f to Color.Black.copy(alpha = 0.15f),
                            0.75f to Color.Black.copy(alpha = 0.35f),
                            1f to Color.Black.copy(alpha = 0.70f),
                        )
                    )
                )
        )
    }
}
