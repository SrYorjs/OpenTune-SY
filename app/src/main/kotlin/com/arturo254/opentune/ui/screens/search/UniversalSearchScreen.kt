package com.arturo254.opentune.ui.screens.search

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.UniversalAudioSelectionModeKey
import com.arturo254.opentune.core.model.ArtistIdentity
import com.arturo254.opentune.core.model.TrackIdentity
import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.sources.offline.OfflineTrackState
import com.arturo254.opentune.sources.resolution.AudioSelectionMode
import com.arturo254.opentune.sources.resolution.SourceVariantKey
import com.arturo254.opentune.sources.search.UnifiedTrackSearchResult
import com.arturo254.opentune.sources.audius.AudiusCollectionType
import com.arturo254.opentune.ui.screens.*
import com.arturo254.opentune.utils.rememberEnumPreference
import com.arturo254.opentune.viewmodels.UniversalSearchViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.Locale

/** Signal: one universal result stream, explicit versions, quality and offline state. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UniversalSearchScreen(query: String, pureBlack: Boolean, viewModel: UniversalSearchViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val player = LocalPlayerConnection.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val audiusState by viewModel.audiusState.collectAsStateWithLifecycle()
    val offlineAssets by viewModel.offlineAssets.collectAsStateWithLifecycle()
    val current = player?.mediaMetadata?.collectAsStateWithLifecycle()?.value
    val playing = player?.isPlaying?.collectAsStateWithLifecycle()?.value == true
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var resolving by remember { mutableStateOf<String?>(null) }
    var downloading by remember { mutableStateOf<String?>(null) }
    var dialog by remember(query) { mutableStateOf<String?>(null) }
    val manual = remember { mutableStateMapOf<String, SourceVariantKey>() }
    var quality by rememberEnumPreference(UniversalAudioSelectionModeKey, AudioSelectionMode.AUTOMATIC)
    // During the debounce interval do not relabel the previous query's results.
    val sameQuery = state.query == query.trim()
    val entries = if (sameQuery) state.result.tracks else emptyList()
    var visibleCount by remember(query) { mutableIntStateOf(50) }
    var lastPageRequest by remember(query) { mutableStateOf("") }
    var sourceDetails by remember(query) { mutableStateOf(false) }
    var category by remember(query) { mutableStateOf(SearchResultCategory.ALL) }
    var activeArtist by remember(query) { mutableStateOf<String?>(null) }
    var activeAlbum by remember(query) { mutableStateOf<String?>(null) }
    val albums = remember(entries) { groupSearchAlbums(entries) }
    val remoteMatchesQuery = audiusState.query == query.trim()
    val discoveredArtists = if (remoteMatchesQuery) audiusState.artists else emptyList()
    val discoveredPlaylists = if (remoteMatchesQuery) audiusState.playlists else emptyList()
    val selectedCollection = audiusState.selected.takeIf { remoteMatchesQuery }
    val collectionVariants = if (selectedCollection != null) audiusState.tracks else emptyList()
    val collectionEntries = remember(collectionVariants) {
        collectionVariants.map { variant ->
            UnifiedTrackSearchResult(
                identity = TrackIdentity(
                    title = variant.title,
                    artists = variant.artists.map { ArtistIdentity(name = it) },
                    durationMs = variant.durationMs,
                    isrc = variant.isrc,
                    version = variant.version,
                ),
                variants = listOf(variant),
                recommendedVariant = variant,
            )
        }
    }
    val selectedTracks = when (category) {
        SearchResultCategory.ALL, SearchResultCategory.SONGS -> entries
        SearchResultCategory.ARTISTS, SearchResultCategory.PLAYLISTS ->
            if (selectedCollection != null) collectionEntries else emptyList()
        SearchResultCategory.ALBUMS ->
            albums.firstOrNull { it.key == activeAlbum }?.tracks.orEmpty()
    }
    val showingTracks = category == SearchResultCategory.ALL ||
        category == SearchResultCategory.SONGS ||
        (category == SearchResultCategory.ARTISTS && selectedCollection != null) ||
        (category == SearchResultCategory.PLAYLISTS && selectedCollection != null) ||
        (category == SearchResultCategory.ALBUMS && activeAlbum != null)
    val availableCount = when (category) {
        SearchResultCategory.ARTISTS ->
            if (selectedCollection == null) discoveredArtists.size else selectedTracks.size
        SearchResultCategory.PLAYLISTS ->
            if (selectedCollection == null) discoveredPlaylists.size else selectedTracks.size
        SearchResultCategory.ALBUMS -> if (activeAlbum == null) albums.size else selectedTracks.size
        else -> selectedTracks.size
    }
    val visibleEntries = selectedTracks.take(visibleCount)
    val failedSources = if (sameQuery && !state.isLoading) {
        state.result.sources.values.filter { it.error != null }
    } else {
        emptyList()
    }
    val nextPageToken = state.result.sources.values
        .mapNotNull { it.continuationToken }
        .sorted().joinToString("|")
    val loading = state.isLoading || (!sameQuery && query.isNotBlank())
    val discoveryLoading = remoteMatchesQuery && audiusState.loading
    val bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
    BackHandler(enabled = selectedCollection != null) {
        viewModel.closeAudiusCollection()
        visibleCount = 50
    }

    LaunchedEffect(query) {
        viewModel.query.value = query
        list.scrollToItem(0)
    }
    LaunchedEffect(list, query, category, activeArtist, activeAlbum, availableCount, visibleCount, nextPageToken, state.isLoadingMore) {
        if (!sameQuery || state.isLoading ||
            (category != SearchResultCategory.ALL && category != SearchResultCategory.SONGS)
        ) return@LaunchedEffect
        snapshotFlow {
            val info = list.layoutInfo
            info.totalItemsCount > 0 &&
                (info.visibleItemsInfo.lastOrNull()?.index ?: -1) >= info.totalItemsCount - 6
        }.distinctUntilChanged().filter { it }.collect {
            if (availableCount > visibleCount) {
                visibleCount += 50
            } else if (state.result.hasMore && !state.isLoadingMore) {
                val requestKey = "$visibleCount:$nextPageToken"
                if (lastPageRequest != requestKey) {
                    lastPageRequest = requestKey
                    viewModel.loadMore()
                }
            }
        }
    }
    val play: (UnifiedTrackSearchResult, List<SourceVariant>) -> Unit = { result, variants ->
        if (player != null && resolving == null && variants.isNotEmpty()) {
            scope.launch {
                resolving = result.signalKey()
                try {
                    val played = if (selectedCollection != null) {
                        val index = collectionVariants.indexOfFirst { source ->
                            result.variants.any { it.provider == source.provider && it.sourceId == source.sourceId }
                        }
                        viewModel.playAudiusCollection(
                            player,
                            collectionVariants,
                            index,
                            selectedCollection.title,
                        )
                    } else {
                        viewModel.playSearchResults(
                            player,
                            result,
                            state.result,
                            variants,
                            context.getString(R.string.queue_searched_songs),
                        )
                    }
                    if (!played) {
                        Toast.makeText(context, R.string.universal_versions_play_error, Toast.LENGTH_SHORT).show()
                    }
                } finally { resolving = null }
            }
        }
    }
    Box(Modifier.fillMaxSize().background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)) {
        NovaBackdrop(NovaSpace.RESULTS, active = false, modifier = Modifier.fillMaxSize(), pureBlack = pureBlack)
        LazyColumn(state = list, modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = bottom + 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item("signal/header") { SignalHeader(query) }
            item("signal/categories") {
                SignalCategories(category) { selected ->
                    category = selected
                    activeArtist = null
                    activeAlbum = null
                    viewModel.closeAudiusCollection()
                    visibleCount = 50
                    lastPageRequest = ""
                    scope.launch { list.animateScrollToItem(0) }
                }
            }
            if (failedSources.isNotEmpty()) item("signal/partial") {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    TextButton(onClick = { sourceDetails = !sourceDetails }) {
                        Text(
                            if (failedSources.size == 1) "1 fuente no disponible · Ver detalles"
                            else "${failedSources.size} fuentes no disponibles · Ver detalles"
                        )
                    }
                    if (sourceDetails) {
                        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                            Column(Modifier.padding(12.dp)) {
                                failedSources.forEach { source ->
                                    Text(
                                        "${signalProvider(source.provider)}: ${sourceFailureLabel(source.error)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                TextButton(onClick = viewModel::retrySearch) {
                                    Text("Reintentar proveedores")
                                }
                            }
                        }
                    }
                }
            }
            if ((loading || discoveryLoading) && entries.isEmpty() &&
                discoveredArtists.isEmpty() && discoveredPlaylists.isEmpty()
            ) item("signal/loading") { NovaLoading("Buscando música…") }
            if (!loading && !discoveryLoading && entries.isEmpty() &&
                discoveredArtists.isEmpty() && discoveredPlaylists.isEmpty()
            ) item("signal/empty") {
                NovaMessage(
                    title = if (state.error != null) "No pudimos completar la búsqueda" else if (query.isBlank()) "Encuentra tu próxima canción" else "No hay coincidencias",
                    message = if (state.error != null) "Comprueba tu conexión y vuelve a enviar la búsqueda." else "Prueba con el título, un artista o una parte del nombre.",
                )
            }
            if (selectedCollection != null &&
                (category == SearchResultCategory.ARTISTS || category == SearchResultCategory.PLAYLISTS)
            ) {
                item("signal/collection/header") {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        TextButton(onClick = {
                            viewModel.closeAudiusCollection()
                            visibleCount = 50
                            scope.launch { list.animateScrollToItem(0) }
                        }) { Text(if (category == SearchResultCategory.ARTISTS) "Volver a artistas" else "Volver a listas") }
                        NovaArtwork(selectedCollection.artworkUrl, Modifier.size(100.dp))
                        Text(selectedCollection.title, style = MaterialTheme.typography.headlineMedium)
                        if (selectedCollection.subtitle.isNotBlank()) Text(
                            selectedCollection.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        selectedCollection.description?.takeIf(String::isNotBlank)?.let { description ->
                            Text(description, maxLines = 4, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall)
                        }
                        if (collectionVariants.isNotEmpty()) {
                            Button(onClick = {
                                val activePlayer = player ?: return@Button
                                scope.launch {
                                    val played = viewModel.playAudiusCollection(
                                        activePlayer, collectionVariants, 0, selectedCollection.title,
                                    )
                                    if (!played) Toast.makeText(context,
                                        R.string.universal_versions_play_error,
                                        Toast.LENGTH_SHORT).show()
                                }
                            }) { Text("Reproducir todo") }
                        }
                    }
                }
                if (audiusState.loadingCollection) item("signal/collection/loading") {
                    NovaLoading("Cargando canciones de Audius…")
                }
                if (audiusState.collectionError != null) item("signal/collection/error") {
                    NovaMessage(title = "No se pudo cargar esta colección",
                        message = audiusState.collectionError.orEmpty())
                }
                if (!audiusState.loadingCollection && collectionVariants.isEmpty() &&
                    audiusState.collectionError == null
                ) item("signal/collection/empty") {
                    NovaMessage(title = "Sin canciones disponibles",
                        message = "Esta colección no tiene pistas reproducibles públicamente.")
                }
            }
            if (category == SearchResultCategory.ALL && selectedCollection == null) {
                if (discoveredArtists.isNotEmpty()) {
                    item("signal/featured/artists") {
                        Text("Artistas", modifier = Modifier.padding(horizontal = 24.dp),
                            style = MaterialTheme.typography.titleMedium)
                    }
                    items(discoveredArtists.take(3), key = { "signal/preview/artist/" + it.id }) { artist ->
                        SignalCollectionRow(
                            title = artist.name,
                            subtitle = artist.handle.takeIf(String::isNotBlank)?.let { "@$it" }
                                ?: "Artista de Audius",
                            artworkUrl = artist.artworkUrl,
                            onClick = {
                                category = SearchResultCategory.ARTISTS
                                viewModel.openAudiusArtist(artist)
                                visibleCount = 50
                                scope.launch { list.animateScrollToItem(0) }
                            },
                        )
                    }
                }
                if (discoveredPlaylists.isNotEmpty()) {
                    item("signal/featured/playlists") {
                        Text("Listas", modifier = Modifier.padding(horizontal = 24.dp),
                            style = MaterialTheme.typography.titleMedium)
                    }
                    items(discoveredPlaylists.take(3), key = { "signal/preview/playlist/" + it.id }) { playlist ->
                        SignalCollectionRow(
                            title = playlist.name,
                            subtitle = playlist.creator.ifBlank { "Playlist de Audius" },
                            artworkUrl = playlist.artworkUrl,
                            onClick = {
                                category = SearchResultCategory.PLAYLISTS
                                viewModel.openAudiusPlaylist(playlist)
                                visibleCount = 50
                                scope.launch { list.animateScrollToItem(0) }
                            },
                        )
                    }
                }
            }
            if (category == SearchResultCategory.ARTISTS && selectedCollection == null) {
                if (discoveryLoading) item("signal/artists/loading") {
                    NovaLoading("Buscando artistas de Audius…")
                }
                if (!discoveryLoading && discoveredArtists.isEmpty()) item("signal/artists/empty") {
                    NovaMessage(title = "Sin artistas",
                        message = audiusState.artistError
                            ?: "No encontramos perfiles de artistas para esta búsqueda en Audius.")
                }
                items(discoveredArtists.take(visibleCount), key = { "signal/artist/" + it.id }) { artist ->
                    SignalCollectionRow(
                        title = artist.name,
                        subtitle = artist.handle.takeIf(String::isNotBlank)?.let { "@$it" }
                            ?: "Artista de Audius",
                        artworkUrl = artist.artworkUrl,
                        onClick = {
                            viewModel.openAudiusArtist(artist)
                            visibleCount = 50
                            scope.launch { list.animateScrollToItem(0) }
                        },
                    )
                }
            }
            if (category == SearchResultCategory.PLAYLISTS && selectedCollection == null) {
                if (discoveryLoading) item("signal/playlists/loading") {
                    NovaLoading("Buscando listas de Audius…")
                }
                if (!discoveryLoading && discoveredPlaylists.isEmpty()) item("signal/playlists/empty") {
                    NovaMessage(title = "Sin listas",
                        message = audiusState.playlistError
                            ?: "No encontramos playlists para esta búsqueda en Audius.")
                }
                items(discoveredPlaylists.take(visibleCount), key = { "signal/playlist/" + it.id }) { playlist ->
                    SignalCollectionRow(
                        title = playlist.name,
                        subtitle = playlist.creator.ifBlank { "Playlist de Audius" },
                        artworkUrl = playlist.artworkUrl,
                        onClick = {
                            viewModel.openAudiusPlaylist(playlist)
                            visibleCount = 50
                            scope.launch { list.animateScrollToItem(0) }
                        },
                    )
                }
            }
            if (category == SearchResultCategory.ALBUMS && activeAlbum == null) {
                if (!loading && albums.isEmpty() && entries.isNotEmpty()) item("signal/no-albums") {
                    NovaMessage(title = "No hay álbumes", message = "Las fuentes consultadas no proporcionaron álbumes para esta búsqueda.")
                }
                items(albums.take(visibleCount), key = { "signal/album/" + it.key }) { album ->
                    SignalCollectionRow(
                        title = album.title,
                        subtitle = album.artist.ifBlank { "Álbum" },
                        artworkUrl = album.artworkUrl,
                        onClick = {
                            activeAlbum = album.key
                            visibleCount = 50
                            lastPageRequest = ""
                            scope.launch { list.animateScrollToItem(0) }
                        },
                    )
                }
            }
            if (category == SearchResultCategory.ALBUMS && activeAlbum != null) item("signal/back-album") {
                TextButton(
                    onClick = {
                        activeAlbum = null
                        visibleCount = 50
                        scope.launch { list.animateScrollToItem(0) }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) { Text("Volver a álbumes") }
            }
            if (showingTracks) itemsIndexed(visibleEntries, key = { index, result -> universalResultRowKey(index, result.signalKey()) },
                contentType = { _, _ -> "signal/recording" }) { index, result ->
                val key = result.signalKey()
                val sources = result.variants.map { it.provider to it.sourceId }.toSet()
                val asset = offlineAssets.firstOrNull { asset -> asset.discoverySources.any { (it.provider to it.sourceId) in sources } }
                val offline = asset?.takeIf { it.state == OfflineTrackState.AVAILABLE }?.toSourceVariant()
                    ?: result.variants.firstOrNull { it.provider == ProviderId.OPEN_TUNE_OFFLINE }
                val preferred = viewModel.selectVariant(result, result.variants + listOfNotNull(offline), quality, manual[key])
                val fallbacks = (result.variants + listOfNotNull(offline))
                    .filterNot { it.provider == preferred?.provider && it.sourceId == preferred?.sourceId }
                    .sortedBy { variant ->
                        when (variant.provider) {
                            ProviderId.OPEN_TUNE_OFFLINE, ProviderId.LOCAL -> 0
                            ProviderId.AUDIUS -> 1
                            ProviderId.YOUTUBE_MUSIC -> 2
                            else -> 3
                        }
                    }
                val active = (current != null && (result.variants + listOfNotNull(offline)).any { it.sourceId == current.id }) ||
                    (current != null && (preferred == null || preferred.isCatalogOnly()) &&
                        current.title.equals(result.identity.title, true) && result.identity.artists.any { artist ->
                            current.artists.any { it.name.equals(artist.name, true) }
                        })
                val pending = downloading == key || asset?.state == OfflineTrackState.PENDING
                val canDownload = offline == null && !pending && result.variants.any {
                    when (it.provider) {
                        ProviderId.LOCAL, ProviderId.OPEN_TUNE_OFFLINE -> false
                        ProviderId.AUDIUS -> it.downloadable == true
                        else -> true
                    }
                }
                SignalResult(
                    index = index, result = result, preferred = preferred, active = active, playing = playing,
                    resolving = resolving == key, downloaded = offline != null, downloading = pending,
                    modifier = Modifier.animateItem(),
                    onVersions = { dialog = key },
                    onPlay = if (player != null && (active || preferred != null || fallbacks.isNotEmpty())) {
                        { if (active) player.player.togglePlayPause() else play(result, listOfNotNull(preferred) + fallbacks) }
                    } else null,
                    onDownload = if (canDownload) { {
                        if (downloading == null) scope.launch {
                            downloading = key
                            try {
                                viewModel.enqueueOffline(result).onSuccess {
                                    Toast.makeText(context, R.string.catalog_offline_download_started, Toast.LENGTH_SHORT).show()
                                }.onFailure {
                                    Toast.makeText(context, it.message ?: context.getString(R.string.catalog_source_unplayable), Toast.LENGTH_SHORT).show()
                                }
                            } finally { downloading = null }
                        }
                    } } else null,
                )
            }
            if (selectedCollection?.type == AudiusCollectionType.ARTIST &&
                audiusState.hasMoreTracks && !audiusState.loadingCollection
            ) item("signal/artist/more") {
                TextButton(onClick = viewModel::loadMoreAudiusTracks,
                    modifier = Modifier.padding(horizontal = 20.dp)) { Text("Cargar más canciones") }
            }
            if (sameQuery && selectedCollection == null && category != SearchResultCategory.PLAYLISTS && state.isLoadingMore) item("signal/more-loading") {
                NovaLoading("Cargando más resultados…")
            } else if (sameQuery && selectedCollection == null && visibleCount < availableCount) item("signal/more") {
                TextButton(onClick = { visibleCount += 50 }, modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text("Mostrar más resultados")
                }
            } else if (sameQuery && selectedCollection == null &&
                (category == SearchResultCategory.ALL || category == SearchResultCategory.SONGS ||
                    category == SearchResultCategory.ALBUMS) &&
                state.result.hasMore && entries.isNotEmpty()
            ) item("signal/more") {
                TextButton(onClick = viewModel::loadMore, modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text("Buscar más resultados")
                }
            }
        }
    }
    val selected = (entries + collectionEntries).firstOrNull { it.signalKey() == dialog }
    if (selected != null) {
        UniversalVersionQualityDialog(
            selected = selected,
            relatedVersions = viewModel.relatedVersions(selected),
            qualityMode = quality,
            manualVariant = manual[selected.signalKey()],
            onQualityModeChange = { quality = it },
            onSelectVariant = { result, variant ->
                manual[result.signalKey()] = SourceVariantKey(variant.provider, variant.sourceId)
                dialog = null
                play(result, listOf(variant))
            },
            onClearManual = { manual.remove(selected.signalKey()); dialog = null },
            onDismiss = { dialog = null },
        )
    }
}

@Composable
private fun SignalHeader(query: String) {
    Text(
        text = query.ifBlank { "Encuentra tu música" },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-1.2).sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun SignalCategories(
    selected: SearchResultCategory,
    onSelect: (SearchResultCategory) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(SearchResultCategory.entries, key = { it.name }) { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(category) },
                label = { Text(category.title) },
            )
        }
    }
}

@Composable
private fun SignalCollectionRow(
    title: String,
    subtitle: String,
    artworkUrl: String?,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = .8f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NovaArtwork(artworkUrl, Modifier.size(60.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                painter = painterResource(R.drawable.chevron_right),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SignalResult(
    index: Int, result: UnifiedTrackSearchResult, preferred: SourceVariant?, active: Boolean, playing: Boolean,
    resolving: Boolean, downloaded: Boolean, downloading: Boolean, onPlay: (() -> Unit)?,
    onVersions: () -> Unit, onDownload: (() -> Unit)?, modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val display = preferred ?: result.recommendedVariant
    val artwork = display?.artworkUrl ?: result.variants.firstNotNullOfOrNull { it.artworkUrl }
    val providers = (result.variants + listOfNotNull(preferred)).map { it.provider }.distinct().joinToString(" · ") { signalProvider(it) }
    val quality = if (display?.isCatalogOnly() == true) null else signalQuality(display)
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(20.dp))
        .background(if (active) colors.primary.copy(alpha = .1f) else colors.surfaceContainerLow.copy(alpha = .8f))) {
        Row(Modifier.fillMaxWidth().clickable(enabled = onPlay != null, role = Role.Button) { onPlay?.invoke() }
            .padding(14.dp), horizontalArrangement = Arrangement.spacedBy(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                NovaArtwork(artwork, Modifier.size(68.dp))
                if (resolving) CircularProgressIndicator(Modifier.align(Alignment.Center).size(25.dp), strokeWidth = 2.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(result.identity.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = if (active) colors.primary else colors.onSurface)
                Text(result.identity.artists.joinToString { it.name }, style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (quality != null) Text(quality, style = MaterialTheme.typography.labelSmall, color = colors.primary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (!resolving && onPlay != null) Icon(painterResource(if (active && playing) R.drawable.pause else R.drawable.play),
                contentDescription = if (active && playing) "Pausar" else "Reproducir", tint = colors.primary, modifier = Modifier.size(22.dp))
        }
        HorizontalDivider(Modifier.padding(horizontal = 14.dp), color = colors.outlineVariant.copy(alpha = .35f))
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(providers, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (downloaded || downloading || display?.isCatalogOnly() == true) Text(
                    when { downloaded -> "Disponible sin conexión"; downloading -> "Preparando descarga…"; else -> "Se buscará una versión reproducible" },
                    style = MaterialTheme.typography.labelSmall, color = colors.primary)
            }
            TextButton(onClick = onVersions) { Text("Versiones") }
            if (downloading) CircularProgressIndicator(Modifier.padding(12.dp).size(20.dp), strokeWidth = 2.dp)
            else if (onDownload != null) NovaIcon(R.drawable.download, "Descargar ${result.identity.title}", onDownload)
        }
    }
}

/** Includes version traits and album so alternate recordings cannot share choices. */
private fun UnifiedTrackSearchResult.signalKey(): String = identity.isrc?.takeIf { it.isNotBlank() }?.let { "isrc:${it.uppercase(Locale.ROOT)}" }
    ?: listOf(identity.title.trim().lowercase(Locale.ROOT), identity.artists.joinToString(",") { it.name.trim().lowercase(Locale.ROOT) },
        identity.durationMs ?: -1L, identity.album?.canonicalId ?: identity.album?.title.orEmpty(),
        identity.version.traits.map { it.name }.sorted().joinToString(","), identity.version.label.orEmpty().trim().lowercase(Locale.ROOT)).joinToString("|")

