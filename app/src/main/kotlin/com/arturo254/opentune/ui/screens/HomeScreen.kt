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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
    val list = rememberLazyListState()
    val refresh = rememberPullToRefreshState()

    val quickPicks by viewModel.quickPicks.collectAsState()
    val speedDial by viewModel.speedDialSongs.collectAsState()
    val forgotten by viewModel.forgottenFavorites.collectAsState()
    val keepListening by viewModel.keepListening.collectAsState()
    val homePage by viewModel.homePage.collectAsState()
    val selectedChip by viewModel.selectedChip.collectAsState()
    val accountImage by viewModel.accountImageUrl.collectAsState()
    val metadataMap by viewModel.allItemsMetadata.collectAsState()
    val trending by viewModel.globalCatalogTrending.collectAsState()
    val loading by viewModel.isLoading.collectAsState()
    val refreshing by viewModel.isRefreshing.collectAsState()
    val now by player.mediaMetadata.collectAsState()
    val playing by player.isPlaying.collectAsState()
    val (showChips) = rememberPreference(ShowHomeCategoryChipsKey, true)
    var shuffling by remember { mutableStateOf(false) }

    val recent = remember(keepListening) {
        keepListening.orEmpty().filterIsInstance<Song>()
            .distinctBy { it.id }.take(15)
    }
    val picks = remember(quickPicks) {
        quickPicks.orEmpty().distinctBy { it.id }.take(24)
    }
    val dial = remember(speedDial) {
        speedDial.distinctBy { it.id }.take(14)
    }
    val rediscover = remember(forgotten) {
        forgotten.orEmpty().distinctBy { it.id }.take(16)
    }
    val spotlight = remember(picks, recent, dial, rediscover) {
        (recent + picks + dial + rediscover).distinctBy { it.id }.firstOrNull()
    }
    val stageQueue = remember(spotlight, picks, recent, dial) {
        (listOfNotNull(spotlight) + picks + recent + dial).distinctBy { it.id }
    }
    val currentHome by rememberUpdatedState(homePage)
    val backStack by navController.currentBackStackEntryAsState()
    val scrollTop = backStack?.savedStateHandle
        ?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollTop?.value) {
        if (scrollTop?.value == true) {
            list.animateScrollToItem(0)
            backStack?.savedStateHandle?.set("scrollToTop", false)
        }
    }
    LaunchedEffect(list, homePage?.continuation, ambientActive) {
        if (!ambientActive) return@LaunchedEffect
        snapshotFlow {
            val info = list.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            val more = currentHome?.continuation
            more to (info.totalItemsCount > 0 && last >= info.totalItemsCount - 5)
        }
            .distinctUntilChanged()
            .filter { (continuation, nearEnd) -> continuation != null && nearEnd }
            .collect { (continuation, _) ->
                viewModel.loadMoreYouTubeItems(continuation)
            }
    }

    BackHandler(enabled = ambientActive && selectedChip != null) {
        viewModel.toggleChip(selectedChip)
    }
    LaunchedEffect(showChips, selectedChip) {
        if (!showChips && selectedChip != null) viewModel.toggleChip(selectedChip)
    }

    val playSong: (Song, List<Song>) -> Unit = { song, songs ->
        if (now?.id == song.id) {
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
    val showSongMenu: (Song) -> Unit = { song ->
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
    val shuffle = {
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

    Box(Modifier.fillMaxSize().galaxScreenBackground()) {
        AetherAmbient(
            modifier = Modifier.fillMaxSize(),
            active = ambientActive,
            variant = 0,
            intensity = .70f,
        )
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize().pullToRefresh(
                state = refresh,
                isRefreshing = refreshing,
                onRefresh = viewModel::refresh,
            ),
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            verticalArrangement = Arrangement.spacedBy(21.dp),
        ) {
            item(key = "aether_masthead") {
                AetherHomeMasthead(
                    accountImage = accountImage,
                    onAccount = { navController.navigate("account") },
                    onSearch = onOpenSearchTab,
                    onRecognize = {
                        navController.navigate(
                            com.arturo254.opentune.ui.screens.musicrecognition.MusicRecognitionRoute
                        )
                    },
                )
            }
            if (spotlight != null) {
                item(key = "aether_spotlight") {
                    AetherReveal(key = spotlight.id) {
                        AetherOpeningStage(
                            song = spotlight,
                            selected = now?.id == spotlight.id,
                            playing = playing,
                            active = ambientActive,
                            onClick = { playSong(spotlight, stageQueue) },
                            onMenu = { showSongMenu(spotlight) },
                        )
                    }
                }
            }
            item(key = "aether_actions") {
                AetherDuetActions(
                    busy = shuffling,
                    onShuffle = shuffle,
                    onLibrary = onOpenLibraryTab,
                )
            }
            if (now != null) {
                item(key = "aether_current") {
                    AetherListeningRail(
                        metadata = now!!,
                        playing = playing,
                        onTap = { player.player.togglePlayPause() },
                    )
                }
            }
            if (showChips && homePage?.chips?.isNotEmpty() == true) {
                item(key = "aether_filters") {
                    ChipsRow(
                        chips = homePage?.chips.orEmpty().map { it to it.title },
                        currentValue = selectedChip,
                        onValueUpdate = viewModel::toggleChip,
                    )
                }
            }
            if (recent.isNotEmpty()) {
                item(key = "aether_recents_header") {
                    AetherSectionTitle(
                        title = stringResource(R.string.keep_listening),
                        eyebrow = "01 / ESCUCHA",
                    )
                }
                item(key = "aether_recents_shelf") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 22.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(recent, key = { "aether_recent:" + it.id }) { song ->
                            AetherCoverTile(
                                song = song,
                                active = now?.id == song.id,
                                playing = playing,
                                onPlay = { playSong(song, recent) },
                                onMenu = { showSongMenu(song) },
                            )
                        }
                    }
                }
            }
            if (picks.isNotEmpty()) {
                item(key = "aether_picks_title") {
                    AetherSectionTitle(title = "Hecho para ti", eyebrow = "02 / SELECCIÓN")
                }
                itemsIndexed(
                    picks.take(12),
                    key = { index, _ -> "aether_pick:" + index },
                    contentType = { _, _ -> "aether_song" },
                ) { index, song ->
                    AetherMusicLine(
                        song = song,
                        index = index,
                        active = now?.id == song.id,
                        playing = playing,
                        modifier = Modifier.animateItem(),
                        onPlay = { playSong(song, picks) },
                        onMenu = { showSongMenu(song) },
                    )
                }
            }
            if (dial.isNotEmpty()) {
                item(key = "aether_dial_title") {
                    AetherSectionTitle(
                        title = stringResource(R.string.speed_dial),
                        eyebrow = "03 / EN ROTACIÓN",
                    )
                }
                item(key = "aether_dial_row") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 22.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(dial, key = { "aether_dial:" + it.id }) { song ->
                            AetherCoverTile(
                                song = song,
                                active = now?.id == song.id,
                                playing = playing,
                                onPlay = { playSong(song, dial) },
                                onMenu = { showSongMenu(song) },
                            )
                        }
                    }
                }
            }
            if (rediscover.isNotEmpty()) {
                item(key = "aether_rediscover_title") {
                    AetherSectionTitle(
                        title = stringResource(R.string.forgotten_favorites),
                        eyebrow = "04 / REDESCUBRIR",
                    )
                }
                item(key = "aether_rediscover_shelf") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 22.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(rediscover, key = { "aether_forgotten:" + it.id }) { song ->
                            AetherCoverTile(
                                song = song,
                                active = now?.id == song.id,
                                playing = playing,
                                onPlay = { playSong(song, rediscover) },
                                onMenu = { showSongMenu(song) },
                            )
                        }
                    }
                }
            }
            if (trending.isNotEmpty() && selectedChip == null) {
                item(key = "aether_trending_header") {
                    AetherSectionTitle(title = "Tendencias", eyebrow = "05 / ACTUALIDAD")
                }
                item(key = "aether_trending_catalog") {
                    AetherTrendingShelf(
                        tracks = trending,
                        playerConnection = player,
                        onPlay = viewModel::playSourceVariant,
                    )
                }
            }
            if (homePage?.sections?.isNotEmpty() == true) {
                item(key = "aether_catalog_title") {
                    AetherSectionTitle(title = "En tu universo", eyebrow = "06 / CATÁLOGO")
                }
                homePage?.sections.orEmpty().forEachIndexed { index, section ->
                    item(key = galaxHomeSectionRowKey(index, true)) {
                        AetherReveal(key = index) {
                            AetherCatalogHeading(
                                section = section,
                                navController = navController,
                            )
                        }
                    }
                    item(key = galaxHomeSectionRowKey(index, false)) {
                        AetherCatalogShelf(
                            section = section,
                            navController = navController,
                        )
                    }
                }
            }
            if (loading) {
                item(key = "aether_loading") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(26.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                }
            }
            item(key = "aether_invitation") {
                AetherCrossfadeInvitation(onSearch = onOpenSearchTab)
            }
            item(key = "aether_bottom") {
                Spacer(Modifier.height(30.dp))
            }
        }
        Indicator(
            isRefreshing = refreshing,
            state = refresh,
            modifier = Modifier.align(Alignment.TopCenter)
                .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
        )
    }
}

internal fun galaxHomeSectionRowKey(index: Int, heading: Boolean): String =
    (if (heading) "home-section-title:" else "home-section-content:") + index
