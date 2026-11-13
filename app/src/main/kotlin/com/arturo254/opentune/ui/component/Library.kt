package com.arturo254.opentune.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.arturo254.opentune.R
import com.arturo254.opentune.db.entities.Album
import com.arturo254.opentune.db.entities.Artist
import com.arturo254.opentune.db.entities.Playlist
import com.arturo254.opentune.ui.original.component.LibraryAlbumGridItem as OriginalAlbumGrid
import com.arturo254.opentune.ui.original.component.LibraryAlbumListItem as OriginalAlbumList
import com.arturo254.opentune.ui.original.component.LibraryArtistGridItem as OriginalArtistGrid
import com.arturo254.opentune.ui.original.component.LibraryArtistListItem as OriginalArtistList
import com.arturo254.opentune.ui.original.component.LibraryPlaylistListItem as OriginalPlaylistList
import kotlinx.coroutines.CoroutineScope

@Composable
fun LibraryArtistListItem(
    navController: NavController,
    menuState: MenuState,
    coroutineScope: CoroutineScope,
    artist: Artist,
    modifier: Modifier = Modifier,
) = OriginalArtistList(
    navController = navController,
    menuState = menuState,
    coroutineScope = coroutineScope,
    artist = artist,
    modifier = modifier,
)

@Composable
fun LibraryArtistGridItem(
    navController: NavController,
    menuState: MenuState,
    coroutineScope: CoroutineScope,
    artist: Artist,
    modifier: Modifier = Modifier,
) = OriginalArtistGrid(
    navController = navController,
    menuState = menuState,
    coroutineScope = coroutineScope,
    artist = artist,
    modifier = modifier,
)

@Composable
fun LibraryAlbumListItem(
    modifier: Modifier = Modifier,
    navController: NavController,
    menuState: MenuState,
    album: Album,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
) = OriginalAlbumList(
    modifier = modifier,
    navController = navController,
    menuState = menuState,
    album = album,
    isActive = isActive,
    isPlaying = isPlaying,
)

@Composable
fun LibraryAlbumGridItem(
    modifier: Modifier = Modifier,
    navController: NavController,
    menuState: MenuState,
    coroutineScope: CoroutineScope,
    album: Album,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
) = OriginalAlbumGrid(
    modifier = modifier,
    navController = navController,
    menuState = menuState,
    coroutineScope = coroutineScope,
    album = album,
    isActive = isActive,
    isPlaying = isPlaying,
)

@Composable
fun LibraryPlaylistListItem(
    navController: NavController,
    menuState: MenuState,
    coroutineScope: CoroutineScope,
    playlist: Playlist,
    modifier: Modifier = Modifier,
    showDragHandle: Boolean = false,
    dragHandleModifier: Modifier = Modifier,
) {
    if (showDragHandle) {
        Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            OriginalPlaylistList(
                navController = navController,
                menuState = menuState,
                coroutineScope = coroutineScope,
                playlist = playlist,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier.size(48.dp).then(dragHandleModifier),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painter = painterResource(R.drawable.drag_handle), contentDescription = null)
            }
        }
    } else {
        OriginalPlaylistList(
            navController = navController,
            menuState = menuState,
            coroutineScope = coroutineScope,
            playlist = playlist,
            modifier = modifier,
        )
    }
}
