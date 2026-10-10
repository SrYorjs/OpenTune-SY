package com.arturo254.opentune.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem

/**
 * Spacing in dp. Compose handles device density; callers must not scale these
 * dimensions manually. Exceptional values preserve current media-row geometry.
 */
@Immutable
data class OpenTuneSpacingTokens(
    val none: Dp = 0.dp,
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val mediaRowPadding: Dp = 10.dp,
    val mediaRowArtworkGap: Dp = 14.dp,
    val mediaRowTextGap: Dp = 3.dp,
    val badgeHorizontalPadding: Dp = 8.dp,
    val badgeVerticalPadding: Dp = 5.dp,
    val qualityFilterGap: Dp = 6.dp,
    val searchBottomInset: Dp = 128.dp,
)

val DefaultOpenTuneSpacingTokens = OpenTuneSpacingTokens()

internal val LocalOpenTuneSpacingTokens =
    staticCompositionLocalOf<OpenTuneSpacingTokens> {
        error("OpenTune spacing tokens are not installed in this composition")
    }

val OpenTuneDesignSystem.spacing: OpenTuneSpacingTokens
    @Composable
    @ReadOnlyComposable
    get() = LocalOpenTuneSpacingTokens.current
