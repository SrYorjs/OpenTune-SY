package com.arturo254.opentune.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem

/**
 * OpenTune-specific type roles derived from the active Material 3 typography.
 *
 * The app theme remains responsible for the font family, text scaling and
 * underlying type scale. Keep feature code independent of font assets/sizes.
 */
@Immutable
data class OpenTuneTypographyTokens(
    val screenTitle: TextStyle,
    val sectionTitle: TextStyle,
    val songTitle: TextStyle,
    val songSubtitle: TextStyle,
    val metadata: TextStyle,
    val badge: TextStyle,
    val playerTitle: TextStyle,
)

internal val LocalOpenTuneTypographyTokens =
    staticCompositionLocalOf<OpenTuneTypographyTokens> {
        error("OpenTune typography tokens are not installed in this composition")
    }

val OpenTuneDesignSystem.typography: OpenTuneTypographyTokens
    @Composable
    @ReadOnlyComposable
    get() = LocalOpenTuneTypographyTokens.current

internal fun createOpenTuneTypographyTokens(
    materialTypography: Typography,
): OpenTuneTypographyTokens =
    OpenTuneTypographyTokens(
        screenTitle = materialTypography.headlineLarge,
        sectionTitle = materialTypography.titleLarge,
        songTitle = materialTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
        songSubtitle = materialTypography.bodyMedium,
        metadata = materialTypography.labelSmall,
        badge = materialTypography.labelSmall,
        playerTitle = materialTypography.titleLarge,
    )
