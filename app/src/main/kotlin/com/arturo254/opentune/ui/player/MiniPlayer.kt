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
import com.arturo254.opentune.utils.rememberPreference

@Composable
fun MiniPlayer(
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
    navController: NavController,
    state: BottomSheetState
) {
    NewMiniPlayer(
        position = position,
        duration = duration,
        modifier = modifier,
        pureBlack = pureBlack,
        navController = navController,
        state = state
    )
}

@Composable
private fun NewMiniPlayer(
    position: Long,
    duration: Long,
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
    val miniPlayerShape = OpenTuneDesignSystem.shapes.miniPlayer
    val surfaceTokens = OpenTuneDesignSystem.surfaces

    SwipeableMiniPlayerBox(
        modifier = modifier,
        swipeSensitivity = swipeSensitivity,
        swipeThumbnail = swipeThumbnail,
        playerConnection = playerConnection,
        layoutDirection = layoutDirection,
        coroutineScope = coroutineScope,
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
                position = position,
                duration = duration,
                playerConnection = playerConnection,
                navController = navController,
                    state = state,
                    transitionDirection = transitionDirection,
                )
            }
        }
    }
}