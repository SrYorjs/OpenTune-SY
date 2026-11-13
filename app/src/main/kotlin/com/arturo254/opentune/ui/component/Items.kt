package com.arturo254.opentune.ui.component

import com.arturo254.opentune.extensions.metadata

import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.zIndex
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.Download.STATE_COMPLETED
import androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING
import androidx.media3.exoplayer.offline.Download.STATE_QUEUED
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.arturo254.opentune.LocalDatabase
import com.arturo254.opentune.LocalDownloadUtil
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.CropThumbnailToSquareKey
import com.arturo254.opentune.constants.DisableBlurKey
import com.arturo254.opentune.constants.GridThumbnailCornerRadius
import com.arturo254.opentune.constants.GridThumbnailHeight
import com.arturo254.opentune.constants.ListItemHeight
import com.arturo254.opentune.constants.ListThumbnailSize
import com.arturo254.opentune.constants.SwipeToSongKey
import com.arturo254.opentune.constants.ThumbnailCornerRadius
import com.arturo254.opentune.db.entities.Album
import com.arturo254.opentune.db.entities.Artist
import com.arturo254.opentune.db.entities.Playlist
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.AlbumItem
import com.arturo254.opentune.innertube.models.ArtistItem
import com.arturo254.opentune.innertube.models.PlaylistItem
import com.arturo254.opentune.innertube.models.SongItem
import com.arturo254.opentune.innertube.models.YTItem
import com.arturo254.opentune.models.ItemMetadata
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.playback.queues.LocalAlbumRadio
import com.arturo254.opentune.ui.theme.PlayerColorExtractor
import com.arturo254.opentune.ui.theme.extractThemeColor
import com.arturo254.opentune.ui.utils.resize
import com.arturo254.opentune.utils.joinByBullet
import com.arturo254.opentune.utils.makeTimeString
import com.arturo254.opentune.utils.rememberPreference
import com.arturo254.opentune.utils.reportException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

const val ActiveBoxAlpha = 0.6f

@Composable
inline fun ListItem(
    modifier: Modifier = Modifier,
    title: String,
    noinline subtitle: (@Composable RowScope.() -> Unit)? = null,
    thumbnailContent: @Composable () -> Unit,
    trailingContent: @Composable RowScope.() -> Unit = {},
    isActive: Boolean = false,
) = com.arturo254.opentune.ui.original.component.ListItem(
    modifier = modifier, title = title, subtitle = subtitle,
    thumbnailContent = thumbnailContent, trailingContent = trailingContent, isActive = isActive,
)

@Composable
fun ListItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String?,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailContent: @Composable () -> Unit,
    trailingContent: @Composable RowScope.() -> Unit = {},
    isActive: Boolean = false,
) = com.arturo254.opentune.ui.original.component.ListItem(
    modifier = modifier, title = title, subtitle = subtitle, badges = badges,
    thumbnailContent = thumbnailContent, trailingContent = trailingContent, isActive = isActive,
)

@Composable
fun GridItem(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    subtitle: @Composable () -> Unit,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailContent: @Composable BoxWithConstraintsScope.() -> Unit,
    thumbnailRatio: Float = 1f,
    fillMaxWidth: Boolean = false,
) = com.arturo254.opentune.ui.original.component.GridItem(
    modifier = modifier, title = title, subtitle = subtitle, badges = badges,
    thumbnailContent = thumbnailContent, thumbnailRatio = thumbnailRatio, fillMaxWidth = fillMaxWidth,
)

@Composable
private fun playlistCountText(
    playlist: Playlist,
    autoPlaylist: Boolean,
): String =
    if (autoPlaylist) {
        ""
    } else if (playlist.songCount == 0 && playlist.playlist.remoteSongCount != null) {
        pluralStringResource(
            R.plurals.n_song,
            playlist.playlist.remoteSongCount,
            playlist.playlist.remoteSongCount,
        )
    } else {
        pluralStringResource(
            R.plurals.n_song,
            playlist.songCount,
            playlist.songCount,
        )
    }

