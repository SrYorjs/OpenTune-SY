package com.arturo254.opentune.ui.mochy.component

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints

internal fun glassDownscale(effectiveRadius: Float, low: Boolean = false): Int = when {
    effectiveRadius >= 64f -> 4

    low && effectiveRadius >= 32f -> 4
    effectiveRadius >= 16f -> 2
    low && effectiveRadius >= 8f -> 2
    else -> 1
}


private class FullSize {
    var width = 0
    var height = 0
}


private fun Modifier.downscaledLayout(downscale: () -> Int, full: FullSize): Modifier = layout { measurable, constraints ->
    val k = downscale().coerceAtLeast(1)
    val fullWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else 0
    val fullHeight = if (constraints.hasBoundedHeight) constraints.maxHeight else 0
    full.width = fullWidth
    full.height = fullHeight
    val placeable = measurable.measure(
        Constraints.fixed((fullWidth + k - 1) / k, (fullHeight + k - 1) / k)
    )
    layout(fullWidth, fullHeight) { placeable.place(0, 0) }
}


private class MaskedEffectCache {
    var radius = -1f
    var width = -1f
    var height = -1f
    var tileOrdinal = -1
    var mask: Brush? = null
    var effect: RenderEffect? = null
}


@RequiresApi(Build.VERSION_CODES.S)
private fun maskedBlurEffect(radius: Float, tileMode: android.graphics.Shader.TileMode, mask: android.graphics.Shader): RenderEffect {
    val content = if (radius > 0.5f) {
        android.graphics.RenderEffect.createBlurEffect(radius, radius, tileMode)
    } else {
        android.graphics.RenderEffect.createOffsetEffect(0f, 0f)
    }
    return android.graphics.RenderEffect.createBlendModeEffect(
        content,
        android.graphics.RenderEffect.createShaderEffect(mask),
        android.graphics.BlendMode.DST_IN,
    ).asComposeRenderEffect()
}


@Composable
internal fun DownscaledBlur(
    radius: () -> Float,
    tileMode: android.graphics.Shader.TileMode?,
    mask: Brush?,
    modifier: Modifier = Modifier,
    
    offscreen: (() -> Boolean)? = null,

    growDown: Boolean? = null,

    growAcross: Boolean = false,
    growRamp: Float = 1f,
    draw: DrawScope.(fullSize: Size) -> Unit,
) {

    val currentRadius by rememberUpdatedState(radius)
    val currentOffscreen by rememberUpdatedState(offscreen)

    val downscale by remember { derivedStateOf { glassDownscale(currentRadius()) } }
    val maskCache = remember { MaskedEffectCache() }
    val full = remember { FullSize() }

    Box(
        modifier = modifier
            .clipToBounds()
            .then(

                if (mask != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    Modifier
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(brush = mask, blendMode = BlendMode.DstIn)
                        }
                } else Modifier
            )
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .downscaledLayout({ downscale }, full)
                .graphicsLayer {
                    val k = downscale.toFloat()
                    scaleX = k
                    scaleY = k
                    transformOrigin = TransformOrigin(0f, 0f)
                    val r = currentRadius() / k
                    renderEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val mode = tileMode ?: android.graphics.Shader.TileMode.DECAL
                        if (mask is ShaderBrush) {
                            val quantized = if (r > 0.5f) BlurEffectCache.quantize(r) else 0f
                            val cache = maskCache
                            if (cache.effect == null || cache.radius != quantized || cache.width != size.width ||
                                cache.height != size.height || cache.tileOrdinal != mode.ordinal || cache.mask != mask
                            ) {
                                cache.effect = maskedBlurEffect(quantized, mode, mask.createShader(size))
                                cache.radius = quantized
                                cache.width = size.width
                                cache.height = size.height
                                cache.tileOrdinal = mode.ordinal
                                cache.mask = mask
                            }
                            cache.effect
                        } else {
                            BlurEffectCache.get(r, mode)
                        }
                    } else {
                        null
                    }
                    compositingStrategy = if (currentOffscreen?.invoke() == true) {
                        CompositingStrategy.Offscreen
                    } else {
                        CompositingStrategy.Auto
                    }
                    clip = true
                }
        ) {
            val k = downscale.toFloat()
            val fullSize = Size(full.width.toFloat(), full.height.toFloat())
            scale(1f / k, 1f / k, pivot = Offset.Zero) { draw(fullSize) }
        }
    }
}


