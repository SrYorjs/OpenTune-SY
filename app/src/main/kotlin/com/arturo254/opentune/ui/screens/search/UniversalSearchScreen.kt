package com.arturo254.opentune.ui.screens.search

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
import coil3.compose.AsyncImage
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.catalog.CatalogPlaybackResolver
import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.core.model.TrackVersionTrait
import com.arturo254.opentune.extensions.ExtraIsMusicVideo
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.innertube.models.WatchEndpoint
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.playback.queues.ListQueue
import com.arturo254.opentune.playback.queues.YouTubeQueue
import com.arturo254.opentune.sources.deezer.toDeezerCatalogReference
import com.arturo254.opentune.sources.search.UnifiedTrackSearchResult
import com.arturo254.opentune.ui.component.liquidGlassScreenBackground
import com.arturo254.opentune.viewmodels.UniversalSearchViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@Composable
fun UniversalSearchScreen(
    query: String,
    pureBlack: Boolean,
    viewModel: UniversalSearchViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val state by viewModel.state.collectAsState()
    val lazyListState = rememberLazyListState()
    val activeMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val scope = rememberCoroutineScope()
    var resolvingCatalogKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        viewModel.query.value = query
    }

    LaunchedEffect(lazyListState, state.result.tracks.size, state.result.hasMore) {
        if (!state.result.hasMore) return@LaunchedEffect

        snapshotFlow {
            val layoutInfo = lazyListState.layoutInfo
            val lastVisibleIndex =
                layoutInfo.visibleItemsInfo
                    .lastOrNull()
                    ?.index
                    ?: -1

            layoutInfo.totalItemsCount > 0 &&
                lastVisibleIndex >= layoutInfo.totalItemsCount - 5
        }.distinctUntilChanged()
            .filter { it }
            .collect {
                viewModel.loadMore()
            }
    }

    LazyColumn(
        state = lazyListState,
        contentPadding =
            PaddingValues(
                start = 12.dp,
                top = 12.dp,
                end = 12.dp,
                bottom = 128.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier =
            Modifier
                .fillMaxSize()
                .liquidGlassScreenBackground(pureBlack = pureBlack),
    ) {
        if (state.result.failedSourceCount > 0) {
            item(key = "source_warning") {
                Text(
                    text = stringResource(R.string.some_sources_unavailable),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }

        if (state.isLoading && state.result.tracks.isEmpty()) {
            item(key = "loading") {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            }
        }

        if (
            state.query.isNotBlank() &&
            !state.isLoading &&
            state.result.tracks.isEmpty()
        ) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.no_results_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
                )
            }
        }

        items(
            items = state.result.tracks,
            key = { result ->
                result.variants
                    .joinToString("|") { variant ->
                        variant.provider.value + ":" + variant.sourceId
                    }
            },
        ) { result ->
            val playable = result.recommendedVariant
            val deezerFallback =
                if (playable == null) {
                    result.variants.firstOrNull { it.provider == ProviderId.DEEZER }
                } else {
                    null
                }
            val resultKey =
                result.variants.joinToString("|") { variant ->
                    variant.provider.value + ":" + variant.sourceId
                }
            val isActive =
                playable?.sourceId == activeMetadata?.id ||
                    (
                        playable == null &&
                            activeMetadata?.title.equals(result.identity.title, ignoreCase = true) &&
                            result.identity.artists.any { identityArtist ->
                                activeMetadata?.artists
                                    ?.any {
                                        it.name.equals(
                                            identityArtist.name,
                                            ignoreCase = true,
                                        )
                                    } == true
                            }
                    )

            UnifiedTrackRow(
                result = result,
                isActive = isActive,
                isPlaying = isPlaying,
                isResolving = resolvingCatalogKey == resultKey,
                onClick =
                    when {
                        isActive ->
                            {
                                {
                                    playerConnection.player.togglePlayPause()
                                }
                            }

                        playable != null ->
                            {
                                {
                                    when (playable.provider) {
                                        ProviderId.YOUTUBE_MUSIC ->
                                            playerConnection.playQueue(
                                                YouTubeQueue(
                                                    endpoint =
                                                        WatchEndpoint(
                                                            videoId = playable.sourceId,
                                                        ),
                                                    preloadItem = playable.toOpenTuneMetadata(),
                                                ),
                                            )

                                        else ->
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title =
                                                        context.getString(
                                                            R.string.queue_searched_songs,
                                                        ),
                                                    items = listOf(playable.toPlayableMediaItem()),
                                                ),
                                            )
                                    }
                                }
                            }

                        deezerFallback != null ->
                            {
                                {
                                    if (resolvingCatalogKey == null) {
                                        scope.launch {
                                            resolvingCatalogKey = resultKey
                                            try {
                                                val selected =
                                                    CatalogPlaybackResolver.resolveToMediaItem(
                                                        deezerFallback
                                                            .toDeezerCatalogReference(),
                                                    ) ?: return@launch

                                                playerConnection.player.setMediaItem(selected)
                                                playerConnection.player.prepare()
                                                playerConnection.player.playWhenReady = true
                                            } finally {
                                                resolvingCatalogKey = null
                                            }
                                        }
                                    }
                                }
                            }

                        else -> null
                    },
            )
        }

        if (state.isLoadingMore) {
            item(key = "loading_more") {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun UnifiedTrackRow(
    result: UnifiedTrackSearchResult,
    isActive: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    onClick: (() -> Unit)?,
) {
    val recommended = result.recommendedVariant
    val artwork =
        recommended?.artworkUrl
            ?: result.variants.firstNotNullOfOrNull { it.artworkUrl }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = onClick != null,
                    onClick = { onClick?.invoke() },
                ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(10.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                if (artwork != null) {
                    AsyncImage(
                        model = artwork,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.library_music),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(26.dp),
                    )
                }

                if (isActive) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.28f)),
                    )
                    Icon(
                        painter =
                            painterResource(
                                if (isPlaying) R.drawable.pause else R.drawable.play,
                            ),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                } else if (isResolving) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.24f)),
                    )
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = Color.White,
                        modifier = Modifier.size(25.dp),
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = result.identity.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color =
                        if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = result.identity.artists.joinToString { it.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text =
                        result.variants
                            .distinctBy { it.provider }
                            .joinToString(" • ") { variant -> providerName(variant.provider) },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.76f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                qualitySummary(recommended)?.let { quality ->
                    Text(
                        text = quality,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            val displayProvider =
                recommended?.provider
                    ?: result.variants
                        .firstOrNull { it.provider == ProviderId.DEEZER }
                        ?.provider

            if (displayProvider != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = providerShortName(displayProvider),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                )
            }
        }
    }
}

