package com.arturo254.opentune.ui.screens.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import androidx.compose.runtime.remember
import com.arturo254.opentune.spotify.SpotifyMapper
import com.arturo254.opentune.spotify.models.SpotifyPlaylist
import com.arturo254.opentune.R
import com.arturo254.opentune.core.designsystem.liquid.galaxLiquidChrome
import com.arturo254.opentune.db.entities.Album
import com.arturo254.opentune.db.entities.Artist
import com.arturo254.opentune.db.entities.Playlist
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.ui.screens.GalaxEditorialBackdrop
import com.arturo254.opentune.ui.screens.GalaxEditorialOrb
import com.arturo254.opentune.ui.screens.GalaxEditorialSurface
import com.arturo254.opentune.ui.screens.GalaxEditorialReveal
import com.arturo254.opentune.ui.screens.GalaxEditorialHeading
import kotlin.math.min

internal enum class ArchiveSection {
    ALL, SONGS, ALBUMS, ARTISTS, PLAYLISTS
}

internal data class ArchiveCounter(
    val songs: Int,
    val albums: Int,
    val artists: Int,
    val playlists: Int,
)

@Composable
internal fun ArchiveBackdrop(
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier.fillMaxSize()) {
        GalaxEditorialBackdrop(
            modifier = Modifier.fillMaxSize(),
            density = 31,
            active = active,
            intensity = .44f,
            accent = scheme.primary,
            secondary = scheme.tertiary,
        )
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.verticalGradient(
                    .0f to Color.Transparent,
                    .54f to scheme.background.copy(alpha = .1f),
                    1f to scheme.background.copy(alpha = .98f),
                )
            )
        }
    }
}

@Composable
internal fun ArchiveHeader(
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 24.dp, end = 21.dp, top = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = "GALAXOT",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 2.8.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Biblioteca",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.5).sp,
            )
        }
        ArchiveIconControl(
            icon = R.drawable.search,
            description = "Buscar en biblioteca",
            onClick = onSearch,
        )
        ArchiveIconControl(
            icon = R.drawable.more_horiz,
            description = "Opciones",
            onClick = onSettings,
        )
    }
}

@Composable
internal fun ArchiveIconControl(
    icon: Int,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier.size(45.dp).clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .galaxLiquidChrome(shape, emphasis = .68f)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(21.dp),
        )
    }
}

