package com.arturo254.opentune.core.designsystem.liquid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * Lightweight glass treatment for controls that sit INSIDE the NavHost source.
 *
 * The real Kyant backdrop is available to sibling foreground overlays (nav
 * and mini player). Re-sampling that same NavHost from its descendants would
 * create a recursive RenderNode tree, so in-route buttons/cards/chips use a
 * visually coordinated translucent specular material without another capture.
 * All shared UI reads the SAME global variant and appearance settings.
 */
@Composable
fun Modifier.galaxLiquidChrome(
    shape: Shape = RoundedCornerShape(24.dp),
    emphasis: Float = 1f,
): Modifier {
    val variant = LocalGalaxLiquidVariant.current
    if (variant == GalaxLiquidVariant.DISABLED) return this
    val finish = galvanicFinish(variant)
    val optics = LocalGalaxLiquidTuning.current ?: galvanicRecipe(variant)
    val palette = MaterialTheme.colorScheme
    val dark = palette.background.luminance() < .45f
    val factor = emphasis.coerceIn(.1f, 1.4f)
    val accent = when (finish.colorStyle) {
        1 -> Color(0xFF26C8F7)
        2 -> Color(0xFFEA99F9)
        3 -> Color(0xFFAE8BFF)
        else -> palette.primary
    }
    val surface = if (dark) palette.surfaceContainerHigh else palette.surfaceContainerLow
    val sheen = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = if (dark) .17f * factor else .34f * factor),
            accent.copy(alpha = (.07f + optics.tintAlpha * .33f) * factor),
            surface.copy(alpha = if (dark) .79f else .83f),
        ),
    )
    val rim = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = finish.rimAlpha * factor),
            accent.copy(alpha = finish.rimAlpha * .46f * factor),
            palette.outlineVariant.copy(alpha = .34f * factor),
        ),
    )
    return this
        .clip(shape)
        .background(sheen)
        .border((.65f + finish.rimAlpha * .5f).dp, rim, shape)
}
