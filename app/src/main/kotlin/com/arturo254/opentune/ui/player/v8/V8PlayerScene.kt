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
import com.arturo254.opentune.ui.player.visual.GalaxPlayerSceneOverlay
import com.arturo254.opentune.ui.player.layout.GalaxPlayerControlDock
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.canvas.models.CanvasArtwork

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
    val canvasFrame = remember(artworkUrl, canvasArtwork?.preferredAnimationUrl) {
        mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
    }
    val receiveFrame: (androidx.compose.ui.graphics.ImageBitmap?) -> Unit = remember(artworkUrl, canvasArtwork?.preferredAnimationUrl) {
        { canvasFrame.value = it }
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
                CanvasGlassSource(canvasFrame)
            }
        }
        if (!immersiveGlass || !canvasArtwork?.preferredAnimationUrl.isNullOrBlank()) {

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

        GalaxPlayerSceneOverlay(
            index = "16",
            subtitle = "GALAXTHEME",
            isPlaying = isPlaying,
            landscape = isLandscape,
            accent = gradientColors.firstOrNull() ?: androidx.compose.material3.MaterialTheme.colorScheme.primary,
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
            if (isLandscape) {
                controls()
            } else {
                GalaxPlayerControlDock(
                    accent = gradientColors.firstOrNull()
                        ?: androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    isPlaying = isPlaying,
                ) {
                    controls()
                }
            }
            Spacer(Modifier.height(if (isLandscape) 16.dp else 24.dp))
        }
    }
}

@Composable
private fun CanvasGlassSource(frameState: State<androidx.compose.ui.graphics.ImageBitmap?>) {
    frameState.value?.let { frame ->
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
