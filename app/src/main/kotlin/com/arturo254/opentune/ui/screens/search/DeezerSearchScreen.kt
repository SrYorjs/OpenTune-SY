package com.arturo254.opentune.ui.screens.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
import com.arturo254.opentune.sources.deezer.toDeezerCatalogReference
import com.arturo254.opentune.ui.component.LiquidGlassCard
import com.arturo254.opentune.ui.component.liquidGlassScreenBackground
import com.arturo254.opentune.utils.joinByBullet
import com.arturo254.opentune.utils.makeTimeString
import com.arturo254.opentune.viewmodels.DeezerSearchViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

@Composable
fun DeezerSearchScreen(
    query: String,
    pureBlack: Boolean,
    viewModel: DeezerSearchViewModel = hiltViewModel(),
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
        item(key = "deezer_header") {
            DeezerProviderHeader(
                showingTrending = state.showingTrending,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }

        if (state.loading && state.tracks.isEmpty()) {
            item(key = "deezer_loading") {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        state.error?.let {
            item(key = "deezer_error") {
                LiquidGlassCard(
                    cornerRadius = 24.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(18.dp),
                    ) {
                        Text(
                            text = "El catálogo no respondió",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "OpenTune seguirá disponible con Biblioteca y YouTube Music.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
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

            DeezerTrackRow(
                track = track,
                isActive = active,
                isPlaying = active && isPlaying,
                isResolving = resolvingId == track.sourceId,
                onClick = {
                    if (active) {
                        playerConnection.player.togglePlayPause()
                        return@DeezerTrackRow
                    }
                    if (resolvingId != null) return@DeezerTrackRow

                    scope.launch {
                        resolvingId = track.sourceId
                        try {
                            val selected =
                                CatalogPlaybackResolver.resolveToMediaItem(
                                    track.toDeezerCatalogReference()
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
                                                    candidate.toCatalogReference()
                                                )
                                            }
                                        }
                                        .awaitAll()
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
private fun DeezerProviderHeader(
    showingTrending: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(30.dp)
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        colors =
                            listOf(
                                primary.copy(alpha = 0.20f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
                                surface.copy(alpha = 0.84f),
                            )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 19.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = "DEEZER • GLOBAL CATALOG",
                style = MaterialTheme.typography.labelMedium,
                color = primary,
                fontWeight = FontWeight.ExtraBold,
            )
            AnimatedContent(
                targetState = showingTrending,
                transitionSpec = {
                    fadeIn(animationSpec = spring()) togetherWith
                        fadeOut(animationSpec = spring())
                },
                label = "deezerHeader",
            ) { trending ->
                Text(
                    text = if (trending) "Música popular ahora" else "Catálogo global",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                )
            }
            Text(
                text = "Descubrimiento de Deezer con reproducción resuelta por el motor de OpenTune.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DeezerTrackRow(
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
                                if (isPlaying) R.drawable.pause else R.drawable.play
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

            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
            ) {
                Icon(
                    painter = painterResource(R.drawable.graphic_eq),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