private fun providerName(provider: ProviderId): String =
    when (provider) {
        ProviderId.YOUTUBE_MUSIC -> "YouTube Music"
        ProviderId.LOCAL -> "Local"
        ProviderId.DEEZER -> "Deezer"
        ProviderId.SPOTIFY -> "Spotify"
        else -> provider.value
    }

private fun providerShortName(provider: ProviderId): String =
    when (provider) {
        ProviderId.YOUTUBE_MUSIC -> "YT Music"
        ProviderId.LOCAL -> "Local"
        ProviderId.DEEZER -> "Deezer"
        ProviderId.SPOTIFY -> "Spotify"
        else -> provider.value
    }

private fun qualitySummary(variant: SourceVariant?): String? {
    val quality = variant?.quality ?: return null
    val parts = mutableListOf<String>()

    if (quality.lossless == true) {
        parts += "Lossless"
    }
    quality.bitDepth?.takeIf { it > 0 }?.let { parts += "$it-bit" }
    quality.sampleRateHz
        ?.takeIf { it > 0 }
        ?.let { sampleRate ->
            val khz = sampleRate / 1000.0
            parts +=
                if (sampleRate % 1000 == 0) {
                    (sampleRate / 1000).toString() + " kHz"
                } else {
                    String.format(java.util.Locale.US, "%.1f kHz", khz)
                }
        }
    quality.bitrateBitsPerSecond
        ?.takeIf { it > 0 }
        ?.let { bitrate -> parts += (bitrate / 1000).toString() + " kbps" }

    return parts.takeIf { it.isNotEmpty() }?.joinToString(" • ")
}

private fun SourceVariant.toOpenTuneMetadata(): MediaMetadata {
    val durationSeconds =
        durationMs
            ?.takeIf { it >= 0L }
            ?.div(1_000L)
            ?.coerceAtMost(Int.MAX_VALUE.toLong())
            ?.toInt()
            ?: MediaMetadata.UNKNOWN_DURATION

    return MediaMetadata(
        id = sourceId,
        title = title,
        artists =
            artists.map { artist ->
                MediaMetadata.Artist(
                    id = null,
                    name = artist,
                )
            },
        duration = durationSeconds,
        thumbnailUrl = artworkUrl,
        album =
            albumId?.let { id ->
                MediaMetadata.Album(
                    id = id,
                    title = albumName.orEmpty(),
                )
            },
        explicit = explicit ?: false,
    )
}

private fun SourceVariant.toPlayableMediaItem(): MediaItem {
    val metadata = toOpenTuneMetadata()

    return MediaItem
        .Builder()
        .setMediaId(sourceId)
        .setUri(sourceId)
        .setCustomCacheKey(sourceId)
        .setTag(metadata)
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata
                .Builder()
                .setTitle(title)
                .setSubtitle(artists.joinToString())
                .setArtist(artists.joinToString())
                .setArtworkUri(artworkUrl?.toUri())
                .setAlbumTitle(albumName)
                .setMediaType(MEDIA_TYPE_MUSIC)
                .setExtras(
                    Bundle().apply {
                        putBoolean(
                            ExtraIsMusicVideo,
                            TrackVersionTrait.MUSIC_VIDEO in version.traits,
                        )
                    },
                ).build(),
        ).build()
}
