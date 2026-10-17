package com.arturo254.opentune.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * GalaxOT 4.5.2 — brand palette. All values are opaque semantic surfaces;
 * transparency belongs to the separate Glass Engine (4.5.4–4.5.7).
 *
 * The bright halo acts as the primary CTA on dark surfaces. The deeper
 * galaxy blue is primary in light mode, keeping readable on-primary text.
 */
object GalaxPalette {
    val Void = Color(0xFF040912)
    val DeepSpace = Color(0xFF071021)
    val Nebula = Color(0xFF102B59)
    val Galaxy = Color(0xFF286AD0)
    val Halo = Color(0xFF65BAFF)
    val Ice = Color(0xFFDFF5FF)
    val Mist = Color(0xFFAFC4E0)
    val Cyan = Color(0xFF9DD7FF)
    val LightCanvas = Color(0xFFF4F8FF)
    val Ink = Color(0xFF0B1B34)
}

/** No global mutable state. Pure deterministic default schemes for testing. */
fun galaxColorScheme(dark: Boolean): ColorScheme =
    if (dark) {
        darkColorScheme(
            primary = GalaxPalette.Halo,
            onPrimary = Color(0xFF052139),
            primaryContainer = GalaxPalette.Nebula,
            onPrimaryContainer = GalaxPalette.Ice,
            secondary = Color(0xFFB6C8EA),
            onSecondary = GalaxPalette.Ink,
            secondaryContainer = Color(0xFF223C64),
            onSecondaryContainer = Color(0xFFE0ECFF),
            tertiary = Color(0xFF92E1D5),
            onTertiary = Color(0xFF062E2C),
            tertiaryContainer = Color(0xFF174F4C),
            onTertiaryContainer = Color(0xFFCEFFF4),
            background = GalaxPalette.DeepSpace,
            onBackground = Color(0xFFEAF2FF),
            surface = Color(0xFF0B182D),
            onSurface = Color(0xFFEAF2FF),
            surfaceVariant = Color(0xFF253B58),
            onSurfaceVariant = Color(0xFFBACCE5),
            outline = Color(0xFF839BBE),
            outlineVariant = Color(0xFF354967),
            inverseSurface = Color(0xFFE6F0FF),
            inverseOnSurface = Color(0xFF10233E),
            inversePrimary = Color(0xFF175BAC),
            surfaceTint = GalaxPalette.Halo,
            scrim = Color.Black,
        ).copy(
            surfaceDim = GalaxPalette.Void,
            surfaceBright = Color(0xFF293C58),
            surfaceContainerLowest = GalaxPalette.Void,
            surfaceContainerLow = Color(0xFF101F37),
            surfaceContainer = Color(0xFF152640),
            surfaceContainerHigh = Color(0xFF1B2F4E),
            surfaceContainerHighest = Color(0xFF263B58),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF155AAA),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFD8E8FF),
            onPrimaryContainer = Color(0xFF082446),
            secondary = Color(0xFF3E5E8B),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFDFEAFF),
            onSecondaryContainer = Color(0xFF102B4B),
            tertiary = Color(0xFF176C6A),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFB3F2EB),
            onTertiaryContainer = Color(0xFF003A38),
            background = GalaxPalette.LightCanvas,
            onBackground = GalaxPalette.Ink,
            surface = Color(0xFFFCFDFF),
            onSurface = GalaxPalette.Ink,
            surfaceVariant = Color(0xFFDDE7F5),
            onSurfaceVariant = Color(0xFF42536C),
            outline = Color(0xFF647B9C),
            outlineVariant = Color(0xFFB9CAE2),
            inverseSurface = Color(0xFF142C4A),
            inverseOnSurface = Color(0xFFEBF3FF),
            inversePrimary = GalaxPalette.Halo,
            surfaceTint = Color(0xFF155AAA),
            scrim = Color.Black,
        ).copy(
            surfaceDim = Color(0xFFD5E2F4),
            surfaceBright = Color.White,
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color(0xFFF0F6FF),
            surfaceContainer = Color(0xFFEAF2FF),
            surfaceContainerHigh = Color(0xFFE2EDFD),
            surfaceContainerHighest = Color(0xFFD8E6F8),
        )
    }

/** WCAG relative luminance and contrast, independent of display density. */
fun galaxRelativeLuminance(color: Color): Double {
    fun channel(value: Float): Double {
        val c = value.toDouble().coerceIn(0.0, 1.0)
        return if (c <= 0.04045) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
    }
    return 0.2126 * channel(color.red) +
        0.7152 * channel(color.green) +
        0.0722 * channel(color.blue)
}

fun galaxContrastRatio(foreground: Color, background: Color): Double {
    val a = galaxRelativeLuminance(foreground)
    val b = galaxRelativeLuminance(background)
    return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
}
