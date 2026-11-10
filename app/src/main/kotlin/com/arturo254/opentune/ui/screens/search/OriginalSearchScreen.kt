package com.arturo254.opentune.ui.screens.search

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.arturo254.opentune.LocalDatabase
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.PauseSearchHistoryKey
import com.arturo254.opentune.db.entities.SearchHistory
import com.arturo254.opentune.sources.resolution.originalRecordingOptions
import com.arturo254.opentune.sources.search.UnifiedTrackSearchResult
import com.arturo254.opentune.ui.visual.component.GlassState
import com.arturo254.opentune.ui.visual.component.glassRoot
import com.arturo254.opentune.ui.visual.search.MoodTile
import com.arturo254.opentune.ui.visual.search.SearchArtwork
import com.arturo254.opentune.ui.visual.search.SearchCollapseDurationMs
import com.arturo254.opentune.ui.visual.search.SearchCollapseEasing
import com.arturo254.opentune.ui.visual.search.SearchColors
import com.arturo254.opentune.ui.visual.search.SearchField
import com.arturo254.opentune.ui.visual.search.SearchGlassTopBar
import com.arturo254.opentune.ui.visual.search.SearchHorizontalPadding
import com.arturo254.opentune.ui.visual.search.SearchLoadingDots
import com.arturo254.opentune.ui.visual.search.SearchSectionHeader
import com.arturo254.opentune.ui.visual.search.SearchSimpleRow
import com.arturo254.opentune.ui.visual.search.SearchAvatar
import com.arturo254.opentune.ui.visual.search.rememberCollapseProgress
import com.arturo254.opentune.ui.visual.search.searchTopBarHeight
import com.arturo254.opentune.utils.rememberPreference
import com.arturo254.opentune.viewmodels.MoodAndGenresViewModel
import com.arturo254.opentune.viewmodels.coverKey
import com.arturo254.opentune.viewmodels.UniversalSearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun OriginalSearchScreen(
    navController: NavController,
    pureBlack: Boolean,
    ambientActive: Boolean,
    genresViewModel: MoodAndGenresViewModel = hiltViewModel(),
    searchViewModel: UniversalSearchViewModel = hiltViewModel(),
) {
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val (pauseHistory) = rememberPreference(PauseSearchHistoryKey, false)
    val history by remember(database) { database.searchHistory() }.collectAsStateWithLifecycle(emptyList())
    val sections by genresViewModel.moodAndGenres.collectAsStateWithLifecycle()
    val genresLoading by genresViewModel.isLoading.collectAsStateWithLifecycle()
    val searchState by searchViewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var active by rememberSaveable { mutableStateOf(false) }
    val browseListState = rememberLazyListState()
    val activeListState = rememberLazyListState()
    val glassState = remember { GlassState() }
    val collapse = rememberCollapseProgress(browseListState)
    val activeProgress = animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(SearchCollapseDurationMs, easing = SearchCollapseEasing),
        label = "searchActive",
    )
    val progressProvider = remember(collapse, activeProgress) {
        { maxOf(collapse.value, activeProgress.value) }
    }
    val commit: (String) -> Unit = { raw ->
        val text = raw.trim()
        if (text.isNotBlank()) {
            focusManager.clearFocus()
            keyboard?.hide()
            active = false
            if (!pauseHistory) {
                scope.launch(Dispatchers.IO) {
                    database.query { insert(SearchHistory(query = text)) }
                }
            }
            navController.navigate("search/" + Uri.encode(text))
        }
    }

    LaunchedEffect(active, ambientActive, query.text) {
        if (active && ambientActive) searchViewModel.query.value = query.text
    }
    BackHandler(enabled = active) {
        active = false
        focusManager.clearFocus()
        keyboard?.hide()
    }
    LaunchedEffect(active) {
        if (active) runCatching { focusRequester.requestFocus() }
        else keyboard?.hide()
    }
    DisposableEffect(Unit) {
        onDispose { focusManager.clearFocus() }
    }
    val showBrowse by remember { derivedStateOf { activeProgress.value < 0.995f } }
    val showActive by remember { derivedStateOf { activeProgress.value > 0.005f } }
    Box(Modifier.fillMaxSize().background(SearchColors.page(pureBlack))) {
        Box(
            Modifier.fillMaxSize()
                .glassRoot(glassState, isActive = { progressProvider() > 0.01f })
        ) {
            if (showBrowse) {
                Box(
                    Modifier.fillMaxSize().graphicsLayer { alpha = 1f - activeProgress.value }
                ) {
                    OriginalSearchBrowseContent(
                        listState = browseListState,
                        sections = sections.orEmpty(),
                        loading = genresLoading,
                        navController = navController,
                        viewModel = genresViewModel,
                    )
                }
            }
            if (showActive) {
                Box(
                    Modifier.fillMaxSize().graphicsLayer { alpha = activeProgress.value }
                ) {
                    OriginalSearchActiveContent(
                        listState = activeListState,
                        query = query.text,
                        history = history,
                        results = if (searchState.query == query.text.trim()) searchState.result.tracks else emptyList(),
                        loading = searchState.isLoading,
                        onFill = { text -> query = TextFieldValue(text, TextRange(text.length)) },
                        onSubmit = commit,
                        onDeleteHistory = { entry ->
                            scope.launch(Dispatchers.IO) { database.query { delete(entry) } }
                        },
                        onPlay = { result ->
                            if (playerConnection != null) {
                                scope.launch {
                                    val options = originalRecordingOptions(
                                        result.identity,
                                        listOfNotNull(result.recommendedVariant) + result.variants,
                                    )
                                    val played = searchViewModel.playSearchResults(
                                        playerConnection,
                                        result,
                                        searchState.result,
                                        options,
                                        navController.context.getString(R.string.queue_searched_songs),
                                    )
                                    if (!played) Toast.makeText(
                                        navController.context,
                                        R.string.universal_versions_play_error,
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            }
                        },
                    )
                }
            }
        }
        SearchGlassTopBar(
            glassState = glassState,
            progressProvider = progressProvider,
            pureBlack = pureBlack,
            title = stringResource(R.string.search),
            trailing = {
                SearchAvatar(imageUrl = null, onClick = { navController.navigate("account") })
            },
        ) {
            SearchField(
                value = query,
                onValueChange = { query = it },
                onSearch = commit,
                placeholder = stringResource(R.string.prism_search_hint),
                focused = active,
                leading = if (active) {
                    {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                            tint = SearchColors.Secondary,
                            modifier = Modifier.size(20.dp).clickable {
                                active = false
                                focusManager.clearFocus()
                            },
                        )
                    }
                } else null,
                trailing = if (query.text.isNotEmpty()) {
                    {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                            tint = SearchColors.Secondary,
                            modifier = Modifier.size(20.dp).clickable {
                                query = TextFieldValue()
                            },
                        )
                    }
                } else null,
                focusRequester = focusRequester,
                onFocusChanged = { focused -> if (focused) active = true },
                readOnlyClick = if (!active) ({ active = true }) else null,
            )
        }
    }
}