@Composable
private fun playlistPlaceholderIcon(
    playlist: Playlist,
    autoPlaylist: Boolean,
): Int =
    when (playlist.playlist.name) {
        stringResource(R.string.liked) -> R.drawable.favorite_border
        stringResource(R.string.offline) -> R.drawable.offline
        stringResource(R.string.cached_playlist) -> R.drawable.cached
        else -> if (autoPlaylist) R.drawable.trending_up else R.drawable.queue_music
    }

private val LibraryCardThumbnailSize = 72.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryPlaylistFeatureCard(
    playlist: Playlist,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    autoPlaylist: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    Row(modifier.fillMaxWidth().clip(shape).padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        com.arturo254.opentune.ui.screens.GalaxCoverMosaic(playlist.thumbnails, modifier = Modifier.size(64.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(playlist.playlist.name, style = MaterialTheme.typography.titleMedium,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(playlistCountText(playlist, autoPlaylist), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailingContent()
    }
}

@Composable
fun LibraryAlbumSpotlightCard(
    album: Album,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    onPlay: (() -> Unit)? = null,
) {
    Column(modifier.width(156.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(156.dp)) {
            com.arturo254.opentune.core.designsystem.component.Artwork(album.album.thumbnailUrl,
                Modifier.fillMaxSize(), shape = com.arturo254.opentune.core.designsystem.theme.GalaxScreenStyle.artworkShape)
            if (onPlay != null) LiquidGlassIconButton(onClick = onPlay, size = 40.dp, selected = isActive,
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)) {
                Icon(painterResource(if (isActive && isPlaying) R.drawable.pause else R.drawable.play),
                    stringResource(if (isActive && isPlaying) R.string.player_a11y_pause else R.string.play),
                    Modifier.size(20.dp))
            }
        }
        Text(album.album.title, style = MaterialTheme.typography.titleSmall, maxLines = 1,
            overflow = TextOverflow.Ellipsis, color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        Text(album.artists.joinToString { it.name }, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun LibraryArtistSpotlightCard(
    artist: Artist,
    modifier: Modifier = Modifier,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            LocalThumbnail(
                thumbnailUrl = artist.artist.thumbnailUrl,
                isActive = false,
                isPlaying = false,
                shape = CircleShape,
                modifier = Modifier.size(80.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = artist.artist.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = pluralStringResource(R.plurals.n_song, artist.songCount, artist.songCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row {
                trailingContent()
            }
        }
    }
}

enum class GridPosition {
    LEFT, RIGHT, SINGLE
}

@Composable
fun LibraryHeroFavoriteTile(
    title: String,
    @DrawableRes iconRes: Int,
    badgeText: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
) {
    val animatedColor by animateColorAsState(accentColor, spring())
    val shape = RoundedCornerShape(26.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        animatedColor.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.88f),
                    )
                )
            )
            .border(
                0.8.dp,
                animatedColor.copy(alpha = 0.20f),
                shape,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(animatedColor.copy(alpha = 0.16f)),
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = animatedColor,
                    modifier = Modifier.size(28.dp),
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = badgeText.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = animatedColor,
                    maxLines = 1,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Icon(
                painter = painterResource(R.drawable.chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
fun LibraryPinnedCollectionTile(
    title: String,
    @DrawableRes iconRes: Int,
    gridPosition: GridPosition,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
) {
    val animatedColor by animateColorAsState(accentColor, spring())
    val shape = RoundedCornerShape(22.dp)

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.72f))
            .border(
                0.75.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f),
                shape,
            )
            .padding(16.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(animatedColor.copy(alpha = 0.13f)),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = animatedColor,
                modifier = Modifier.size(22.dp),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun GridItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailContent: @Composable BoxWithConstraintsScope.() -> Unit,
    thumbnailRatio: Float = 1f,
    fillMaxWidth: Boolean = false,
) = com.arturo254.opentune.ui.original.component.GridItem(
    modifier = modifier, title = title, subtitle = subtitle, badges = badges,
    thumbnailContent = thumbnailContent, thumbnailRatio = thumbnailRatio, fillMaxWidth = fillMaxWidth,
)

@Composable
fun SongListItem(
    song: Song,
    modifier: Modifier = Modifier,
    albumIndex: Int? = null,
    viewCountText: String? = null,
    metadata: ItemMetadata? = null,
    showLikedIcon: Boolean = true,
    showInLibraryIcon: Boolean = false,
    showDownloadIcon: Boolean = true,
    badges: @Composable RowScope.() -> Unit = {
        if (metadata != null) {
            if (showLikedIcon && metadata.isLiked) Icon.Favorite()
            if (song.song.explicit) Icon.Explicit()
            if (showInLibraryIcon && metadata.isInLibrary) Icon.Library()
            if (showDownloadIcon) Icon.Download(metadata.downloadState)
        } else {
            if (showLikedIcon && song.song.liked) {
                Icon.Favorite()
            }
            if (song.song.explicit) {
                Icon.Explicit()
            }
            if (showInLibraryIcon && song.song.inLibrary != null) {
                Icon.Library()
            }
            if (showDownloadIcon) {
                val download by LocalDownloadUtil.current.getDownload(song.id)
                    .collectAsState(initial = null)
                Icon.Download(download?.state)
            }
        }
    },
    isSelected: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    isSwipeable: Boolean = true,
    swipeContentBackgroundColor: Color? = null,
    trailingContent: @Composable RowScope.() -> Unit = {},
)  = com.arturo254.opentune.ui.original.component.SongListItem(
    song = song, modifier = modifier, albumIndex = albumIndex, showLikedIcon = showLikedIcon,
    showInLibraryIcon = showInLibraryIcon, showDownloadIcon = showDownloadIcon,
    downloadState = metadata?.downloadState, badges = badges, isSelected = isSelected,
    isActive = isActive, isPlaying = isPlaying, isSwipeable = isSwipeable,
    backgroundColor = swipeContentBackgroundColor ?: Color.Unspecified,
    trailingContent = trailingContent,
)

@Composable
fun SongGridItem(
    song: Song,
    modifier: Modifier = Modifier,
    metadata: ItemMetadata? = null,
    showLikedIcon: Boolean = true,
    showInLibraryIcon: Boolean = false,
    showDownloadIcon: Boolean = true,
    badges: @Composable RowScope.() -> Unit = {
        if (metadata != null) {
            if (showLikedIcon && metadata.isLiked) Icon.Favorite()
            if (showInLibraryIcon && metadata.isInLibrary) Icon.Library()
            if (showDownloadIcon) Icon.Download(metadata.downloadState)
        } else {
            if (showLikedIcon && song.song.liked) {
                Icon.Favorite()
            }
            if (showInLibraryIcon && song.song.inLibrary != null) {
                Icon.Library()
            }
            if (showDownloadIcon) {
                val download by LocalDownloadUtil.current.getDownload(song.id).collectAsState(initial = null)
                Icon.Download(download?.state)
            }
        }
    },
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
)  = com.arturo254.opentune.ui.original.component.SongGridItem(
    song = song, modifier = modifier, showLikedIcon = showLikedIcon,
    showInLibraryIcon = showInLibraryIcon, showDownloadIcon = showDownloadIcon,
    downloadState = metadata?.downloadState, badges = badges, isActive = isActive,
    isPlaying = isPlaying, fillMaxWidth = fillMaxWidth,
)

@Composable
fun ArtistListItem(
    artist: Artist,
    modifier: Modifier = Modifier,
    metadata: ItemMetadata? = null,
    badges: @Composable RowScope.() -> Unit = {
        if (metadata != null) {
            if (metadata.isLiked) Icon.Favorite()
        } else if (artist.artist.bookmarkedAt != null) {
            Icon(
                painter = painterResource(R.drawable.favorite),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .size(18.dp)
                    .padding(end = 2.dp),
            )
        }
    },
    trailingContent: @Composable RowScope.() -> Unit = {},
)  = com.arturo254.opentune.ui.original.component.ArtistListItem(
    artist = artist, modifier = modifier, badges = badges, trailingContent = trailingContent,
)

@Composable
fun ArtistGridItem(
    artist: Artist,
    modifier: Modifier = Modifier,
    metadata: ItemMetadata? = null,
    badges: @Composable RowScope.() -> Unit = {
        if (metadata != null) {
            if (metadata.isLiked) Icon.Favorite()
        } else if (artist.artist.bookmarkedAt != null) {
            Icon.Favorite()
        }
    },
    fillMaxWidth: Boolean = false,
)  = com.arturo254.opentune.ui.original.component.ArtistGridItem(
    artist = artist, modifier = modifier, badges = badges, fillMaxWidth = fillMaxWidth,
)

@Composable
fun AlbumListItem(
    album: Album,
    modifier: Modifier = Modifier,
    metadata: ItemMetadata? = null,
    showLikedIcon: Boolean = true,
    badges: @Composable RowScope.() -> Unit = {
        if (metadata != null) {
            if (showLikedIcon && metadata.isLiked) Icon.Favorite()
            if (album.album.explicit) Icon.Explicit()
            Icon.Download(metadata.downloadState)
        } else {
            val database = LocalDatabase.current
            val downloadUtil = LocalDownloadUtil.current
            var songs by remember {
                mutableStateOf(emptyList<Song>())
            }

            LaunchedEffect(Unit) {
                database.albumSongs(album.id).collect {
                    songs = it
                }
            }

            var downloadState by remember {
                mutableStateOf(Download.STATE_STOPPED)
            }

            LaunchedEffect(songs) {
                if (songs.isEmpty()) return@LaunchedEffect
                downloadUtil.downloads.collect { downloads ->
                    downloadState = when {
                        songs.all { downloads[it.id]?.state == STATE_COMPLETED } -> STATE_COMPLETED
                        songs.all { downloads[it.id]?.state in listOf(STATE_QUEUED, STATE_DOWNLOADING, STATE_COMPLETED) } -> STATE_DOWNLOADING
                        else -> Download.STATE_STOPPED
                    }
                }
            }

            if (showLikedIcon && album.album.bookmarkedAt != null) {
                Icon.Favorite()
            }
            if (album.album.explicit) {
                Icon.Explicit()
            }
            Icon.Download(downloadState)
        }
    },
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
)  = com.arturo254.opentune.ui.original.component.AlbumListItem(
    album = album, modifier = modifier, showLikedIcon = showLikedIcon,
    downloadState = metadata?.downloadState, badges = badges,
    isActive = isActive, isPlaying = isPlaying, trailingContent = trailingContent,
)

@Composable
fun AlbumGridItem(
    album: Album,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope,
    metadata: ItemMetadata? = null,
    badges: @Composable RowScope.() -> Unit = {
        if (metadata != null) {
            if (metadata.isLiked) Icon.Favorite()
            if (album.album.explicit) Icon.Explicit()
            Icon.Download(metadata.downloadState)
        } else {
            val database = LocalDatabase.current
            val downloadUtil = LocalDownloadUtil.current
            var songs by remember { mutableStateOf(emptyList<Song>()) }

            LaunchedEffect(Unit) {
                database.albumSongs(album.id).collect { songs = it }
            }

            var downloadState by remember { mutableStateOf(Download.STATE_STOPPED) }

            LaunchedEffect(songs) {
                if (songs.isEmpty()) return@LaunchedEffect
                downloadUtil.downloads.collect { downloads ->
                    downloadState = when {
                        songs.all { downloads[it.id]?.state == STATE_COMPLETED } -> STATE_COMPLETED
                        songs.all { downloads[it.id]?.state in listOf(STATE_QUEUED, STATE_DOWNLOADING, STATE_COMPLETED) } -> STATE_DOWNLOADING
                        else -> Download.STATE_STOPPED
                    }
                }
            }

            if (album.album.bookmarkedAt != null) {
                Icon.Favorite()
            }
            if (album.album.explicit) {
                Icon.Explicit()
            }
            Icon.Download(downloadState)
        }
    },
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
)  = com.arturo254.opentune.ui.original.component.AlbumGridItem(
    album = album, modifier = modifier, coroutineScope = coroutineScope,
    downloadState = metadata?.downloadState, badges = badges,
    isActive = isActive, isPlaying = isPlaying, fillMaxWidth = fillMaxWidth,
)

@Composable
fun PlaylistListItem(
    playlist: Playlist,
    modifier: Modifier = Modifier,
    autoPlaylist: Boolean = false,
    metadata: ItemMetadata? = null,
    badges: @Composable RowScope.() -> Unit = {
        if (metadata != null) {
            if (metadata.isLiked) Icon.Favorite()
        }
    },
    trailingContent: @Composable RowScope.() -> Unit = {}
)  = com.arturo254.opentune.ui.original.component.PlaylistListItem(
    playlist = playlist, modifier = modifier, autoPlaylist = autoPlaylist,
    downloadState = metadata?.downloadState, badges = badges, trailingContent = trailingContent,
)

@Composable
fun OverlayPlaylistListItem(
    playlist: Playlist,
    modifier: Modifier = Modifier,
    autoPlaylist: Boolean = false,
    badges: @Composable RowScope.() -> Unit = {},
    trailingContent: @Composable RowScope.() -> Unit = {},
    onClick: (() -> Unit)? = null,
) {
    var showPreview by remember { mutableStateOf(false) }
    val backgroundUrl = playlist.thumbnails.getOrNull(0)
    val (disableBlur) = rememberPreference(DisableBlurKey, false)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick?.invoke() }
    ) {
        Box(modifier = Modifier.height(120.dp)) {
            if (!backgroundUrl.isNullOrBlank()) {
                AsyncImage(
                    model = backgroundUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
                            startY = 40f
                        )
                    )
                )
            } else {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlaylistThumbnail(
                    thumbnails = playlist.thumbnails,
                    size = 72.dp,
                    placeHolder = {
                        val painter = when (playlist.playlist.name) {
                            stringResource(R.string.liked) -> R.drawable.favorite_border
                            stringResource(R.string.offline) -> R.drawable.offline
                            stringResource(R.string.cached_playlist) -> R.drawable.cached
                            else -> if (autoPlaylist) R.drawable.trending_up else R.drawable.queue_music
                        }
                        Icon(
                            painter = painterResource(painter),
                            contentDescription = null,
                            tint = LocalContentColor.current.copy(alpha = 0.9f),
                            modifier = Modifier.size(36.dp)
                        )
                    },
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.playlist.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    val subtitle = if (autoPlaylist) "" else {
                        if (playlist.songCount == 0 && playlist.playlist.remoteSongCount != null) {
                            pluralStringResource(R.plurals.n_song, playlist.playlist.remoteSongCount, playlist.playlist.remoteSongCount)
                        } else {
                            pluralStringResource(R.plurals.n_song, playlist.songCount, playlist.songCount)
                        }
                    }
                    Text(
                        text = subtitle,
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(modifier = Modifier.padding(start = 8.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.End) {
                    trailingContent()
                }
            }
        }
    }

    if (showPreview) {
        AlertDialog(
            onDismissRequest = { showPreview = false },
            confirmButton = { TextButton(onClick = { showPreview = false }) { Text(stringResource(R.string.close_dialog)) } },
            text = {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)) {
                    AsyncImage(model = backgroundUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }
        )
    }
}

@Composable
fun PlaylistGridItem(
    playlist: Playlist,
    modifier: Modifier = Modifier,
    autoPlaylist: Boolean = false,
    metadata: ItemMetadata? = null,
    badges: @Composable RowScope.() -> Unit = {
        if (metadata != null) {
            if (metadata.isLiked) Icon.Favorite()
        }
    },
    fillMaxWidth: Boolean = false,
)  = com.arturo254.opentune.ui.original.component.PlaylistGridItem(
    playlist = playlist, modifier = modifier, autoPlaylist = autoPlaylist,
    downloadState = metadata?.downloadState, badges = badges, fillMaxWidth = fillMaxWidth,
)

@Composable
fun MediaMetadataListItem(
    mediaMetadata: MediaMetadata,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    shouldLoadImage: Boolean = true,
    trailingContent: @Composable RowScope.() -> Unit = {},
)  = com.arturo254.opentune.ui.original.component.MediaMetadataListItem(
    mediaMetadata = if (shouldLoadImage) mediaMetadata else mediaMetadata.copy(thumbnailUrl = null),
    modifier = modifier, isSelected = isSelected, isActive = isActive,
    isPlaying = isPlaying, trailingContent = trailingContent,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeListItem(
    item: YTItem,
    modifier: Modifier = Modifier,
    albumIndex: Int? = null,
    viewCountText: String? = null,
    metadata: ItemMetadata? = null,
    isSelected: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    isSwipeable: Boolean = true,
    trailingContent: @Composable RowScope.() -> Unit = {},
    badges: @Composable RowScope.() -> Unit = {
        if (metadata != null) {
            if (metadata.isLiked) Icon.Favorite()
            if (item.explicit) Icon.Explicit()
            if (metadata.isInLibrary) Icon.Library()
            if (item is SongItem) Icon.Download(metadata.downloadState)
        } else {
            val database = LocalDatabase.current
            val song by database.song(item.id).collectAsState(initial = null)
            val album by database.album(item.id).collectAsState(initial = null)

            if ((item is SongItem && song?.song?.liked == true) ||
                (item is AlbumItem && album?.album?.bookmarkedAt != null)
            ) {
                Icon.Favorite()
            }
            if (item.explicit) Icon.Explicit()
            if (item is SongItem && song?.song?.inLibrary != null) {
                Icon.Library()
            }
            if (item is SongItem) {
                val downloads by LocalDownloadUtil.current.downloads.collectAsState()
                Icon.Download(downloads[item.id]?.state)
            }
        }
    },
)  = com.arturo254.opentune.ui.original.component.YouTubeListItem(
    item = item, modifier = modifier, albumIndex = albumIndex,
    isSelected = isSelected, isActive = isActive, isPlaying = isPlaying,
    isSwipeable = isSwipeable, trailingContent = trailingContent,
    isLiked = metadata?.isLiked, downloadState = metadata?.downloadState, badges = badges,
)

@Composable
fun YouTubeGridItem(
    item: YTItem,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope? = null,
    metadata: ItemMetadata? = null,
    badges: @Composable RowScope.() -> Unit = {
        if (metadata != null) {
            if (metadata.isLiked) Icon.Favorite()
            if (item.explicit) Icon.Explicit()
            if (metadata.isInLibrary) Icon.Library()
            if (item is SongItem) Icon.Download(metadata.downloadState)
        } else {
            val database = LocalDatabase.current
            val song by database.song(item.id).collectAsState(initial = null)
            val album by database.album(item.id).collectAsState(initial = null)

            if (item is SongItem && song?.song?.liked == true ||
                item is AlbumItem && album?.album?.bookmarkedAt != null
            ) {
                Icon.Favorite()
            }
            if (item.explicit) Icon.Explicit()
            if (item is SongItem && song?.song?.inLibrary != null) Icon.Library()
            if (item is SongItem) {
                val downloads by LocalDownloadUtil.current.downloads.collectAsState()
                Icon.Download(downloads[item.id]?.state)
            }
        }
    },
    thumbnailRatio: Float = 1f,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
)  = com.arturo254.opentune.ui.original.component.YouTubeGridItem(
    item = item, modifier = modifier, coroutineScope = coroutineScope,
    isLiked = metadata?.isLiked, downloadState = metadata?.downloadState, badges = badges,
    thumbnailRatio = thumbnailRatio, isActive = isActive,
    isPlaying = isPlaying, fillMaxWidth = fillMaxWidth,
)

@Composable
fun LocalSongsGrid(
    title: String,
    subtitle: String,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailUrl: String?,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
    modifier: Modifier = Modifier,
) = com.arturo254.opentune.ui.original.component.LocalSongsGrid(
    title = title,
    subtitle = subtitle,
    badges = badges,
    thumbnailUrl = thumbnailUrl,
    isActive = isActive,
    isPlaying = isPlaying,
    fillMaxWidth = fillMaxWidth,
    modifier = modifier,
)

@Composable
fun LocalArtistsGrid(
    title: String,
    subtitle: String,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailUrl: String?,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
    modifier: Modifier = Modifier,
) = com.arturo254.opentune.ui.original.component.LocalArtistsGrid(
    title = title,
    subtitle = subtitle,
    badges = badges,
    thumbnailUrl = thumbnailUrl,
    isActive = isActive,
    isPlaying = isPlaying,
    fillMaxWidth = fillMaxWidth,
    modifier = modifier,
)

@Composable
fun LocalAlbumsGrid(
    title: String,
    subtitle: String,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailUrl: String?,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
    modifier: Modifier = Modifier,
) = com.arturo254.opentune.ui.original.component.LocalAlbumsGrid(
    title = title,
    subtitle = subtitle,
    badges = badges,
    thumbnailUrl = thumbnailUrl,
    isActive = isActive,
    isPlaying = isPlaying,
    fillMaxWidth = fillMaxWidth,
    modifier = modifier,
)

@Composable
fun ItemThumbnail(
    thumbnailUrl: String?,
    isActive: Boolean,
    isPlaying: Boolean,
    shape: Shape,
    modifier: Modifier = Modifier,
    albumIndex: Int? = null,
    isSelected: Boolean = false,
    shouldLoadImage: Boolean = true,
    @DrawableRes placeholderIconRes: Int? = null,
    thumbnailRatio: Float = 1f
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .aspectRatio(thumbnailRatio)
            .clip(shape)
    ) {
        val (cropThumbnailToSquare, _) = rememberPreference(CropThumbnailToSquareKey, false)
        val isYouTubeThumb = thumbnailUrl?.contains("ytimg.com", ignoreCase = true) == true
        val shouldApplySquareCrop = cropThumbnailToSquare && isYouTubeThumb && kotlin.math.abs(thumbnailRatio - 1f) < 0.001f
        val widthPx = if (maxWidth == Dp.Infinity) null else with(density) { maxWidth.roundToPx().coerceAtLeast(1) }
        val heightPx = if (maxHeight == Dp.Infinity) null else with(density) { maxHeight.roundToPx().coerceAtLeast(1) }

        if (albumIndex == null) {
            if (shouldLoadImage) {
                val request = remember(thumbnailUrl, widthPx, heightPx) {
                    ImageRequest.Builder(context)
                        .data(thumbnailUrl?.resize(544, 544))
                        .allowHardware(true)
                        .apply {
                            if (widthPx != null && heightPx != null) {
                                size(widthPx, heightPx)
                            }
                        }
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .let { if (shouldApplySquareCrop) it.aspectRatio(1f) else it }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }

        if (albumIndex != null) {
            AnimatedVisibility(
                visible = !isActive,
                enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
                exit = shrinkOut(shrinkTowards = Alignment.Center) + fadeOut()
            ) {
                Text(
                    text = albumIndex.toString(),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        if (isSelected) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
                    .clip(shape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    painter = painterResource(R.drawable.done),
                    contentDescription = null
                )
            }
        }

        PlayingIndicatorBox(
            isActive = isActive,
            playWhenReady = isPlaying,
            color = if (albumIndex != null) MaterialTheme.colorScheme.onBackground else Color.White,
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = if (albumIndex != null)
                        Color.Transparent
                    else
                        Color.Black.copy(alpha = ActiveBoxAlpha),
                    shape = shape
                )
        )
    }
}

@Composable
fun LocalThumbnail(
    thumbnailUrl: String?,
    isActive: Boolean,
    isPlaying: Boolean,
    shape: Shape,
    modifier: Modifier = Modifier,
    showCenterPlay: Boolean = false,
    playButtonVisible: Boolean = false,
    thumbnailRatio: Float = 1f,
) = com.arturo254.opentune.ui.original.component.LocalThumbnail(
    thumbnailUrl = thumbnailUrl,
    isActive = isActive,
    isPlaying = isPlaying,
    shape = shape,
    modifier = modifier,
    showCenterPlay = showCenterPlay,
    playButtonVisible = playButtonVisible,
    thumbnailRatio = thumbnailRatio,
)

@Composable
fun PlaylistThumbnail(
    thumbnails: List<String>,
    size: Dp,
    placeHolder: @Composable () -> Unit,
    shape: Shape,
) = com.arturo254.opentune.ui.original.component.PlaylistThumbnail(
    thumbnails = thumbnails,
    size = size,
    placeHolder = placeHolder,
    shape = shape,
)

@Composable
fun BoxScope.OverlayPlayButton(
    visible: Boolean
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
            .align(Alignment.Center)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = ActiveBoxAlpha))
        ) {
            Icon(
                painter = painterResource(R.drawable.play),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun BoxScope.AlbumPlayButton(
    visible: Boolean,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = ActiveBoxAlpha))
                .clickable(onClick = onClick)
        ) {
            Icon(
                painter = painterResource(R.drawable.play),
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}

@Composable
fun SwipeToSongBox(
    modifier: Modifier = Modifier,
    mediaItem: MediaItem,
    content: @Composable BoxScope.() -> Unit,
) {
    val database = LocalDatabase.current
    val currentSong by remember(database, mediaItem.mediaId) {
        database.song(mediaItem.mediaId)
    }.collectAsState(initial = null)
    val isInLibrary = currentSong?.song?.inLibrary != null
    val color = MaterialTheme.colorScheme.primary
    val label = stringResource(if (isInLibrary) R.string.remove_from_library else R.string.add_to_library)
    com.arturo254.opentune.ui.original.component.SongSwipeBox(
        modifier = modifier,
        library = {
            com.arturo254.opentune.ui.original.component.SwipeAction(
                icon = if (isInLibrary) R.drawable.library_add_check else R.drawable.library_add,
                color = color,
                label = label,
                mediaItem = { mediaItem },
            ) {
                database.transaction {
                    mediaItem.metadata?.let { insert(it) }
                    getSongByIdBlocking(mediaItem.mediaId)?.song?.let { update(it.toggleLibrary()) }
                }
            }
        },
        queue = {
            com.arturo254.opentune.ui.original.component.queueSwipeAction { mediaItem }
        },
        content = {
            Box(content = content)
        },
    )
}

private object Icon {
    @Composable
    fun Favorite() {
        Icon(
            painter = painterResource(R.drawable.favorite),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .size(18.dp)
                .padding(end = 2.dp)
        )
    }

    @Composable
    fun Library() {
        Icon(
            painter = painterResource(R.drawable.library_add_check),
            contentDescription = null,
            modifier = Modifier
                .size(18.dp)
                .padding(end = 2.dp)
        )
    }

    @Composable
    fun Download(state: Int?) {
        when (state) {
            STATE_COMPLETED -> Icon(
                painter = painterResource(R.drawable.offline),
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .padding(end = 2.dp)
            )
            STATE_QUEUED, STATE_DOWNLOADING -> CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier
                    .size(16.dp)
                    .padding(end = 2.dp)
            )
            else -> {   }
        }
    }

    @Composable
    fun Explicit() {
        Icon(
            painter = painterResource(R.drawable.explicit),
            contentDescription = null,
            modifier = Modifier
                .size(18.dp)
                .padding(end = 2.dp)
        )
    }
}
