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

@Composable
fun MiniPlayer(
    playerState: PlayerState,
    playbackIndex: Int,
    queueSize: Int,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
    navController: NavController,
    state: BottomSheetState
) {
    NewMiniPlayer(
        playerState = playerState,
        playbackIndex = playbackIndex,
        queueSize = queueSize,
        modifier = modifier,
        pureBlack = pureBlack,
        navController = navController,
        state = state
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
    state: BottomSheetState
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()
    val swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)
    val swipeThumbnail by rememberPreference(com.arturo254.opentune.constants.SwipeThumbnailKey, true)
    val glassEnabled = LocalEnableLiquidGlass.current
    val configuredLiquidRadius = com.arturo254.opentune.core.designsystem.liquid.LocalGalaxLiquidTuning.current?.cornerRadiusDp
    val miniPlayerShape = if (glassEnabled && configuredLiquidRadius != null) {
        androidx.compose.foundation.shape.RoundedCornerShape(configuredLiquidRadius.dp)
    } else OpenTuneDesignSystem.shapes.miniPlayer
    val surfaceTokens = OpenTuneDesignSystem.surfaces

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
    ) { offsetX, transitionDirection ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(MiniPlayerHeight)
                .preferredFrameRate(120f)
                // Clip independently of the glass shader: the opaque fallback
                // must retain rounded corners when Liquid Glass is disabled.
                .clip(miniPlayerShape)
                .background(
                    color = if (glassEnabled) {
                        Color.Transparent
                    } else {
                        if (pureBlack) Color.Black.copy(alpha = surfaceTokens.opacity.miniPlayerPureBlack)
                        else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = surfaceTokens.opacity.miniPlayerOpaque)
                    }
                )
                .playerVisualGlassEffect(
                    shape = miniPlayerShape,
                    pureBlack = pureBlack,
                )
                .then(
                    if (!glassEnabled) {
                        Modifier.border(
                            width = surfaceTokens.borders.miniPlayerOpaqueWidth,
                            color = surfaceTokens.borders.miniPlayerOpaqueColor,
                            shape = miniPlayerShape,
                        )
                    } else {
                        Modifier
                    }
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MiniPlayerHeight)
                    .graphicsLayer {
                        translationX = offsetX
                    }
            ) {
                NewMiniPlayerContent(
                    pureBlack = pureBlack,
                    playerState = playerState,
                    playerConnection = playerConnection,
                navController = navController,
                    state = state,
                    transitionDirection = transitionDirection,
                )
            }
        }
    }
}