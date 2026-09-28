package com.arturo254.opentune.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.skydoves.cloudy.Sky

/**
 * The app now uses Kyant0/AndroidLiquidGlass' Backdrop engine for interactive
 * glass surfaces. Sky remains available for legacy screens that still use it.
 */
val LocalSky = staticCompositionLocalOf<Sky?> { null }
val LocalEnableLiquidGlass = staticCompositionLocalOf { false }
val LocalLiquidBackdrop = staticCompositionLocalOf<Backdrop?> { null }

@Composable
fun Modifier.liquidGlassSurface(
    shape: RoundedCornerShape,
    pureBlack: Boolean,
    blurRadius: Int = 6,
    refraction: Float = 0.06f,
    curve: Float = 0.14f,
    dispersion: Float = 0.003f,
    edge: Float = 0.20f,
): Modifier {
    val enabled = LocalEnableLiquidGlass.current
    val backdrop = LocalLiquidBackdrop.current

    if (!enabled) return this

    val darkSurface =
        pureBlack || MaterialTheme.colorScheme.background.luminance() < 0.45f
    val fallbackSurface =
        if (darkSurface) {
            Color.White.copy(alpha = 0.055f)
        } else {
            Color.White.copy(alpha = 0.30f)
        }
    val borderColor =
        if (darkSurface) {
            Color.White.copy(alpha = 0.18f)
        } else {
            Color.White.copy(alpha = 0.58f)
        }

    if (backdrop == null) {
        return this
            .clip(shape)
            .background(fallbackSurface)
            .border(0.8.dp, borderColor, shape)
    }

    val blurDp = blurRadius.coerceIn(2, 12).dp
    val lensRadius = (11f + refraction.coerceIn(0f, 0.14f) * 110f).dp
    val lensDepth = (17f + curve.coerceIn(0f, 0.30f) * 58f).dp
    val highlightAlpha = (0.34f + edge.coerceIn(0f, 1f) * 0.34f).coerceIn(0.34f, 0.68f)

    return this
        .drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(blurDp.toPx())
                lens(
                    lensRadius.toPx(),
                    lensDepth.toPx(),
                    chromaticAberration = dispersion >= 0.0015f,
                )
            },
            highlight = {
                Highlight.Ambient.copy(alpha = highlightAlpha)
            },
            shadow = {
                Shadow(
                    radius = 8.dp,
                    color = Color.Black.copy(alpha = if (darkSurface) 0.18f else 0.10f),
                )
            },
            innerShadow = {
                InnerShadow(
                    radius = 4.dp,
                    alpha = if (darkSurface) 0.22f else 0.14f,
                )
            },
            onDrawSurface = {
                drawRect(fallbackSurface)
            },
        )
        .clip(shape)
        .border(
            width = 0.75.dp,
            color = borderColor,
            shape = shape,
        )
}