private fun signalProvider(provider: ProviderId): String = when (provider) {
    ProviderId.YOUTUBE_MUSIC -> "YouTube Music"
    ProviderId.DEEZER -> "Deezer"
    ProviderId.SPOTIFY -> "Spotify"
    ProviderId.TIDAL -> "TIDAL"
    ProviderId.APPLE_MUSIC -> "Apple Music"
    ProviderId.AUDIUS -> "Audius"
    ProviderId.LOCAL -> "Local"
    ProviderId.OPEN_TUNE_OFFLINE -> "Offline"
    else -> provider.value
}

internal fun sourceFailureLabel(error: com.arturo254.opentune.sources.search.UnifiedSearchError?): String {
    val type = error?.type.orEmpty()
    val details = error?.message.orEmpty()
    return when {
        "TimeoutException" in type -> "No respondió antes del límite de espera"
        "UnknownHostException" in type -> "Sin conexión con el proveedor"
        "SocketTimeoutException" in type -> "El servidor tardó demasiado"
        "unauthorized" in details.lowercase() || "http 401" in details.lowercase() ||
            "http 403" in details.lowercase() -> "El proveedor requiere credenciales válidas"
        "http 429" in details.lowercase() -> "Límite de solicitudes alcanzado"
        "http 503" in details.lowercase() -> "El servicio reporta mantenimiento o indisponibilidad"
        else -> "No se pudo conectar con el proveedor"
    }
}

private fun signalQuality(variant: SourceVariant?): String? {
    val quality = variant?.quality ?: return null
    return buildList {
        if (quality.lossless == true) add("Lossless")
        quality.bitDepth?.takeIf { it > 0 }?.let { add("$it-bit") }
        quality.sampleRateHz?.takeIf { it > 0 }?.let {
            add(if (it % 1000 == 0) "${it / 1000} kHz" else String.format(Locale.US, "%.1f kHz", it / 1000.0))
        }
        quality.bitrateBitsPerSecond?.takeIf { it > 0 }?.let { add("${it / 1000} kbps") }
    }.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

internal fun universalResultRowKey(index: Int, identity: String): String = "universal-result:$index:$identity"
