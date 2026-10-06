package com.arturo254.opentune.ui.screens.search

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.catalog.CatalogPlaybackResolver
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.sources.spotify.toSpotifyCatalogReference
import com.arturo254.opentune.ui.component.LiquidGlassCard
import com.arturo254.opentune.ui.component.liquidGlassScreenBackground
import com.arturo254.opentune.utils.joinByBullet
import com.arturo254.opentune.utils.makeTimeString
import com.arturo254.opentune.viewmodels.SpotifySearchViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

@Composable
fun SpotifySearchScreen(
    query: String,
    pureBlack: Boolean,
    viewModel: SpotifySearchViewModel = hiltViewModel(),
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val state by viewModel.state.collectAsState()
    val activeMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val scope = rememberCoroutineScope()
    var resolvingId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        viewModel.query.value = query
    }

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
        item(key = "spotify_header") {
            LiquidGlassCard(
                cornerRadius = 24.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.spotify_icon),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(25.dp),
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.provider_spotify),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text =
                                if (state.isConnected) {
                                    stringResource(R.string.spotify_source_ready)
                                } else {
                                    stringResource(R.string.spotify_not_connected)
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (state.loading && state.tracks.isEmpty()) {
            item(key = "spotify_loading") {
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

        state.error?.let {
            item(key = "spotify_error") {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                )
            }
        }

        if (
            state.isConnected &&
            query.isNotBlank() &&
            !state.loading &&
            state.error == null &&
            state.tracks.isEmpty()
        ) {
            item(key = "spotify_empty") {
                Text(
                    text = stringResource(R.string.no_results_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
                )
            }
        }

        itemsIndexed(
            items = state.tracks,
            key = { _, track -> track.sourceId },
        ) { index, track ->
            val active =
                activeMetadata?.title.equals(track.title, ignoreCase = true) &&
                    track.artists.any { sourceArtist ->
                        activeMetadata?.artists
                            ?.any { it.name.equals(sourceArtist, ignoreCase = true) } == true
                    }

            SpotifyTrackRow(
                track = track,
                isActive = active,
                isPlaying = active && isPlaying,
                isResolving = resolvingId == track.sourceId,
                onClick = {
                    if (active) {
                        playerConnection.player.togglePlayPause()
                        return@SpotifyTrackRow
                    }
                    if (resolvingId != null) return@SpotifyTrackRow

                    scope.launch {
                        resolvingId = track.sourceId
                        try {
                            val selected =
                                CatalogPlaybackResolver.resolveToMediaItem(
                                    track.toSpotifyCatalogReference(),
                                ) ?: return@launch

                            playerConnection.player.setMediaItem(selected)
                            playerConnection.player.prepare()
                            playerConnection.player.playWhenReady = true

                            val currentMediaId = selected.mediaId
                            val remainingTracks =
                                (state.tracks.drop(index + 1) + state.tracks.take(index))
                                    .take(10)

                            launch {
                                val resolved =
                                    remainingTracks
                                        .map { candidate ->
                                            async {
                                                CatalogPlaybackResolver.resolveToMediaItem(
                                                    candidate.toSpotifyCatalogReference(),
                                                )
                                            }
                                        }.awaitAll()
                                        .filterNotNull()

                                if (
                                    resolved.isNotEmpty() &&
                                    playerConnection.player.currentMediaItem?.mediaId == currentMediaId
                                ) {
                                    playerConnection.player.addMediaItems(resolved)
                                }
                            }
                        } finally {
                            resolvingId = null
                        }
                    }
                },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun SpotifyTrackRow(
    track: SourceVariant,
    isActive: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlassCard(
        cornerRadius = 22.dp,
        modifier =
            modifier
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
                AsyncImage(
                    model = track.artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

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
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color =
                        if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
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

                Text(
                    text =
                        joinByBullet(
                            track.albumName,
                            track.durationMs
                                ?.takeIf { it > 0L }
                                ?.let(::makeTimeString),
                        ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                painter = painterResource(R.drawable.spotify_icon),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
