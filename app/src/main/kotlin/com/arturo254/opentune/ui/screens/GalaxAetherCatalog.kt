package com.arturo254.opentune.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.innertube.models.AlbumItem
import com.arturo254.opentune.innertube.models.ArtistItem
import com.arturo254.opentune.innertube.models.PlaylistItem
import com.arturo254.opentune.innertube.models.SongItem
import com.arturo254.opentune.innertube.models.WatchEndpoint
import com.arturo254.opentune.innertube.models.YTItem
import com.arturo254.opentune.innertube.pages.HomePage
import com.arturo254.opentune.models.toMediaMetadata
import com.arturo254.opentune.playback.queues.YouTubeQueue
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.menu.YouTubeSongMenu
import com.arturo254.opentune.ui.menu.YouTubeAlbumMenu
import com.arturo254.opentune.ui.menu.YouTubeArtistMenu
import com.arturo254.opentune.ui.menu.YouTubePlaylistMenu

@Composable
internal fun AetherCatalogHeading(
    section: HomePage.Section,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (!section.label.isNullOrBlank()) {
                Text(
                    text = section.label.orEmpty().uppercase(),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.2.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-.45).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (section.endpoint?.browseId != null) {
            AetherGlassPlate(
                modifier = Modifier.size(40.dp),
                radius = 14.dp,
                emphasis = .50f,
                onClick = {
                    val target = section.endpoint?.browseId.orEmpty()
                    if (target == "FEmusic_moods_and_genres") {
                        navController.navigate(Screens.MoodAndGenres.route)
                    } else if (target.isNotBlank()) {
                        navController.navigate("browse/$target")
                    }
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_forward),
                    contentDescription = "Ver colección",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Center).size(18.dp),
                )
            }
        }
    }
}

@Composable
internal fun AetherCatalogShelf(
    section: HomePage.Section,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val entries = remember(section) {
        section.items.distinctBy { it.javaClass.simpleName + "/" + it.id }
    }
    if (entries.isEmpty()) return
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(horizontal = 22.dp),
    ) {
        itemsIndexed(
            entries,
            key = { index, item -> "aether_catalog:" + index + ":" + item.id },
            contentType = { _, item -> item.javaClass.simpleName },
        ) { index, item ->
            AetherCatalogItem(
                item = item,
                navController = navController,
                index = index,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AetherCatalogItem(
    item: YTItem,
    navController: NavController,
    index: Int,
    modifier: Modifier = Modifier,
) {
    val player = LocalPlayerConnection.current
    val current = player?.mediaMetadata?.collectAsState()?.value
    val playing = player?.isPlaying?.collectAsState()?.value == true
    val menu = LocalMenuState.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val playingThis = item is SongItem && current?.id == item.id
    val onPlay: () -> Unit = {
        when (item) {
            is SongItem -> {
                if (playingThis) {
                    player?.player?.togglePlayPause()
                } else {
                    player?.playQueue(
                        YouTubeQueue(
                            item.endpoint ?: WatchEndpoint(videoId = item.id),
                            item.toMediaMetadata(),
                        ),
                    )
                }
            }
            is AlbumItem -> navController.navigate("album/${item.id}")
            is ArtistItem -> navController.navigate("artist/${item.id}")
            is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
        }
        Unit
    }
    val onLongPress: () -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        menu.show {
            when (item) {
                is SongItem -> YouTubeSongMenu(
                    song = item,
                    navController = navController,
                    onDismiss = menu::dismiss,
                )
                is AlbumItem -> YouTubeAlbumMenu(
                    albumItem = item,
                    navController = navController,
                    onDismiss = menu::dismiss,
                )
                is ArtistItem -> YouTubeArtistMenu(
                    artist = item,
                    onDismiss = menu::dismiss,
                )
                is PlaylistItem -> YouTubePlaylistMenu(
                    playlist = item,
                    coroutineScope = scope,
                    onDismiss = menu::dismiss,
                )
            }
        }
    }

    Column(
        modifier = modifier.width(165.dp)
            .combinedClickable(onClick = onPlay, onLongClick = onLongPress),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        val artworkShape = if (item is ArtistItem) CircleShape else RoundedCornerShape(25.dp)
        Box(
            modifier = Modifier.size(165.dp).clip(artworkShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            AsyncImage(
                model = item.thumbnail,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            if (item !is ArtistItem) {
                Box(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .height(67.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, AetherInk.midnight.copy(alpha = .79f)),
                            ),
                        ),
                )
                Row(
                    modifier = Modifier.align(Alignment.BottomStart).padding(11.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(
                            if (playingThis && playing) R.drawable.pause
                            else if (item is SongItem) R.drawable.play
                            else R.drawable.arrow_forward
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = Color.White,
                    )
                    Text(
                        text = when (item) {
                            is SongItem -> if (playingThis && playing) "Sonando" else "Escuchar"
                            is AlbumItem -> "Álbum"
                            is ArtistItem -> "Artista"
                            is PlaylistItem -> "Lista"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
            }
        }
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = when (item) {
                is SongItem -> item.artists.joinToString { it.name }
                is AlbumItem -> item.artists.orEmpty().joinToString { it.name }
                is ArtistItem -> "Artista"
                is PlaylistItem -> item.author?.name.orEmpty()
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun AetherCatalogEmpty(
    modifier: Modifier = Modifier,
    onSearch: () -> Unit,
) {
    AetherGlassPlate(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp).height(142.dp),
        radius = 26.dp,
        emphasis = .55f,
        onClick = onSearch,
    ) {
        Column(
            modifier = Modifier.align(Alignment.CenterStart).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Explora tu música",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Abre Buscar para descubrir más",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            painter = painterResource(R.drawable.search),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 20.dp).size(24.dp),
        )
    }
}
