package com.arturo254.opentune.ui.player.v7

import com.kyant.backdrop.backdrops.layerBackdrop
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.ui.player.visual.V7PlayerBackdrop

/**
 * V7's cinematic layout, now separate from the monolithic Player.kt.
 * Preserve the historical portrait/landscape insets and final spacers.
 * Canvas and backdrop implementations remain owned by the visual engine.
 */
@Composable
fun V7PlayerScene(
    isLandscape: Boolean,
    thumbnailUrl: String?,
    disableBlur: Boolean,
    queuePeekHeight: Dp,
    nestedScrollConnection: NestedScrollConnection,
    playerBackdrop: com.kyant.backdrop.Backdrop? = null,
    immersiveGlass: Boolean = false,
    gradientColors: List<androidx.compose.ui.graphics.Color> = emptyList(),
    controls: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (immersiveGlass) {
            com.arturo254.opentune.ui.player.visual.PlayerArtworkAtmosphere(
                artworkUrl = thumbnailUrl,
                gradientColors = gradientColors,
                isPlaying = true,
                disableBlur = disableBlur,
                modifier = Modifier.then(
                    if (playerBackdrop != null) Modifier.layerBackdrop(playerBackdrop) else Modifier
                ),
            )
        } else {
            V7PlayerBackdrop(
                thumbnailUrl = thumbnailUrl,
                disableBlur = disableBlur,
                label = if (isLandscape) "v7BackdropLandscape" else "v7BackdropPortrait",
            )
        }


        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = queuePeekHeight)
                .windowInsetsPadding(
                    WindowInsets.systemBars.only(
                        if (isLandscape) WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                        else WindowInsetsSides.Horizontal
                    )
                )
                .nestedScroll(nestedScrollConnection),
        ) {
            controls()
            Spacer(Modifier.height(if (isLandscape) 16.dp else 24.dp))
        }
    }
}
