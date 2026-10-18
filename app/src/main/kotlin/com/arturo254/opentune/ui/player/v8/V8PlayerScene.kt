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
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
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
    var canvasFrame by remember(artworkUrl, canvasArtwork?.preferredAnimationUrl) {
        mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
    }
    val receiveFrame: (androidx.compose.ui.graphics.ImageBitmap?) -> Unit = remember(artworkUrl, canvasArtwork?.preferredAnimationUrl) {
        { canvasFrame = it }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        if (immersiveGlass) {
            Box(Modifier.fillMaxSize().then(
                if (playerBackdrop != null) Modifier.layerBackdrop(playerBackdrop) else Modifier
            )) {
                com.arturo254.opentune.ui.player.visual.PlayerArtworkAtmosphere(
                    artworkUrl = artworkUrl,
                    gradientColors = gradientColors,
                    isPlaying = isPlaying,
                    disableBlur = disableBlur,
                )
                canvasFrame?.let { frame ->
                    androidx.compose.foundation.Image(
                        bitmap = frame, contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
                        modifier = Modifier.fillMaxWidth().fillMaxHeight(.73f)
                            .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
                            .drawWithCache {
                                val mask = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    0f to androidx.compose.ui.graphics.Color.Black,
                                    .65f to androidx.compose.ui.graphics.Color.Black,
                                    1f to androidx.compose.ui.graphics.Color.Transparent,
                                )
                                onDrawWithContent {
                                    drawContent()
                                    drawRect(mask, blendMode = androidx.compose.ui.graphics.BlendMode.DstIn)
                                }
                            },
                    )
                }
            }
        }
        if (!immersiveGlass || !canvasArtwork?.preferredAnimationUrl.isNullOrBlank()) {
            // Preserve the V8 Canvas/video experience without capturing the
            // video player itself into the Kyant backdrop render graph.
            CompositionLocalProvider(
                com.arturo254.opentune.ui.player.LocalCanvasFrameReceiver provides
                    (if (immersiveGlass) receiveFrame else null),
            ) {
            V8PlayerBackdrop(
                thumbnailUrl = artworkUrl,
                canvasArtwork = canvasArtwork,
                isPlaying = isPlaying,
                disableBlur = disableBlur,
                label = "v8BackdropPortrait",
                liquidCanvas = immersiveGlass,
            )
            }
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