@Composable
private fun OriginalSearchBrowseContent(
    listState: LazyListState,
    sections: List<com.arturo254.opentune.innertube.pages.MoodAndGenres.Item>,
    loading: Boolean,
    navController: NavController,
    viewModel: MoodAndGenresViewModel,
) {
    val bottomInset = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = searchTopBarHeight(withTitle = true),
            bottom = bottomInset + 24.dp,
        ),
    ) {
        if (loading && sections.isEmpty()) {
            item("loading") {
                Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                    SearchLoadingDots()
                }
            }
        }
        if (sections.isNotEmpty()) {
            item("genre_header") {
                SearchSectionHeader(
                    title = stringResource(R.string.prism_genres),
                    ruleAbove = false,
                    ruleBelow = true,
                )
            }
            itemsIndexed(
                items = sections.chunked(2),
                key = { index, _ -> "mood_" + index },
            ) { _, row ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = SearchHorizontalPadding, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { item ->
                        LaunchedEffect(item.endpoint.browseId, item.endpoint.params) {
                            viewModel.requestCovers(item.endpoint.browseId, item.endpoint.params)
                        }
                        MoodTile(
                            title = item.title,
                            stripeColor = item.stripeColor,
                            covers = viewModel.covers[
                                coverKey(item.endpoint.browseId, item.endpoint.params)
                            ].orEmpty(),
                            onClick = {
                                navController.navigate(
                                    "youtube_browse/" + item.endpoint.browseId +
                                        "?params=" + Uri.encode(item.endpoint.params.orEmpty())
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun OriginalSearchActiveContent(
    listState: LazyListState,
    query: String,
    history: List<SearchHistory>,
    results: List<UnifiedTrackSearchResult>,
    loading: Boolean,
    onFill: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onDeleteHistory: (SearchHistory) -> Unit,
    onPlay: (UnifiedTrackSearchResult) -> Unit,
) {
    val bottomInset = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = searchTopBarHeight(withTitle = false),
            bottom = bottomInset + 24.dp,
        ),
    ) {
        if (query.isBlank()) {
            if (history.isNotEmpty()) {
                item("history_header") {
                    SearchSectionHeader(title = stringResource(R.string.search_history))
                }
                items(
                    items = history.distinctBy { it.query },
                    key = { it.id },
                ) { entry ->
                    SearchSimpleRow(
                        text = entry.query,
                        leadingIcon = R.drawable.history,
                        onClick = { onSubmit(entry.query) },
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OriginalSearchRowGlyph(R.drawable.close) { onDeleteHistory(entry) }
                                Spacer(Modifier.width(4.dp))
                                OriginalSearchRowGlyph(R.drawable.arrow_top_left) { onFill(entry.query) }
                            }
                        },
                    )
                }
            }
        } else {
            if (results.isNotEmpty()) {
                item("result_header") {
                    SearchSectionHeader(title = stringResource(R.string.search_top_result))
                }
                items(
                    results.take(12).distinctBy { result ->
                        result.identity.isrc ?: result.identity.title + ":" +
                            result.identity.artists.joinToString { it.name }
                    },
                    key = { result ->
                        result.identity.isrc ?: result.identity.title + ":" +
                            result.identity.artists.joinToString { it.name }
                    },
                ) { result ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onPlay(result) }
                            .padding(horizontal = SearchHorizontalPadding, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SearchArtwork(
                            url = result.recommendedVariant?.artworkUrl
                                ?: result.variants.firstNotNullOfOrNull { it.artworkUrl },
                            size = 44.dp,
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(result.identity.title, color = SearchColors.Primary)
                            Text(
                                result.identity.artists.joinToString { it.name },
                                color = SearchColors.Secondary,
                            )
                        }
                        OriginalSearchRowGlyph(R.drawable.arrow_top_left) {
                            onFill(result.identity.title)
                        }
                    }
                }
            }
            if (loading && results.isEmpty()) {
                item("loading") {
                    Box(
                        Modifier.fillMaxWidth().height(180.dp),
                        contentAlignment = Alignment.Center,
                    ) { SearchLoadingDots() }
                }
            }
            item("search_full") {
                SearchSimpleRow(
                    text = query,
                    onClick = { onSubmit(query) },
                    leadingIcon = R.drawable.search,
                    pill = true,
                )
            }
        }
    }
}

@Composable
private fun OriginalSearchRowGlyph(icon: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(28.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = SearchColors.Secondary,
            modifier = Modifier.size(16.dp),
        )
    }
}