@Composable
internal fun ArchiveHero(
    counters: ArchiveCounter,
    imageUrls: List<String>,
    active: Boolean,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val midnight = Color(0xFF08111F)
    Box(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 19.dp, vertical = 5.dp)
            .height(322.dp)
            .clip(RoundedCornerShape(33.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF13223F),
                        Color(0xFF14213A),
                        midnight,
                    ),
                ),
            ),
    ) {
        GalaxEditorialBackdrop(
            modifier = Modifier.fillMaxSize(),
            density = 30,
            active = active,
            accent = Color(0xFFA8C3FF),
            secondary = Color(0xFFBCA9F5),
            intensity = .9f,
        )
        GalaxEditorialOrb(
            modifier = Modifier.fillMaxSize(),
            active = active,
            rings = 4,
            accent = Color(0xFF94C1FF),
            secondary = Color(0xFFA8A0E7),
        )
        Row(
            modifier = Modifier.align(Alignment.TopEnd)
                .padding(end = 19.dp, top = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            imageUrls.take(3).forEachIndexed { index, url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size((59 - index * 6).dp)
                        .clip(RoundedCornerShape(14.dp)),
                )
            }
        }
        Box(
            modifier = Modifier.matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            midnight.copy(alpha = .18f),
                            midnight.copy(alpha = .94f),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart)
                .fillMaxWidth().padding(horizontal = 24.dp, vertical = 23.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            Text(
                text = "TU COLECCIÓN",
                color = Color(0xFFBBD5FF),
                letterSpacing = 2.sp,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                text = "Todo lo que\nes tuyo.",
                style = MaterialTheme.typography.headlineMedium,
                lineHeight = 37.sp,
                letterSpacing = (-1.15).sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ArchiveMetric("Canciones", counters.songs, Modifier.weight(1f))
                ArchiveMetric("Álbumes", counters.albums, Modifier.weight(1f))
                ArchiveMetric("Listas", counters.playlists, Modifier.weight(1f))
                Box(
                    modifier = Modifier.size(47.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = .15f))
                        .galaxLiquidChrome(RoundedCornerShape(16.dp), emphasis = .65f)
                        .clickable(onClick = onPlay),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = "Reproducir",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ArchiveMetric(title: String, count: Int, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = count.toString(),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = title,
            color = Color.White.copy(alpha = .68f),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

@Composable
internal fun ArchiveSectionTabs(
    selected: ArchiveSection,
    counter: ArchiveCounter,
    onSelected: (ArchiveSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf(
        ArchiveSection.ALL to "Todo",
        ArchiveSection.SONGS to "Canciones",
        ArchiveSection.ALBUMS to "Álbumes",
        ArchiveSection.ARTISTS to "Artistas",
        ArchiveSection.PLAYLISTS to "Listas",
    )
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(horizontal = 19.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        tabs.forEach { (tab, title) ->
            val isActive = tab == selected
            val shape = RoundedCornerShape(17.dp)
            Row(
                modifier = Modifier.height(43.dp).clip(shape)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = .16f)
                        else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = .70f),
                    )
                    .galaxLiquidChrome(shape, emphasis = if (isActive) 1f else .48f)
                    .clickable { onSelected(tab) }
                    .padding(horizontal = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                if (isActive) {
                    Box(
                        Modifier.size(6.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
internal fun ArchiveChapter(
    title: String,
    onOpen: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    GalaxEditorialHeading(
        title = title,
        modifier = modifier,
        action = if (onOpen != null) "Ver todo" else null,
        onAction = onOpen,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ArchiveSongRow(
    song: Song,
    selected: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    val palette = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 19.dp)
            .clip(shape)
            .background(if (selected) palette.primary.copy(alpha = .08f) else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 9.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(
            modifier = Modifier.size(59.dp).clip(RoundedCornerShape(16.dp))
                .background(palette.surfaceContainerHigh),
        ) {
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) palette.primary else palette.onSurface,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
            Text(
                text = song.artists.joinToString { it.name },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = palette.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier.size(39.dp).clip(RoundedCornerShape(13.dp))
                .background(palette.surfaceContainerLow)
                .galaxLiquidChrome(RoundedCornerShape(13.dp), emphasis = .48f),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(
                    if (selected && playing) R.drawable.pause else R.drawable.play,
                ),
                contentDescription = null,
                tint = palette.primary,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ArchiveAlbumCard(
    album: Album,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(162.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ArchiveArtwork(album.thumbnailUrl, Modifier.size(162.dp))
        Text(
            text = album.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = album.artists.joinToString { it.name },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun ArchiveArtistCard(
    artist: Artist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(120.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier.size(111.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            AsyncImage(
                model = artist.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Text(
            text = artist.title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ArchivePlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.width(162.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier.size(162.dp)
                .clip(RoundedCornerShape(23.dp))
                .background(
                    Brush.linearGradient(
                        listOf(colors.surfaceContainerHigh, colors.primary.copy(alpha = .14f)),
                    ),
                ),
        ) {
            val images = playlist.thumbnails.take(4)
            if (images.isNotEmpty()) {
                ArchiveArtwork(images.first(), Modifier.fillMaxSize())
            } else {
                Icon(
                    painter = painterResource(R.drawable.queue_music),
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.align(Alignment.Center).size(48.dp),
                )
            }
        }
        Text(
            text = playlist.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = playlist.songCount.toString() + " canciones",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
internal fun ArchiveArtwork(
    url: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(23.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.album),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center).size(36.dp),
            )
        }
    }
}

@Composable
internal fun ArchiveEmpty(
    title: String,
    subtitle: String,
    onExplore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GalaxEditorialSurface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp).height(232.dp),
        radius = 28.dp,
        onClick = onExplore,
    ) {
        GalaxEditorialOrb(
            modifier = Modifier.matchParentSize(),
            active = false,
            rings = 3,
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}

@Composable
internal fun ArchiveQuickAccess(
    onLiked: () -> Unit,
    onOffline: () -> Unit,
    onTop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 19.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ArchiveShortcut(
            title = "Favoritos",
            icon = R.drawable.favorite_filled,
            modifier = Modifier.weight(1f),
            onClick = onLiked,
        )
        ArchiveShortcut(
            title = "Offline",
            icon = R.drawable.download,
            modifier = Modifier.weight(1f),
            onClick = onOffline,
        )
        ArchiveShortcut(
            title = "Mi top",
            icon = R.drawable.trending_up,
            modifier = Modifier.weight(1f),
            onClick = onTop,
        )
    }
}

@Composable
private fun ArchiveShortcut(
    title: String,
    icon: Int,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    GalaxEditorialSurface(
        modifier = modifier.height(95.dp),
        radius = 21.dp,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(13.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(21.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun ArchiveSpotifyCard(
    playlist: SpotifyPlaylist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val thumbnail = remember(playlist) { SpotifyMapper.getPlaylistThumbnail(playlist) }
    Column(
        modifier = modifier.width(162.dp).clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(162.dp)) {
            ArchiveArtwork(thumbnail, Modifier.fillMaxSize())
            Box(
                modifier = Modifier.align(Alignment.BottomEnd)
                    .padding(9.dp).size(29.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.spotify_icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = (playlist.tracks?.total ?: 0).toString() + " canciones",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
internal fun ArchiveCollectionRow(
    title: String,
    subtitle: String,
    imageUrl: String?,
    kind: ArchiveSection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(21.dp)
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 19.dp)
            .clip(shape)
            .background(scheme.surfaceContainerLow.copy(alpha = .50f))
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(72.dp)
                .clip(
                    if (kind == ArchiveSection.ARTISTS) CircleShape
                    else RoundedCornerShape(17.dp)
                )
                .background(scheme.surfaceContainerHigh),
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    painter = painterResource(
                        when (kind) {
                            ArchiveSection.PLAYLISTS -> R.drawable.queue_music
                            ArchiveSection.ARTISTS -> R.drawable.person
                            else -> R.drawable.album
                        }
                    ),
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.align(Alignment.Center).size(28.dp),
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            painter = painterResource(R.drawable.arrow_forward),
            contentDescription = null,
            tint = scheme.primary,
            modifier = Modifier.size(19.dp),
        )
        Spacer(Modifier.width(3.dp))
    }
}
