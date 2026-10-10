package com.arturo254.opentune.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem

/**
 * Material 3 still owns standard surfaces. These roles preserve the existing
 * OpenTune-specific card, glass and overlay values without introducing effects.
 */
@Immutable
data class OpenTuneElevationTokens(
    val flat: Dp = 0.dp,
    val releaseNotesCard: Dp = 4.dp,
    val releaseNotesSurfaceTint: Dp = 6.dp,
    val artworkSettingsDialog: Dp = 6.dp,
)

@Immutable
data class OpenTuneShadowTokens(
    val glassOuterRadius: Dp = 8.dp,
    val glassInnerRadius: Dp = 4.dp,
)

@Immutable
data class OpenTuneOpacityTokens(
    val selectedGlassIcon: Float = 0.10f,
    val selectedGlassPill: Float = 0.11f,
    val glassClusterOpaque: Float = 0.92f,
    val activeArtworkOverlay: Float = 0.28f,
    val innerGlassShadowDark: Float = 0.22f,
    val innerGlassShadowLight: Float = 0.14f,
    val glassCardBlackTop: Float = 0.075f,
    val glassCardBlackAccent: Float = 0.045f,
    val glassCardBlackBottom: Float = 0.045f,
    val glassCardLightTop: Float = 0.16f,
    val glassCardLightAccent: Float = 0.055f,
    val glassCardLightSurface: Float = 0.08f,
    val glassCardOpaqueGradientEnd: Float = 0.82f,
    val miniPlayerPureBlack: Float = 0.82f,
    val miniPlayerOpaque: Float = 0.88f,
)

@Immutable
data class OpenTuneBorderTokens(
    val fallbackGlassWidth: Dp = 0.8.dp,
    val activeGlassWidth: Dp = 0.75.dp,
    val glassCardWidth: Dp = 0.8.dp,
    val artworkPreviewWidth: Dp = 1.dp,
    val miniPlayerOpaqueWidth: Dp = 1.dp,
    val glassCardPureBlackColor: Color,
    val glassCardStandardColor: Color,
    val glassCardOpaqueColor: Color,
    val artworkPreviewColor: Color,
    val miniPlayerOpaqueColor: Color,
)

@Immutable
data class OpenTuneSurfaceTokens(
    val elevations: OpenTuneElevationTokens,
    val shadows: OpenTuneShadowTokens,
    val opacity: OpenTuneOpacityTokens,
    val borders: OpenTuneBorderTokens,
)

internal fun createOpenTuneSurfaceTokens(
    scheme: ColorScheme,
): OpenTuneSurfaceTokens =
    OpenTuneSurfaceTokens(
        elevations = OpenTuneElevationTokens(),
        shadows = OpenTuneShadowTokens(),
        opacity = OpenTuneOpacityTokens(),
        borders =
            OpenTuneBorderTokens(
                glassCardPureBlackColor = Color.White.copy(alpha = 0.18f),
                glassCardStandardColor = Color.White.copy(alpha = 0.22f),
                glassCardOpaqueColor = scheme.outlineVariant.copy(alpha = 0.20f),
                artworkPreviewColor = scheme.outline.copy(alpha = 0.50f),
                miniPlayerOpaqueColor = scheme.outline.copy(alpha = 0.48f),
            ),
    )

internal val LocalOpenTuneSurfaceTokens =
    staticCompositionLocalOf<OpenTuneSurfaceTokens> {
        error("OpenTune surface tokens are not installed in this composition")
    }

val OpenTuneDesignSystem.surfaces: OpenTuneSurfaceTokens
    @Composable
    @ReadOnlyComposable
    get() = LocalOpenTuneSurfaceTokens.current
