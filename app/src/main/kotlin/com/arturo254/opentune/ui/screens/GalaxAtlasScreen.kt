package com.arturo254.opentune.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.core.designsystem.theme.galaxScreenBackground
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.innertube.models.SongItem
import com.arturo254.opentune.models.toMediaMetadata
import com.arturo254.opentune.playback.queues.ListQueue
import com.arturo254.opentune.playback.queues.YouTubeQueue
import com.arturo254.opentune.viewmodels.HomeViewModel
import com.arturo254.opentune.viewmodels.MoodAndGenresViewModel

@Composable
fun GalaxDiscoverScreen(
    navController: NavController,
    onOpenSearch: () -> Unit,
    genresModel: MoodAndGenresViewModel = hiltViewModel(),
    home: HomeViewModel = hiltViewModel(),
    ambientActive: Boolean = true,
) {
    val genres by genresModel.moodAndGenres.collectAsState()
    val loadingGenres by genresModel.isLoading.collectAsState()
    val failedGenres by genresModel.failed.collectAsState()
    val recommendations by home.quickPicks.collectAsState()
    val similar by home.similarRecommendations.collectAsState()
    val player = LocalPlayerConnection.current
    val metadata = player?.mediaMetadata?.collectAsState()?.value
    val playing = player?.isPlaying?.collectAsState()?.value == true
    val state = rememberLazyListState()

    val genreTiles = remember(genres) {
        genres.orEmpty()
            .distinctBy { it.endpoint.browseId + ":" + it.title }
            .take(16)
    }
    val localSongs = remember(recommendations) {
        recommendations.orEmpty().distinctBy { it.id }.take(14)
    }
    val remoteSongs = remember(similar) {
        similar.orEmpty().flatMap { it.items }
            .filterIsInstance<SongItem>()
            .distinctBy { it.id }
            .take(14)
    }
    val suggestions = remember {
        listOf(
            "Pop" to Color(0xFF7CA3D6),
            "Electrónica" to Color(0xFF8C78CA),
            "Rock" to Color(0xFF557FAD),
            "R&B" to Color(0xFFB285BC),
            "Hip-hop" to Color(0xFF6583B9),
            "Jazz" to Color(0xFF718FB7),
        )
    }
    val playRemote: (SongItem) -> Unit = { song ->
        if (metadata?.id == song.id) {
            player?.player?.togglePlayPause()
        } else {
            player?.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
        }
    }
    val playLocal: (Song) -> Unit = { song ->
        if (metadata?.id == song.id) {
            player?.player?.togglePlayPause()
        } else {
            player?.playQueue(
                ListQueue(
                    title = "Descubrir",
                    items = localSongs.map { it.toMediaItem() },
                    startIndex = localSongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0),
                ),
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().galaxScreenBackground(),
    ) {
        AetherAmbient(
            modifier = Modifier.fillMaxSize(),
            active = ambientActive,
            variant = 1,
            intensity = .76f,
        )
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            verticalArrangement = Arrangement.spacedBy(21.dp),
        ) {
            item(key = "atlas_masthead") {
                AtlasTitleBar(onSearch = onOpenSearch)
            }
            item(key = "atlas_launch") {
                AetherReveal("atlas-open") {
                    AtlasLaunchStage(
                        active = ambientActive,
                        onSearch = onOpenSearch,
                    )
                }
            }
            item(key = "atlas_gate_title") {
                AetherSectionTitle(
                    title = "Explorar",
                    eyebrow = "01 / NAVEGAR",
                )
            }
            item(key = "atlas_gateways") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 19.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AtlasGateway(
                        title = "Éxitos",
                        subtitle = "Charts",
                        icon = R.drawable.trending_up,
                        tint = AetherInk.cyan,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("charts_screen") },
                    )
                    AtlasGateway(
                        title = "Novedades",
                        subtitle = "Estrenos",
                        icon = R.drawable.album,
                        tint = AetherInk.lilac,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("new_release") },
                    )
                }
            }
            item(key = "atlas_genre_title") {
                AetherSectionTitle(
                    title = "Géneros",
                    eyebrow = "02 / FRECUENCIAS",
                )
            }
            if (genreTiles.isNotEmpty()) {
                itemsIndexed(
                    items = genreTiles.chunked(2),
                    key = { index, _ -> galaxDiscoveryGenreRowKey(index, fallback = false) },
                    contentType = { _, _ -> "atlas_genres" },
                ) { index, pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 19.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        pair.forEachIndexed { column, genre ->
                            AtlasGenreTile(
                                title = genre.title,
                                tint = Color(genre.stripeColor),
                                large = (index + column) % 3 == 0,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    navController.navigate(
                                        "youtube_browse/${genre.endpoint.browseId}?params=${Uri.encode(genre.endpoint.params.orEmpty())}"
                                    )
                                },
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            } else if (!loadingGenres) {
                itemsIndexed(
                    items = suggestions.chunked(2),
                    key = { index, _ -> galaxDiscoveryGenreRowKey(index, fallback = true) },
                    contentType = { _, _ -> "atlas_fallback" },
                ) { index, pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 19.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        pair.forEachIndexed { column, genre ->
                            AtlasGenreTile(
                                title = genre.first,
                                tint = genre.second,
                                large = (index + column) % 3 == 0,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    navController.navigate("search/${Uri.encode(genre.first)}")
                                },
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            if (loadingGenres) {
                item(key = "atlas_wait_genres") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(Modifier.height(24.dp), strokeWidth = 2.dp)
                    }
                }
            }
            if (failedGenres) {
                item(key = "atlas_retry") {
                    TextButton(
                        onClick = genresModel::refresh,
                        modifier = Modifier.padding(horizontal = 19.dp),
                    ) {
                        Text("Reintentar")
                    }
                }
            }
            if (genreTiles.size >= 8) {
                item(key = "atlas_more_genres") {
                    AtlasAllGenres {
                        navController.navigate(Screens.MoodAndGenres.route)
                    }
                }
            }
            if (remoteSongs.isNotEmpty() || localSongs.isNotEmpty()) {
                item(key = "atlas_songs_title") {
                    AetherSectionTitle(
                        title = "Para tus oídos",
                        eyebrow = "03 / SELECCIÓN",
                    )
                }
            }
            if (remoteSongs.isNotEmpty()) {
                val feature = remoteSongs.first()
                item(key = "atlas_featured_track") {
                    AtlasFeaturedSong(
                        title = feature.title,
                        artist = feature.artists.joinToString { it.name },
                        artwork = feature.thumbnail,
                        active = metadata?.id == feature.id,
                        playing = playing,
                        onClick = { playRemote(feature) },
                    )
                }
                itemsIndexed(
                    items = remoteSongs.drop(1),
                    key = { index, _ -> "atlas_remote:" + index },
                    contentType = { _, _ -> "atlas_song" },
                ) { index, song ->
                    AtlasSongEntry(
                        title = song.title,
                        artist = song.artists.joinToString { it.name },
                        artwork = song.thumbnail,
                        active = metadata?.id == song.id,
                        playing = playing,
                        index = index + 1,
                        onPlay = { playRemote(song) },
                    )
                }
            } else if (localSongs.isNotEmpty()) {
                val feature = localSongs.first()
                item(key = "atlas_local_featured") {
                    AtlasFeaturedSong(
                        title = feature.title,
                        artist = feature.artists.joinToString { it.name },
                        artwork = feature.thumbnailUrl,
                        active = metadata?.id == feature.id,
                        playing = playing,
                        onClick = { playLocal(feature) },
                    )
                }
                itemsIndexed(
                    items = localSongs.drop(1),
                    key = { index, _ -> "atlas_local:" + index },
                    contentType = { _, _ -> "atlas_song" },
                ) { index, song ->
                    AtlasSongEntry(
                        title = song.title,
                        artist = song.artists.joinToString { it.name },
                        artwork = song.thumbnailUrl,
                        active = metadata?.id == song.id,
                        playing = playing,
                        index = index + 1,
                        onPlay = { playLocal(song) },
                    )
                }
            }
            if (remoteSongs.isEmpty() && localSongs.isEmpty() && !loadingGenres) {
                item(key = "atlas_empty") {
                    AtlasEmptyState(onSearch = onOpenSearch)
                }
            }
            item(key = "atlas_spacer") {
                Spacer(Modifier.height(37.dp))
            }
        }
    }
}

internal fun galaxDiscoveryGenreRowKey(index: Int, fallback: Boolean): String =
    (if (fallback) "discovery-fallback:" else "discovery-genre:") + index
