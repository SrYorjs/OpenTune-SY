package com.arturo254.opentune.ui.visual.component

import android.net.ConnectivityManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService
import coil3.compose.AsyncImage
import com.arturo254.opentune.canvas.providers.AppleMusicCanvasProvider
import com.arturo254.opentune.ui.player.CanvasArtworkPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
internal fun OriginalHomeCanvas(
    id: String,
    title: String,
    artist: String,
    album: String?,
    artwork: String?,
    eligibleForCanvas: Boolean,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var canvasUrl by remember(id) { mutableStateOf<String?>(null) }
    LaunchedEffect(id, title, artist, album, eligibleForCanvas, active) {
        if (!eligibleForCanvas || !active || title.isBlank() || artist.isBlank()) return@LaunchedEffect
        val connectivity = context.getSystemService<ConnectivityManager>()
        if (connectivity?.isActiveNetworkMetered == true) return@LaunchedEffect
        if (canvasUrl != null) return@LaunchedEffect
        val storefront = Locale.getDefault().country
            .takeIf { it.length == 2 }?.lowercase(Locale.ROOT) ?: "us"
        canvasUrl = runCatching {
            withContext(Dispatchers.IO) {
                AppleMusicCanvasProvider.getBySongArtist(title, artist, album, storefront)
                    ?.preferredAnimationUrl
            }
        }.getOrNull()
    }
    Box(modifier = modifier) {
        AsyncImage(
            model = artwork,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        val animated = canvasUrl
        if (animated != null && eligibleForCanvas && active) {
            CanvasArtworkPlayer(
                primaryUrl = animated,
                fallbackUrl = null,
                isPlaying = active,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
