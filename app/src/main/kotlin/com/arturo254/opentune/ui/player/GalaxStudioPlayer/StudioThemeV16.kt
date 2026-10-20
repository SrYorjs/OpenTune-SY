package com.arturo254.opentune.ui.player.GalaxStudioPlayer

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.unit.Dp
import com.arturo254.opentune.canvas.models.CanvasArtwork
import com.arturo254.opentune.ui.player.v8.V8PlayerScene
import com.kyant.backdrop.backdrops.LayerBackdrop

@Composable
fun StudioThemeV16(
    isLandscape: Boolean,
    artworkUrl: String?,
    canvasArtwork: CanvasArtwork?,
    isPlaying: Boolean,
    disableBlur: Boolean,
    queuePeekHeight: Dp,
    playerBackdrop: LayerBackdrop?,
    immersiveGlass: Boolean,
    gradientColors: List<Color>,
    nestedScrollConnection: NestedScrollConnection,
    controls: @Composable ColumnScope.() -> Unit,
) {
    V8PlayerScene(
        isLandscape=isLandscape,
        artworkUrl=artworkUrl,
        canvasArtwork=canvasArtwork,
        isPlaying=isPlaying,
        disableBlur=disableBlur,
        queuePeekHeight=queuePeekHeight,
        playerBackdrop=playerBackdrop,
        immersiveGlass=immersiveGlass,
        gradientColors=gradientColors,
        nestedScrollConnection=nestedScrollConnection,
        controls=controls,
    )
}
