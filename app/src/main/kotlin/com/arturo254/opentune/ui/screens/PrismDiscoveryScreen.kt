package com.arturo254.opentune.ui.screens

import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.arturo254.opentune.LocalDatabase
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.core.designsystem.liquid.galaxLiquidEffect
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.innertube.models.SongItem
import com.arturo254.opentune.models.toMediaMetadata
import com.arturo254.opentune.playback.queues.ListQueue
import com.arturo254.opentune.playback.queues.YouTubeQueue
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.menu.SongMenu
import com.arturo254.opentune.ui.menu.YouTubeSongMenu
import com.arturo254.opentune.ui.screens.search.*
import com.arturo254.opentune.viewmodels.HomeViewModel
import com.arturo254.opentune.viewmodels.MoodAndGenresViewModel

/** Search starts with an editorial mosaic, never the Home's featured-track layout. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PrismDiscoveryScreen(
    navController: NavController,
    onOpenSearch: () -> Unit,
    genresModel: MoodAndGenresViewModel,
    home: HomeViewModel,
    ambientActive: Boolean,
) {
    val genres by genresModel.moodAndGenres.collectAsStateWithLifecycle()
    val loading by genresModel.isLoading.collectAsStateWithLifecycle()
    val failed by genresModel.failed.collectAsStateWithLifecycle()
    val picks by home.quickPicks.collectAsStateWithLifecycle()
    val recommendations by home.similarRecommendations.collectAsStateWithLifecycle()
    val database = LocalDatabase.current
    val history by
        remember(database) { database.searchHistory() }.collectAsStateWithLifecycle(emptyList())
    val player = LocalPlayerConnection.current
    val current = player?.mediaMetadata?.collectAsStateWithLifecycle()?.value
    val menus = LocalMenuState.current
    val list = rememberLazyListState()
    val queueTitle = stringResource(R.string.prism_explore)
    val remote =
        remember(recommendations) {
            recommendations
                .orEmpty()
                .flatMap { it.items }
                .filterIsInstance<SongItem>()
                .distinctBy { it.id }
                .take(10)
        }
    val local = remember(picks) { picks.orEmpty().distinctBy { it.id }.take(10) }
    val tiles =
        remember(genres) {
            genres
                .orEmpty()
                .distinctBy { it.endpoint.browseId + it.endpoint.params + it.title }
                .take(8)
        }
    val fallback =
        listOf(
                R.string.prism_electronic,
                R.string.prism_jazz,
                R.string.prism_pop,
                R.string.prism_rock,
                R.string.prism_soul,
                R.string.prism_hiphop,
            )
            .map { stringResource(it) }
    val backStack by navController.currentBackStackEntryAsState()
    val top = backStack?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()
    LaunchedEffect(top?.value, ambientActive) {
        if (ambientActive && top?.value == true) {
            list.animateScrollToItem(0)
            backStack?.savedStateHandle?.set("scrollToTop", false)
        }
    }
    PrismScreenFrame(active = ambientActive) { layout, padding ->
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (layout.compactHeight && layout.columns >= 3) {
                item("prism/console") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { PrismDiscoveryMasthead(true) }
                        Box(Modifier.weight(1f)) { PrismLauncher(onOpenSearch) }
                    }
                }
            } else {
                item("prism/heading") { PrismDiscoveryMasthead(layout.compactHeight) }
                item("prism/search") { PrismLauncher(onOpenSearch) }
            }
            item("prism/destinations") {
                val cards: @Composable RowScope.() -> Unit = {
                    PrismDestination(
                        stringResource(R.string.prism_charts),
                        stringResource(R.string.prism_charts_detail),
                        R.drawable.trending_up,
                        Modifier.weight(1f),
                    ) {
                        navController.navigate("charts_screen")
                    }
                    PrismDestination(
                        stringResource(R.string.prism_releases),
                        stringResource(R.string.prism_releases_detail),
                        R.drawable.album,
                        Modifier.weight(1f),
                    ) {
                        navController.navigate("new_release")
                    }
                }
                if (layout.columns > 1)
                    Row(
                        Modifier.padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        content = cards,
                    )
                else
                    Column(
                        Modifier.padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        PrismDestination(
                            stringResource(R.string.prism_charts),
                            stringResource(R.string.prism_charts_detail),
                            R.drawable.trending_up,
                            Modifier.fillMaxWidth(),
                        ) {
                            navController.navigate("charts_screen")
                        }
                        PrismDestination(
                            stringResource(R.string.prism_releases),
                            stringResource(R.string.prism_releases_detail),
                            R.drawable.album,
                            Modifier.fillMaxWidth(),
                        ) {
                            navController.navigate("new_release")
                        }
                    }
            }
            if (history.isNotEmpty()) {
                item("prism/history-title") {
                    PrismSectionTitle(stringResource(R.string.prism_recent_searches))
                }
                item("prism/history") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(history.distinctBy { it.query }.take(8), key = { it.id }) { entry ->
                            SuggestionChip(
                                onClick = {
                                    navController.navigate("search/${Uri.encode(entry.query)}")
                                },
                                label = { Text(entry.query) },
                                icon = {
                                    Icon(
                                        painterResource(R.drawable.history),
                                        null,
                                        Modifier.size(18.dp),
                                    )
                                },
                                shape = RoundedCornerShape(16.dp),
                            )
                        }
                    }
                }
            }
            item("prism/genres-heading") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        PrismSectionTitle(
                            stringResource(R.string.prism_genres),
                            stringResource(R.string.prism_genres_detail),
                        )
                    }
                    TextButton(
                        onClick = { navController.navigate(Screens.MoodAndGenres.route) },
                        modifier = Modifier.padding(end = 16.dp),
                    ) {
                        Text(stringResource(R.string.prism_see_all))
                    }
                }
            }
            itemsIndexed(
                tiles.chunked(layout.columns),
                key = { index, _ -> galaxDiscoveryGenreRowKey(index, false) },
            ) { row, entries ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    entries.forEachIndexed { column, genre ->
                        PrismGenreTile(
                            genre.title,
                            Color(genre.stripeColor),
                            row * layout.columns + column,
                            onClick = {
                                navController.navigate(
                                    "youtube_browse/${genre.endpoint.browseId}?params=${Uri.encode(genre.endpoint.params.orEmpty())}"
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(layout.columns - entries.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (loading && tiles.isEmpty())
                item("prism/loading") {
                    LinearProgressIndicator(Modifier.padding(horizontal = 24.dp).fillMaxWidth())
                }
            if (!loading && tiles.isEmpty()) {
                itemsIndexed(
                    fallback.chunked(layout.columns),
                    key = { index, _ -> galaxDiscoveryGenreRowKey(index, true) },
                ) { row, entries ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        entries.forEachIndexed { column, genre ->
                            PrismGenreTile(
                                genre,
                                MaterialTheme.colorScheme.tertiary,
                                row * layout.columns + column,
                                { navController.navigate("search/${Uri.encode(genre)}") },
                                Modifier.weight(1f),
                            )
                        }
                        repeat(layout.columns - entries.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            if (failed)
                item("prism/retry") {
                    PrismStatus(
                        stringResource(R.string.prism_catalog_failed),
                        stringResource(R.string.prism_catalog_failed_detail),
                        stringResource(R.string.prism_retry),
                        genresModel::refresh,
                    )
                }
            if (remote.isNotEmpty() || local.isNotEmpty()) {
                item("prism/recommendations-heading") {
                    PrismSectionTitle(
                        stringResource(R.string.prism_unexpected),
                        stringResource(R.string.prism_unexpected_detail),
                    )
                }
                item("prism/recommendations") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (remote.isNotEmpty())
                            items(remote, key = { "remote/${it.id}" }) { song ->
                                Column(
                                    Modifier.width(layout.tileWidth.coerceIn(140.dp, 220.dp))
                                        .combinedClickable(
                                            onClick = {
                                                if (current?.id == song.id)
                                                    player?.player?.togglePlayPause()
                                                else
                                                    player?.playQueue(
                                                        YouTubeQueue.radio(song.toMediaMetadata())
                                                    )
                                            },
                                            onLongClick = {
                                                menus.show {
                                                    YouTubeSongMenu(
                                                        song = song,
                                                        navController = navController,
                                                        onDismiss = menus::dismiss,
                                                    )
                                                }
                                            },
                                        ),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    PrismArtwork(
                                        song.thumbnail,
                                        Modifier.fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(18.dp)),
                                    )
                                    Text(
                                        song.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 2,
                                    )
                                    Text(
                                        song.artists.joinToString { it.name },
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        else
                            items(local, key = { "local/${it.id}" }) { song ->
                                Column(
                                    Modifier.width(layout.tileWidth.coerceIn(140.dp, 220.dp))
                                        .combinedClickable(
                                            onClick = {
                                                if (current?.id == song.id)
                                                    player?.player?.togglePlayPause()
                                                else
                                                    player?.playQueue(
                                                        ListQueue(
                                                            title = queueTitle,
                                                            items = local.map { it.toMediaItem() },
                                                            startIndex =
                                                                local
                                                                    .indexOfFirst {
                                                                        it.id == song.id
                                                                    }
                                                                    .coerceAtLeast(0),
                                                        )
                                                    )
                                            },
                                            onLongClick = {
                                                menus.show {
                                                    SongMenu(
                                                        originalSong = song,
                                                        navController = navController,
                                                        onDismiss = menus::dismiss,
                                                    )
                                                }
                                            },
                                        ),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    PrismArtwork(
                                        song.thumbnailUrl,
                                        Modifier.fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(18.dp)),
                                    )
                                    Text(
                                        song.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 2,
                                    )
                                    Text(
                                        song.artists.joinToString { it.name },
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrismLauncher(onOpenSearch: () -> Unit) {
    Row(
        Modifier.padding(horizontal = 24.dp)
            .fillMaxWidth()
            .galaxLiquidEffect(shapeOverride = RoundedCornerShape(22.dp))
            .clickable(role = Role.Button, onClick = onOpenSearch)
            .heightIn(min = 60.dp)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(R.drawable.search),
            null,
            Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.tertiary,
        )
        Text(
            stringResource(R.string.prism_launch_search),
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Icon(painterResource(R.drawable.arrow_forward), null, Modifier.size(20.dp))
    }
}

@Composable
private fun PrismDestination(
    title: String,
    detail: String,
    icon: Int,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .galaxLiquidEffect(shapeOverride = RoundedCornerShape(24.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painterResource(icon),
            null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(24.dp),
        )
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal fun galaxDiscoveryGenreRowKey(index: Int, fallback: Boolean): String =
    (if (fallback) "discovery-fallback:" else "discovery-genre:") + index
