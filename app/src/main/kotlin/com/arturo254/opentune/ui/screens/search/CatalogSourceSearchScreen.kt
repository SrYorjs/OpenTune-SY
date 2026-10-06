package com.arturo254.opentune.ui.screens.search

import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import coil3.compose.AsyncImage
import com.arturo254.opentune.LocalDatabase
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.catalog.CatalogPlaybackResolver
import com.arturo254.opentune.catalog.CatalogTrackReference
import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.playback.ExoDownloadService
import com.arturo254.opentune.sources.apple.toAppleMusicCatalogReference
import com.arturo254.opentune.sources.tidal.toTidalCatalogReference
import com.arturo254.opentune.ui.component.liquidGlassScreenBackground
import com.arturo254.opentune.utils.makeTimeString
import com.arturo254.opentune.viewmodels.AppleMusicSearchViewModel
import com.arturo254.opentune.viewmodels.CatalogProviderSearchState
import com.arturo254.opentune.viewmodels.TidalSearchViewModel
import kotlinx.coroutines.launch

@Composable
fun TidalSearchScreen(
    query: String,
    pureBlack: Boolean,
    viewModel: TidalSearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(query) {
        viewModel.query.value = query
    }

    CatalogSourceSearchContent(
        state = state,
        provider = ProviderId.TIDAL,
        pureBlack = pureBlack,
        allowOfflineDownload = true,
    )
}

@Composable
fun AppleMusicSearchScreen(
    query: String,
    pureBlack: Boolean,
    viewModel: AppleMusicSearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(query) {
        viewModel.query.value = query
    }

    CatalogSourceSearchContent(
        state = state,
        provider = ProviderId.APPLE_MUSIC,
        pureBlack = pureBlack,
        allowOfflineDownload = false,
    )
}

@Composable
private fun CatalogSourceSearchContent(
    state: CatalogProviderSearchState,
    provider: ProviderId,
    pureBlack: Boolean,
    allowOfflineDownload: Boolean,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val activeMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val scope = rememberCoroutineScope()

    var resolvingPlaybackId by remember { mutableStateOf<String?>(null) }
    var resolvingDownloadId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
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
                .liquidGlassScreenBackground(pureBlack),
    ) {
        if (state.loading && state.tracks.isEmpty()) {
            item(key = "catalog_loading") {
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

        state.error?.let { error ->
            item(key = "catalog_error") {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                )
            }
        }

        items(
            items = state.tracks,
            key = { track -> track.provider.value + ":" + track.sourceId },
        ) { track ->
            val active =
                activeMetadata?.title.equals(track.title, ignoreCase = true) &&
                    track.artists.any { sourceArtist ->
                        activeMetadata?.artists
                            ?.any { it.name.equals(sourceArtist, ignoreCase = true) } == true
                    }

            CatalogSourceTrackRow(
                track = track,
                isActive = active,
                isPlaying = active && isPlaying,
                isResolvingPlayback = resolvingPlaybackId == track.sourceId,
                isResolvingDownload = resolvingDownloadId == track.sourceId,
                allowOfflineDownload = allowOfflineDownload,
                onClick = {
                    if (active) {
                        playerConnection.player.togglePlayPause()
                        return@CatalogSourceTrackRow
                    }
                    if (resolvingPlaybackId != null) return@CatalogSourceTrackRow

                    scope.launch {
                        resolvingPlaybackId = track.sourceId
                        try {
                            val selected =
                                CatalogPlaybackResolver.resolveToMediaItem(
                                    track.toCatalogReference(),
                                ) ?: run {
                                    Toast
                                        .makeText(
                                            context,
                                            R.string.catalog_source_unplayable,
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    return@launch
                                }

                            playerConnection.player.setMediaItem(selected)
                            playerConnection.player.prepare()
                            playerConnection.player.playWhenReady = true
                        } finally {
                            resolvingPlaybackId = null
                        }
                    }
                },
                onDownload =
                    if (allowOfflineDownload) {
                        {
                            if (resolvingDownloadId == null) {
                                scope.launch {
                                    resolvingDownloadId = track.sourceId
                                    try {
                                        val metadata =
                                            CatalogPlaybackResolver.resolveToMetadata(
                                                track.toCatalogReference(),
                                            ) ?: run {
                                                Toast
                                                    .makeText(
                                                        context,
                                                        R.string.catalog_source_unplayable,
                                                        Toast.LENGTH_SHORT,
                                                    ).show()
                                                return@launch
                                            }

                                        database.transaction {
                                            insert(metadata)
                                        }

                                        val request =
                                            DownloadRequest
                                                .Builder(
                                                    metadata.id,
                                                    metadata.id.toUri(),
                                                ).setCustomCacheKey(metadata.id)
                                                .setData(metadata.title.toByteArray())
                                                .build()

                                        DownloadService.sendAddDownload(
                                            context,
                                            ExoDownloadService::class.java,
                                            request,
                                            false,
                                        )

                                        Toast
                                            .makeText(
                                                context,
                                                R.string.catalog_offline_download_started,
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                    } finally {
                                        resolvingDownloadId = null
                                    }
                                }
                            }
                        }
                    } else {
                        null
                    },
            )
        }
    }
}

@Composable
private fun CatalogSourceTrackRow(
    track: SourceVariant,
    isActive: Boolean,
    isPlaying: Boolean,
    isResolvingPlayback: Boolean,
    isResolvingDownload: Boolean,
    allowOfflineDownload: Boolean,
    onClick: () -> Unit,
    onDownload: (() -> Unit)?,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
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
                if (track.artworkUrl != null) {
                    AsyncImage(
                        model = track.artworkUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.library_music),
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                    )
                }

                when {
                    isActive -> {
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
                    }

                    isResolvingPlayback -> {
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
            }

            Spacer(Modifier.width(14.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = track.artists.joinToString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val detail =
                    listOfNotNull(
                        track.albumName?.takeIf(String::isNotBlank),
                        track.durationMs?.takeIf { it > 0L }?.let(::makeTimeString),
                        track.isrc?.takeIf(String::isNotBlank)?.let { "ISRC $it" },
                    ).joinToString(" • ")
                if (detail.isNotBlank()) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.76f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (allowOfflineDownload && onDownload != null) {
                IconButton(
                    onClick = onDownload,
                    enabled = !isResolvingDownload,
                ) {
                    if (isResolvingDownload) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp),
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.download),
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }
}

private fun SourceVariant.toCatalogReference(): CatalogTrackReference =
    when (provider) {
        ProviderId.TIDAL -> toTidalCatalogReference()
        ProviderId.APPLE_MUSIC -> toAppleMusicCatalogReference()
        else -> error("Unsupported catalog provider: " + provider.value)
    }
