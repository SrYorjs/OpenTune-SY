package com.arturo254.opentune.ui.player.host

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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
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
import com.arturo254.opentune.ui.player.BottomSheetPlayer
import com.arturo254.opentune.ui.component.BottomSheetState
import com.arturo254.opentune.ui.component.COLLAPSED_ANCHOR
import com.arturo254.opentune.ui.component.DISMISSED_ANCHOR
import com.arturo254.opentune.ui.component.EXPANDED_ANCHOR
import com.arturo254.opentune.ui.component.rememberBottomSheetState
import com.arturo254.opentune.utils.rememberPreference
import kotlinx.coroutines.flow.first

internal data class PlayerHostState(
    val bottomSheetState: BottomSheetState,
    val playerAwareWindowInsets: WindowInsets,
)

/**
 * Canonical visual PlayerHost for the entire application.
 * Owns one player sheet, persisted anchor restoration, listener lifecycle and
 * system-bar orchestration. The old Activity interface delegates here.
 */
@Composable
internal fun rememberPlayerHostState(
    playerConnection: PlayerConnection?,
    onSystemBarsVisibilityChange: (Boolean, Boolean, Boolean, Boolean) -> Unit,
    bottomInset: Dp,
    bottomNavPadding: Dp,
    shouldShowNavigationBar: Boolean,
    useRail: Boolean,
    maxHeight: Dp,
    windowsInsets: WindowInsets,
    isYearInMusicScreen: Boolean,
    isAlwaysOnDisplayScreen: Boolean,
): PlayerHostState {
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

    val miniPlayerAnchor by remember(playerBottomSheetState) {
        derivedStateOf {
            settledHostPlayerAnchor(
                isExpanded = playerBottomSheetState.isExpanded,
                isCollapsed = playerBottomSheetState.isCollapsed,
                isDismissed = playerBottomSheetState.isDismissed,
            )
        }
    }

    var miniPlayerAnchorPersistenceEnabled by remember(playerConnection) {
        mutableStateOf(false)
    }

    val isPlayerExpanded by remember(playerBottomSheetState) {
        derivedStateOf { playerBottomSheetState.isExpanded }
    }

    LaunchedEffect(
        miniPlayerAnchor,
        isYearInMusicScreen,
        isAlwaysOnDisplayScreen,
        miniPlayerAnchorPersistenceEnabled,
    ) {
        val settledAnchor = miniPlayerAnchor
        if (
            settledAnchor != null &&
            shouldPersistHostAnchor(
                isYearInMusicScreen = isYearInMusicScreen,
                isAlwaysOnDisplayScreen = isAlwaysOnDisplayScreen,
                persistenceEnabled = miniPlayerAnchorPersistenceEnabled,
            )
        ) {
            setSavedMiniPlayerAnchor(settledAnchor)
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

    val latestSystemBarsVisibility by rememberUpdatedState(onSystemBarsVisibilityChange)
    LaunchedEffect(
        isYearInMusicScreen,
        isAlwaysOnDisplayScreen,
        isPlayerExpanded,
        playerFullscreen,
    ) {
        latestSystemBarsVisibility(
            isYearInMusicScreen,
            isAlwaysOnDisplayScreen,
            isPlayerExpanded,
            playerFullscreen,
        )
    }

    LaunchedEffect(isYearInMusicScreen, playerConnection) {
        val player = playerConnection?.player ?: return@LaunchedEffect

        if (isYearInMusicScreen) {
            if (yearInMusicSavedPlayerAnchor == -1) {
                // A special fullscreen route can arrive during sheet drag.
                // Preserve the last settled anchor instead of converting an
                // intermediate animation frame into COLLAPSED.
                yearInMusicSavedPlayerAnchor =
                    settledHostPlayerAnchor(
                        isExpanded = playerBottomSheetState.isExpanded,
                        isCollapsed = playerBottomSheetState.isCollapsed,
                        isDismissed = playerBottomSheetState.isDismissed,
                    ) ?: savedMiniPlayerAnchor
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
            windowsInsets,
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

    return PlayerHostState(
        bottomSheetState = playerBottomSheetState,
        playerAwareWindowInsets = playerAwareWindowInsets,
    )
}

/**
 * Canonical Player 2.0 presentation entry point for MainActivity.
 *
 * Stage 4.4 still delegates V1–V8 layouts to BottomSheetPlayer. Future 4.5+
 * layouts migrate behind this stable host instead of branching in Activity.
 */
@Composable
fun PlayerHost(
    state: BottomSheetState,
    navController: NavController,
    lyricsSyncOffset: Int,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
) {
    BottomSheetPlayer(
        state = state,
        navController = navController,
        lyricsSyncOffset = lyricsSyncOffset,
        modifier = modifier,
        pureBlack = pureBlack,
    )
}
