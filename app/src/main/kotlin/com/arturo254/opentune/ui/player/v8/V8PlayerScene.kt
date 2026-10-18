package com.arturo254.opentune.ui.player.v8

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
    playerBackdrop: com.kyant.backdrop.backdrops.LayerBackdrop? = null,
    immersiveGlass: Boolean = false,
    gradientColors: List<androidx.compose.ui.graphics.Color> = emptyList(),
    controls: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (immersiveGlass) {
            // Record only the static cover owned by the CURRENT track.
            // Canvas/video is drawn as a later sibling, never inside a
            // layerBackdrop node (SurfaceView and HWUI capture incompatibility).
            com.arturo254.opentune.ui.player.visual.PlayerArtworkAtmosphere(
                artworkUrl = artworkUrl,
                gradientColors = gradientColors,
                isPlaying = isPlaying,
                disableBlur = disableBlur,
                modifier = Modifier.then(
                    if (playerBackdrop != null) Modifier.layerBackdrop(playerBackdrop) else Modifier
                ),
            )
        }
        if (!immersiveGlass || !canvasArtwork?.preferredAnimationUrl.isNullOrBlank()) {
            // Preserve the V8 Canvas/video experience without capturing the
            // video player itself into the Kyant backdrop render graph.
            V8PlayerBackdrop(
                thumbnailUrl = artworkUrl,
                canvasArtwork = canvasArtwork,
                isPlaying = isPlaying,
                disableBlur = disableBlur,
                label = "v8BackdropPortrait",
            )
        }

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
