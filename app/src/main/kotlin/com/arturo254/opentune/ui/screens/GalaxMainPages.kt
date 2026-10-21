package com.arturo254.opentune.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.arturo254.opentune.ui.screens.library.LibraryScreen
import com.arturo254.opentune.viewmodels.HomeViewModel
import com.arturo254.opentune.viewmodels.MoodAndGenresViewModel
import kotlinx.coroutines.launch

@Composable
fun GalaxMainPages(
    navController: NavController,
    pager: PagerState,
    onOpenSearch: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    BackHandler(enabled = pager.currentPage != 0) {
        scope.launch { pager.animateScrollToPage(0) }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(state = pager,
            key = { Screens.MainScreens[it].route },
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (page) {
                0 -> HomeScreen(
                    navController = navController,
                    ambientActive = pager.currentPage == 0,
                    onOpenSearchTab = {
                        scope.launch { pager.animateScrollToPage(1) }
                    },
                    onOpenLibraryTab = {
                        scope.launch { pager.animateScrollToPage(2) }
                    },
                )
                1 -> GalaxDiscoverScreen(
                    navController = navController,
                    onOpenSearch = onOpenSearch,
                    ambientActive = pager.currentPage == 1,
                )
                2 -> LibraryScreen(
                    navController = navController,
                    ambientActive = pager.currentPage == 2,
                    onOpenSearchTab = {
                        scope.launch { pager.animateScrollToPage(1) }
                    },
                )
            }
        }
        val backdrop = MaterialTheme.colorScheme.background
        Box(
            modifier = Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth().height(88.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            backdrop.copy(alpha = .96f),
                        ),
                    ),
                ),
        )
    }
}

