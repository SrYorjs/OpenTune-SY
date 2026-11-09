package com.arturo254.opentune.ui.visual.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

data class ArtworkPalette(
    val accent: Color,
    val onAccent: Color,
    val container: Color,
    val onContainer: Color,
    val surface: Color,
) {
    companion object {
        val Neutral = ArtworkPalette(
            accent = Color.White,
            onAccent = Color.Black,
            container = Color.White.copy(alpha = 0.15f),
            onContainer = Color.White,
            surface = Color(0xFF121212),
        )

        

        fun from(seed: Color): ArtworkPalette {
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(seed.toArgb(), hsl)

            
            
            val saturation = hsl[1].coerceIn(0.25f, 0.85f)

            val accent = hslColor(hsl[0], saturation, 0.72f)
            val container = hslColor(hsl[0], saturation * 0.55f, 0.26f)
            val surface = hslColor(hsl[0], saturation * 0.35f, 0.11f)

            return ArtworkPalette(
                accent = accent,
                onAccent = contrastingOn(accent),
                container = container,
                onContainer = contrastingOn(container),
                surface = surface,
            )
        }

        private fun hslColor(hue: Float, saturation: Float, lightness: Float): Color =
            Color(ColorUtils.HSLToColor(floatArrayOf(hue, saturation.coerceIn(0f, 1f), lightness.coerceIn(0f, 1f))))

        

        private fun contrastingOn(background: Color): Color {
            val bg = background.toArgb()
            val onWhite = ColorUtils.calculateContrast(android.graphics.Color.WHITE, bg)
            val onBlack = ColorUtils.calculateContrast(android.graphics.Color.BLACK, bg)
            return if (onWhite >= onBlack) Color.White else Color.Black
        }
    }
}
