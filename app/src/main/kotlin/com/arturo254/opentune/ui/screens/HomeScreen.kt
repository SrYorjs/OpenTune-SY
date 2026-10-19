package com.arturo254.opentune.ui.screens

import androidx.compose.ui.platform.LocalContext

import androidx.compose.runtime.setValue

import androidx.compose.runtime.mutableStateOf

import android.widget.Toast

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberUpdatedState
import com.arturo254.opentune.extensions.togglePlayPause
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.material3.MaterialTheme
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.arturo254.opentune.innertube.utils.parseCookieString
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.InnerTubeCookieKey
import com.arturo254.opentune.constants.ShowHomeCategoryChipsKey
import com.arturo254.opentune.ui.component.ChipsRow
import com.arturo254.opentune.ui.component.LocalBottomSheetPageState
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.component.NavigationTitle
import com.arturo254.opentune.ui.component.liquidGlassScreenBackground
import com.arturo254.opentune.ui.utils.SnapLayoutInfoProvider
import com.arturo254.opentune.utils.rememberPreference
import com.arturo254.opentune.models.toMediaMetadata
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.playback.queues.ListQueue
import com.arturo254.opentune.ui.menu.SongMenu
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.arturo254.opentune.viewmodels.HomeViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    var shufflePending by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val quickPicks by viewModel.quickPicks.collectAsState()
    val speedDialSongs by viewModel.speedDialSongs.collectAsState()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsState()
    val keepListening by viewModel.keepListening.collectAsState()
    val homePage by viewModel.homePage.collectAsState()
    val globalCatalogTrending by viewModel.globalCatalogTrending.collectAsState()
    val allItemsMetadata by viewModel.allItemsMetadata.collectAsState()

    val featuredTrack = remember(quickPicks, speedDialSongs, forgottenFavorites, keepListening) {
        quickPicks?.firstOrNull()
            ?: speedDialSongs.firstOrNull()
            ?: forgottenFavorites?.firstOrNull()
            ?: keepListening?.filterIsInstance<Song>()?.firstOrNull()
    }

    val orbitSongs = remember(quickPicks, speedDialSongs, keepListening, forgottenFavorites, featuredTrack) {
        (
            quickPicks.orEmpty() +
                speedDialSongs +
                keepListening.orEmpty().filterIsInstance<Song>() +
                forgottenFavorites.orEmpty()
        ).distinctBy { it.id }
            .filterNot { it.id == featuredTrack?.id }
            .take(4)
    }
    val highlightedIds = remember(orbitSongs, featuredTrack) {
        orbitSongs.mapTo(mutableSetOf()) { it.id }.apply {
            featuredTrack?.let { add(it.id) }
        }
    }

    var showAllRecommendations by rememberSaveable { mutableStateOf(false) }
    val latestShowAllRecommendations by rememberUpdatedState(showAllRecommendations)

    val selectedChip by viewModel.selectedChip.collectAsState()

    val isLoading: Boolean by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    val forgottenFavoritesLazyGridState = rememberLazyGridState()

    val accountName by viewModel.accountName.collectAsState()
    val accountImageUrl by viewModel.accountImageUrl.collectAsState()
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val (showHomeCategoryChips) = rememberPreference(ShowHomeCategoryChipsKey, true)
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val url = if (isLoggedIn) accountImageUrl else null

    val scope = rememberCoroutineScope()
    val lazylistState = rememberLazyListState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazylistState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    val latestHomePage by rememberUpdatedState(homePage)
    LaunchedEffect(Unit) {
        snapshotFlow { lazylistState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                val len = lazylistState.layoutInfo.totalItemsCount
                val continuation = latestHomePage?.continuation
                if (
                    latestShowAllRecommendations &&
                    lastVisibleIndex != null &&
                    continuation != null &&
                    lastVisibleIndex >= len - 3
                ) {
                    viewModel.loadMoreYouTubeItems(continuation)
                }
            }
    }

    if (selectedChip != null) {
        BackHandler {

            viewModel.toggleChip(selectedChip)
        }
    }

    LaunchedEffect(showHomeCategoryChips, selectedChip) {
        if (!showHomeCategoryChips && selectedChip != null) {
            viewModel.toggleChip(selectedChip)
        }
    }

    LaunchedEffect(forgottenFavorites) {
        forgottenFavoritesLazyGridState.scrollToItem(0)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .liquidGlassScreenBackground()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .pullToRefresh(
                    state = pullRefreshState,
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refresh
                )
        ) {
            val horizontalLazyGridItemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
            val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor
            val forgottenFavoritesSnapLayoutInfoProvider = remember(forgottenFavoritesLazyGridState) {
                SnapLayoutInfoProvider(
                    lazyGridState = forgottenFavoritesLazyGridState,
                    positionInLayout = { layoutSize, itemSize ->
                        (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                    }
                )
            }

            LazyColumn(
                state = lazylistState,
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
            ) {
                item(key = "galax_home_identity") {
                    GalaxHomeIdentity(
                        title = stringResource(R.string.galax_home_title),
                        subtitle = stringResource(R.string.galax_home_subtitle),
                        accountName = accountName,
                        avatarUrl = url,
                    )
                }
                item(key = "galax_home_actions") {
                    GalaxHomeActions(
                        canShuffle = !shufflePending,
                        isShuffling = shufflePending,
                        onShuffle = {
                            if (!shufflePending) {
                                shufflePending = true
                                viewModel.playGalaxShuffle(playerConnection) { started ->
                                    shufflePending = false
                                    if (!started) {
                                        Toast.makeText(
                                            context,
                                            R.string.galax_shuffle_unavailable,
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }
                            }
                        },
                        onRecognition = {
                            navController.navigate(com.arturo254.opentune.ui.screens.musicrecognition.MusicRecognitionRoute)
                        },
                    )
                }
                featuredTrack?.let { featured ->
                    item(key = "galax_featured_track") {
                        val active = featured.id == mediaMetadata?.id
                        GalaxFeaturedTrack(featured, active && isPlaying) {
                            if (active) playerConnection.player.togglePlayPause()
                            else if (quickPicks.orEmpty().any { it.id == featured.id }) {
                                playerConnection.playQueue(
                                    com.arturo254.opentune.playback.queues.YouTubeQueue.radio(
                                        featured.toMediaMetadata(),
                                    ),
                                )
                            } else {
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = context.getString(R.string.galax_home_title),
                                        items = listOf(featured.toMediaItem()),
                                    ),
                                )
                            }
                        }
                    }
                }
                if (orbitSongs.isNotEmpty()) {
                    item(key = "galax_orbit_intro") {
                        GalaxHomeOrbitHeading(
                            title = "Tu música en órbita",
                            subtitle = "Tus canciones giran alrededor de lo que más te gusta.",
                            songCount = orbitSongs.size,
                        )
                    }
                    item(key = "galax_orbit_mosaic") {
                        GalaxHomeMixBoard(
                            songs = orbitSongs,
                            activeId = mediaMetadata?.id,
                            isPlaying = isPlaying,
                            onPlay = { song ->
                                if (song.id == mediaMetadata?.id) {
                                    playerConnection.player.togglePlayPause()
                                } else {
                                    playerConnection.playQueue(
                                        ListQueue(
                                            title = context.getString(R.string.galax_home_title),
                                            items = orbitSongs.map { it.toMediaItem() },
                                            startIndex = orbitSongs.indexOfFirst { it.id == song.id }
                                                .coerceAtLeast(0),
                                        ),
                                    )
                                }
                            },
                            onLongPress = { song ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    SongMenu(
                                        originalSong = song,
                                        navController = navController,
                                        metadata = allItemsMetadata[song.id],
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            },
                        )
                    }
                }

                if (showHomeCategoryChips && homePage?.chips?.isNotEmpty() == true) {
                    item(key = "galax_home_filter_heading") {
                        GalaxHomeChapterDivider("FILTRA TU GALAXIA")
                    }
                    item(key = "galax_home_filter_chips") {
                        ChipsRow(
                            chips = homePage?.chips.orEmpty().map { it to it.title },
                            currentValue = selectedChip,
                            onValueUpdate = {
                                viewModel.toggleChip(it)
                            }
                        )
                    }
                }

                item(key = "galax_chapter_personal") {
                    GalaxHomeChapterDivider("HECHO PARA TI  /  01")
                }

            keepListening?.takeIf { it.isNotEmpty() }?.let { items ->
                item {
                    GalaxHomeZoneHeader(
                        title = stringResource(R.string.keep_listening),
                        zone = GalaxHomeZone.RECENT,
                        modifier = Modifier.animateItem(),
                    )
                }

                item {
                    KeepListeningSection(
                        keepListening = items,
                        mediaMetadata = mediaMetadata,
                        isPlaying = isPlaying,
                        navController = navController,
                        playerConnection = playerConnection,
                        menuState = menuState,
                        haptic = haptic,
                        scope = scope,
                        metadataMap = allItemsMetadata
                    )
                }
            }


                quickPicks?.filterNot { it.id in highlightedIds }?.takeIf { it.isNotEmpty() }?.let { picks ->
                item {
                    GalaxHomeZoneHeader(
                        title = stringResource(R.string.quick_picks),
                        zone = GalaxHomeZone.CURATED,
                        modifier = Modifier.animateItem(),
                    )
                }

                item {
                    GalaxListeningDeck(
                        quickPicks = picks,
                        mediaMetadata = mediaMetadata,
                        isPlaying = isPlaying,
                        navController = navController,
                        playerConnection = playerConnection,
                        menuState = menuState,
                        haptic = haptic,
                        metadataMap = allItemsMetadata
                    )
                }
            }


            speedDialSongs.filterNot { it.id in highlightedIds }.takeIf { it.isNotEmpty() }?.let { songs ->
                item {
                    GalaxHomeZoneHeader(
                        title = stringResource(R.string.speed_dial),
                        zone = GalaxHomeZone.MIX,
                        modifier = Modifier.animateItem(),
                    )
                }

                item {
                    SpeedDialSection(
                        speedDialSongs = songs,
                        mediaMetadata = mediaMetadata,
                        isPlaying = isPlaying,
                        navController = navController,
                        playerConnection = playerConnection,
                        menuState = menuState,
                        haptic = haptic,
                        metadataMap = allItemsMetadata
                    )
                }
            }


            AccountPlaylistsContainer(
                viewModel = viewModel,
                accountName = accountName,
                accountImageUrl = url,
                mediaMetadata = mediaMetadata,
                isPlaying = isPlaying,
                navController = navController,
                playerConnection = playerConnection,
                menuState = menuState,
                haptic = haptic,
                scope = scope
            )

            forgottenFavorites?.takeIf { it.isNotEmpty() }?.let { favorites ->
                item {
                    GalaxHomeZoneHeader(
                        title = stringResource(R.string.forgotten_favorites),
                        zone = GalaxHomeZone.MEMORY,
                        modifier = Modifier.animateItem(),
                    )
                }

                item {
                    ForgottenFavoritesSection(
                        forgottenFavorites = favorites,
                        mediaMetadata = mediaMetadata,
                        isPlaying = isPlaying,
                        horizontalLazyGridItemWidth = horizontalLazyGridItemWidth,
                        lazyGridState = forgottenFavoritesLazyGridState,
                        snapLayoutInfoProvider = forgottenFavoritesSnapLayoutInfoProvider,
                        navController = navController,
                        playerConnection = playerConnection,
                        menuState = menuState,
                        haptic = haptic,
                        metadataMap = allItemsMetadata
                    )
                }
            }

                if (selectedChip == null) {
                    item(key = "galax_chapter_discovery") {
                        GalaxHomeChapterDivider("EXPLORAR EL UNIVERSO  /  02")
                    }
                    item(key = "global_catalog_title") {
                        GalaxHomeZoneHeader(
                            title = "Más allá de tu biblioteca",
                            zone = GalaxHomeZone.TRENDS,
                        )
                    }
                    item(key = "global_catalog_discovery") {
                        GlobalCatalogHomeSection(
                            tracks = globalCatalogTrending,
                            playerConnection = playerConnection,
                            onPlay = viewModel::playSourceVariant,
                            modifier = Modifier.animateItem(),
                        )
                    }
                }

            SimilarRecommendationsContainer(
                viewModel = viewModel,
                mediaMetadata = mediaMetadata,
                isPlaying = isPlaying,
                navController = navController,
                playerConnection = playerConnection,
                menuState = menuState,
                haptic = haptic,
                scope = scope
            )

            homePage?.sections?.takeIf { it.isNotEmpty() }?.let {
                item(key = "galax_chapter_catalog") {
                    GalaxHomeChapterDivider("RECOMENDADO PARA TU UNIVERSO  /  03")
                }
            }

            homePage?.sections?.take(
                if (showAllRecommendations) Int.MAX_VALUE else 3,
            )?.forEachIndexed { index, section ->
                item(key = "galax_home_remote_title_$index") {
                    HomePageSectionTitle(
                        section = section,
                        navController = navController,
                        modifier = Modifier.animateItem()
                    )
                }

                item(key = "galax_home_remote_content_$index") {
                    HomePageSectionContent(
                        section = section,
                        mediaMetadata = mediaMetadata,
                        isPlaying = isPlaying,
                        navController = navController,
                        playerConnection = playerConnection,
                        menuState = menuState,
                        haptic = haptic,
                        scope = scope,
                        metadataMap = allItemsMetadata
                    )
                }
            }

            if ((homePage?.sections?.size ?: 0) > 3 || homePage?.continuation != null) {
                item(key = "galax_home_more_discovery") {
                    GalaxHomeCatalogControl(
                        expanded = showAllRecommendations,
                        hiddenCount = ((homePage?.sections?.size ?: 0) - 3).coerceAtLeast(0),
                        onToggle = { showAllRecommendations = !showAllRecommendations },
                    )
                }
            }

            if (isLoading || (
                showAllRecommendations &&
                    homePage?.continuation != null &&
                    homePage?.sections?.isNotEmpty() == true
                )) {
                item {
                    HomeLoadingShimmer(modifier = Modifier.animateItem())
                }
            }
            }

            Indicator(
                isRefreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
            )
        }
    }
}
