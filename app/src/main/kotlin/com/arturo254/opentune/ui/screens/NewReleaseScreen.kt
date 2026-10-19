package com.arturo254.opentune.ui.screens

import androidx.compose.runtime.remember
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.arturo254.opentune.ui.screens.GalaxTopAppBar as TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.innertube.models.AlbumItem
import com.arturo254.opentune.innertube.models.AlbumReleaseType
import com.arturo254.opentune.ui.component.IconButton
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.component.shimmer.ShimmerHost
import com.arturo254.opentune.ui.menu.YouTubeAlbumMenu
import com.arturo254.opentune.ui.utils.backToMain
import com.arturo254.opentune.viewmodels.NewReleaseUiState
import com.arturo254.opentune.viewmodels.NewReleaseViewModel

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun NewReleaseScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: NewReleaseViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val uiState by viewModel.uiState.collectAsState()
    val albumsList by viewModel.albums.collectAsState(initial = emptyList())
    val singlesList by viewModel.singles.collectAsState(initial = emptyList())
    val epsList by viewModel.eps.collectAsState(initial = emptyList())

    val coroutineScope = rememberCoroutineScope()
    val systemBarsTopPadding =
        with(LocalDensity.current) { WindowInsets.systemBars.getTop(this).toDp() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        LazyColumn(
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {

            item { Spacer(modifier = Modifier.height(72.dp)) }

            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AnimatedVisibility(
                        visible = uiState is NewReleaseUiState.Success,
                        enter = fadeIn(tween(500)) + slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                            initialOffsetY = { -it / 2 },
                        ),
                    ) {
                        NewReleaseHero(
                            albumCount = albumsList.size,
                            singleCount = singlesList.size,
                            epCount = epsList.size,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                }
            }

            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AnimatedVisibility(visible = uiState is NewReleaseUiState.Loading) {
                        ShimmerHost {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(96.dp)
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            )
                        }
                    }
                }
            }

            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AnimatedVisibility(
                        visible = uiState is NewReleaseUiState.Success && albumsList.isNotEmpty(),
                        enter = fadeIn(tween(600)) + slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                            initialOffsetY = { -it / 2 },
                        ),
                    ) {
                        FeaturedCarousel(
                            albums = albumsList,
                            isPlaying = isPlaying,
                            mediaMetadata = mediaMetadata,
                            navController = navController,
                            haptic = haptic,
                            menuState = menuState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                        )
                    }
                }
            }

            item {
                ReleaseSectionHeader(
                    title = stringResource(R.string.albums),
                    count = if (uiState is NewReleaseUiState.Loading) null else albumsList.size,
                    iconRes = R.drawable.album,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 4.dp
                    ),
                )
            }
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AnimatedContent(
                        targetState = uiState is NewReleaseUiState.Loading,
                        transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(250)) },
                        label = "albums_carousel",
                    ) { isLoading ->
                        if (isLoading || albumsList.isEmpty()) {
                            CarouselShimmerRow()
                        } else {
                            ExpressiveCarousel(
                                items = albumsList,
                                releaseType = AlbumReleaseType.ALBUM,
                                isPlaying = isPlaying,
                                mediaMetadata = mediaMetadata,
                                navController = navController,
                                haptic = haptic,
                                menuState = menuState,
                            )
                        }
                    }
                }
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
            }

            item {
                ReleaseSectionHeader(
                    title = stringResource(R.string.singles),
                    count = if (uiState is NewReleaseUiState.Loading) null else singlesList.size,
                    iconRes = R.drawable.music_note,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 4.dp
                    ),
                )
            }
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AnimatedContent(
                        targetState = uiState is NewReleaseUiState.Loading,
                        transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(250)) },
                        label = "singles_carousel",
                    ) { isLoading ->
                        if (isLoading || singlesList.isEmpty()) {
                            CarouselShimmerRow()
                        } else {
                            ExpressiveCarousel(
                                items = singlesList,
                                releaseType = AlbumReleaseType.SINGLE,
                                isPlaying = isPlaying,
                                mediaMetadata = mediaMetadata,
                                navController = navController,
                                haptic = haptic,
                                menuState = menuState,
                            )
                        }
                    }
                }
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
            }

            item {
                ReleaseSectionHeader(
                    title = "EPs",
                    count = if (uiState is NewReleaseUiState.Loading) null else epsList.size,
                    iconRes = R.drawable.queue_music,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 4.dp
                    ),
                )
            }
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AnimatedContent(
                        targetState = uiState is NewReleaseUiState.Loading,
                        transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(250)) },
                        label = "eps_carousel",
                    ) { isLoading ->
                        if (isLoading || epsList.isEmpty()) {
                            CarouselShimmerRow()
                        } else {
                            ExpressiveCarousel(
                                items = epsList,
                                releaseType = AlbumReleaseType.EP,
                                isPlaying = isPlaying,
                                mediaMetadata = mediaMetadata,
                                navController = navController,
                                haptic = haptic,
                                menuState = menuState,
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            when (val state = uiState) {
                is NewReleaseUiState.Error -> item {
                    ErrorState(
                        message = state.throwable?.message
                            ?: stringResource(R.string.error_unknown),
                        onRetry = viewModel::retry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                    )
                }

                is NewReleaseUiState.Empty -> item {
                    EmptyState(
                        onRefresh = viewModel::retry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                    )
                }

                else -> Unit
            }
        }

        TopAppBar(
            modifier = Modifier.align(Alignment.TopCenter),
            title = { Text(stringResource(R.string.new_release_albums)) },
            navigationIcon = {
                IconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain,
                ) {
                    Icon(
                        painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                    )
                }
            },
            scrollBehavior = scrollBehavior,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NewReleaseHero(
    albumCount: Int,
    singleCount: Int,
    epCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.new_release_albums), style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold)
        GalaxMediaFacts(listOf(
            "$albumCount ${stringResource(R.string.albums)}",
            "$singleCount ${stringResource(R.string.singles)}", "$epCount EPs",
        ))
    }
}

