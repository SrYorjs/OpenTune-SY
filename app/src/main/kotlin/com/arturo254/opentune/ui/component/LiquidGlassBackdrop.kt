package com.arturo254.opentune.ui.component

import androidx.compose.foundation.background
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
 * Shared glass material for the two floating player/navigation surfaces.
 *
 * The blur is deliberately restrained. Cloudy refreshes backdrop captures while
 * scrolling, so a smaller radius keeps the effect responsive and avoids the
 * bright flash/jank that a heavy frosted layer can produce.
 */
@Composable
fun Modifier.liquidGlassSurface(
    shape: RoundedCornerShape,
    pureBlack: Boolean,
    blurRadius: Int = 8,
    refraction: Float = 0.07f,
    curve: Float = 0.15f,
    dispersion: Float = 0.004f,
    edge: Float = 0.10f,
): Modifier {
    val sky = LocalSky.current
    val enabled = LocalEnableLiquidGlass.current
    var surfaceSize by remember { mutableStateOf(Size.Zero) }

    if (!enabled || sky == null) return this

    val center = remember(surfaceSize) {
        Offset(
            x = surfaceSize.width * 0.5f,
            y = surfaceSize.height * 0.5f,
        )
    }

    return this
        .onSizeChanged { size ->
            val nextSize = Size(size.width.toFloat(), size.height.toFloat())
            if (nextSize != surfaceSize) {
                surfaceSize = nextSize
            }
        }
        .clip(shape)
        .cloudy(
            sky = sky,
            radius = blurRadius,
            shape = shape,
            // Do not use a white scrim here. It was the source of the
            // "flash white while scrolling" behavior on bright content.
            tint = Color.Black.copy(
                alpha = if (pureBlack) 0.075f else 0.045f
            ),
        )
        .then(
            if (surfaceSize != Size.Zero) {
                Modifier.liquidGlass(
                    lensCenter = center,
                    lensSize = surfaceSize,
                    cornerRadius = 30f,
                    refraction = refraction,
                    curve = curve,
                    dispersion = dispersion,
                    saturation = 1.025f,
                    contrast = 1.012f,
                    tint = Color.Transparent,
                    edge = edge,
                )
            } else {
                Modifier
            }
        )
        .border(
            width = 1.dp,
            color = Color.White.copy(
                alpha = if (pureBlack) 0.13f else 0.18f
            ),
            shape = shape,
        )
}
