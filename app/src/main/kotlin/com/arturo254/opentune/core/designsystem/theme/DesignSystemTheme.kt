package com.arturo254.opentune.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.arturo254.opentune.core.designsystem.DesignSystemEnvironment
import com.arturo254.opentune.core.designsystem.LocalDesignSystemEnvironment
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem

/**
 * Installs the Design System boundary inside the existing Material theme.
 *
 * This is deliberately visual-neutral in 3.2. Later token providers are added
 * here so the legacy UI can migrate incrementally without changing app theme
 * ownership or feature behavior.
 */
@Composable
fun ProvideOpenTuneDesignSystem(
    content: @Composable () -> Unit,
) {
    val environment =
        remember {
            DesignSystemEnvironment(
                schemaVersion = OpenTuneDesignSystem.SchemaVersion,
            )
        }
    val colorScheme = MaterialTheme.colorScheme
    val materialTypography = MaterialTheme.typography
    val colors =
        remember(colorScheme) {
            createOpenTuneColorRoles(colorScheme)
        }
    val typography =
        remember(materialTypography) {
            createOpenTuneTypographyTokens(materialTypography)
        }

    CompositionLocalProvider(
        LocalDesignSystemEnvironment provides environment,
        LocalOpenTuneColorRoles provides colors,
        LocalOpenTuneTypographyTokens provides typography,
        LocalOpenTuneSpacingTokens provides DefaultOpenTuneSpacingTokens,
        LocalOpenTuneSizeTokens provides DefaultOpenTuneSizeTokens,
        content = content,
    )
}
