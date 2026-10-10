package com.arturo254.opentune.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem

/**
 * Shared physical layout dimensions. Legacy constants in constants/Dimensions.kt
 * delegate to these values until feature screens migrate in 3.15.
 *
 * Corners/shapes and motion belong to 3.6/3.8, not this contract.
 */
@Immutable
data class OpenTuneSizeTokens(
    val floatingToolbarHeight: Dp = 72.dp,
    val slimFloatingToolbarHeight: Dp = 64.dp,
    val floatingToolbarHorizontalPadding: Dp = 16.dp,
    val floatingToolbarBottomPadding: Dp = 12.dp,
    val miniPlayerHeight: Dp = 64.dp,
    val miniPlayerBottomSpacing: Dp = 8.dp,
    val queuePeekHeight: Dp = 64.dp,
    val appBarHeight: Dp = 64.dp,
    val listItemHeight: Dp = 72.dp,
    val suggestionItemHeight: Dp = 60.dp,
    val searchFilterHeight: Dp = 50.dp,
    val listThumbnailSize: Dp = 54.dp,
    val smallGridThumbnailHeight: Dp = 116.dp,
    val gridThumbnailHeight: Dp = 148.dp,
    val albumThumbnailSize: Dp = 156.dp,
    val playerHorizontalPadding: Dp = 32.dp,
    val searchRowArtwork: Dp = 60.dp,
    val searchPlaceholderIcon: Dp = 26.dp,
    val searchOverlayIcon: Dp = 28.dp,
    val searchResolvingIndicator: Dp = 25.dp,
    val searchVersionsButton: Dp = 36.dp,
    val searchVersionsIcon: Dp = 21.dp,
    val searchLoadingIndicator: Dp = 28.dp,
)

/** Single shared instance used by both Compose and existing legacy constants. */
val DefaultOpenTuneSizeTokens = OpenTuneSizeTokens()

internal val LocalOpenTuneSizeTokens =
    staticCompositionLocalOf<OpenTuneSizeTokens> {
        error("OpenTune size tokens are not installed in this composition")
    }

val OpenTuneDesignSystem.sizes: OpenTuneSizeTokens
    @Composable
    @ReadOnlyComposable
    get() = LocalOpenTuneSizeTokens.current

/**
 * The device's real Compose density, including fontScale.
 * Dp values already scale automatically when rendered; convert to px only
 * at Android/canvas interop boundaries, inside with(density) { dp.toPx() }.
 */
val OpenTuneDesignSystem.density: Density
    @Composable
    @ReadOnlyComposable
    get() = LocalDensity.current
