package com.arturo254.opentune.core.designsystem.glass

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop

/**
 * Kyant 2.0.1 native optics with a SAFE, render-node-independent draw source.
 *
 * The former implementation captured animated NavHost content using
 * rememberLayerBackdrop / Modifier.layerBackdrop. On Samsung Android 17 that
 * caused a fatal RenderThread recursive RenderNode traversal. This source is
 * pure Canvas draw commands: no recorded GraphicsLayer, no LayoutCoordinates,
 * no recursive capture, no Android view snapshots or additional blur pipeline.
 *
 * It is an ambient GalaxOT backdrop, NOT a screenshot of scrolling content.
 * A future real-content provider requires a separate device-tested design.
 */
val LocalGalaxSafeCanvasBackdrop = staticCompositionLocalOf { false }

@Composable
fun rememberGalaxCanvasBackdrop(pureBlack: Boolean): Backdrop {
    val colors = MaterialTheme.colorScheme
    val base = if (pureBlack) Color.Black else colors.surface
    val nebula = colors.primary.copy(alpha = if (pureBlack) 0.44f else 0.23f)
    val edge = colors.tertiary.copy(alpha = if (pureBlack) 0.31f else 0.14f)

    // Remember the drawing closure so Kyant can retain its CanvasBackdrop.
    val drawAmbient: DrawScope.() -> Unit = remember(base, nebula, edge) {
        {
            drawRect(base)
            val radius = size.maxDimension.coerceAtLeast(1f)
            val first = Offset(size.width * 0.78f, size.height * 0.16f)
            val second = Offset(size.width * 0.18f, size.height * 0.88f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(nebula, Color.Transparent),
                    center = first,
                    radius = radius * 0.68f,
                ),
                radius = radius * 0.68f,
                center = first,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(edge, Color.Transparent),
                    center = second,
                    radius = radius * 0.55f,
                ),
                radius = radius * 0.55f,
                center = second,
            )
        }
    }
    return rememberCanvasBackdrop(onDraw = drawAmbient)
}
