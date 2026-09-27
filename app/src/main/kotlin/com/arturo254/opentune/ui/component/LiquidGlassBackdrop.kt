package com.arturo254.opentune.ui.component

import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.skydoves.cloudy.Sky
import com.skydoves.cloudy.cloudy
import com.skydoves.cloudy.liquidGlass

val LocalSky = androidx.compose.runtime.staticCompositionLocalOf<Sky?> { null }

val LocalEnableLiquidGlass = androidx.compose.runtime.staticCompositionLocalOf { false }

/**
 * Shared Liquid Glass surface used by the navigation bar and MiniPlayer.
 *
 * Keeping the material in one place is important: both floating surfaces should
 * have the same optical treatment, and duplicating the blur pipeline makes the
 * UI heavier than necessary.
 */
@Composable
fun Modifier.liquidGlassSurface(
    shape: RoundedCornerShape,
    pureBlack: Boolean,
    blurRadius: Int = 12,
    refraction: Float = 0.085f,
    curve: Float = 0.17f,
    dispersion: Float = 0.006f,
    edge: Float = 0.12f,
): Modifier {
    val sky = LocalSky.current
    val enabled = LocalEnableLiquidGlass.current
    var surfaceSize by remember { mutableStateOf(Size.Zero) }

    val center = remember(surfaceSize) {
        Offset(
            x = surfaceSize.width / 2f,
            y = surfaceSize.height / 2f,
        )
    }

    if (!enabled || sky == null) return this

    return this
        .onSizeChanged {
            surfaceSize = Size(it.width.toFloat(), it.height.toFloat())
        }
        .clip(shape)
        .cloudy(
            sky = sky,
            radius = blurRadius,
            shape = shape,
            tint = if (pureBlack) {
                Color.White.copy(alpha = 0.055f)
            } else {
                Color.White.copy(alpha = 0.13f)
            },
        )
        .then(
            if (surfaceSize != Size.Zero) {
                Modifier.liquidGlass(
                    lensCenter = center,
                    lensSize = surfaceSize,
                    cornerRadius = 28f,
                    refraction = refraction,
                    curve = curve,
                    dispersion = dispersion,
                    saturation = 1.035f,
                    contrast = 1.018f,
                    tint = Color.Transparent,
                    edge = edge,
                )
            } else {
                Modifier
            }
        )
        .border(
            width = 1.dp,
            color = Color.White.copy(alpha = if (pureBlack) 0.15f else 0.24f),
            shape = shape,
        )
}
