package com.arturo254.opentune.ui.screens.library

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.arturo254.opentune.LocalDatabase
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.PureBlackKey
import com.arturo254.opentune.db.entities.Album
import com.arturo254.opentune.db.entities.Artist
import com.arturo254.opentune.db.entities.Playlist
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.playback.queues.ListQueue
import com.arturo254.opentune.ui.component.CreatePlaylistDialog
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.menu.AlbumMenu
import com.arturo254.opentune.ui.menu.ArtistMenu
import com.arturo254.opentune.ui.menu.PlaylistMenu
import com.arturo254.opentune.ui.menu.SongMenu
import com.arturo254.opentune.ui.visual.component.GlassState
import com.arturo254.opentune.ui.visual.library.LibraryCardEntry
import com.arturo254.opentune.ui.visual.library.LibraryCardGrid
import com.arturo254.opentune.ui.visual.library.LibraryEmptyState
import com.arturo254.opentune.ui.visual.library.LibraryListRow
import com.arturo254.opentune.ui.visual.library.LibraryManagePill
import com.arturo254.opentune.ui.visual.library.LibraryRule
import com.arturo254.opentune.ui.visual.library.LibraryScaffold
import com.arturo254.opentune.ui.visual.library.LibrarySectionTitle
import com.arturo254.opentune.ui.visual.library.LibraryWideCard
import com.arturo254.opentune.ui.visual.search.SearchAvatar
import com.arturo254.opentune.ui.visual.search.SearchHorizontalPadding
import com.arturo254.opentune.ui.visual.search.rememberCollapseProgress
import com.arturo254.opentune.utils.rememberPreference
import com.arturo254.opentune.viewmodels.LibraryMixViewModel
import androidx.media3.exoplayer.offline.Download
import com.arturo254.opentune.LocalDownloadUtil
import com.arturo254.opentune.spotify.SpotifyLibraryViewModel
import com.arturo254.opentune.spotify.SpotifyMapper
import com.arturo254.opentune.ui.visual.library.LibraryTab
import com.arturo254.opentune.ui.visual.library.LibraryTabChips
import com.arturo254.opentune.ui.visual.library.LibraryPinKind
import com.arturo254.opentune.ui.visual.library.LibraryPinnedTile
import com.arturo254.opentune.ui.visual.library.LibraryProgressCard
import com.arturo254.opentune.ui.visual.library.rememberLibraryPins
import com.arturo254.opentune.ui.visual.library.rememberLibraryImportUi
import com.arturo254.opentune.ui.visual.library.originalLocalTab
import com.arturo254.opentune.ui.visual.library.originalCachedTab
import com.arturo254.opentune.ui.visual.library.originalImportedTab
import com.arturo254.opentune.viewmodels.CachePlaylistViewModel
import kotlinx.coroutines.launch

