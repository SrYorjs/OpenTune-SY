package com.arturo254.opentune.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem

/**
 * Semantic colors used by OpenTune-specific UI.
 *
 * These roles are derived from Material 3 rather than replacing it. Feature UI
 * should keep using MaterialTheme.colorScheme for standard Material semantics
 * and use these roles only when the app needs a music/glass-specific meaning.
 */
@Immutable
data class OpenTuneColorRoles(
    val surfaceGlass: Color,
    val surfaceGlassStrong: Color,
    val surfaceInteractive: Color,
    val surfaceInteractiveStrong: Color,
    val surfacePlayer: Color,
    val outlineGlass: Color,
    val outlineGlassStrong: Color,
    val shadowGlass: Color,
    val overlayScrim: Color,
    val overlayArtwork: Color,
    val textMuted: Color,
    val textSubtle: Color,
    val badgeAccent: Color,
    val badgeAccentStrong: Color,
    val accentPositive: Color,
    val onAccentPositive: Color,
    val accentWarning: Color,
    val onAccentWarning: Color,
)

internal val LocalOpenTuneColorRoles =
    staticCompositionLocalOf<OpenTuneColorRoles> {
        error("OpenTune semantic colors are not installed in this composition")
    }

val OpenTuneDesignSystem.colors: OpenTuneColorRoles
    @Composable
    @ReadOnlyComposable
    get() = LocalOpenTuneColorRoles.current

internal fun createOpenTuneColorRoles(
    colorScheme: ColorScheme,
): OpenTuneColorRoles {
    val darkSurface = colorScheme.background.luminance() < 0.45f

    return OpenTuneColorRoles(
        surfaceGlass =
            if (darkSurface) {
                Color.White.copy(alpha = 0.070f)
            } else {
                Color.White.copy(alpha = 0.24f)
            },
        surfaceGlassStrong =
            if (darkSurface) {
                Color.White.copy(alpha = 0.11f)
            } else {
                Color.White.copy(alpha = 0.34f)
            },
        surfaceInteractive = colorScheme.surfaceContainerLow,
        surfaceInteractiveStrong = colorScheme.surfaceContainerHigh,
        surfacePlayer = colorScheme.surfaceContainerLowest,
        outlineGlass =
            if (darkSurface) {
                Color.White.copy(alpha = 0.18f)
            } else {
                Color.White.copy(alpha = 0.58f)
            },
        outlineGlassStrong =
            if (darkSurface) {
                Color.White.copy(alpha = 0.28f)
            } else {
                Color.White.copy(alpha = 0.72f)
            },
        shadowGlass =
            Color.Black.copy(
                alpha = if (darkSurface) 0.18f else 0.10f,
            ),
        overlayScrim = colorScheme.scrim.copy(alpha = 0.52f),
        overlayArtwork = Color.Black.copy(alpha = 0.24f),
        textMuted = colorScheme.onSurfaceVariant.copy(alpha = 0.76f),
        textSubtle = colorScheme.onSurfaceVariant.copy(alpha = 0.60f),
        badgeAccent = colorScheme.primary.copy(alpha = 0.10f),
        badgeAccentStrong = colorScheme.primaryContainer,
        accentPositive = colorScheme.tertiary,
        onAccentPositive = colorScheme.onTertiary,
        accentWarning = colorScheme.secondary,
        onAccentWarning = colorScheme.onSecondary,
    )
}
