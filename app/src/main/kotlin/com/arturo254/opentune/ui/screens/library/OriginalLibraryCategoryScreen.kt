package com.arturo254.opentune.ui.screens.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.arturo254.opentune.LocalDatabase
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.PureBlackKey
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.playback.queues.ListQueue
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.menu.AlbumMenu
import com.arturo254.opentune.ui.menu.ArtistMenu
import com.arturo254.opentune.ui.menu.PlaylistMenu
import com.arturo254.opentune.ui.menu.SongMenu
import com.arturo254.opentune.ui.visual.library.LibraryEntityListScreen
import com.arturo254.opentune.ui.visual.library.LibraryGridEntry
import com.arturo254.opentune.ui.visual.library.LibraryGridScreen
import com.arturo254.opentune.ui.visual.library.sortForLibraryIndex
import com.arturo254.opentune.utils.rememberPreference
import com.arturo254.opentune.viewmodels.LibraryMixViewModel

@Composable
fun OriginalLibraryCategoryScreen(
    navController: NavController,
    category: String,
    viewModel: LibraryMixViewModel = hiltViewModel(),
) {
    val database = LocalDatabase.current
    val songs by database.allSongs().collectAsStateWithLifecycle(emptyList())
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val (pureBlack) = rememberPreference(PureBlackKey, false)
    val player = LocalPlayerConnection.current
    val menus = LocalMenuState.current
    val scope = rememberCoroutineScope()
    val recent = remember(songs) {
        songs.filter { it.song.inLibrary != null }
            .sortedByDescending { it.song.inLibrary }
    }
    when (category) {
        "songs" -> {
            val ordered = remember(songs) { sortForLibraryIndex(songs) { it.title } }
            LibraryEntityListScreen(
                title = stringResource(R.string.songs),
                subtitle = stringResource(R.string.visual_library_songs_subtitle),
                searchPlaceholder = stringResource(R.string.visual_library_songs_hint),
                items = songs,
                idOf = { it.id },
                nameOf = { it.title },
                subtitleOf = { it.artists.joinToString { artist -> artist.name } },
                thumbnailOf = { it.thumbnailUrl },
                playingIdOf = { it.id },
                onClick = { song ->
                    player?.playQueue(
                        ListQueue(
                            title = "Biblioteca",
                            items = ordered.map { it.toMediaItem() },
                            startIndex = ordered.indexOfFirst { it.id == song.id }.coerceAtLeast(0),
                        )
                    )
                },
                onLongClick = { song ->
                    menus.show {
                        SongMenu(
                            originalSong = song,
                            navController = navController,
                            onDismiss = menus::dismiss,
                        )
                    }
                },
                pureBlack = pureBlack,
            )
        }
        "playlists" -> {
            LibraryEntityListScreen(
                title = stringResource(R.string.playlists),
                subtitle = stringResource(R.string.visual_library_playlists_subtitle),
                searchPlaceholder = stringResource(R.string.visual_library_playlists_hint),
                items = playlists,
                idOf = { it.id },
                nameOf = { it.title },
                subtitleOf = { it.songCount.toString() },
                thumbnailOf = { it.thumbnails.firstOrNull() },
                sharedElementKeyOf = { "playlist-" + it.id },
                onClick = { navController.navigate("local_playlist/" + it.id) },
                onLongClick = {
                    playlist -> menus.show {
                        PlaylistMenu(
                            playlist = playlist,
                            coroutineScope = scope,
                            onDismiss = menus::dismiss,
                        )
                    }
                },
                pureBlack = pureBlack,
            )
        }
        "artists" -> {
            LibraryEntityListScreen(
                title = stringResource(R.string.artists),
                subtitle = stringResource(R.string.visual_library_artists_subtitle),
                searchPlaceholder = stringResource(R.string.visual_library_artists_hint),
                items = artists,
                idOf = { it.id },
                nameOf = { it.title },
                subtitleOf = { it.songCount.toString() },
                thumbnailOf = { it.thumbnailUrl },
                roundThumbnails = true,
                onClick = { navController.navigate("artist/" + it.id) },
                onLongClick = {
                    artist -> menus.show {
                        ArtistMenu(
                            originalArtist = artist,
                            coroutineScope = scope,
                            onDismiss = menus::dismiss,
                        )
                    }
                },
                pureBlack = pureBlack,
            )
        }
        "albums" -> {
            val entries = remember(albums) {
                albums.map {
                    LibraryGridEntry(
                        id = it.id,
                        title = it.title,
                        subtitle = it.artists.joinToString { artist -> artist.name },
                        thumbnailUrl = it.thumbnailUrl,
                        sharedElementKey = "album-" + it.id,
                    )
                }
            }
            LibraryGridScreen(
                title = stringResource(R.string.albums),
                subtitle = stringResource(R.string.visual_library_albums_subtitle),
                searchPlaceholder = stringResource(R.string.visual_library_albums_hint),
                entries = entries,
                onOpen = { navController.navigate("album/" + it.id) },
                onPlay = { albums.firstOrNull()?.let { navController.navigate("album/" + it.id) } },
                onShuffle = { albums.randomOrNull()?.let { navController.navigate("album/" + it.id) } },
                pureBlack = pureBlack,
            )
        }
        "added" -> {
            val entries = remember(recent) {
                recent.map {
                    LibraryGridEntry(
                        id = it.id,
                        title = it.title,
                        subtitle = it.artists.joinToString { artist -> artist.name },
                        thumbnailUrl = it.thumbnailUrl,
                    )
                }
            }
            val play: (Int, Boolean) -> Unit = { index, shuffled ->
                if (recent.isNotEmpty()) {
                    val ordered = if (shuffled) recent.shuffled() else recent
                    player?.playQueue(
                        ListQueue(
                            title = "Agregados recientemente",
                            items = ordered.map { it.toMediaItem() },
                            startIndex = if (shuffled) 0 else index,
                        )
                    )
                }
            }
            LibraryGridScreen(
                title = stringResource(R.string.visual_library_recently_added),
                subtitle = stringResource(R.string.visual_library_added_subtitle),
                searchPlaceholder = stringResource(R.string.visual_library_songs_hint),
                entries = entries,
                onOpen = { entry -> play(recent.indexOfFirst { it.id == entry.id }.coerceAtLeast(0), false) },
                onPlay = { play(0, false) },
                onShuffle = { play(0, true) },
                pureBlack = pureBlack,
            )
        }
    }
}