@Composable
fun OriginalLibraryHomeScreen(
    navController: NavController,
    ambientActive: Boolean,
    onOpenSearchTab: () -> Unit,
    viewModel: LibraryMixViewModel = hiltViewModel(),
    cacheViewModel: CachePlaylistViewModel = hiltViewModel(),
    spotifyViewModel: SpotifyLibraryViewModel = hiltViewModel(),
) {
    val database = LocalDatabase.current
    val player = LocalPlayerConnection.current
    val menu = LocalMenuState.current
    val scope = rememberCoroutineScope()
    val songs by database.allSongs().collectAsStateWithLifecycle(emptyList())
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val cachedSongs by cacheViewModel.cachedSongs.collectAsStateWithLifecycle()
    val spotify by spotifyViewModel.playlists.collectAsStateWithLifecycle()
    val downloads by LocalDownloadUtil.current.downloads.collectAsStateWithLifecycle()
    val pinsState = rememberLibraryPins()
    val pins = pinsState.pins
    val importUi = rememberLibraryImportUi()
    val downloadedIds = remember(downloads) {
        downloads.values
            .filter { it.state == Download.STATE_COMPLETED }
            .map { it.request.id }
            .toSet()
    }
    val importedSongs = remember(songs) { songs.filter { it.song.isLocal } }
    val localSongs = remember(songs, downloadedIds) {
        songs.filter { it.song.isLocal || it.id in downloadedIds }
    }
    val syncedPlaylists = remember(playlists) {
        playlists.filter { it.playlist.browseId != null }
    }
    var tab by rememberSaveable { mutableStateOf(LibraryTab.All) }
    val tabSongs = when (tab) {
        LibraryTab.All -> songs
        LibraryTab.Local -> localSongs
        LibraryTab.Cached -> cachedSongs
        LibraryTab.Imported -> importedSongs
    }
    val tabAlbums = remember(albums, tabSongs, tab) {
        if (tab == LibraryTab.All) albums
        else {
            val ids = tabSongs.mapNotNull { it.song.albumId }.toSet()
            albums.filter { it.id in ids }
        }
    }
    val tabArtists = remember(artists, tabSongs, tab) {
        if (tab == LibraryTab.All) artists
        else {
            val ids = tabSongs.flatMap { it.artists.map { artist -> artist.id } }.toSet()
            artists.filter { it.id in ids }
        }
    }
    val tabPlaylists = when (tab) {
        LibraryTab.All -> playlists
        LibraryTab.Local -> playlists.filter { it.playlist.isLocal }
        LibraryTab.Cached -> emptyList()
        LibraryTab.Imported -> syncedPlaylists
    }
    val activeDownloads = remember(downloads) {
        downloads.values.filter {
            it.state == Download.STATE_DOWNLOADING ||
                it.state == Download.STATE_QUEUED ||
                it.state == Download.STATE_RESTARTING
        }
    }
    val downloadProgress = if (activeDownloads.isEmpty()) 1f else
        activeDownloads.map { it.percentDownloaded.takeIf { p -> p >= 0f } ?: 0f }
            .average().toFloat().div(100f).coerceIn(0f, 1f)
    val current = player?.mediaMetadata?.collectAsStateWithLifecycle()?.value
    val playing = player?.isPlaying?.collectAsStateWithLifecycle()?.value == true
    val (pureBlack) = rememberPreference(PureBlackKey, false)
    var createPlaylist by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val listState = rememberLazyListState()
    val glassState = remember { GlassState() }
    val collapse = rememberCollapseProgress(listState)
    val bottomInset = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
    val recentlyAdded = remember(songs) {
        songs.filter { it.song.inLibrary != null }
            .sortedByDescending { it.song.inLibrary }
    }
    val likedSongs = remember(songs) { songs.filter { it.song.liked } }
    val filteredSongs = remember(tabSongs, query.text) {
        tabSongs.filter {
            it.title.contains(query.text.trim(), ignoreCase = true) ||
                it.artists.any { artist -> artist.name.contains(query.text.trim(), ignoreCase = true) }
        }
    }
    val filteredAlbums = remember(tabAlbums, query.text) {
        tabAlbums.filter { it.title.contains(query.text.trim(), ignoreCase = true) }
    }
    val filteredArtists = remember(tabArtists, query.text) {
        tabArtists.filter { it.title.contains(query.text.trim(), ignoreCase = true) }
    }
    val filteredPlaylists = remember(tabPlaylists, query.text) {
        tabPlaylists.filter { it.title.contains(query.text.trim(), ignoreCase = true) }
    }
    val playSong: (Song) -> Unit = { song ->
        if (current?.id == song.id) {
            player?.player?.togglePlayPause()
        } else {
            player?.playQueue(
                ListQueue(
                    title = "Biblioteca",
                    items = filteredSongs.map { it.toMediaItem() },
                    startIndex = filteredSongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0),
                )
            )
        }
    }
    val songMenu: (Song) -> Unit = { song ->
        menu.show {
            SongMenu(song, navController = navController, onDismiss = menu::dismiss)
        }
    }
    val albumMenu: (Album) -> Unit = { album ->
        menu.show {
            AlbumMenu(originalAlbum = album, navController = navController, onDismiss = menu::dismiss)
        }
    }
    val artistMenu: (Artist) -> Unit = { artist ->
        menu.show {
            ArtistMenu(originalArtist = artist, coroutineScope = scope, onDismiss = menu::dismiss)
        }
    }
    val playlistMenu: (Playlist) -> Unit = { playlist ->
        menu.show {
            PlaylistMenu(playlist = playlist, coroutineScope = scope, onDismiss = menu::dismiss)
        }
    }
    val entry by navController.currentBackStackEntryAsState()
    val scrollToTop = entry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()
    LaunchedEffect(scrollToTop?.value, ambientActive) {
        if (ambientActive && scrollToTop?.value == true) {
            listState.animateScrollToItem(0)
            entry?.savedStateHandle?.set("scrollToTop", false)
        }
    }
    BackHandler(ambientActive && query.text.isNotEmpty()) {
        query = TextFieldValue()
    }
    BackHandler(ambientActive && query.text.isEmpty() && tab != LibraryTab.All) {
        tab = LibraryTab.All
    }
    if (createPlaylist) CreatePlaylistDialog(onDismiss = { createPlaylist = false })
    Box(Modifier.fillMaxSize()) {
        LibraryScaffold(
            title = stringResource(R.string.filter_library),
            subtitle = null,
            searchPlaceholder = stringResource(R.string.search_library),
            query = query,
            onQueryChange = { query = it },
            listState = listState,
            glassState = glassState,
            collapseProvider = { collapse.value },
            pureBlack = pureBlack,
            bottomInset = bottomInset,
            trailing = {
                SearchAvatar(
                    imageUrl = null,
                    onClick = { navController.navigate("account") },
                )
            },
        ) {
            item("library_tabs") {
                LibraryTabChips(
                    selected = tab,
                    onSelect = { next ->
                        tab = next
                        query = TextFieldValue()
                        scope.launch { listState.scrollToItem(0) }
                    },
                    modifier = Modifier.padding(horizontal = SearchHorizontalPadding),
                )
                Spacer(Modifier.height(14.dp))
            }
            if (tab == LibraryTab.All || tab == LibraryTab.Local) {
                item("library_progress") {
                    LibraryProgressCard(
                        title = stringResource(R.string.visual_library_manage_downloads),
                        running = activeDownloads.isNotEmpty(),
                        progress = downloadProgress,
                        onClick = { navController.navigate("download_queue") },
                        modifier = Modifier.padding(horizontal = SearchHorizontalPadding),
                    )
                    LibraryProgressCard(
                        title = stringResource(R.string.visual_library_import_running),
                        running = importUi.running,
                        progress = null,
                        onClick = { navController.navigate("settings/storage") },
                        modifier = Modifier.padding(horizontal = SearchHorizontalPadding),
                    )
                }
            }
            if (query.text.isNotBlank()) {
                if (filteredSongs.isNotEmpty()) {
                    item("results_songs_title") {
                        LibrarySectionTitle(stringResource(R.string.songs))
                        LibraryRule()
                    }
                    items(filteredSongs, key = { "song_" + it.id }) { song ->
                        LibraryListRow(
                            title = song.title,
                            subtitle = song.artists.joinToString { it.name },
                            thumbnailUrl = song.thumbnailUrl,
                            isActive = current?.id == song.id,
                            isPlaying = playing && current?.id == song.id,
                            songId = song.id,
                            onClick = { playSong(song) },
                            onLongClick = { songMenu(song) },
                        )
                    }
                }
                if (filteredPlaylists.isNotEmpty()) {
                    item("results_playlists_title") {
                        LibrarySectionTitle(stringResource(R.string.playlists))
                        LibraryRule()
                    }
                    items(filteredPlaylists, key = { "playlist_" + it.id }) { playlist ->
                        LibraryListRow(
                            title = playlist.title,
                            subtitle = playlist.songCount.toString(),
                            thumbnailUrl = playlist.thumbnails.firstOrNull(),
                            sharedElementKey = "playlist-" + playlist.id,
                            onClick = { navController.navigate("local_playlist/" + playlist.id) },
                            onLongClick = { playlistMenu(playlist) },
                        )
                    }
                }
                if (filteredAlbums.isNotEmpty()) {
                    item("results_albums_title") {
                        LibrarySectionTitle(stringResource(R.string.albums))
                        LibraryRule()
                    }
                    items(filteredAlbums, key = { "album_" + it.id }) { album ->
                        LibraryListRow(
                            title = album.title,
                            subtitle = album.artists.joinToString { it.name },
                            thumbnailUrl = album.thumbnailUrl,
                            sharedElementKey = "album-" + album.id,
                            onClick = { navController.navigate("album/" + album.id) },
                            onLongClick = { albumMenu(album) },
                        )
                    }
                }
                if (filteredArtists.isNotEmpty()) {
                    item("results_artists_title") {
                        LibrarySectionTitle(stringResource(R.string.artists))
                        LibraryRule()
                    }
                    items(filteredArtists, key = { "artist_" + it.id }) { artist ->
                        LibraryListRow(
                            title = artist.title,
                            subtitle = artist.songCount.toString(),
                            thumbnailUrl = artist.thumbnailUrl,
                            round = true,
                            onClick = { navController.navigate("artist/" + artist.id) },
                            onLongClick = { artistMenu(artist) },
                        )
                    }
                }
                if (filteredSongs.isEmpty() && filteredPlaylists.isEmpty() &&
                    filteredAlbums.isEmpty() && filteredArtists.isEmpty()
                ) {
                    item("empty_search") { LibraryEmptyState() }
                }
                return@LibraryScaffold
            }
            when (tab) {
                LibraryTab.Local -> {
                    originalLocalTab(
                        songs = localSongs,
                        albums = tabAlbums,
                        artists = tabArtists,
                        playlists = tabPlaylists,
                        importedFileCount = importedSongs.size,
                        onImport = importUi.openPicker,
                        onOpen = navController::navigate,
                        onPlay = playSong,
                        onSongMenu = songMenu,
                        nowPlayingId = current?.id,
                        isPlaying = playing,
                    )
                    return@LibraryScaffold
                }
                LibraryTab.Cached -> {
                    originalCachedTab(
                        songs = cachedSongs,
                        onOpen = navController::navigate,
                        onPlay = playSong,
                        onSongMenu = songMenu,
                        nowPlayingId = current?.id,
                        isPlaying = playing,
                    )
                    return@LibraryScaffold
                }
                LibraryTab.Imported -> {
                    originalImportedTab(
                        localSongs = importedSongs,
                        localAlbums = tabAlbums,
                        syncPlaylists = syncedPlaylists,
                        spotifyPlaylists = spotify,
                        onOpen = navController::navigate,
                        onImport = importUi.openPicker,
                        onSync = {
                            viewModel.syncAllLibrary()
                            spotifyViewModel.refreshPlaylists()
                        },
                    )
                    return@LibraryScaffold
                }
                LibraryTab.All -> Unit
            }
            if (songs.isEmpty() && albums.isEmpty() && artists.isEmpty() && playlists.isEmpty()) {
                item("empty_library") {
                    LibraryEmptyState()
                    com.arturo254.opentune.ui.visual.search.SearchSimpleRow(
                        text = stringResource(R.string.search),
                        onClick = onOpenSearchTab,
                    )
                }
                return@LibraryScaffold
            }
            item("playlist_create") {
                LibraryManagePill(
                    text = stringResource(R.string.visual_library_create_playlist),
                    onClick = { createPlaylist = true },
                )
                Spacer(Modifier.height(16.dp))
            }
            item("categories") {
                LibraryCardGrid(
                    entries = listOf(
                        LibraryCardEntry(
                            stringResource(R.string.songs),
                            songs.size,
                            songs.take(4).map { it.thumbnailUrl },
                            { navController.navigate("library/songs") },
                        ),
                        LibraryCardEntry(
                            stringResource(R.string.playlists),
                            playlists.size,
                            playlists.take(4).flatMap { it.thumbnails.take(1) },
                            { navController.navigate("library/playlists") },
                        ),
                        LibraryCardEntry(
                            stringResource(R.string.albums),
                            albums.size,
                            albums.take(4).map { it.thumbnailUrl },
                            { navController.navigate("library/albums") },
                        ),
                        LibraryCardEntry(
                            stringResource(R.string.artists),
                            artists.size,
                            artists.take(4).map { it.thumbnailUrl },
                            { navController.navigate("library/artists") },
                            roundCovers = true,
                        ),
                    )
                )
            }
            if (recentlyAdded.isNotEmpty()) {
                item("recent") {
                    Spacer(Modifier.height(10.dp))
                    LibraryWideCard(
                        title = stringResource(R.string.visual_library_recently_added),
                        count = recentlyAdded.size,
                        covers = recentlyAdded.take(4).map { it.thumbnailUrl },
                        onClick = { navController.navigate("library/added") },
                        modifier = Modifier.padding(horizontal = SearchHorizontalPadding),
                    )
                }
            }
            if (likedSongs.isNotEmpty()) {
                item("liked") {
                    Spacer(Modifier.height(10.dp))
                    LibraryWideCard(
                        title = stringResource(R.string.liked),
                        count = likedSongs.size,
                        covers = likedSongs.take(4).map { it.thumbnailUrl },
                        onClick = { navController.navigate("auto_playlist/liked") },
                        modifier = Modifier.padding(horizontal = SearchHorizontalPadding),
                        sharedElementKey = "playlist-liked",
                    )
                }
            }
            item("bottom") { Spacer(Modifier.height(24.dp)) }
        }
    }
}