@Composable
fun GalaxDiscoverScreen(
    navController: NavController,
    onOpenSearch: () -> Unit,
    genresModel: MoodAndGenresViewModel = hiltViewModel(),
    home: HomeViewModel = hiltViewModel(),
    ambientActive: Boolean = true,
) {
    val genres by genresModel.moodAndGenres.collectAsState()
    val genresLoading by genresModel.isLoading.collectAsState()
    val genresFailed by genresModel.failed.collectAsState()
    val similar by home.similarRecommendations.collectAsState()
    val recommendations by home.quickPicks.collectAsState()
    val player = LocalPlayerConnection.current
    val current = player?.mediaMetadata?.collectAsState()
    val playback = player?.isPlaying?.collectAsState()
    val now = current?.value
    val playing = playback?.value == true

    val discoveries = remember(similar) {
        similar.orEmpty()
            .flatMap { it.items }
            .filterIsInstance<SongItem>()
            .distinctBy { it.id }
            .take(12)
    }
    val local = remember(recommendations) {
        recommendations.orEmpty()
            .distinctBy { it.id }
            .take(12)
    }
    val categories = remember(genres) {
        genres.orEmpty()
            .distinctBy { it.endpoint.browseId + "/" + it.title }
            .take(10)
    }
    val fallbackCategories = remember {
        listOf(
            "Pop" to 0xFF6F8DD1,
            "Rock" to 0xFF8E7AA9,
            "Hip-hop" to 0xFF6363A9,
            "Electrónica" to 0xFF487A9E,
        )
    }

    val onPlayRemote: (SongItem) -> Unit = { track ->
        if (now?.id == track.id) {
            player?.player?.togglePlayPause()
        } else {
            player?.playQueue(YouTubeQueue.radio(track.toMediaMetadata()))
        }
    }
    val onPlayLocal: (Song) -> Unit = { song ->
        if (now?.id == song.id) {
            player?.player?.togglePlayPause()
        } else {
            player?.playQueue(
                ListQueue(
                    title = "Para ti",
                    items = local.map { it.toMediaItem() },
                    startIndex = local.indexOfFirst { it.id == song.id }.coerceAtLeast(0),
                ),
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().galaxScreenBackground(),
    ) {
        GalaxEditorialBackdrop(
            modifier = Modifier.fillMaxSize(),
            active = ambientActive,
            density = 32,
            intensity = .73f,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = "discovery_hero") {
                GalaxDiscoveryHero(
                    onSearch = onOpenSearch,
                    active = ambientActive,
                )
            }
            item(key = "discovery_navigation_title") {
                GalaxEditorialHeading("Explorar")
            }
            item(key = "discovery_shortcuts") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GalaxDiscoveryShortcut(
                        title = "Éxitos",
                        icon = R.drawable.trending_up,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("charts_screen") },
                    )
                    GalaxDiscoveryShortcut(
                        title = "Novedades",
                        icon = R.drawable.album,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("new_release") },
                    )
                }
            }
            item(key = "discovery_genre_title") {
                GalaxEditorialHeading(stringResource(R.string.mood_and_genres))
            }
            if (categories.isNotEmpty()) {
                itemsIndexed(
                    items = categories.chunked(2),
                    key = { index, _ -> galaxDiscoveryGenreRowKey(index, fallback = false) },
                    contentType = { _, _ -> "genre_pair" },
                ) { _, pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        pair.forEach { genre ->
                            GalaxDiscoveryCategory(
                                title = genre.title,
                                swatch = genre.stripeColor,
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
            } else if (!genresLoading) {
                itemsIndexed(
                    items = fallbackCategories.chunked(2),
                    key = { index, _ -> galaxDiscoveryGenreRowKey(index, fallback = true) },
                ) { _, pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        pair.forEach { genre ->
                            GalaxDiscoveryCategory(
                                title = genre.first,
                                swatch = genre.second,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    navController.navigate("search/${Uri.encode(genre.first)}")
                                },
                            )
                        }
                    }
                }
            }
            if (genresLoading) {
                item(key = "genre_loading") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(25.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                }
            }
            if (genresFailed) {
                item(key = "genre_retry") {
                    TextButton(
                        onClick = genresModel::refresh,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    ) { Text(stringResource(R.string.retry)) }
                }
            }
            if (categories.size >= 8) {
                item(key = "genres_all") {
                    GalaxDiscoveryCategoryMore(
                        onClick = { navController.navigate(Screens.MoodAndGenres.route) },
                    )
                }
            }
            if (discoveries.isNotEmpty() || local.isNotEmpty()) {
                item(key = "discovery_mix_title") {
                    GalaxEditorialHeading("Para ti")
                }
            }
            if (discoveries.isNotEmpty()) {
                items(
                    items = discoveries,
                    key = { "remote_track:" + it.id },
                    contentType = { "track" },
                ) { track ->
                    GalaxDiscoverySong(
                        title = track.title,
                        artist = track.artists.joinToString { it.name },
                        artwork = track.thumbnail,
                        active = now?.id == track.id,
                        playing = playing,
                        onClick = { onPlayRemote(track) },
                    )
                }
            } else if (local.isNotEmpty()) {
                items(
                    items = local,
                    key = { "local_track:" + it.id },
                    contentType = { "track" },
                ) { song ->
                    GalaxDiscoverySong(
                        title = song.title,
                        artist = song.artists.joinToString { it.name },
                        artwork = song.thumbnailUrl,
                        active = now?.id == song.id,
                        playing = playing,
                        onClick = { onPlayLocal(song) },
                    )
                }
            }
            if (now != null) {
                item(key = "current_discovery") {
                    GalaxDiscoveryNowPlaying(
                        title = now.title,
                        artist = now.artists.joinToString { it.name },
                        artwork = now.thumbnailUrl,
                        active = playing,
                        onClick = { player?.player?.togglePlayPause() },
                    )
                }
            }
            item(key = "discovery_end") {
                Spacer(modifier = Modifier.height(34.dp))
            }
        }
    }
}

@Composable
fun GalaxPortal(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    GalaxEditorialSurface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        radius = 22.dp,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun GalaxCollectionTile(
    title: String,
    icon: Int,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    GalaxEditorialSurface(
        modifier = modifier.height(124.dp),
        radius = 25.dp,
        accent = accent,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            androidx.compose.material3.Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(25.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
            )
        }
    }
}

internal fun galaxDiscoveryGenreRowKey(index: Int, fallback: Boolean): String =
    (if (fallback) "discovery-fallback:" else "discovery-genre:") + index
