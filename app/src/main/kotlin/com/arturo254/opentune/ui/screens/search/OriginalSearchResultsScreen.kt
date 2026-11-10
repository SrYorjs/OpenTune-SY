package com.arturo254.opentune.ui.screens.search

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.core.model.ArtistIdentity
import com.arturo254.opentune.core.model.TrackIdentity
import com.arturo254.opentune.sources.resolution.originalRecordingOptions
import com.arturo254.opentune.sources.search.UnifiedTrackSearchResult
import com.arturo254.opentune.ui.visual.component.GlassState
import com.arturo254.opentune.ui.visual.component.glassRoot
import com.arturo254.opentune.ui.visual.search.OriginalSearchCollectionRow
import com.arturo254.opentune.ui.visual.search.OriginalSearchResultRow
import com.arturo254.opentune.ui.visual.search.OriginalSearchTopResult
import com.arturo254.opentune.ui.visual.search.SearchAvatar
import com.arturo254.opentune.ui.visual.search.SearchCategoryRow
import com.arturo254.opentune.ui.visual.search.SearchColors
import com.arturo254.opentune.ui.visual.search.SearchField
import com.arturo254.opentune.ui.visual.search.SearchGlassTopBar
import com.arturo254.opentune.ui.visual.search.SearchLoadingDots
import com.arturo254.opentune.ui.visual.search.SearchSectionHeader
import com.arturo254.opentune.ui.visual.search.SearchSimpleRow
import com.arturo254.opentune.ui.visual.search.rememberCollapseProgress
import com.arturo254.opentune.ui.visual.search.searchTopBarHeight
import com.arturo254.opentune.viewmodels.UniversalSearchViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@Composable
fun OriginalSearchResultsScreen(
    navController: NavController,
    query: String,
    pureBlack: Boolean,
    viewModel: UniversalSearchViewModel = hiltViewModel(),
) {
    val player = LocalPlayerConnection.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val collectionState by viewModel.audiusState.collectAsStateWithLifecycle()
    val current = player?.mediaMetadata?.collectAsStateWithLifecycle()?.value
    val playing = player?.isPlaying?.collectAsStateWithLifecycle()?.value == true
    val listState = rememberLazyListState()
    val glassState = remember { GlassState() }
    val collapse = rememberCollapseProgress(listState)
    val progressProvider = remember(collapse) { { collapse.value } }
    var field by remember(query) { mutableStateOf(TextFieldValue(query, TextRange(query.length))) }
    var category by remember(query) { mutableIntStateOf(0) }
    var selectedAlbum by remember(query) { mutableStateOf<String?>(null) }
    var visibleCount by remember(query) { mutableIntStateOf(50) }
    var loadingTrack by remember(query) { mutableStateOf<String?>(null) }
    val sameQuery = state.query == query.trim()
    val tracks = if (sameQuery) state.result.tracks else emptyList()
    val albums = remember(tracks) { groupSearchAlbums(tracks) }
    val playlists = if (collectionState.query == query.trim()) collectionState.playlists else emptyList()
    val selectedCollection = collectionState.selected?.takeIf { collectionState.query == query.trim() }
    val selectedVariants = if (selectedCollection != null) collectionState.tracks else emptyList()
    val collectionTracks = remember(selectedVariants) {
        selectedVariants.map { variant ->
            UnifiedTrackSearchResult(
                identity = TrackIdentity(
                    title = variant.title,
                    artists = variant.artists.map { ArtistIdentity(name = it) },
                    durationMs = variant.durationMs,
                    isrc = variant.isrc,
                    version = variant.version,
                ),
                variants = listOf(variant),
                recommendedVariant = variant,
            )
        }
    }
    val currentAlbum = albums.firstOrNull { it.key == selectedAlbum }
    val shown = when {
        selectedCollection != null && category == 3 -> collectionTracks
        currentAlbum != null && category == 2 -> currentAlbum.tracks
        category == 0 || category == 1 -> tracks
        else -> emptyList()
    }
    val top = if (category == 0) shown.firstOrNull() else null
    val bottomInset = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
    val loading = state.isLoading || !sameQuery
    val showCategories = listOf(
        stringResource(R.string.prism_results_all),
        stringResource(R.string.prism_results_songs),
        stringResource(R.string.prism_results_albums),
        stringResource(R.string.prism_results_playlists),
    )
    val play: (UnifiedTrackSearchResult) -> Unit = { result ->
        if (player != null && loadingTrack == null) {
            scope.launch {
                loadingTrack = result.identity.title
                try {
                    val played = if (selectedCollection != null && category == 3) {
                        val index = selectedVariants.indexOfFirst { variant ->
                            result.variants.any {
                                it.provider == variant.provider && it.sourceId == variant.sourceId
                            }
                        }
                        viewModel.playAudiusCollection(
                            player, selectedVariants, index.coerceAtLeast(0), selectedCollection.title
                        )
                    } else {
                        viewModel.playSearchResults(
                            player,
                            result,
                            if (currentAlbum != null) state.result.copy(tracks = currentAlbum.tracks)
                            else state.result,
                            originalRecordingOptions(
                                result.identity,
                                listOfNotNull(result.recommendedVariant) + result.variants,
                            ),
                            navController.context.getString(R.string.queue_searched_songs),
                        )
                    }
                    if (!played) {
                        Toast.makeText(
                            navController.context,
                            R.string.universal_versions_play_error,
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                } finally {
                    loadingTrack = null
                }
            }
        }
    }
    val download: (UnifiedTrackSearchResult) -> Unit = { result ->
        scope.launch {
            viewModel.enqueueOffline(result)
                .onSuccess {
                    Toast.makeText(
                        navController.context,
                        R.string.catalog_offline_download_started,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                .onFailure {
                    Toast.makeText(
                        navController.context,
                        R.string.catalog_source_unplayable,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
        }
    }
    val closeSelection: () -> Unit = {
        selectedAlbum = null
        viewModel.closeAudiusCollection()
        visibleCount = 50
    }
    BackHandler(selectedAlbum != null || selectedCollection != null) { closeSelection() }
    LaunchedEffect(query) { viewModel.query.value = query }
    LaunchedEffect(category, selectedAlbum, selectedCollection) { listState.scrollToItem(0) }
    LaunchedEffect(listState, sameQuery, visibleCount, category, state.isLoadingMore, state.result.hasMore) {
        if (!sameQuery || (category != 0 && category != 1)) return@LaunchedEffect
        snapshotFlow {
            val info = listState.layoutInfo
            info.totalItemsCount > 0 &&
                (info.visibleItemsInfo.lastOrNull()?.index ?: -1) >= info.totalItemsCount - 5
        }.distinctUntilChanged().filter { it }.collect {
            if (tracks.size > visibleCount) visibleCount += 50
            else if (state.result.hasMore && !state.isLoadingMore) viewModel.loadMore()
        }
    }
    Box(Modifier.fillMaxSize().background(SearchColors.page(pureBlack))) {
        Box(Modifier.fillMaxSize().glassRoot(glassState, isActive = { progressProvider() > 0.01f })) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = searchTopBarHeight(withTitle = true, extra = 46.dp),
                    bottom = bottomInset + 24.dp,
                ),
            ) {
                if (selectedCollection != null && category == 3) {
                    item("collection_header") {
                        SearchSectionHeader(title = selectedCollection.title, ruleAbove = false)
                    }
                } else if (currentAlbum != null && category == 2) {
                    item("album_header") {
                        SearchSectionHeader(title = currentAlbum.title, ruleAbove = false)
                    }
                }
                if (loading && tracks.isEmpty() && category <= 1) {
                    item("loading") {
                        Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                            SearchLoadingDots()
                        }
                    }
                }
                if (top != null) {
                    item("top_heading") {
                        SearchSectionHeader(title = stringResource(R.string.search_top_result), ruleAbove = false)
                    }
                    item("top_card") {
                        OriginalSearchTopResult(
                            item = top,
                            isActive = current != null && top.variants.any { it.sourceId == current.id },
                            isPlaying = playing,
                            onClick = { play(top) },
                            onLongClick = { download(top) },
                        )
                    }
                    item("top_gap") { Box(Modifier.height(22.dp)) }
                }
                val visibleTracks = if (top != null) shown.drop(1).take(visibleCount) else shown.take(visibleCount)
                if (visibleTracks.isNotEmpty()) {
                    item("music_heading") {
                        SearchSectionHeader(
                            title = stringResource(R.string.prism_results_songs),
                            ruleAbove = false,
                        )
                    }
                    itemsIndexed(
                        visibleTracks,
                        key = { index, result -> universalResultRowKey(index, result.identity.title) },
                    ) { _, result ->
                        OriginalSearchResultRow(
                            item = result,
                            isActive = current != null && result.variants.any { it.sourceId == current.id },
                            onClick = { play(result) },
                            onLongClick = { download(result) },
                        )
                    }
                }
                if (category == 2 && currentAlbum == null) {
                    item("album_heading") {
                        SearchSectionHeader(
                            title = stringResource(R.string.prism_results_albums),
                            ruleAbove = false,
                        )
                    }
                    items(albums.take(visibleCount), key = { it.key }) { album ->
                        OriginalSearchCollectionRow(
                            album.title, album.artist, album.artworkUrl,
                            onClick = { selectedAlbum = album.key },
                        )
                    }
                }
                if (category == 3 && selectedCollection == null) {
                    item("playlist_heading") {
                        SearchSectionHeader(
                            title = stringResource(R.string.prism_results_playlists),
                            ruleAbove = false,
                        )
                    }
                    items(playlists.take(visibleCount), key = { it.id }) { playlist ->
                        OriginalSearchCollectionRow(
                            playlist.name, playlist.creator, playlist.artworkUrl,
                            onClick = { viewModel.openAudiusPlaylist(playlist) },
                        )
                    }
                }
                if (category == 0 && playlists.isNotEmpty()) {
                    item("featured_heading") {
                        SearchSectionHeader(
                            title = stringResource(R.string.prism_results_playlists),
                            ruleAbove = false,
                        )
                    }
                    items(playlists.take(6), key = { "playlist_" + it.id }) { playlist ->
                        OriginalSearchCollectionRow(
                            playlist.name, playlist.creator, playlist.artworkUrl,
                            onClick = {
                                category = 3
                                viewModel.openAudiusPlaylist(playlist)
                            },
                        )
                    }
                }
                if (!loading && shown.isEmpty() &&
                    (category == 0 || category == 1 || currentAlbum != null || selectedCollection != null)
                ) {
                    item("empty") {
                        SearchSimpleRow(
                            text = stringResource(R.string.prism_results_empty),
                            onClick = viewModel::retrySearch,
                        )
                    }
                }
                if (state.isLoadingMore || (category == 3 && collectionState.loadingCollection)) {
                    item("more_loading") {
                        Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
                            SearchLoadingDots()
                        }
                    }
                }
            }
        }
        SearchGlassTopBar(
            glassState = glassState,
            progressProvider = progressProvider,
            pureBlack = pureBlack,
            title = stringResource(R.string.search),
            trailing = { SearchAvatar(imageUrl = null, onClick = { navController.navigate("account") }) },
            below = {
                SearchCategoryRow(
                    categories = showCategories,
                    selectedIndex = category,
                    onSelect = {
                        category = it
                        closeSelection()
                    },
                )
            },
        ) {
            SearchField(
                value = field,
                onValueChange = { field = it },
                onSearch = { entered ->
                    val value = entered.trim()
                    if (value.isNotBlank() && value != query) {
                        focusManager.clearFocus()
                        navController.navigate("search/" + Uri.encode(value))
                    }
                },
                placeholder = stringResource(R.string.prism_search_hint),
                leading = {
                    Icon(
                        painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                        tint = SearchColors.Secondary,
                        modifier = Modifier.size(20.dp).clickable {
                            if (selectedAlbum != null || selectedCollection != null) closeSelection()
                            else navController.navigateUp()
                        },
                    )
                },
            )
        }
    }
}
