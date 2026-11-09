/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.arturo254.opentune.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.arturo254.opentune.ui.component.LocalEnableLiquidGlass
import com.arturo254.opentune.ui.player.visual.playerVisualGlassEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import com.arturo254.opentune.constants.MiniPlayerHeight
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.theme.shapes
import com.arturo254.opentune.core.designsystem.theme.surfaces
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.clip
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.constants.SwipeSensitivityKey
import com.arturo254.opentune.ui.component.BottomSheetState
import com.arturo254.opentune.ui.player.state.PlayerState
import com.arturo254.opentune.utils.rememberPreference
import com.arturo254.opentune.ui.visual.player.OriginalMiniPlayer
import com.arturo254.opentune.ui.player.actions.rememberPlayerActions

@Composable
fun MiniPlayer(
    playerState: PlayerState,
    playbackIndex: Int,
    queueSize: Int,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
    navController: NavController,
    state: BottomSheetState,
    dockMergeProgress: Float = 0f,
) {
    NewMiniPlayer(
        playerState = playerState,
        playbackIndex = playbackIndex,
        queueSize = queueSize,
        modifier = modifier,
        pureBlack = pureBlack,
        navController = navController,
        state = state,
        dockMergeProgress = dockMergeProgress,
    )
}

@Composable
private fun NewMiniPlayer(
    playerState: PlayerState,
    playbackIndex: Int,
    queueSize: Int,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
    navController: NavController,
    state: BottomSheetState,
    dockMergeProgress: Float,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()
    val playerActions = rememberPlayerActions(playerConnection)
    val swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)
    val swipeThumbnail by rememberPreference(com.arturo254.opentune.constants.SwipeThumbnailKey, true)
    SwipeableMiniPlayerBox(
        modifier = modifier,
        swipeSensitivity = swipeSensitivity,
        swipeThumbnail = swipeThumbnail,
        playerConnection = playerConnection,
        layoutDirection = layoutDirection,
        coroutineScope = coroutineScope,
        trackId = playerState.trackId,
        playbackIndex = playbackIndex,
        queueSize = queueSize,
        isPlayerExpanded = state.isExpanded,
        pureBlack = pureBlack,
        useLegacyBackground = false
    ) { offsetX, _ ->
        OriginalMiniPlayer(
            playerState = playerState,
            pureBlack = pureBlack,
            horizontalOffset = offsetX,
            onOpen = state::expandSoft,
            onPlayPause = playerActions::playPause,
            onNext = playerActions::next,
            onPrevious = playerActions::previous,
        )
    }
}
