package com.arturo254.opentune

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.only
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.arturo254.opentune.constants.AppBarHeight
import com.arturo254.opentune.constants.FloatingToolbarBottomPadding
import com.arturo254.opentune.constants.MiniPlayerBottomSpacing
import com.arturo254.opentune.constants.MiniPlayerHeight
import com.arturo254.opentune.constants.MiniPlayerLastAnchorKey
import com.arturo254.opentune.constants.PlayerFullscreenKey
import com.arturo254.opentune.constants.UseNewMiniPlayerDesignKey
import com.arturo254.opentune.playback.PlayerConnection
import com.arturo254.opentune.ui.component.BottomSheetState
import com.arturo254.opentune.ui.component.COLLAPSED_ANCHOR
import com.arturo254.opentune.ui.component.DISMISSED_ANCHOR
import com.arturo254.opentune.ui.component.EXPANDED_ANCHOR
import com.arturo254.opentune.ui.component.rememberBottomSheetState
import com.arturo254.opentune.utils.rememberPreference
import kotlinx.coroutines.flow.first

internal data class MainActivityPlayerUiState(
    val bottomSheetState: BottomSheetState,
    val playerAwareWindowInsets: WindowInsets,
)

/**
 * Owns the visual player sheet state, persisted anchor restoration and
 * fullscreen/system-bar behavior.
 */
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
    val (useNewMiniPlayerDesign) =
        rememberPreference(
            UseNewMiniPlayerDesignKey,
            defaultValue = true,
        )

    val (savedMiniPlayerAnchor, setSavedMiniPlayerAnchor) =
        rememberPreference(
            MiniPlayerLastAnchorKey,
            defaultValue = COLLAPSED_ANCHOR,
        )

    val playerBottomSheetState =
        rememberBottomSheetState(
            dismissedBound = 0.dp,
            collapsedBound =
                bottomInset +
                    (if (shouldShowNavigationBar && !useRail) {
                        FloatingToolbarBottomPadding
                    } else {
                        0.dp
                    }) +
                    bottomNavPadding +
                    (if (useNewMiniPlayerDesign) MiniPlayerBottomSpacing else 0.dp) +
                    MiniPlayerHeight,
            expandedBound = maxHeight,
        )

    val miniPlayerAnchor by remember {
        derivedStateOf {
            when {
                playerBottomSheetState.isExpanded -> EXPANDED_ANCHOR
                playerBottomSheetState.isDismissed -> DISMISSED_ANCHOR
                else -> COLLAPSED_ANCHOR
            }
        }
    }

    var miniPlayerAnchorPersistenceEnabled by remember(playerConnection) {
        mutableStateOf(false)
    }

    val isPlayerExpanded by remember {
        derivedStateOf { playerBottomSheetState.isExpanded }
    }

    LaunchedEffect(
        miniPlayerAnchor,
        isYearInMusicScreen,
        isAlwaysOnDisplayScreen,
        miniPlayerAnchorPersistenceEnabled,
    ) {
        if (
            !isYearInMusicScreen &&
            !isAlwaysOnDisplayScreen &&
            miniPlayerAnchorPersistenceEnabled
        ) {
            setSavedMiniPlayerAnchor(miniPlayerAnchor)
        }
    }

    var yearInMusicSavedPlayerAnchor by rememberSaveable {
        mutableStateOf(-1)
    }

    val (playerFullscreen) =
        rememberPreference(
            PlayerFullscreenKey,
            defaultValue = false,
        )

    LaunchedEffect(
        isYearInMusicScreen,
        isAlwaysOnDisplayScreen,
        isPlayerExpanded,
        playerFullscreen,
    ) {
        systemUiController.updateSystemBarsVisibility(
            isYearInMusicScreen = isYearInMusicScreen,
            isAlwaysOnDisplayScreen = isAlwaysOnDisplayScreen,
            isPlayerExpanded = isPlayerExpanded,
            playerFullscreen = playerFullscreen,
        )
    }

    LaunchedEffect(isYearInMusicScreen, playerConnection) {
        val player = playerConnection?.player ?: return@LaunchedEffect

        if (isYearInMusicScreen) {
            if (yearInMusicSavedPlayerAnchor == -1) {
                yearInMusicSavedPlayerAnchor =
                    when {
                        playerBottomSheetState.isExpanded -> EXPANDED_ANCHOR
                        playerBottomSheetState.isCollapsed -> COLLAPSED_ANCHOR
                        playerBottomSheetState.isDismissed -> DISMISSED_ANCHOR
                        else -> COLLAPSED_ANCHOR
                    }
            }

            if (!playerBottomSheetState.isDismissed) {
                playerBottomSheetState.dismiss()
            }
        } else if (yearInMusicSavedPlayerAnchor != -1) {
            val anchorToRestore = yearInMusicSavedPlayerAnchor
            yearInMusicSavedPlayerAnchor = -1

            if (player.currentMediaItem == null) {
                playerBottomSheetState.dismiss()
            } else {
                when (anchorToRestore) {
                    EXPANDED_ANCHOR -> playerBottomSheetState.expandSoft()
                    COLLAPSED_ANCHOR -> playerBottomSheetState.collapseSoft()
                    DISMISSED_ANCHOR -> playerBottomSheetState.dismiss()
                    else -> playerBottomSheetState.collapseSoft()
                }
            }
        }
    }

    var restoredMiniPlayerAnchor by remember(playerConnection) {
        mutableStateOf(false)
    }

    LaunchedEffect(
        playerConnection,
        savedMiniPlayerAnchor,
        isYearInMusicScreen,
    ) {
        if (restoredMiniPlayerAnchor) return@LaunchedEffect

        val player = playerConnection?.player ?: return@LaunchedEffect
        val connection = playerConnection ?: return@LaunchedEffect
        connection.queueRestoreCompleted.first { it }

        if (player.currentMediaItem == null) {
            if (!playerBottomSheetState.isDismissed) {
                playerBottomSheetState.dismiss()
            }
        } else if (!isYearInMusicScreen) {
            when (savedMiniPlayerAnchor) {
                EXPANDED_ANCHOR -> playerBottomSheetState.expandSoft()
                COLLAPSED_ANCHOR -> playerBottomSheetState.collapseSoft()
                DISMISSED_ANCHOR -> playerBottomSheetState.dismiss()
                else -> playerBottomSheetState.collapseSoft()
            }
        }

        restoredMiniPlayerAnchor = true
        miniPlayerAnchorPersistenceEnabled = true
    }

    DisposableEffect(
        playerConnection,
        playerBottomSheetState,
        isYearInMusicScreen,
    ) {
        val player =
            playerConnection?.player
                ?: return@DisposableEffect onDispose { }

        val listener =
            object : Player.Listener {
                override fun onMediaItemTransition(
                    mediaItem: MediaItem?,
                    reason: Int,
                ) {
                    if (
                        reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED &&
                        mediaItem != null &&
                        playerBottomSheetState.isDismissed &&
                        !isYearInMusicScreen
                    ) {
                        playerBottomSheetState.collapseSoft()
                    }
                }
            }

        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    val playerAwareWindowInsets =
        remember(
            useRail,
            bottomInset,
            bottomNavPadding,
            playerBottomSheetState.isDismissed,
        ) {
            var bottom = bottomInset
            bottom += bottomNavPadding
            if (!playerBottomSheetState.isDismissed) {
                bottom += MiniPlayerHeight
            }

            windowsInsets
                .only(
                    (if (useRail) {
                        WindowInsetsSides.Right
                    } else {
                        WindowInsetsSides.Horizontal
                    }) + WindowInsetsSides.Top,
                )
                .add(
                    WindowInsets(
                        top = AppBarHeight,
                        bottom = bottom,
                    ),
                )
        }

    return MainActivityPlayerUiState(
        bottomSheetState = playerBottomSheetState,
        playerAwareWindowInsets = playerAwareWindowInsets,
    )
}
