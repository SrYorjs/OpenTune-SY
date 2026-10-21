package com.arturo254.opentune.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.ShowHomeCategoryChipsKey
import com.arturo254.opentune.core.designsystem.theme.galaxScreenBackground
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.playback.queues.ListQueue
import com.arturo254.opentune.ui.component.ChipsRow
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.menu.SongMenu
import com.arturo254.opentune.utils.rememberPreference
import com.arturo254.opentune.viewmodels.HomeViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
    onOpenSearchTab: () -> Unit = { navController.navigate(Screens.Search.route) },
    onOpenLibraryTab: () -> Unit = { navController.navigate(Screens.Library.route) },
    ambientActive: Boolean = true,
) {
    val player = LocalPlayerConnection.current ?: return
    val menu = LocalMenuState.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val refreshState = rememberPullToRefreshState()
    val quickPicks by viewModel.quickPicks.collectAsState()
    val speedDial by viewModel.speedDialSongs.collectAsState()
    val forgotten by viewModel.forgottenFavorites.collectAsState()
    val keepListening by viewModel.keepListening.collectAsState()
    val homePage by viewModel.homePage.collectAsState()
    val selectedChip by viewModel.selectedChip.collectAsState()
    val accountName by viewModel.accountName.collectAsState()
    val accountImage by viewModel.accountImageUrl.collectAsState()
    val metadataMap by viewModel.allItemsMetadata.collectAsState()
    val trending by viewModel.globalCatalogTrending.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val nowPlaying by player.mediaMetadata.collectAsState()
    val playing by player.isPlaying.collectAsState()
    val (showChips) = rememberPreference(ShowHomeCategoryChipsKey, true)
    var shuffling by remember { mutableStateOf(false) }

    val recent = remember(keepListening) {
        keepListening.orEmpty().filterIsInstance<Song>()
            .distinctBy { it.id }.take(12)
    }
    val picks = remember(quickPicks) {
        quickPicks.orEmpty().distinctBy { it.id }.take(18)
    }
    val dial = remember(speedDial) {
        speedDial.distinctBy { it.id }.take(12)
    }
    val remembered = remember(forgotten) {
        forgotten.orEmpty().distinctBy { it.id }.take(12)
    }
    val featured = remember(picks, recent, dial, remembered) {
        (picks + recent + dial + remembered).firstOrNull()
    }
    val currentHome by rememberUpdatedState(homePage)
    val backStack by navController.currentBackStackEntryAsState()
    val scrollTop = backStack?.savedStateHandle
        ?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollTop?.value) {
        if (scrollTop?.value == true) {
            listState.animateScrollToItem(0)
            backStack?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            val layout = listState.layoutInfo
            val last = layout.visibleItemsInfo.lastOrNull()?.index ?: -1
            layout.totalItemsCount > 0 && last >= layout.totalItemsCount - 5
        }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                viewModel.loadMoreYouTubeItems(currentHome?.continuation)
            }
    }

    BackHandler(enabled = selectedChip != null) {
        viewModel.toggleChip(selectedChip)
    }
    LaunchedEffect(showChips, selectedChip) {
        if (!showChips && selectedChip != null) viewModel.toggleChip(selectedChip)
    }

    val playQueue: (Song, List<Song>) -> Unit = { song, songs ->
        if (nowPlaying?.id == song.id) {
            player.player.togglePlayPause()
        } else {
            player.playQueue(
                ListQueue(
                    title = context.getString(R.string.home),
                    items = songs.map { it.toMediaItem() },
                    startIndex = songs.indexOfFirst { it.id == song.id }.coerceAtLeast(0),
                ),
            )
        }
    }

    val showMenu: (Song) -> Unit = { song ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        menu.show {
            SongMenu(
                originalSong = song,
                navController = navController,
                metadata = metadataMap[song.id],
                onDismiss = menu::dismiss,
            )
        }
    }

    val startShuffle = {
        if (!shuffling) {
            shuffling = true
            viewModel.playGalaxShuffle(player) { started ->
                shuffling = false
                if (!started) {
                    Toast.makeText(
                        context,
                        R.string.galax_shuffle_unavailable,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().galaxScreenBackground(),
    ) {
        GalaxEditorialBackdrop(
            modifier = Modifier.fillMaxSize(),
            active = ambientActive,
            density = 33,
            intensity = .62f,
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
                .pullToRefresh(
                    state = refreshState,
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refresh,
                ),
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            item(key = "editorial_header") {
                GalaxHomeEditorialTopBar(
                    accountName = accountName,
                    accountImage = accountImage,
                    onProfile = { navController.navigate("account") },
                    onRecognition = {
                        navController.navigate(
                            com.arturo254.opentune.ui.screens.musicrecognition.MusicRecognitionRoute
                        )
                    },
                    onSearch = onOpenSearchTab,
                )
            }
            if (featured != null) {
                item(key = "editorial_feature") {
                    GalaxEditorialReveal {
                        GalaxHomeImmersiveFeature(
                            song = featured,
                            active = nowPlaying?.id == featured.id,
                            isPlaying = playing,
                            animationsActive = ambientActive,
                            onPlay = {
                                playQueue(
                                    featured,
                                    listOf(featured) + (picks + recent + dial)
                                        .distinctBy { it.id }
                                        .filterNot { it.id == featured.id },
                                )
                            },
                            onLongPress = { showMenu(featured) },
                        )
                    }
                }
            }
            item(key = "editorial_actions") {
                GalaxHomeNavigationRow(
                    onShuffle = startShuffle,
                    onLibrary = onOpenLibraryTab,
                    shuffling = shuffling,
                )
            }
            if (nowPlaying != null) {
                item(key = "current_track") {
                    GalaxHomeCurrentStrip(
                        current = nowPlaying,
                        isPlaying = playing,
                        onClick = { player.player.togglePlayPause() },
                    )
                }
            }
            if (showChips && !homePage?.chips.isNullOrEmpty()) {
                item(key = "editorial_filters") {
                    ChipsRow(
                        chips = homePage?.chips.orEmpty().map { it to it.title },
                        currentValue = selectedChip,
                        onValueUpdate = { viewModel.toggleChip(it) },
                    )
                }
            }
            if (recent.isNotEmpty()) {
                item(key = "recent_title") {
                    GalaxEditorialReveal {
                        GalaxEditorialHeading(stringResource(R.string.keep_listening))
                    }
                }
                item(key = "recent_shelf") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 22.dp),
                        horizontalArrangement = Arrangement.spacedBy(13.dp),
                    ) {
                        items(recent, key = { "recent:" + it.id }) { song ->
                            GalaxHomePoster(
                                song = song,
                                isActive = nowPlaying?.id == song.id,
                                playing = playing,
                                onClick = { playQueue(song, recent) },
                                onLongPress = { showMenu(song) },
                            )
                        }
                    }
                }
            }
            if (picks.isNotEmpty()) {
                item(key = "picks_title") {
                    GalaxEditorialHeading("Para ti")
                }
                items(picks.take(9), key = { "pick:" + it.id }) { song ->
                    GalaxHomeCompactTrack(
                        song = song,
                        active = nowPlaying?.id == song.id,
                        isPlaying = playing,
                        modifier = Modifier.animateItem(),
                        onClick = { playQueue(song, picks) },
                        onLongPress = { showMenu(song) },
                    )
                }
            }
            if (dial.isNotEmpty()) {
                item(key = "dial_title") {
                    GalaxEditorialHeading(stringResource(R.string.speed_dial))
                }
                item(key = "dial_shelf") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 22.dp),
                        horizontalArrangement = Arrangement.spacedBy(13.dp),
                    ) {
                        items(dial, key = { "dial:" + it.id }) { song ->
                            GalaxHomePoster(
                                song = song,
                                isActive = nowPlaying?.id == song.id,
                                playing = playing,
                                onClick = { playQueue(song, dial) },
                                onLongPress = { showMenu(song) },
                            )
                        }
                    }
                }
            }
            if (remembered.isNotEmpty()) {
                item(key = "remembered_title") {
                    GalaxEditorialHeading(stringResource(R.string.forgotten_favorites))
                }
                item(key = "remembered_shelf") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 22.dp),
                        horizontalArrangement = Arrangement.spacedBy(13.dp),
                    ) {
                        items(remembered, key = { "remembered:" + it.id }) { song ->
                            GalaxHomePoster(
                                song = song,
                                isActive = nowPlaying?.id == song.id,
                                playing = playing,
                                onClick = { playQueue(song, remembered) },
                                onLongPress = { showMenu(song) },
                            )
                        }
                    }
                }
            }
            if (selectedChip == null && trending.isNotEmpty()) {
                item(key = "trending_title") {
                    GalaxEditorialHeading("Tendencias")
                }
                item(key = "trending_shelf") {
                    GlobalCatalogHomeSection(
                        tracks = trending,
                        playerConnection = player,
                        onPlay = viewModel::playSourceVariant,
                    )
                }
            }
            if (homePage?.sections?.isNotEmpty() == true) {
                item(key = "discover_title") {
                    GalaxEditorialHeading("Descubre")
                }
                homePage?.sections.orEmpty().forEachIndexed { index, section ->
                    item(key = "section_title_$" + "index") {
                        GalaxEditorialReveal {
                            HomePageSectionTitle(
                                section = section,
                                navController = navController,
                            )
                        }
                    }
                    item(key = "section_content_$" + "index") {
                        HomePageSectionContent(
                            section = section,
                            mediaMetadata = nowPlaying,
                            isPlaying = playing,
                            navController = navController,
                            playerConnection = player,
                            menuState = menu,
                            haptic = haptic,
                            scope = scope,
                            metadataMap = metadataMap,
                        )
                    }
                }
            }
            if (isLoading) {
                item(key = "editorial_loading") {
                    Box(
                        Modifier.fillMaxWidth().padding(20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(25.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                }
            }
            item(key = "discovery_link") {
                GalaxHomeDiscoveryBridge(onOpenSearch = onOpenSearchTab)
            }
            item(key = "editorial_bottom") {
                Spacer(Modifier.height(30.dp))
            }
        }
        Indicator(
            isRefreshing = isRefreshing,
            state = refreshState,
            modifier = Modifier.align(Alignment.TopCenter)
                .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
        )
    }
}
