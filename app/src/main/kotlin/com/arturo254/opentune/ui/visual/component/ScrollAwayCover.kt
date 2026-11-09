package com.arturo254.opentune.ui.visual.component

import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import com.arturo254.opentune.ui.visual.BlurQuality
import com.arturo254.opentune.constants.DisableBlurKey
import com.arturo254.opentune.utils.rememberPreference

private const val CoverZoom = 0.22f
private const val CoverSoftBy = 0.55f
private const val CoverFadeFrom = 0.2f
private const val CoverFadeTo = 0.85f
private const val CoverDim = 0.22f
private const val CoverSoftRadius = 30f

internal fun coverSmoothstep(from: Float, to: Float, x: Float): Float {
    val t = ((x - from) / (to - from)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

@Composable
fun rememberCoverBlurAvailable(): Boolean {
    val (disableBlur) = rememberPreference(DisableBlurKey, defaultValue = false)
    return !disableBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}

@Composable
fun rememberElementBlurAvailable(): Boolean {
    val (disableBlur) = rememberPreference(DisableBlurKey, defaultValue = false)
    return !disableBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}

@Composable
fun ScrollAwayCover(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    
    follow: Float = 0f,
    
    softFrom: Float = 0f,
    content: @Composable BoxScope.() -> Unit,
) {
    val frameBlur = rememberElementBlurAvailable()
    val highQuality = LocalBlurQuality.current == BlurQuality.HIGH
    val frame = rememberGraphicsLayer()

    Box(
        modifier = modifier
            .graphicsLayer {
                val p = progress()
                translationY = p * size.height * follow
                val zoom = 1f + p * CoverZoom
                scaleX = zoom
                scaleY = zoom
                transformOrigin = TransformOrigin(0.5f, 0.35f)
                alpha = 1f - coverSmoothstep(CoverFadeFrom, CoverFadeTo, p)
                
                compositingStrategy = if (p > 0f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
            }
            .drawWithContent {
                drawContent()
                val p = progress()
                if (p > 0f) {
                    drawRect(Color.Black.copy(alpha = CoverDim * p))
                    
                    
                    val clear = 0.1f + 0.45f * coverSmoothstep(0f, 0.8f, p)
                    val top = size.height * (1f - clear)
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Black,
                            1f to Color.Transparent,
                            startY = top,
                            endY = size.height,
                        ),
                        topLeft = Offset(0f, top),
                        size = Size(size.width, size.height - top),
                        blendMode = BlendMode.DstIn,
                    )
                }
            },
    ) {
        
        
        
        
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    compositingStrategy = if (progress() > 0f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
                }
                .then(
                    if (frameBlur) {
                        Modifier.drawWithContent {
                            frame.record { this@drawWithContent.drawContent() }
                            drawLayer(frame)
                        }
                    } else {
                        Modifier
                    }
                ),
            content = content,
        )
        if (frameBlur && highQuality) {
            
            DownscaledBlur(
                radius = { CoverSoftRadius * coverSmoothstep(softFrom, CoverSoftBy, progress()) },
                tileMode = Shader.TileMode.CLAMP,
                mask = null,
                modifier = Modifier
                    .matchParentSize()
                    
                    .graphicsLayer { alpha = if (progress() > softFrom) 1f else 0f },
            ) {
                drawLayer(frame)
            }
        } else if (frameBlur) {
            
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val soft = coverSmoothstep(softFrom, CoverSoftBy, progress())
                        alpha = soft
                        
                        renderEffect = if (soft > 0f) BlurEffectCache.get(CoverSoftRadius, Shader.TileMode.CLAMP) else null
                        clip = true
                    }
                    .drawBehind { drawLayer(frame) },
            )
        }
    }
}
