package com.arturo254.opentune.ui.player.v8

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.canvas.models.CanvasArtwork

/**
 * V8 immersive composition only. The existing visual engine resolves cover,
 * Canvas and animations, and the enclosing Player controls state and actions.
 *
 * Keeps the original layout's 24dp portrait/16dp landscape trailing spacer,
 * and its horizontal/bottom system insets; no extra surface or gesture owner.
 */
@Composable
fun V8PlayerScene(
    isLandscape: Boolean,
    artworkUrl: String?,
    canvasArtwork: CanvasArtwork?,
    isPlaying: Boolean,
    disableBlur: Boolean,
    queuePeekHeight: Dp,
    nestedScrollConnection: NestedScrollConnection,
    controls: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        V8PlayerBackdrop(
            thumbnailUrl = artworkUrl,
            canvasArtwork = canvasArtwork,
            isPlaying = isPlaying,
            disableBlur = disableBlur,
            // Historical V8 always uses this transition label in both modes.
            label = "v8BackdropPortrait",
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = queuePeekHeight)
                .windowInsetsPadding(
                    WindowInsets.systemBars.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                )
                .nestedScroll(nestedScrollConnection),
        ) {
            controls()
            Spacer(Modifier.height(if (isLandscape) 16.dp else 24.dp))
        }
    }
}
