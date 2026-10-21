package com.arturo254.opentune.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.ShowHomeCategoryChipsKey
import com.arturo254.opentune.core.designsystem.component.GalaxOrbitField
import com.arturo254.opentune.core.designsystem.liquid.galaxLiquidChrome
import com.arturo254.opentune.core.designsystem.theme.galaxScreenBackground
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.models.toMediaMetadata
import com.arturo254.opentune.playback.queues.ListQueue
import com.arturo254.opentune.playback.queues.YouTubeQueue
import com.arturo254.opentune.ui.component.ChipsRow
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.menu.SongMenu
import com.arturo254.opentune.utils.rememberPreference
import com.arturo254.opentune.viewmodels.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val connection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val menuState = LocalMenuState.current
    val quickPicks by viewModel.quickPicks.collectAsState()
    val speedDial by viewModel.speedDialSongs.collectAsState()
    val favorites by viewModel.forgottenFavorites.collectAsState()
    val keepListening by viewModel.keepListening.collectAsState()
    val homePage by viewModel.homePage.collectAsState()
    val selectedChip by viewModel.selectedChip.collectAsState()
    val trending by viewModel.globalCatalogTrending.collectAsState()
    val metadataMap by viewModel.allItemsMetadata.collectAsState()
    val accountName by viewModel.accountName.collectAsState()
    val accountImage by viewModel.accountImageUrl.collectAsState()
    val current by connection.mediaMetadata.collectAsState()
    val isPlaying by connection.isPlaying.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val (showChips) = rememberPreference(ShowHomeCategoryChipsKey, true)
    val refreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var shufflePending by remember { mutableStateOf(false) }
    var showAllRecommendations by rememberSaveable { mutableStateOf(false) }

    val recentSongs = remember(keepListening) {
        keepListening.orEmpty().filterIsInstance<Song>().distinctBy { it.id }.take(8)
    }
    val selections = remember(quickPicks, speedDial, favorites, recentSongs) {
        (quickPicks.orEmpty() + speedDial + favorites.orEmpty() + recentSongs)
            .distinctBy { it.id }
    }
    val featured = selections.firstOrNull()
    val currentPage by rememberUpdatedState(homePage)
    val showAll by rememberUpdatedState(showAllRecommendations)
    val backStack by navController.currentBackStackEntryAsState()
    val scrollToTop = backStack?.savedStateHandle
        ?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            listState.animateScrollToItem(0)
            backStack?.savedStateHandle?.set("scrollToTop", false)
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            info.visibleItemsInfo.lastOrNull()?.index to info.totalItemsCount
        }.collect { (last, count) ->
            val continuation = currentPage?.continuation
            if (showAll && continuation != null && last != null && last >= count - 3) {
                viewModel.loadMoreYouTubeItems(continuation)
            }
        }
    }
    BackHandler(enabled = selectedChip != null) { viewModel.toggleChip(selectedChip) }
    LaunchedEffect(showChips, selectedChip) {
        if (!showChips && selectedChip != null) viewModel.toggleChip(selectedChip)
    }

    val playSong: (Song, List<Song>) -> Unit = { song, queue ->
        if (current?.id == song.id) {
            connection.player.togglePlayPause()
        } else {
            connection.playQueue(
                ListQueue(
                    title = context.getString(R.string.home),
                    items = queue.map { it.toMediaItem() },
                    startIndex = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0),
                )
            )
        }
    }
    val openMenu: (Song) -> Unit = { song ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        menuState.show {
            SongMenu(
                originalSong = song,
                navController = navController,
                metadata = metadataMap[song.id],
                onDismiss = menuState::dismiss,
            )
        }
    }

    Box(Modifier.fillMaxSize().galaxScreenBackground()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().pullToRefresh(
                state = refreshState,
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refresh,
            ),
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            verticalArrangement = Arrangement.spacedBy(23.dp),
        ) {
            item("header") {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(start = 23.dp, end = 23.dp, top = 23.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("GALAXOT", color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 2.5.sp, fontWeight = FontWeight.Medium)
                        Text(stringResource(R.string.home),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold, letterSpacing = (-1.4).sp)
                        if (!accountName.isNullOrBlank()) {
                            Text(accountName.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Box(
                        Modifier.size(46.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .galaxLiquidChrome(CircleShape)
                            .clickable { navController.navigate("account") },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!accountImage.isNullOrBlank()) {
                            AsyncImage(accountImage, null, Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop)
                        } else {
                            Icon(painterResource(R.drawable.person), null,
                                tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
            if (featured != null) {
                item("feature") {
                    GalaxHomeCover(
                        song = featured,
                        isPlaying = current?.id == featured.id && isPlaying,
                        onPlay = { playSong(featured, selections) },
                        onLongClick = { openMenu(featured) },
                    )
                }
            }
            item("actions") {
                Row(Modifier.fillMaxWidth().padding(horizontal = 23.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GalaxHomeActionPill(
                        title = stringResource(R.string.shuffle),
                        icon = R.drawable.shuffle,
                        busy = shufflePending,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (!shufflePending) {
                            shufflePending = true
                            viewModel.playGalaxShuffle(connection) { started ->
                                shufflePending = false
                                if (!started) Toast.makeText(context,
                                    R.string.galax_shuffle_unavailable, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    GalaxHomeActionPill(
                        title = stringResource(R.string.search),
                        icon = R.drawable.search,
                        modifier = Modifier.weight(1f),
                    ) { navController.navigate(Screens.Search.route) }
                    GalaxHomeActionPill(
                        title = stringResource(R.string.galax_your_library),
                        icon = R.drawable.library_outlined,
                        modifier = Modifier.weight(1f),
                    ) { navController.navigate(Screens.Library.route) }
                }
            }
            if (showChips && homePage?.chips?.isNotEmpty() == true) {
                item("filters") {
                    ChipsRow(
                        chips = homePage?.chips.orEmpty().map { it to it.title },
                        currentValue = selectedChip,
                        onValueUpdate = viewModel::toggleChip,
                    )
                }
            }
            if (recentSongs.isNotEmpty()) {
                item("recent_title") { GalaxHomeSimpleHeading(stringResource(R.string.keep_listening)) }
                item("recent") {
                    LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 23.dp),
                        horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                        items(recentSongs, key = { it.id }) { song ->
                            GalaxHomeAlbumCard(song, current?.id == song.id && isPlaying,
                                onClick = { playSong(song, recentSongs) },
                                onLongClick = { openMenu(song) })
                        }
                    }
                }
            }
            val picks = quickPicks.orEmpty().distinctBy { it.id }.take(8)
            if (picks.isNotEmpty()) {
                item("picks_title") { GalaxHomeSimpleHeading(stringResource(R.string.quick_picks)) }
                items(picks, key = { "pick:" + it.id }) { song ->
                    GalaxHomeTrackRow(song, current?.id == song.id && isPlaying,
                        onClick = { playSong(song, picks) },
                        onLongClick = { openMenu(song) })
                }
            }
            if (speedDial.isNotEmpty()) {
                item("speed_title") { GalaxHomeSimpleHeading(stringResource(R.string.speed_dial)) }
                item("speed") {
                    LazyRow(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 23.dp),
                        horizontalArrangement = Arrangement.spacedBy(13.dp),
                    ) {
                        items(speedDial.distinctBy { it.id }, key = { it.id }) { song ->
                            GalaxHomeAlbumCard(song, current?.id == song.id && isPlaying,
                                onClick = { playSong(song, speedDial) },
                                onLongClick = { openMenu(song) })
                        }
                    }
                }
            }
            if (trending.isNotEmpty() && selectedChip == null) {
                item("trending_title") { GalaxHomeSimpleHeading("Tendencias") }
                item("trending") {
                    GlobalCatalogHomeSection(tracks = trending, playerConnection = connection,
                        onPlay = viewModel::playSourceVariant)
                }
            }
            val sections = homePage?.sections.orEmpty()
            if (sections.isNotEmpty()) {
                item("catalog_title") { GalaxHomeSimpleHeading("Descubre") }
                sections.take(if (showAllRecommendations) Int.MAX_VALUE else 3)
                    .forEachIndexed { index, section ->
                        item("catalog_title_$index") {
                            HomePageSectionTitle(section = section, navController = navController)
                        }
                        item("catalog_$index") {
                            HomePageSectionContent(
                                section = section,
                                mediaMetadata = current,
                                isPlaying = isPlaying,
                                navController = navController,
                                playerConnection = connection,
                                menuState = menuState,
                                haptic = haptic,
                                scope = scope,
                                metadataMap = metadataMap,
                            )
                        }
                    }
                if (sections.size > 3 || homePage?.continuation != null) {
                    item("catalog_more") {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            TextButton(onClick = { showAllRecommendations = !showAllRecommendations }) {
                                Text(if (showAllRecommendations) "Ver menos" else "Ver más")
                            }
                        }
                    }
                }
            }
            if (isLoading) {
                item("loading") {
                    Box(Modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(26.dp), strokeWidth = 2.dp)
                    }
                }
            }
            item("bottom") { Spacer(Modifier.height(16.dp)) }
        }
        Indicator(
            isRefreshing = isRefreshing, state = refreshState,
            modifier = Modifier.align(Alignment.TopCenter)
                .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalaxHomeCover(song: Song, isPlaying: Boolean, onPlay: () -> Unit, onLongClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(30.dp)
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(304.dp)
            .clip(shape).background(
                Brush.linearGradient(listOf(Color(0xFF12233B),
                    MaterialTheme.colorScheme.primary.copy(alpha = .35f), Color(0xFF070E1E)))
            ).combinedClickable(onClick = onPlay, onLongClick = onLongClick),
    ) {
        GalaxOrbitField(Modifier.fillMaxSize(), active = isPlaying, intensity = .44f)
        AsyncImage(
            model = song.thumbnailUrl, contentDescription = null,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 21.dp)
                .size(166.dp).clip(RoundedCornerShape(23.dp)),
            contentScale = ContentScale.Crop,
        )
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(
            listOf(Color(0xFF081322).copy(alpha = .88f),
                Color(0xFF081322).copy(alpha = .45f), Color.Transparent)
        )))
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth(.72f).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text("EN ROTACIÓN", style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = .65f), letterSpacing = 1.8.sp)
            Text(song.title, style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2,
                overflow = TextOverflow.Ellipsis, lineHeight = 28.sp)
            Text(song.artists.joinToString { it.name },
                color = Color.White.copy(alpha = .7f),
                style = MaterialTheme.typography.bodySmall, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Box(
                Modifier.size(49.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = .16f))
                    .galaxLiquidChrome(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                    contentDescription = null, tint = Color.White, modifier = Modifier.size(27.dp))
            }
        }
    }
}

@Composable
private fun GalaxHomeSimpleHeading(title: String) {
    Row(Modifier.fillMaxWidth().padding(start = 23.dp, end = 23.dp, bottom = 0.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, letterSpacing = (-.45).sp,
            modifier = Modifier.weight(1f))
    }
}

@Composable
private fun GalaxHomeActionPill(title: String, icon: Int, modifier: Modifier = Modifier,
                                busy: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(19.dp)
    Row(modifier.height(49.dp).clip(shape)
        .background(MaterialTheme.colorScheme.surfaceContainerLow)
        .galaxLiquidChrome(shape, .63f).clickable(enabled = !busy, onClick = onClick)
        .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
        } else {
            Icon(painterResource(icon), null, modifier = Modifier.size(17.dp),
                tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(7.dp))
        Text(title, style = MaterialTheme.typography.labelSmall, maxLines = 1,
            overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalaxHomeAlbumCard(song: Song, playing: Boolean,
                               onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(Modifier.width(148.dp).combinedClickable(onClick = onClick, onLongClick = onLongClick),
        verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.size(148.dp).clip(RoundedCornerShape(21.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            AsyncImage(song.thumbnailUrl, null, Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop)
            if (playing) {
                Box(Modifier.align(Alignment.BottomEnd).padding(9.dp)
                    .size(31.dp).clip(CircleShape)
                    .background(Color.Black.copy(alpha = .6f))
                    .galaxLiquidChrome(CircleShape), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.pause), null,
                        Modifier.size(17.dp), tint = Color.White)
                }
            }
        }
        Text(song.title, style = MaterialTheme.typography.titleSmall,
            maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
        Text(song.artists.joinToString { it.name },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalaxHomeTrackRow(song: Song, playing: Boolean,
                              onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        .clip(RoundedCornerShape(18.dp))
        .combinedClickable(onClick = onClick, onLongClick = onLongClick)
        .padding(horizontal = 5.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)) {
        AsyncImage(song.thumbnailUrl, null,
            Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (playing) FontWeight.SemiBold else FontWeight.Normal,
                color = if (playing) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface)
            Text(song.artists.joinToString { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(painterResource(if (playing) R.drawable.pause else R.drawable.play), null,
            Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
