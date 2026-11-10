package com.arturo254.opentune.ui.screens.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.media3.exoplayer.offline.Download
import com.arturo254.opentune.LocalDownloadUtil
import com.arturo254.opentune.LocalDatabase
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.PureBlackKey
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.playback.queues.ListQueue
import com.arturo254.opentune.spotify.SpotifyLibraryViewModel
import com.arturo254.opentune.spotify.SpotifyMapper
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.menu.AlbumMenu
import com.arturo254.opentune.ui.menu.ArtistMenu
import com.arturo254.opentune.ui.menu.PlaylistMenu
import com.arturo254.opentune.ui.menu.SongMenu
import com.arturo254.opentune.ui.visual.library.LibraryEntityListScreen
import com.arturo254.opentune.ui.visual.library.LibraryGridEntry
import com.arturo254.opentune.ui.visual.library.LibraryGridScreen
import com.arturo254.opentune.ui.visual.library.LibraryPinKind
import com.arturo254.opentune.ui.visual.library.LibraryPinMenuItem
import com.arturo254.opentune.ui.visual.library.rememberLibraryPins
import com.arturo254.opentune.ui.visual.library.sortForLibraryIndex
import com.arturo254.opentune.utils.rememberPreference
import com.arturo254.opentune.viewmodels.LibraryMixViewModel
import com.arturo254.opentune.viewmodels.CachePlaylistViewModel

@Composable
fun OriginalLibraryCategoryScreen(
    navController: NavController,
    category: String,
    shelf: String = "all",
    viewModel: LibraryMixViewModel = hiltViewModel(),
    cacheViewModel: CachePlaylistViewModel = hiltViewModel(),
    spotifyViewModel: SpotifyLibraryViewModel = hiltViewModel(),
) {
    val database = LocalDatabase.current
    val songs by database.allSongs().collectAsStateWithLifecycle(emptyList())
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val cachedSongs by if (shelf == "cached") {
        cacheViewModel.cachedSongs.collectAsStateWithLifecycle()
    } else {
        remember { mutableStateOf(emptyList<Song>()) }
    }
    val spotify by spotifyViewModel.playlists.collectAsStateWithLifecycle()
    val downloads by LocalDownloadUtil.current.downloads.collectAsStateWithLifecycle()
    val pinned = rememberLibraryPins()
    val downloadedIds = remember(downloads) {
        downloads.values.filter { it.state == Download.STATE_COMPLETED }
            .map { it.request.id }.toSet()
    }
    val shelfSongs = remember(songs, cachedSongs, downloadedIds, shelf) {
        when (shelf) {
            "local" -> songs.filter { it.song.isLocal || it.id in downloadedIds }
            "cached" -> cachedSongs
            "imported" -> songs.filter { it.song.isLocal }
            else -> songs
        }
    }
    val albumIds = remember(shelfSongs) { shelfSongs.mapNotNull { it.song.albumId }.toSet() }
    val artistIds = remember(shelfSongs) {
        shelfSongs.flatMap { it.artists.map { artist -> artist.id } }.toSet()
    }
    val shelfAlbums = remember(albums, albumIds, shelf) {
        if (shelf == "all") albums else albums.filter { it.id in albumIds }
    }
    val shelfArtists = remember(artists, artistIds, shelf) {
        if (shelf == "all") artists else artists.filter { it.id in artistIds }
    }
    val playlistIdsInShelf by remember(shelfSongs) {
        database.playlistIdsContainingSongs(shelfSongs.map { it.id })
    }.collectAsStateWithLifecycle(emptyList())
    val shelfPlaylists = remember(playlists, shelf, playlistIdsInShelf) {
        when (shelf) {
            "local", "cached" -> playlists.filter { it.id in playlistIdsInShelf }
            "imported" -> playlists.filter { it.playlist.browseId != null }
            else -> playlists
        }
    }
    val (pureBlack) = rememberPreference(PureBlackKey, false)
    val player = LocalPlayerConnection.current
    val menus = LocalMenuState.current
    val scope = rememberCoroutineScope()
    val recent = remember(shelfSongs) {
        shelfSongs.filter { it.song.inLibrary != null }
            .sortedByDescending { it.song.inLibrary }
    }
    when (category) {
        "songs" -> {
            val ordered = remember(shelfSongs) { sortForLibraryIndex(shelfSongs) { it.title } }
            LibraryEntityListScreen(
                title = stringResource(R.string.songs),
                subtitle = stringResource(R.string.visual_library_songs_subtitle),
                searchPlaceholder = stringResource(R.string.visual_library_songs_hint),
                items = shelfSongs,
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
                        LibraryPinMenuItem(pinned, LibraryPinKind.SONG, song.id, menus::dismiss)
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
                items = shelfPlaylists,
                idOf = { it.id },
                nameOf = { it.title },
                subtitleOf = { it.songCount.toString() },
                thumbnailOf = { it.thumbnails.firstOrNull() },
                sharedElementKeyOf = { "playlist-" + it.id },
                onClick = { navController.navigate("local_playlist/" + it.id) },
                onLongClick = {
                    playlist -> menus.show {
                        LibraryPinMenuItem(pinned, LibraryPinKind.PLAYLIST, playlist.id, menus::dismiss)
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
                items = shelfArtists,
                idOf = { it.id },
                nameOf = { it.title },
                subtitleOf = { it.songCount.toString() },
                thumbnailOf = { it.thumbnailUrl },
                roundThumbnails = true,
                onClick = { navController.navigate("artist/" + it.id) },
                onLongClick = {
                    artist -> menus.show {
                        LibraryPinMenuItem(pinned, LibraryPinKind.ARTIST, artist.id, menus::dismiss)
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
            val entries = remember(shelfAlbums) {
                shelfAlbums.map {
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
                onPlay = { shelfAlbums.firstOrNull()?.let { navController.navigate("album/" + it.id) } },
                onShuffle = { shelfAlbums.randomOrNull()?.let { navController.navigate("album/" + it.id) } },
                pureBlack = pureBlack,
            )
        }
        "spotify" -> {
            LibraryEntityListScreen(
                title = stringResource(R.string.visual_library_spotify_playlists),
                subtitle = stringResource(R.string.visual_library_imported_explanation),
                searchPlaceholder = stringResource(R.string.visual_library_playlists_hint),
                items = spotify,
                idOf = { it.id },
                nameOf = { it.name },
                subtitleOf = { it.owner?.displayName },
                thumbnailOf = { SpotifyMapper.getPlaylistThumbnail(it) },
                onClick = { navController.navigate("spotify_playlist/" + it.id) },
                onLongClick = { navController.navigate("spotify_playlist/" + it.id) },
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
