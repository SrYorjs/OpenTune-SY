package com.arturo254.opentune.ui.player.GalaxStudioPlayer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.media3.ui.AspectRatioFrameLayout
import coil3.compose.AsyncImage
import com.arturo254.opentune.canvas.models.CanvasArtwork
import com.arturo254.opentune.constants.OpenTuneCanvasKey
import com.arturo254.opentune.ui.player.CanvasArtworkPlayer
import com.arturo254.opentune.ui.utils.highRes
import com.arturo254.opentune.utils.rememberPreference

@Composable
fun StudioVinJaxArtwork(
    artworkUrl: String?,
    canvasArtwork: CanvasArtwork?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val (animatedArtworkEnabled) = rememberPreference(OpenTuneCanvasKey, false)
    Box(
        modifier = modifier.fillMaxSize()
            .graphicsLayer {
                shape = CircleShape
                clip = true
                compositingStrategy = CompositingStrategy.Offscreen
            },
    ) {
        AsyncImage(
            model = artworkUrl?.highRes(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(CircleShape),
        )
        if (animatedArtworkEnabled &&
            canvasArtwork != null &&
            !canvasArtwork.preferredAnimationUrl.isNullOrBlank()
        ) {
            CanvasArtworkPlayer(
                primaryUrl = canvasArtwork.animated,
                fallbackUrl = canvasArtwork.videoUrl,
                isPlaying = isPlaying,
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        }
    }
}