@Composable
private fun HeroStat(
    count: Int,
    label: String,
    iconRes: Int,
    containerColor: Color,
    contentColor: Color,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = containerColor,
            modifier = Modifier.size(52.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                (fadeIn(tween(300)) + slideInVertically { it / 2 }) togetherWith
                        fadeOut(tween(200))
            },
            label = "hero_count_$label",
        ) { c ->
            Text(
                text = c.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun VerticalDividerLine() {
    Box(
        modifier = Modifier
            .height(52.dp)
            .width(1.dp)
            .background(
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(1.dp),
            ),
    )
}

@Composable
private fun ReleaseSectionHeader(
    title: String,
    count: Int?,
    iconRes: Int,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = containerColor,
            modifier = Modifier.size(36.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )

        AnimatedVisibility(visible = count != null && count > 0) {
            Surface(
                shape = RoundedCornerShape(50),
                color = containerColor,
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class
)
@Composable
private fun FeaturedCarousel(
    albums: List<AlbumItem>,
    isPlaying: Boolean,
    mediaMetadata: com.arturo254.opentune.models.MediaMetadata?,
    navController: NavController,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    menuState: com.arturo254.opentune.ui.component.MenuState,
    modifier: Modifier = Modifier,
) {
    val distinctItems = remember(albums) { albums.distinctBy { it.id }.take(5) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GalaxSectionHeading(stringResource(R.string.galax_featured))
        androidx.compose.foundation.lazy.LazyRow(contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(distinctItems, key = { it.id }) { album ->
                FeaturedCarouselCard(album, mediaMetadata?.album?.id == album.id, isPlaying,
                    onClick = { navController.navigate("album/${album.id}") },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show { YouTubeAlbumMenu(albumItem = album, navController = navController, onDismiss = menuState::dismiss) }
                    })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FeaturedCarouselCard(
    album: AlbumItem,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    GalaxReleaseCard(album, isActive, isPlaying, onClick, onLongClick, Modifier.width(208.dp))
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class
)
@Composable
private fun ExpressiveCarousel(
    items: List<AlbumItem>,
    releaseType: AlbumReleaseType,
    isPlaying: Boolean,
    mediaMetadata: com.arturo254.opentune.models.MediaMetadata?,
    navController: NavController,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    menuState: com.arturo254.opentune.ui.component.MenuState,
) {
    val distinctItems = remember(items) { items.distinctBy { it.id } }
    androidx.compose.foundation.lazy.LazyRow(contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(distinctItems, key = { it.id }) { album ->
            GalaxReleaseCard(album, mediaMetadata?.album?.id == album.id, isPlaying,
                onClick = { navController.navigate("album/${album.id}") },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    menuState.show { YouTubeAlbumMenu(albumItem = album, navController = navController, onDismiss = menuState::dismiss) }
                }, modifier = Modifier.width(164.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExpressiveCarouselCard(
    album: AlbumItem,
    releaseType: AlbumReleaseType,
    isActive: Boolean,
    isPlaying: Boolean,
    shape: RoundedCornerShape,
    accentColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    GalaxReleaseCard(album, isActive, isPlaying, onClick, onLongClick, Modifier.width(164.dp))
}

@Composable
private fun CarouselShimmerRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(4) {
            ShimmerHost {
                Column(
                    modifier = Modifier.width(148.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(148.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    )
                    Box(
                        modifier = Modifier
                            .width(96.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    )
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.size(64.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.error),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Text(
            text = stringResource(R.string.error_unknown),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Icon(
                painter = painterResource(R.drawable.replay),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.retry))
        }
    }
}

@Composable
private fun EmptyState(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(64.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.library_music),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Text(
            text = stringResource(R.string.no_results_found),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
        )
        Button(
            onClick = onRefresh,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        ) {
            Icon(
                painter = painterResource(R.drawable.replay),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.refresh))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalaxReleaseCard(album: AlbumItem, isActive: Boolean, isPlaying: Boolean,
    onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            com.arturo254.opentune.core.designsystem.component.Artwork(album.thumbnail, Modifier.fillMaxSize(),
                shape = com.arturo254.opentune.core.designsystem.theme.GalaxScreenStyle.artworkShape)
            if (isActive) Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = .7f),
                modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp)) {
                Icon(painterResource(if (isPlaying) R.drawable.pause else R.drawable.play), null,
                    Modifier.padding(8.dp).size(18.dp))
            }
        }
        Text(album.title, style = MaterialTheme.typography.titleSmall, maxLines = 2,
            overflow = TextOverflow.Ellipsis, color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        Text(album.artists.orEmpty().joinToString { it.name }, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
