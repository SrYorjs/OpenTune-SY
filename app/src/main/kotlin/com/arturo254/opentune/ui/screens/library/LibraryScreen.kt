package com.arturo254.opentune.ui.screens.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.arturo254.opentune.LocalDatabase
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.*
import com.arturo254.opentune.db.entities.*
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.playback.queues.ListQueue
import com.arturo254.opentune.spotify.SpotifyLibraryViewModel
import com.arturo254.opentune.spotify.SpotifyMapper
import com.arturo254.opentune.ui.component.CreatePlaylistDialog
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.menu.*
import com.arturo254.opentune.ui.screens.*
import com.arturo254.opentune.utils.rememberEnumPreference
import com.arturo254.opentune.utils.rememberPreference
import com.arturo254.opentune.viewmodels.LibraryMixViewModel
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.text.Collator
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    navController: NavController,
    viewModel: LibraryMixViewModel = hiltViewModel(),
    spotifyViewModel: SpotifyLibraryViewModel = hiltViewModel(),
    ambientActive: Boolean = true,
    onOpenSearchTab: () -> Unit = { navController.navigate(Screens.Search.route) },
    filterContent: @Composable () -> Unit = {},
    onCategorySelected: ((LibraryFilter) -> Unit)? = null,
) {
    val database = LocalDatabase.current
    val player = LocalPlayerConnection.current
    val menus = LocalMenuState.current
    val scope = rememberCoroutineScope()
    val list = rememberLazyListState()
    val refresh = rememberPullToRefreshState()
    val songs by database.allSongs().collectAsStateWithLifecycle(initialValue = emptyList())
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val spotify by spotifyViewModel.playlists.collectAsStateWithLifecycle()
    val refreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val current = player?.mediaMetadata?.collectAsStateWithLifecycle()?.value
    val playing = player?.isPlaying?.collectAsStateWithLifecycle()?.value == true
    val (showSpotify) = rememberPreference(ShowSpotifyPlaylistsKey, false)
    val (showLiked) = rememberPreference(ShowLikedPlaylistKey, true)
    val (showOffline) = rememberPreference(ShowDownloadedPlaylistKey, true)
    val (showCached) = rememberPreference(ShowCachedPlaylistKey, true)
    val (showTop) = rememberPreference(ShowTopPlaylistKey, true)
    val topSize by viewModel.topValue.collectAsStateWithLifecycle(initialValue = "50")
    var sort by rememberEnumPreference(MixSortTypeKey, MixSortType.CREATE_DATE)
    var descending by rememberPreference(MixSortDescendingKey, true)
    var playlistSort by rememberEnumPreference(PlaylistSortTypeKey, PlaylistSortType.CUSTOM)
    var playlistDescending by rememberPreference(PlaylistSortDescendingKey, true)
    val (tagPreference) = rememberPreference(PlaylistTagsFilterKey, "")
    val tags = remember(tagPreference) { tagPreference.split(',').filter(String::isNotBlank) }
    val tagged by database.playlistIdsByTags(tags).collectAsStateWithLifecycle(initialValue = emptyList())
    var category by rememberSaveable { mutableStateOf(LibraryFilter.LIBRARY) }
    var query by rememberSaveable { mutableStateOf("") }
    var create by rememberSaveable { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    var compact by rememberSaveable { mutableStateOf(false) }
    var reorder by rememberSaveable { mutableStateOf(false) }
    val collator = remember { Collator.getInstance(Locale.getDefault()).apply { strength = Collator.PRIMARY } }
    fun matches(title: String, subtitle: String = "") = query.isBlank() ||
        title.contains(query.trim(), true) || subtitle.contains(query.trim(), true)

    val visibleSongs = remember(songs, query, sort, descending) {
        val entries = songs.distinctBy { it.id }.filter { matches(it.title, it.artists.joinToString { a -> a.name }) }
        val sorted = if (sort == MixSortType.NAME) entries.sortedWith(compareBy(collator) { it.title })
            else entries.sortedBy { it.song.inLibrary }
        if (descending) sorted.asReversed() else sorted
    }
    val visibleAlbums = remember(albums, query, sort, descending) {
        val entries = albums.distinctBy { it.id }.filter { matches(it.title, it.artists.joinToString { a -> a.name }) }
        val sorted = when (sort) {
            MixSortType.NAME -> entries.sortedWith(compareBy(collator) { it.title })
            MixSortType.CREATE_DATE -> entries.sortedBy { it.album.bookmarkedAt }
            MixSortType.LAST_UPDATED -> entries.sortedBy { it.album.lastUpdateTime }
        }
        if (descending) sorted.asReversed() else sorted
    }
    val visibleArtists = remember(artists, query, sort, descending) {
        val entries = artists.distinctBy { it.id }.filter { matches(it.title) }
        val sorted = when (sort) {
            MixSortType.NAME -> entries.sortedWith(compareBy(collator) { it.title })
            MixSortType.CREATE_DATE -> entries.sortedBy { it.artist.bookmarkedAt }
            MixSortType.LAST_UPDATED -> entries.sortedBy { it.artist.lastUpdateTime }
        }
        if (descending) sorted.asReversed() else sorted
    }
    val visiblePlaylists = remember(playlists, query, tags, tagged) {
        playlists.distinctBy { it.id }.filter { matches(it.title) && (tags.isEmpty() || it.id in tagged) }
    }
    val visibleSpotify = remember(spotify, query) { spotify.distinctBy { it.id }.filter { matches(it.name) } }
    val counts = mapOf(
        LibraryFilter.SONGS to visibleSongs.size,
        LibraryFilter.ALBUMS to visibleAlbums.size,
        LibraryFilter.ARTISTS to visibleArtists.size,
        LibraryFilter.PLAYLISTS to visiblePlaylists.size,
        LibraryFilter.SPOTIFY to visibleSpotify.size,
    )
    val select: (LibraryFilter) -> Unit = { next ->
        category = next; reorder = false
        onCategorySelected?.invoke(next)
        scope.launch { list.animateScrollToItem(0) }
    }
    BackHandler(ambientActive && (query.isNotBlank() || reorder || category != LibraryFilter.LIBRARY)) {
        when {
            reorder -> reorder = false
            query.isNotBlank() -> query = ""
            else -> category = LibraryFilter.LIBRARY
        }
    }
    LaunchedEffect(showSpotify) { if (!showSpotify && category == LibraryFilter.SPOTIFY) category = LibraryFilter.LIBRARY }
    val backStack by navController.currentBackStackEntryAsState()
    val top = backStack?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()
    LaunchedEffect(top?.value, ambientActive) {
        if (ambientActive && top?.value == true) {
            list.animateScrollToItem(0); backStack?.savedStateHandle?.set("scrollToTop", false)
        }
    }
    val play: (Song) -> Unit = { song ->
        if (current?.id == song.id) player?.player?.togglePlayPause()
        else player?.playQueue(
            ListQueue(
                title = "Biblioteca",
                items = visibleSongs.map { it.toMediaItem() },
                startIndex = visibleSongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0),
            ),
        )
    }
    val songMenu: (Song) -> Unit = { song ->
        menus.show {
            SongMenu(
                originalSong = song,
                navController = navController,
                onDismiss = menus::dismiss,
            )
        }
    }
    val playlistMenu: (Playlist) -> Unit = { playlist ->
        menus.show {
            PlaylistMenu(
                playlist = playlist,
                coroutineScope = scope,
                onDismiss = menus::dismiss,
            )
        }
    }
    val albumMenu: (Album) -> Unit = { album ->
        menus.show {
            AlbumMenu(
                originalAlbum = album,
                navController = navController,
                onDismiss = menus::dismiss,
            )
        }
    }
    val artistMenu: (Artist) -> Unit = { artist ->
        menus.show {
            ArtistMenu(
                originalArtist = artist,
                coroutineScope = scope,
                onDismiss = menus::dismiss,
            )
        }
    }
    val canReorder = category == LibraryFilter.PLAYLISTS && query.isBlank() && tags.isEmpty() && playlistSort == PlaylistSortType.CUSTOM
    val ordered = remember { mutableStateListOf<Playlist>() }
    var moved by remember { mutableStateOf(false) }
    val reorderState = rememberReorderableLazyListState(list,
        scrollThresholdPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()) { from, to ->
        if (reorder && canReorder && moveLibraryEntry(ordered, from.key, to.key) { it.id } != null) moved = true
    }
    LaunchedEffect(visiblePlaylists, reorderState.isAnyItemDragging, moved) {
        if (!reorderState.isAnyItemDragging && !moved) { ordered.clear(); ordered.addAll(visiblePlaylists) }
    }
    LaunchedEffect(reorderState.isAnyItemDragging, moved) {
        if (moved && !reorderState.isAnyItemDragging) {
            val snapshot = ordered.toList()
            database.transaction { snapshot.forEachIndexed { index, playlist -> setPlaylistCustomOrder(playlist.id, index) } }
            moved = false
        }
    }
    LaunchedEffect(canReorder) { if (!canReorder) reorder = false }
    if (create) CreatePlaylistDialog(onDismiss = { create = false })


    val palette = asterPalette()
    val phase = rememberAsterPhase(ambientActive)
    val coverStack = remember(playlists, albums) {
        (playlists.flatMap { it.thumbnails } + albums.mapNotNull { it.thumbnailUrl }).distinct().take(3)
    }
    Box(Modifier.fillMaxSize()) {
        AsterBackdrop(AsterScene.VAULT, phase, Modifier.fillMaxSize())
        LazyColumn(
            state = list,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize().pullToRefresh(
                isRefreshing = refreshing,
                state = refresh,
                onRefresh = viewModel::syncAllLibrary,
            ),
        ) {
            item("vault/header") {
                VaultHeader(
                    songCount = songs.size,
                    collectionCount = albums.size + playlists.size,
                    artworks = coverStack,
                    onCreate = { create = true },
                    onSettings = { navController.navigate("settings") },
                )
            }
            item("vault/search") { VaultSearch(query, onQuery = { query = it }) }
            item("vault/pinned") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (showLiked) item("liked") {
                        VaultShortcut("Favoritos", "Los que siempre vuelven", R.drawable.favorite, 0) {
                            navController.navigate("auto_playlist/liked")
                        }
                    }
                    if (showOffline) item("downloaded") {
                        VaultShortcut("Sin conexión", "Contigo a donde vayas", R.drawable.offline, 1) {
                            navController.navigate("auto_playlist/downloaded")
                        }
                    }
                    if (showCached) item("cached") {
                        VaultShortcut("En caché", "Listo para volver a sonar", R.drawable.cached, 2) {
                            navController.navigate("cache_playlist/cached")
                        }
                    }
                    if (showTop) item("top") {
                        VaultShortcut("Mi top $topSize", "Tu historia en canciones", R.drawable.trending_up, 3) {
                            navController.navigate("top_playlist/$topSize")
                        }
                    }
                }
            }
            item("vault/categories") {
                VaultCategories(category, showSpotify, counts, select)
            }
            item("vault/external-filters") { filterContent() }
            item("vault/tools") {
                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (category == LibraryFilter.LIBRARY) "TU ARCHIVO" else collectionLabel(category).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.5.sp,
                            color = palette.muted,
                            modifier = Modifier.weight(1f),
                        )
                        if (category == LibraryFilter.LIBRARY || category == LibraryFilter.PLAYLISTS || category == LibraryFilter.ALBUMS) {
                            TextButton(onClick = { compact = !compact }) {
                                Text(if (compact) "Ver portadas" else "Ver lista", color = palette.violet)
                            }
                        }
                        Box {
                            TextButton(onClick = { sortMenu = true }) { Text("Ordenar", color = palette.violet) }
                            DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                                if (category == LibraryFilter.PLAYLISTS) {
                                    PlaylistSortType.entries.forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(when (option) {
                                                    PlaylistSortType.CUSTOM -> "Orden personalizado"
                                                    PlaylistSortType.NAME -> "Nombre"
                                                    PlaylistSortType.CREATE_DATE -> "Fecha de creación"
                                                    PlaylistSortType.LAST_UPDATED -> "Última actualización"
                                                    PlaylistSortType.SONG_COUNT -> "Número de canciones"
                                                })
                                            },
                                            onClick = { playlistSort = option; sortMenu = false },
                                        )
                                    }
                                } else {
                                    MixSortType.entries.forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(when (option) {
                                                    MixSortType.NAME -> "Nombre"
                                                    MixSortType.CREATE_DATE -> "Fecha de incorporación"
                                                    MixSortType.LAST_UPDATED -> "Última actualización"
                                                })
                                            },
                                            onClick = { sort = option; sortMenu = false },
                                        )
                                    }
                                }
                                DropdownMenuItem(
                                    text = { Text("Invertir orden") },
                                    onClick = {
                                        if (category == LibraryFilter.PLAYLISTS) playlistDescending = !playlistDescending
                                        else descending = !descending
                                        sortMenu = false
                                    },
                                )
                            }
                        }
                    }
                    if (category == LibraryFilter.LIBRARY || category == LibraryFilter.SONGS || canReorder) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (category == LibraryFilter.LIBRARY || category == LibraryFilter.SONGS) {
                                FilledTonalButton(
                                    enabled = visibleSongs.isNotEmpty() && player != null,
                                    onClick = {
                                        player?.playQueue(ListQueue(title = "Biblioteca", items = visibleSongs.shuffled().map { it.toMediaItem() }))
                                    },
                                ) {
                                    Icon(painterResource(R.drawable.shuffle), null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Escuchar en aleatorio")
                                }
                            }
                            if (canReorder) {
                                TextButton(onClick = { reorder = !reorder }) {
                                    Text(if (reorder) "Terminar de mover" else "Reorganizar listas", color = palette.violet)
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = palette.line)
                }
            }
            if (category == LibraryFilter.LIBRARY || category == LibraryFilter.PLAYLISTS) {
                if (visiblePlaylists.isNotEmpty()) item("vault/playlists-heading") {
                    AsterHeading(
                        "Tus listas",
                        detail = "Momentos guardados",
                        action = if (category == LibraryFilter.LIBRARY) "Todas" else null,
                        onAction = { select(LibraryFilter.PLAYLISTS) },
                    )
                }
                if (reorder && canReorder) {
                    items(ordered, key = { it.id }) { playlist ->
                        ReorderableItem(reorderState, playlist.id) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                AsterMusicRow(
                                    playlist.title,
                                    "${playlist.songCount} canciones",
                                    playlist.thumbnails.firstOrNull(),
                                    onClick = { navController.navigate("local_playlist/${playlist.id}") },
                                    onMenu = { playlistMenu(playlist) },
                                    modifier = Modifier.weight(1f),
                                )
                                AsterIcon(R.drawable.drag_handle, "Mover ${playlist.title}", {}, Modifier.draggableHandle())
                            }
                        }
                    }
                } else {
                    val shown = if (category == LibraryFilter.LIBRARY) visiblePlaylists.take(4) else visiblePlaylists
                    if (compact) {
                        items(shown, key = { "vault/playlist/${it.id}" }) { playlist ->
                            AsterMusicRow(
                                playlist.title,
                                "${playlist.songCount} canciones",
                                playlist.thumbnails.firstOrNull(),
                                onClick = { navController.navigate("local_playlist/${playlist.id}") },
                                onMenu = { playlistMenu(playlist) },
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                        }
                    } else {
                        items(shown.chunked(2), key = { "vault/playlists/${it.first().id}" }) { pair ->
                            Row(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                pair.forEach { playlist ->
                                    VaultFolder(
                                        title = playlist.title,
                                        subtitle = "${playlist.songCount} canciones",
                                        image = playlist.thumbnails.firstOrNull(),
                                        onOpen = { navController.navigate("local_playlist/${playlist.id}") },
                                        onMenu = { playlistMenu(playlist) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            if (category == LibraryFilter.LIBRARY || category == LibraryFilter.ALBUMS) {
                if (visibleAlbums.isNotEmpty()) item("vault/albums-heading") {
                    AsterHeading(
                        "De principio a fin",
                        detail = "Álbumes",
                        action = if (category == LibraryFilter.LIBRARY) "Todos" else null,
                        onAction = { select(LibraryFilter.ALBUMS) },
                    )
                }
                val shown = if (category == LibraryFilter.LIBRARY) visibleAlbums.take(4) else visibleAlbums
                if (compact) {
                    items(shown, key = { "vault/album/${it.id}" }) { album ->
                        AsterMusicRow(
                            album.title,
                            album.artists.joinToString { it.name },
                            album.thumbnailUrl,
                            onClick = { navController.navigate("album/${album.id}") },
                            onMenu = { albumMenu(album) },
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }
                } else {
                    items(shown.chunked(2), key = { "vault/albums/${it.first().id}" }) { pair ->
                        Row(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            pair.forEach { album ->
                                VaultFolder(
                                    title = album.title,
                                    subtitle = album.artists.joinToString { it.name },
                                    image = album.thumbnailUrl,
                                    onOpen = { navController.navigate("album/${album.id}") },
                                    onMenu = { albumMenu(album) },
                                    modifier = Modifier.weight(1f),
                                    album = true,
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            if (category == LibraryFilter.LIBRARY || category == LibraryFilter.ARTISTS) {
                if (visibleArtists.isNotEmpty()) item("vault/artists-heading") {
                    AsterHeading(
                        "Las voces de tu mundo",
                        detail = "Artistas",
                        action = if (category == LibraryFilter.LIBRARY) "Todos" else null,
                        onAction = { select(LibraryFilter.ARTISTS) },
                    )
                }
                if (category == LibraryFilter.LIBRARY) {
                    item("vault/artists") {
                        LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(visibleArtists.take(12), key = { it.id }) { artist ->
                                VaultArtist(
                                    title = artist.title,
                                    count = artist.songCount,
                                    image = artist.thumbnailUrl,
                                    onOpen = { navController.navigate("artist/${artist.id}") },
                                    onMenu = { artistMenu(artist) },
                                )
                            }
                        }
                    }
                } else {
                    items(visibleArtists, key = { "vault/artist/${it.id}" }) { artist ->
                        AsterMusicRow(
                            artist.title,
                            "${artist.songCount} canciones",
                            artist.thumbnailUrl,
                            circular = true,
                            onClick = { navController.navigate("artist/${artist.id}") },
                            onMenu = { artistMenu(artist) },
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }
                }
            }
            if (showSpotify && (category == LibraryFilter.LIBRARY || category == LibraryFilter.SPOTIFY)) {
                if (visibleSpotify.isNotEmpty()) item("vault/spotify-heading") {
                    AsterHeading(
                        "Desde Spotify",
                        action = if (category == LibraryFilter.LIBRARY) "Todas" else null,
                        onAction = { select(LibraryFilter.SPOTIFY) },
                    )
                }
                items(
                    if (category == LibraryFilter.LIBRARY) visibleSpotify.take(4) else visibleSpotify,
                    key = { "vault/spotify/${it.id}" },
                ) { playlist ->
                    AsterMusicRow(
                        playlist.name,
                        "${playlist.tracks?.total ?: 0} canciones",
                        SpotifyMapper.getPlaylistThumbnail(playlist),
                        onClick = { navController.navigate("spotify_playlist/${playlist.id}") },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
            if (category == LibraryFilter.LIBRARY || category == LibraryFilter.SONGS) {
                if (visibleSongs.isNotEmpty()) item("vault/songs-heading") {
                    AsterHeading(
                        "Canciones que se quedan",
                        action = if (category == LibraryFilter.LIBRARY) "Todas" else null,
                        onAction = { select(LibraryFilter.SONGS) },
                    )
                }
                itemsIndexed(
                    if (category == LibraryFilter.LIBRARY) visibleSongs.take(12) else visibleSongs,
                    key = { _, song -> "vault/song/${song.id}" },
                ) { index, song ->
                    AsterMusicRow(
                        title = song.title,
                        subtitle = song.artists.joinToString { it.name },
                        artwork = song.thumbnailUrl,
                        onClick = { play(song) },
                        onMenu = { songMenu(song) },
                        active = current?.id == song.id,
                        playing = playing,
                        number = index + 1,
                        modifier = asterItemMotion(Modifier.padding(horizontal = 12.dp)),
                    )
                }
            }
            val empty = if (category == LibraryFilter.LIBRARY) {
                counts.filterKeys { showSpotify || it != LibraryFilter.SPOTIFY }.values.all { it == 0 }
            } else counts[category] == 0
            if (empty) item("vault/empty") {
                AsterMessage(
                    title = if (query.isBlank()) "Este espacio es tuyo" else "Todavía no aparece",
                    message = if (query.isBlank()) "Guarda música y reúne tus momentos favoritos en una lista."
                        else "Prueba otro título o el nombre de un artista.",
                    action = if (query.isBlank()) "Descubrir música" else "Limpiar búsqueda",
                    onAction = if (query.isBlank()) onOpenSearchTab else { { query = "" } },
                )
            }
            item("vault/end") { Spacer(Modifier.height(28.dp)) }
        }
        Indicator(
            isRefreshing = refreshing,
            state = refresh,
            modifier = Modifier.align(Alignment.TopCenter).padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
        )
    }
}
