package com.arturo254.opentune.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalMotionDurationScale
import com.arturo254.opentune.core.designsystem.motion.GalaxMotionPolicy
import com.arturo254.opentune.core.designsystem.motion.LocalGalaxMotionPolicy
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
    artworkRadiusDp: Float = 16f,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val environment =
        remember {
            DesignSystemEnvironment(
                schemaVersion = OpenTuneDesignSystem.SchemaVersion,
            )
        }
    // Compose's platform duration scale is the authoritative accessibility
    // source. The app-level override may also disable decorative transitions.
    val platformMotionScale = LocalMotionDurationScale.current.scaleFactor
    val reducedMotion = reduceMotion || platformMotionScale <= 0f
    val galaxMotion = remember(reducedMotion) { GalaxMotionPolicy(reducedMotion) }
    val colorScheme = MaterialTheme.colorScheme
    val materialTypography = MaterialTheme.typography
    val materialShapes = MaterialTheme.shapes
    val colors =
        remember(colorScheme) {
            createOpenTuneColorRoles(colorScheme)
        }
    val surfaces =
        remember(colorScheme) {
            createOpenTuneSurfaceTokens(colorScheme)
        }
    val typography =
        remember(materialTypography) {
            createOpenTuneTypographyTokens(materialTypography)
        }
    val galax = remember(colorScheme, materialTypography, artworkRadiusDp) {
        createGalaxDesignTokens(colorScheme, materialTypography, artworkRadiusDp)
    }
    val shapes =
        remember(materialShapes, artworkRadiusDp) {
            createOpenTuneShapeTokens(materialShapes, artworkRadiusDp)
        }

    CompositionLocalProvider(
        LocalDesignSystemEnvironment provides environment,
        LocalGalaxDesignTokens provides galax,
        LocalGalaxMotionPolicy provides galaxMotion,
        LocalOpenTuneColorRoles provides colors,
        LocalOpenTuneTypographyTokens provides typography,
        LocalOpenTuneSpacingTokens provides DefaultOpenTuneSpacingTokens,
        LocalOpenTuneSizeTokens provides DefaultOpenTuneSizeTokens,
        LocalOpenTuneShapeTokens provides shapes,
        LocalOpenTuneSurfaceTokens provides surfaces,
        LocalOpenTuneMotionTokens provides DefaultOpenTuneMotionTokens,
        content = content,
    )
}
