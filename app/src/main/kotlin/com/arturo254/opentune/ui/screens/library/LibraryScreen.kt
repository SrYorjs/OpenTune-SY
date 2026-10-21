package com.arturo254.opentune.ui.screens.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.arturo254.opentune.LocalDatabase
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.db.entities.Album
import com.arturo254.opentune.db.entities.Artist
import com.arturo254.opentune.db.entities.Playlist
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.playback.queues.ListQueue
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.menu.SongMenu
import com.arturo254.opentune.ui.menu.AlbumMenu
import com.arturo254.opentune.ui.menu.ArtistMenu
import com.arturo254.opentune.ui.screens.GalaxEditorialReveal
import com.arturo254.opentune.ui.screens.GalaxEditorialDivider
import com.arturo254.opentune.ui.screens.Screens
import com.arturo254.opentune.core.designsystem.theme.galaxScreenBackground
import com.arturo254.opentune.viewmodels.LibraryMixViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    navController: NavController,
    viewModel: LibraryMixViewModel = hiltViewModel(),
    spotifyViewModel: com.arturo254.opentune.spotify.SpotifyLibraryViewModel = hiltViewModel(),
    ambientActive: Boolean = true,
) {
    val database = LocalDatabase.current
    val player = LocalPlayerConnection.current
    val menu = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val refreshState = rememberPullToRefreshState()
    val songs by database.allSongs().collectAsState(initial = emptyList())
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val spotifyPlaylists by spotifyViewModel.playlists.collectAsState()
    val (showSpotifyPlaylists) = com.arturo254.opentune.utils.rememberPreference(
        com.arturo254.opentune.constants.ShowSpotifyPlaylistsKey,
        false,
    )
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val metadata = player?.mediaMetadata?.collectAsState()?.value
    val isPlaying = player?.isPlaying?.collectAsState()?.value == true
    var section by rememberSaveable { mutableStateOf(ArchiveSection.ALL) }
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var ascending by rememberSaveable { mutableStateOf(false) }

    val visibleSongs = remember(songs, searchText, ascending) {
        songs.asSequence()
            .distinctBy { it.id }
            .filter {
                searchText.isBlank() ||
                    it.title.contains(searchText, ignoreCase = true) ||
                    it.artists.any { artist -> artist.name.contains(searchText, ignoreCase = true) }
            }
            .toList()
            .let { if (ascending) it.sortedBy { song -> song.title.lowercase() } else it }
    }
    val visibleAlbums = remember(albums, searchText, ascending) {
        albums.asSequence()
            .distinctBy { it.id }
            .filter {
                searchText.isBlank() ||
                    it.title.contains(searchText, ignoreCase = true) ||
                    it.artists.any { artist -> artist.name.contains(searchText, ignoreCase = true) }
            }
            .toList()
            .let { if (ascending) it.sortedBy { album -> album.title.lowercase() } else it }
    }
    val visibleArtists = remember(artists, searchText, ascending) {
        artists.asSequence().distinctBy { it.id }
            .filter { searchText.isBlank() || it.title.contains(searchText, ignoreCase = true) }
            .toList()
            .let { if (ascending) it.sortedBy { artist -> artist.title.lowercase() } else it }
    }
    val visiblePlaylists = remember(playlists, searchText, ascending) {
        playlists.asSequence().distinctBy { it.id }
            .filter { searchText.isBlank() || it.title.contains(searchText, ignoreCase = true) }
            .toList()
            .let { if (ascending) it.sortedBy { playlist -> playlist.title.lowercase() } else it }
    }
    val counters = remember(songs, albums, artists, playlists) {
        ArchiveCounter(
            songs = songs.size,
            albums = albums.size,
            artists = artists.size,
            playlists = playlists.size,
        )
    }
    val heroArt = remember(albums, playlists, songs) {
        (albums.mapNotNull { it.thumbnailUrl } +
            playlists.flatMap { it.thumbnails } +
            songs.mapNotNull { it.thumbnailUrl })
            .distinct().take(3)
    }

    val playSong: (Song, List<Song>) -> Unit = { song, queue ->
        if (metadata?.id == song.id) {
            player?.player?.togglePlayPause()
        } else {
            player?.playQueue(
                ListQueue(
                    title = "Biblioteca",
                    items = queue.map { it.toMediaItem() },
                    startIndex = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0),
                ),
            )
        }
    }
    val showSongMenu: (Song) -> Unit = { song ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        menu.show {
            SongMenu(
                originalSong = song,
                navController = navController,
                onDismiss = menu::dismiss,
            )
        }
    }
    val showAlbumMenu: (Album) -> Unit = { album ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        menu.show {
            AlbumMenu(
                originalAlbum = album,
                navController = navController,
                onDismiss = menu::dismiss,
            )
        }
    }
    val showArtistMenu: (Artist) -> Unit = { artist ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        menu.show {
            ArtistMenu(
                originalArtist = artist,
                coroutineScope = scope,
                onDismiss = menu::dismiss,
            )
        }
    }

    val onShuffle = {
        val selected = visibleSongs
        if (selected.isNotEmpty()) {
            val shuffled = selected.shuffled()
            player?.playQueue(
                ListQueue(
                    title = "Biblioteca",
                    items = shuffled.map { it.toMediaItem() },
                ),
            )
        }
    }
    val onFilter: (ArchiveSection) -> Unit = { next ->
        section = next
        scope.launch { listState.animateScrollToItem(0) }
    }
    BackHandler(enabled = section != ArchiveSection.ALL || searchVisible) {
        when {
            searchVisible -> {
                searchVisible = false
                searchText = ""
            }
            section != ArchiveSection.ALL -> section = ArchiveSection.ALL
        }
    }

    val backStack by navController.currentBackStackEntryAsState()
    val scrollToTop = backStack?.savedStateHandle
        ?.getStateFlow("scrollToTop", false)?.collectAsState()
    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            listState.animateScrollToItem(0)
            backStack?.savedStateHandle?.set("scrollToTop", false)
        }
    }
    Box(Modifier.fillMaxSize().galaxScreenBackground()) {
        ArchiveBackdrop(
            modifier = Modifier.fillMaxSize(),
            active = ambientActive && section == ArchiveSection.ALL,
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
                .pullToRefresh(
                    state = refreshState,
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::syncAllLibrary,
                ),
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            item(key = "archive_header") {
                ArchiveHeader(
                    onSearch = { searchVisible = !searchVisible },
                    onSettings = { navController.navigate("settings") },
                )
            }
            if (searchVisible) {
                item(key = "archive_search") {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        singleLine = true,
                        placeholder = { Text("Buscar en biblioteca") },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    )
                }
            }
            if (section == ArchiveSection.ALL && searchText.isBlank()) {
                item(key = "archive_hero") {
                    GalaxEditorialReveal {
                        ArchiveHero(
                            counters = counters,
                            imageUrls = heroArt,
                            active = ambientActive,
                            onPlay = onShuffle,
                        )
                    }
                }
            }
            item(key = "archive_quick_access") {
                ArchiveQuickAccess(
                    onLiked = { navController.navigate("auto_playlist/liked") },
                    onOffline = { navController.navigate("auto_playlist/downloaded") },
                    onTop = { navController.navigate("top_playlist/50") },
                )
            }
            item(key = "archive_tabs") {
                ArchiveSectionTabs(
                    selected = section,
                    counter = counters,
                    onSelected = onFilter,
                )
            }
            item(key = "archive_options") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 23.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = when (section) {
                            ArchiveSection.ALL -> "Tu música"
                            ArchiveSection.SONGS -> "Canciones"
                            ArchiveSection.ALBUMS -> "Álbumes"
                            ArchiveSection.ARTISTS -> "Artistas"
                            ArchiveSection.PLAYLISTS -> "Listas"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    TextButton(onClick = { ascending = !ascending }) {
                        Text(if (ascending) "A–Z" else "Recientes")
                    }
                }
            }

            if (section == ArchiveSection.ALL || section == ArchiveSection.PLAYLISTS) {
                if (visiblePlaylists.isNotEmpty()) {
                    if (section == ArchiveSection.ALL) {
                        item(key = "playlist_heading") {
                            ArchiveChapter(
                                title = "Tus listas",
                                onOpen = { onFilter(ArchiveSection.PLAYLISTS) },
                            )
                        }
                        item(key = "playlist_carousel") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 21.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                items(
                                    visiblePlaylists.take(12),
                                    key = { "archive_playlist:" + it.id },
                                ) { playlist ->
                                    ArchivePlaylistCard(
                                        playlist = playlist,
                                        onClick = {
                                            navController.navigate("local_playlist/${playlist.id}")
                                        },
                                        onLongClick = {
                                            navController.navigate("local_playlist/${playlist.id}")
                                        },
                                    )
                                }
                            }
                        }
                    } else {
                        itemsIndexed(
                            visiblePlaylists,
                            key = { index, _ -> "archive_playlist_row:" + index },
                            contentType = { _, _ -> "archive_playlist" },
                        ) { _, playlist ->
                            ArchivePlaylistCard(
                                playlist = playlist,
                                modifier = Modifier.padding(start = 21.dp),
                                onClick = {
                                    navController.navigate("local_playlist/${playlist.id}")
                                },
                                onLongClick = {
                                    navController.navigate("local_playlist/${playlist.id}")
                                },
                            )
                        }
                    }
                }
            }
            if ((section == ArchiveSection.ALL || section == ArchiveSection.PLAYLISTS) &&
                showSpotifyPlaylists && spotifyPlaylists.isNotEmpty()
            ) {
                item(key = "archive_spotify_heading") {
                    ArchiveChapter("Spotify")
                }
                item(key = "archive_spotify_row") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 21.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(
                            spotifyPlaylists.distinctBy { it.id }.take(15),
                            key = { "archive_spotify:" + it.id },
                        ) { playlist ->
                            ArchiveSpotifyCard(
                                playlist = playlist,
                                onClick = {
                                    navController.navigate("spotify_playlist/${playlist.id}")
                                },
                            )
                        }
                    }
                }
            }
            if (section == ArchiveSection.ALL || section == ArchiveSection.ALBUMS) {
                if (visibleAlbums.isNotEmpty()) {
                    if (section == ArchiveSection.ALL) {
                        item(key = "albums_heading") {
                            ArchiveChapter(
                                title = "Álbumes",
                                onOpen = { onFilter(ArchiveSection.ALBUMS) },
                            )
                        }
                        item(key = "album_carousel") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 21.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                items(
                                    visibleAlbums.take(12),
                                    key = { "archive_album:" + it.id },
                                ) { album ->
                                    ArchiveAlbumCard(
                                        album = album,
                                        onClick = { navController.navigate("album/${album.id}") },
                                        onLongClick = { showAlbumMenu(album) },
                                    )
                                }
                            }
                        }
                    } else {
                        itemsIndexed(
                            visibleAlbums,
                            key = { index, _ -> "archive_album_row:" + index },
                            contentType = { _, _ -> "archive_album" },
                        ) { _, album ->
                            ArchiveAlbumCard(
                                album = album,
                                modifier = Modifier.padding(start = 21.dp),
                                onClick = { navController.navigate("album/${album.id}") },
                                onLongClick = { showAlbumMenu(album) },
                            )
                        }
                    }
                }
            }
            if (section == ArchiveSection.ALL || section == ArchiveSection.ARTISTS) {
                if (visibleArtists.isNotEmpty()) {
                    if (section == ArchiveSection.ALL) {
                        item(key = "artists_heading") {
                            ArchiveChapter(
                                title = "Artistas",
                                onOpen = { onFilter(ArchiveSection.ARTISTS) },
                            )
                        }
                        item(key = "artists_carousel") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 21.dp),
                                horizontalArrangement = Arrangement.spacedBy(18.dp),
                            ) {
                                items(
                                    visibleArtists.take(12),
                                    key = { "archive_artist:" + it.id },
                                ) { artist ->
                                    ArchiveArtistCard(
                                        artist = artist,
                                        onClick = { navController.navigate("artist/${artist.id}") },
                                    )
                                }
                            }
                        }
                    } else {
                        itemsIndexed(
                            visibleArtists,
                            key = { index, _ -> "archive_artist_row:" + index },
                            contentType = { _, _ -> "archive_artist" },
                        ) { _, artist ->
                            ArchiveArtistCard(
                                artist = artist,
                                modifier = Modifier.padding(start = 21.dp),
                                onClick = { navController.navigate("artist/${artist.id}") },
                            )
                        }
                    }
                }
            }
            if (section == ArchiveSection.ALL || section == ArchiveSection.SONGS) {
                if (visibleSongs.isNotEmpty()) {
                    item(key = "songs_heading") {
                        ArchiveChapter(
                                title = "Canciones",
                                onOpen = { onFilter(ArchiveSection.SONGS) },
                            )
                    }
                    itemsIndexed(
                        if (section == ArchiveSection.ALL) visibleSongs.take(12) else visibleSongs,
                        key = { index, _ -> "archive_song:" + index },
                        contentType = { _, _ -> "archive_song" },
                    ) { _, song ->
                        ArchiveSongRow(
                            song = song,
                            selected = metadata?.id == song.id,
                            playing = isPlaying,
                            modifier = Modifier.animateItem(),
                            onClick = { playSong(song, visibleSongs) },
                            onLongClick = { showSongMenu(song) },
                        )
                    }
                }
            }
            val isEmpty = when (section) {
                ArchiveSection.ALL ->
                    visibleSongs.isEmpty() && visibleAlbums.isEmpty() &&
                        visibleArtists.isEmpty() && visiblePlaylists.isEmpty()
                ArchiveSection.SONGS -> visibleSongs.isEmpty()
                ArchiveSection.ALBUMS -> visibleAlbums.isEmpty()
                ArchiveSection.ARTISTS -> visibleArtists.isEmpty()
                ArchiveSection.PLAYLISTS -> visiblePlaylists.isEmpty()
            }
            if (isEmpty) {
                item(key = "archive_empty") {
                    ArchiveEmpty(
                        title = if (searchText.isBlank()) "Tu música empieza aquí" else "Sin resultados",
                        subtitle = if (searchText.isBlank()) "Descubre y guarda lo que quieras escuchar."
                            else "Prueba con otra búsqueda.",
                        onExplore = {
                            navController.navigate(Screens.Search.route)
                        },
                    )
                }
            }
            item(key = "archive_footer") {
                Spacer(Modifier.height(36.dp))
            }
        }
        Indicator(
            isRefreshing = isRefreshing,
            state = refreshState,
            modifier = Modifier.align(Alignment.TopCenter)
                .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
        )
    }
}
