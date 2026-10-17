package com.arturo254.opentune.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem

/**
 * GalaxOT semantic layer above Material 3. Never bypass the selected
 * ColorScheme: custom palettes, wallpaper colors and pure-black mode remain
 * respected, while the default ColorScheme is GalaxOT galaxy-blue.
 */
@Immutable
data class GalaxColorTokens(
    val cosmicBackground: Color,
    val nebulaSurface: Color,
    val stellarSurface: Color,
    val haloAccent: Color,
    val onHaloAccent: Color,
    val orbitOutline: Color,
    val glassTint: Color,
    val glassRim: Color,
    val galaxyGlow: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textOnArtwork: Color,
    val artworkScrim: Color,
)

@Immutable
data class GalaxTypeTokens(
    val hero: TextStyle,
    val headline: TextStyle,
    val section: TextStyle,
    val track: TextStyle,
    val supporting: TextStyle,
    val eyebrow: TextStyle,
)

@Immutable
data class GalaxGeometryTokens(
    val heroCard: RoundedCornerShape,
    val orbitalCard: RoundedCornerShape,
    val interactiveTile: RoundedCornerShape,
    val floatingIsland: RoundedCornerShape,
    val sheet: RoundedCornerShape,
    val artwork: RoundedCornerShape,
)

@Immutable
data class GalaxIconTokens(
    val compact: Dp = 18.dp,
    val standard: Dp = 24.dp,
    val prominent: Dp = 28.dp,
    val hero: Dp = 32.dp,
    val minimumTouchTarget: Dp = 48.dp,
)

@Immutable
data class GalaxDesignTokens(
    val colors: GalaxColorTokens,
    val typography: GalaxTypeTokens,
    val geometry: GalaxGeometryTokens,
    val iconography: GalaxIconTokens = GalaxIconTokens(),
)

internal val LocalGalaxDesignTokens =
    staticCompositionLocalOf<GalaxDesignTokens> {
        error("GalaxOT tokens are not installed in this composition")
    }

val OpenTuneDesignSystem.galax: GalaxDesignTokens
    @Composable
    @ReadOnlyComposable
    get() = LocalGalaxDesignTokens.current

internal fun createGalaxDesignTokens(
    scheme: ColorScheme,
    typography: Typography,
    artworkRadiusDp: Float,
): GalaxDesignTokens {
    val isDark = scheme.background.luminance() < 0.45f
    return GalaxDesignTokens(
        colors = GalaxColorTokens(
            cosmicBackground = scheme.background,
            nebulaSurface = scheme.surfaceContainerLow,
            stellarSurface = scheme.surfaceContainerHigh,
            haloAccent = scheme.primary,
            onHaloAccent = scheme.onPrimary,
            orbitOutline = scheme.outlineVariant,
            glassTint = scheme.primary.copy(alpha = if (isDark) 0.10f else 0.075f),
            glassRim = scheme.onSurface.copy(alpha = if (isDark) 0.22f else 0.13f),
            galaxyGlow = scheme.primary.copy(alpha = if (isDark) 0.25f else 0.16f),
            textPrimary = scheme.onSurface,
            textSecondary = scheme.onSurfaceVariant,
            textOnArtwork = Color.White,
            artworkScrim = Color.Black.copy(alpha = 0.40f),
        ),
        typography = GalaxTypeTokens(
            hero = typography.displayMedium.copy(fontWeight = FontWeight.Bold),
            headline = typography.headlineLarge,
            section = typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            track = typography.titleMedium,
            supporting = typography.bodyMedium,
            eyebrow = typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        ),
        geometry = GalaxGeometryTokens(
            heroCard = RoundedCornerShape(32.dp),
            orbitalCard = RoundedCornerShape(26.dp),
            interactiveTile = RoundedCornerShape(20.dp),
            floatingIsland = RoundedCornerShape(32.dp),
            sheet = RoundedCornerShape(30.dp),
            artwork = artworkShapeFor(artworkRadiusDp),
        ),
    )
}
