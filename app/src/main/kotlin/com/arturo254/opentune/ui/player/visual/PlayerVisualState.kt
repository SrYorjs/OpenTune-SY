package com.arturo254.opentune.ui.player.visual

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.arturo254.opentune.canvas.models.CanvasArtwork
import com.arturo254.opentune.constants.CanvasSource
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.ui.player.fetchCanvasArtworkForPlayback
import com.arturo254.opentune.ui.theme.PlayerColorExtractor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Canonical presentation-only owner for Player's artwork, Canvas and
 * artwork-derived background colors. It never owns music Media3 playback.
 */
data class PlayerVisualState(
    val artworkUrl: String?,
    val canvasArtwork: CanvasArtwork?,
    val gradientColors: List<Color>,
)

internal const val PLAYER_PALETTE_CACHE_CAPACITY = 64

internal data class PlayerPaletteCacheKey(
    val mediaId: String,
    val artworkUrl: String,
    val fallbackColor: Int,
    val variantColor: Int,
)

/** Offline file wins; remote artwork is kept when offline is unavailable. */
internal fun selectPlayerArtwork(offlineUrl: String?, remoteUrl: String?): String? =
    offlineUrl?.takeIf(String::isNotBlank) ?: remoteUrl?.takeIf(String::isNotBlank)

/** Guards missing titles/artists without starting a provider lookup. */
internal fun canFetchPlayerCanvas(metadata: MediaMetadata?): Boolean =
    metadata != null && metadata.title.isNotBlank() &&
        metadata.artists.any { it.name.isNotBlank() }

internal fun localPlayerCanvas(metadata: MediaMetadata, localUrl: String?): CanvasArtwork? =
    localUrl?.takeIf(String::isNotBlank)?.let { url ->
        CanvasArtwork(
            name = metadata.title,
            artist = metadata.artists.joinToString { it.name },
            albumName = metadata.album?.title,
            animated = url,
            videoUrl = url,
        )
    }

/**
 * Metadata and download revision are keys: a new track gets a fresh Canvas
 * state immediately, and LaunchedEffect cancels stale provider lookups.
 * Color cache includes the actual artwork source AND Material fallback.
 */
@Composable
fun rememberPlayerVisualState(
    metadata: MediaMetadata?,
    downloadRevision: Any?,
    offlineArtworkUrl: String?,
    offlineAnimationLookup: (String) -> String?,
    usesArtworkPalette: Boolean,
): PlayerVisualState {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val artworkUrl = selectPlayerArtwork(offlineArtworkUrl, metadata?.thumbnailUrl)
    val trackId = metadata?.id
    val latestOfflineLookup by rememberUpdatedState(offlineAnimationLookup)

    var canvasArtwork by remember(trackId, downloadRevision) {
        mutableStateOf<CanvasArtwork?>(null)
    }
    LaunchedEffect(trackId, downloadRevision) {
        val track = metadata ?: return@LaunchedEffect
        val local = localPlayerCanvas(track, latestOfflineLookup(track.id))
        if (local != null) {
            canvasArtwork = local
            return@LaunchedEffect
        }
        if (!canFetchPlayerCanvas(track)) return@LaunchedEffect
        try {
            canvasArtwork = withContext(Dispatchers.IO) {
                fetchCanvasArtworkForPlayback(
                    songTitleRaw = track.title,
                    artistNameRaw = track.artists.joinToString { it.name },
                    albumName = track.album?.title,
                    source = CanvasSource.AUTO,
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.e(error, "Player visual Canvas lookup failed")
            canvasArtwork = null
        }
    }

    val fallbackColors = listOf(colors.surface, colors.surfaceVariant)
    val fallbackArgb = colors.surface.toArgb()
    // Access-ordered and bounded. The old "clear at 64" discarded every hot
    // palette at once, causing repeat decodes on long radio/shuffle sessions.
    val paletteCache = remember {
        object : LinkedHashMap<PlayerPaletteCacheKey, List<Color>>(64, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<PlayerPaletteCacheKey, List<Color>>?,
            ): Boolean = size > PLAYER_PALETTE_CACHE_CAPACITY
        }
    }
    var gradientColors by remember(trackId, artworkUrl, usesArtworkPalette, colors.surface, colors.surfaceVariant) {
        mutableStateOf<List<Color>>(emptyList())
    }

    LaunchedEffect(trackId, artworkUrl, usesArtworkPalette, colors.surface, colors.surfaceVariant) {
        if (!usesArtworkPalette || trackId.isNullOrBlank() || artworkUrl.isNullOrBlank()) {
            gradientColors = emptyList()
            return@LaunchedEffect
        }
        val key = PlayerPaletteCacheKey(trackId, artworkUrl, fallbackArgb, colors.surfaceVariant.toArgb())
        val cached = paletteCache[key]
        if (cached != null) {
            gradientColors = cached
            return@LaunchedEffect
        }

        val result = try {
            withContext(Dispatchers.IO) {
                context.imageLoader.execute(
                    ImageRequest.Builder(context)
                        .data(artworkUrl)
                        .size(
                            PlayerColorExtractor.Config.IMAGE_SIZE,
                            PlayerColorExtractor.Config.IMAGE_SIZE,
                        )
                        .allowHardware(false)
                        .build()
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        val bitmap = result?.image?.toBitmap()
        val extracted = if (bitmap != null) {
            withContext(Dispatchers.Default) {
                val palette = Palette.from(bitmap)
                    .maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT)
                    .resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA)
                    .generate()
                PlayerColorExtractor.extractGradientColors(palette, fallbackArgb)
            }
        } else {
            fallbackColors
        }
        paletteCache[key] = extracted
        gradientColors = extracted
    }

    return PlayerVisualState(
        artworkUrl = artworkUrl,
        canvasArtwork = canvasArtwork,
        gradientColors = gradientColors,
    )
}
