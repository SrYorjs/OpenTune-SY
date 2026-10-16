package com.arturo254.opentune

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.arturo254.opentune.playback.PlayerConnection
import com.arturo254.opentune.ui.component.BottomSheetState
import com.arturo254.opentune.ui.player.host.rememberPlayerHostState

/**
 * Compatibility bridge for the MainActivity shell.
 * The canonical player visual lifecycle now lives in ui/player/host.
 */
internal data class MainActivityPlayerUiState(
    val bottomSheetState: BottomSheetState,
    val playerAwareWindowInsets: WindowInsets,
)

@Composable
internal fun rememberMainActivityPlayerUiState(
    playerConnection: PlayerConnection?,
    systemUiController: MainActivitySystemUiController,
    bottomInset: Dp,
    bottomNavPadding: Dp,
    shouldShowNavigationBar: Boolean,
    useRail: Boolean,
    maxHeight: Dp,
    windowsInsets: WindowInsets,
    isYearInMusicScreen: Boolean,
    isAlwaysOnDisplayScreen: Boolean,
): MainActivityPlayerUiState {
    val host = rememberPlayerHostState(
        playerConnection = playerConnection,
        onSystemBarsVisibilityChange = { yearInMusic, alwaysOnDisplay, expanded, fullscreen ->
            systemUiController.updateSystemBarsVisibility(
                isYearInMusicScreen = yearInMusic,
                isAlwaysOnDisplayScreen = alwaysOnDisplay,
                isPlayerExpanded = expanded,
                playerFullscreen = fullscreen,
            )
        },
        bottomInset = bottomInset,
        bottomNavPadding = bottomNavPadding,
        shouldShowNavigationBar = shouldShowNavigationBar,
        useRail = useRail,
        maxHeight = maxHeight,
        windowsInsets = windowsInsets,
        isYearInMusicScreen = isYearInMusicScreen,
        isAlwaysOnDisplayScreen = isAlwaysOnDisplayScreen,
    )
    return MainActivityPlayerUiState(
        bottomSheetState = host.bottomSheetState,
        playerAwareWindowInsets = host.playerAwareWindowInsets,
    )
}
