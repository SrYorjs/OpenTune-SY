package com.arturo254.opentune.ui.component

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.skydoves.cloudy.LiquidGlassDefaults
import com.skydoves.cloudy.Sky
import com.skydoves.cloudy.liquidGlass

val LocalSky = androidx.compose.runtime.staticCompositionLocalOf<Sky?> { null }

val LocalEnableLiquidGlass = androidx.compose.runtime.staticCompositionLocalOf { false }

/**
 * Fast shared glass material for the floating navigation and mini-player.
 *
 * The old version stacked Cloudy's backdrop blur and the liquid-glass shader.
 * That forced a live backdrop capture while scrolling/dragging and was the main
 * source of jank and the bright flash. This version keeps a single lightweight
 * glass pass on Android 13+ and falls back to a translucent capsule below it.
 */
@Composable
fun Modifier.liquidGlassSurface(
    shape: RoundedCornerShape,
    pureBlack: Boolean,
    blurRadius: Int = 4,
    refraction: Float = 0.045f,
    curve: Float = 0.10f,
    dispersion: Float = 0.0015f,
    edge: Float = 0f,
): Modifier {
    val enabled = LocalEnableLiquidGlass.current
    var surfaceSize by remember { mutableStateOf(Size.Zero) }

    if (!enabled) return this

    val center = remember(surfaceSize) {
        Offset(
            x = surfaceSize.width * 0.5f,
            y = surfaceSize.height * 0.5f,
        )
    }

    // blurRadius is intentionally used only to tune the cheap fallback tint.
    // We no longer run a live backdrop blur for these compact moving surfaces.
    val tintStrength = (blurRadius.coerceIn(0, 12) / 12f)
    val baseTint = Color.Black.copy(
        alpha = if (pureBlack) {
            0.09f + (0.025f * tintStrength)
        } else {
            0.045f + (0.02f * tintStrength)
        }
    )
    val useShader = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    return this
        .onSizeChanged { size ->
            val nextSize = Size(size.width.toFloat(), size.height.toFloat())
            if (nextSize != surfaceSize) {
                surfaceSize = nextSize
            }
        }
        .clip(shape)
        .background(baseTint)
        .then(
            if (useShader && surfaceSize != Size.Zero) {
                Modifier.liquidGlass(
                    lensCenter = center,
                    lensSize = surfaceSize,
                    cornerRadius = 30f,
                    refraction = refraction,
                    curve = curve,
                    dispersion = dispersion,
                    saturation = 1.012f,
                    contrast = 1.006f,
                    tint = Color.Transparent,
                    // Keep edge lighting almost imperceptible. NoGlow removes the
                    // persistent white hotspot that looked like a bright dot.
                    edge = edge.coerceIn(0f, 0.012f),
                    glow = LiquidGlassDefaults.NoGlow,
                )
            } else {
                Modifier
            }
        )
        .border(
            width = 1.dp,
            color = Color.White.copy(
                alpha = if (pureBlack) 0.10f else 0.12f
            ),
            shape = shape,
        )
}
