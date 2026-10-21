package com.arturo254.opentune.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.R
import com.arturo254.opentune.core.designsystem.theme.GalaxScreenHeading
import com.arturo254.opentune.core.designsystem.theme.galaxScreenBackground
import com.arturo254.opentune.innertube.models.SongItem
import com.arturo254.opentune.viewmodels.ChartsViewModel
import com.arturo254.opentune.viewmodels.ExploreViewModel

@Composable
fun ExploreScreen(navController: NavController, exploreViewModel: ExploreViewModel = hiltViewModel(),
    chartsViewModel: ChartsViewModel = hiltViewModel()) {
    val explore by exploreViewModel.explorePage.collectAsState()
    val loading by exploreViewModel.isLoading.collectAsState()
    val failed by exploreViewModel.failed.collectAsState()
    val charts by chartsViewModel.chartsPage.collectAsState()
    val chartsLoading by chartsViewModel.isLoading.collectAsState()
    val state = rememberLazyListState()
    val backStack by navController.currentBackStackEntryAsState()
    val top = backStack?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()
    LaunchedEffect(Unit) { if (charts == null) chartsViewModel.loadCharts() }
    LaunchedEffect(top?.value) {
        if (top?.value == true) { state.animateScrollToItem(0); backStack?.savedStateHandle?.set("scrollToTop", false) }
    }
    LazyColumn(Modifier.fillMaxSize().galaxScreenBackground(), state = state,
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item("heading") { GalaxScreenHeading(stringResource(R.string.explore), stringResource(R.string.galax_discover_subtitle)) }
        item("shortcuts") {
            Row(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GalaxCollectionTile(stringResource(R.string.charts), R.drawable.trending_up,
                    MaterialTheme.colorScheme.primary, Modifier.weight(1f)) { navController.navigate("charts_screen") }
                GalaxCollectionTile(stringResource(R.string.new_release_albums), R.drawable.album,
                    MaterialTheme.colorScheme.tertiary, Modifier.weight(1f)) { navController.navigate("new_release") }
            }
        }
        if (loading || chartsLoading) item("loading") { LinearProgressIndicator(Modifier.padding(horizontal = 24.dp).fillMaxWidth()) }
        if (failed) item("retry") { GalaxRetryPanel(onRetry = exploreViewModel::refresh) }
        charts?.sections?.filter { it.items.isNotEmpty() }?.take(2)?.forEachIndexed { index, section ->
            item("chart_title_$index") { GalaxSectionHeading(if (section.title == "Trending") stringResource(R.string.trending) else section.title,
                onClick = { navController.navigate("charts_screen") }) }
            item("chart_shelf_$index") { GalaxRemoteShelf(section.items.take(12), navController) }
        }
        explore?.newReleaseAlbums?.takeIf { it.isNotEmpty() }?.let { albums ->
            item("releases_title") { GalaxSectionHeading(stringResource(R.string.new_release_albums), onClick = { navController.navigate("new_release") }) }
            item("releases") { GalaxRemoteShelf(albums, navController) }
        }
        charts?.sections?.firstOrNull { it.title == "Top music videos" }?.let { videos ->
            item("videos_title") { GalaxSectionHeading(stringResource(R.string.top_music_videos)) }
            item("videos") { GalaxRemoteShelf(videos.items.filterIsInstance<SongItem>(), navController) }
        }
        explore?.moodAndGenres?.let { genres ->
            item("genres_title") { GalaxSectionHeading(stringResource(R.string.mood_and_genres),
                onClick = { navController.navigate(Screens.MoodAndGenres.route) }) }
            items(genres.distinctBy { it.endpoint.browseId to it.endpoint.params }.chunked(2), key = { it.joinToString { genre -> "${genre.endpoint.browseId}:${genre.endpoint.params}" } }, contentType = { "genres" }) { row ->
                Row(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { genre -> MoodAndGenresButton(genre.title, genre.stripeColor,
                        { navController.navigate("youtube_browse/${genre.endpoint.browseId}?params=${Uri.encode(genre.endpoint.params.orEmpty())}") }, Modifier.weight(1f)) }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun GalaxRetryPanel(modifier: Modifier = Modifier, onRetry: () -> Unit) {
    com.arturo254.opentune.ui.component.MenuSurfaceSection(modifier.padding(horizontal = 24.dp).fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.galax_loading_error), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}
