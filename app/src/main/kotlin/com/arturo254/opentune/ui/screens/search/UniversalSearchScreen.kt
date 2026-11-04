package com.arturo254.opentune.ui.screens.search

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.UniversalAudioSelectionModeKey
import com.arturo254.opentune.core.model.ArtistIdentity
import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.core.model.TrackIdentity
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.sources.offline.OfflineTrackState
import com.arturo254.opentune.sources.resolution.AudioSelectionMode
import com.arturo254.opentune.sources.resolution.SourceVariantKey
import com.arturo254.opentune.sources.search.UnifiedTrackSearchResult
import com.arturo254.opentune.ui.screens.*
import com.arturo254.opentune.utils.rememberEnumPreference
import com.arturo254.opentune.viewmodels.UniversalSearchViewModel
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * A focused search workspace: music, collections and source choices share one live result state.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UniversalSearchScreen(
    query: String,
    pureBlack: Boolean,
    viewModel: UniversalSearchViewModel = hiltViewModel(),
    embedded: Boolean = false,
) {
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
    var playbackJob by remember { mutableStateOf<Job?>(null) }
    var downloading by remember { mutableStateOf<String?>(null) }
    var dialog by remember(query) { mutableStateOf<String?>(null) }
    val manual = remember { mutableStateMapOf<String, SourceVariantKey>() }
    var quality by
        rememberEnumPreference(UniversalAudioSelectionModeKey, AudioSelectionMode.AUTOMATIC)
    // During the debounce interval do not relabel the previous query's results.
    val sameQuery = state.query == query.trim()
    val entries = if (sameQuery) state.result.tracks else emptyList()
    var visibleCount by remember(query) { mutableIntStateOf(50) }
    var lastPageRequest by remember(query) { mutableStateOf("") }
    var sourceDetails by remember(query) { mutableStateOf(false) }
    var category by remember(query) { mutableStateOf(SearchResultCategory.ALL) }
    var activeAlbum by remember(query) { mutableStateOf<String?>(null) }
    val albums = remember(entries) { groupSearchAlbums(entries) }
    val remoteMatchesQuery = audiusState.query == query.trim()
    val discoveredPlaylists = if (remoteMatchesQuery) audiusState.playlists else emptyList()
    val selectedCollection = audiusState.selected.takeIf { remoteMatchesQuery }
    val collectionVariants = if (selectedCollection != null) audiusState.tracks else emptyList()
    val collectionEntries =
        remember(collectionVariants) {
            collectionVariants.map { variant ->
                UnifiedTrackSearchResult(
                    identity =
                        TrackIdentity(
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
    val selectedTracks =
        when (category) {
            SearchResultCategory.ALL,
            SearchResultCategory.SONGS -> entries
            SearchResultCategory.PLAYLISTS ->
                if (selectedCollection != null) collectionEntries else emptyList()
            SearchResultCategory.ALBUMS ->
                albums.firstOrNull { it.key == activeAlbum }?.tracks.orEmpty()
        }
    val showingTracks =
        category == SearchResultCategory.ALL ||
            category == SearchResultCategory.SONGS ||
            (category == SearchResultCategory.PLAYLISTS && selectedCollection != null) ||
            (category == SearchResultCategory.ALBUMS && activeAlbum != null)
    val availableCount =
        when (category) {
            SearchResultCategory.PLAYLISTS ->
                if (selectedCollection == null) discoveredPlaylists.size else selectedTracks.size
            SearchResultCategory.ALBUMS ->
                if (activeAlbum == null) albums.size else selectedTracks.size
            else -> selectedTracks.size
        }
    val visibleEntries = selectedTracks.take(visibleCount)
    val failedSources =
        if (sameQuery && !state.isLoading) {
            state.result.sources.values.filter { it.error != null }
        } else {
            emptyList()
        }
    val nextPageToken =
        state.result.sources.values.mapNotNull { it.continuationToken }.sorted().joinToString("|")
    val loading = state.isLoading || (!sameQuery && query.isNotBlank())
    val discoveryLoading = remoteMatchesQuery && audiusState.loading
    BackHandler(enabled = selectedCollection != null || activeAlbum != null) {
        activeAlbum = null
        viewModel.closeAudiusCollection()
        visibleCount = 50
    }

    LaunchedEffect(query) {
        viewModel.query.value = query
        list.scrollToItem(0)
    }
    LaunchedEffect(
        list,
        query,
        category,
        activeAlbum,
        availableCount,
        visibleCount,
        nextPageToken,
        state.isLoadingMore,
    ) {
        if (
            !sameQuery ||
                state.isLoading ||
                (category != SearchResultCategory.ALL && category != SearchResultCategory.SONGS)
        )
            return@LaunchedEffect
        snapshotFlow {
                val info = list.layoutInfo
                info.totalItemsCount > 0 &&
                    (info.visibleItemsInfo.lastOrNull()?.index ?: -1) >= info.totalItemsCount - 6
            }
            .distinctUntilChanged()
            .filter { it }
            .collect {
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
        if (player != null && variants.isNotEmpty()) {
            val requestedKey = result.signalKey()
            playbackJob?.cancel()
            resolving = requestedKey
            playbackJob =
                scope.launch {
                    try {
                        val played =
                            if (selectedCollection != null) {
                                val index =
                                    collectionVariants.indexOfFirst { source ->
                                        result.variants.any {
                                            it.provider == source.provider &&
                                                it.sourceId == source.sourceId
                                        }
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
                                    if (activeAlbum != null)
                                        state.result.copy(tracks = selectedTracks)
                                    else state.result,
                                    variants,
                                    context.getString(R.string.queue_searched_songs),
                                )
                            }
                        if (!played && player.player.playerError == null) {
                            Toast.makeText(
                                    context,
                                    R.string.universal_versions_play_error,
                                    Toast.LENGTH_SHORT,
                                )
                                .show()
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        Toast.makeText(
                                context,
                                R.string.universal_versions_play_error,
                                Toast.LENGTH_SHORT,
                            )
                            .show()
                    } finally {
                        if (resolving == requestedKey) resolving = null
                    }
                }
        }
    }
    val selectedAlbum = albums.firstOrNull { it.key == activeAlbum }
    val closeCollection: () -> Unit = {
        activeAlbum = null
        viewModel.closeAudiusCollection()
        visibleCount = 50
        scope.launch { list.animateScrollToItem(0) }
    }
    PrismScreenFrame(pureBlack = pureBlack, reserveAppBar = !embedded, insetTop = !embedded) {
        layout,
        insets ->
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize(),
            contentPadding = insets,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item("prism/results/header") {
                PrismHeading(
                    title = query.ifBlank { stringResource(R.string.prism_results_title) },
                    eyebrow = stringResource(R.string.prism_results_eyebrow),
                    subtitle =
                        if (query.isBlank()) stringResource(R.string.prism_results_hint)
                        else if (loading) stringResource(R.string.prism_results_searching)
                        else stringResource(R.string.prism_results_count, availableCount),
                    onBack =
                        if (selectedCollection != null || activeAlbum != null) closeCollection
                        else null,
                )
            }
            item("prism/results/categories") {
                PrismResultCategories(category) { selected ->
                    category = selected
                    activeAlbum = null
                    viewModel.closeAudiusCollection()
                    visibleCount = 50
                    lastPageRequest = ""
                    scope.launch { list.animateScrollToItem(0) }
                }
            }
            if (failedSources.isNotEmpty())
                item("prism/results/sources") {
                    PrismSourceNotice(
                        sources =
                            failedSources.map {
                                variantProviderName(it.provider) to sourceFailureLabel(it.error)
                            },
                        expanded = sourceDetails,
                        onExpand = { sourceDetails = !sourceDetails },
                        onRetry = viewModel::retrySearch,
                    )
                }
            if ((loading || discoveryLoading) && entries.isEmpty() && discoveredPlaylists.isEmpty())
                item("prism/results/loading") {
                    PrismResultLoading(stringResource(R.string.prism_results_searching))
                }
            if (
                !loading &&
                    !discoveryLoading &&
                    entries.isEmpty() &&
                    discoveredPlaylists.isEmpty() &&
                    (category == SearchResultCategory.ALL ||
                        category == SearchResultCategory.SONGS) &&
                    query.isNotBlank()
            )
                item("prism/results/empty") {
                    PrismStatus(
                        title =
                            stringResource(
                                if (state.error != null) R.string.prism_results_error
                                else R.string.prism_results_empty
                            ),
                        detail =
                            stringResource(
                                if (state.error != null) R.string.prism_results_error_detail
                                else R.string.prism_results_empty_detail
                            ),
                        actionLabel =
                            if (state.error != null) stringResource(R.string.prism_results_retry)
                            else null,
                        onAction = if (state.error != null) viewModel::retrySearch else null,
                    )
                }
            if (selectedCollection != null && category == SearchResultCategory.PLAYLISTS) {
                item("prism/results/collection") {
                    PrismCollectionHeader(
                        title = selectedCollection.title,
                        subtitle = selectedCollection.subtitle,
                        artwork = selectedCollection.artworkUrl,
                        description = selectedCollection.description,
                        onPlay =
                            if (player != null && collectionVariants.isNotEmpty())
                                ({
                                    scope.launch {
                                        val played =
                                            viewModel.playAudiusCollection(
                                                player,
                                                collectionVariants,
                                                0,
                                                selectedCollection.title,
                                            )
                                        if (!played)
                                            Toast.makeText(
                                                    context,
                                                    R.string.universal_versions_play_error,
                                                    Toast.LENGTH_SHORT,
                                                )
                                                .show()
                                    }
                                })
                            else null,
                    )
                }
                if (audiusState.loadingCollection)
                    item("prism/results/collection-loading") {
                        PrismResultLoading(stringResource(R.string.prism_results_loading))
                    }
                if (audiusState.collectionError != null)
                    item("prism/results/collection-error") {
                        val retryPlaylist =
                            discoveredPlaylists.firstOrNull { it.id == selectedCollection.id }
                        PrismStatus(
                            stringResource(R.string.prism_results_collection_error),
                            audiusState.collectionError,
                            actionLabel =
                                if (retryPlaylist != null)
                                    stringResource(R.string.prism_results_retry)
                                else null,
                            onAction =
                                retryPlaylist?.let { playlist ->
                                    ({ viewModel.openAudiusPlaylist(playlist) })
                                },
                        )
                    }
                if (
                    !audiusState.loadingCollection &&
                        collectionVariants.isEmpty() &&
                        audiusState.collectionError == null
                )
                    item("prism/results/collection-empty") {
                        PrismStatus(
                            stringResource(R.string.prism_results_collection_empty),
                            stringResource(R.string.prism_results_collection_empty_detail),
                        )
                    }
            }
            if (selectedAlbum != null && category == SearchResultCategory.ALBUMS)
                item("prism/results/album-header") {
                    PrismCollectionHeader(
                        selectedAlbum.title,
                        selectedAlbum.artist,
                        selectedAlbum.artworkUrl,
                        onPlay =
                            selectedAlbum.tracks
                                .firstOrNull()
                                ?.takeIf { player != null }
                                ?.let { track ->
                                    {
                                        play(
                                            track,
                                            listOfNotNull(
                                                viewModel.selectVariant(
                                                    track,
                                                    track.variants,
                                                    quality,
                                                    manual[track.signalKey()],
                                                )
                                            ),
                                        )
                                    }
                                },
                    )
                }
            if (category == SearchResultCategory.ALL && discoveredPlaylists.isNotEmpty()) {
                item("prism/results/featured-heading") {
                    PrismSectionTitle(stringResource(R.string.prism_results_playlists))
                }
                item("prism/results/featured-playlists") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(discoveredPlaylists.take(8), key = { it.id }) { playlist ->
                            PrismCollectionTile(
                                playlist.name,
                                playlist.creator,
                                playlist.artworkUrl,
                                onClick = {
                                    category = SearchResultCategory.PLAYLISTS
                                    viewModel.openAudiusPlaylist(playlist)
                                    visibleCount = 50
                                    scope.launch { list.animateScrollToItem(0) }
                                },
                                modifier = Modifier.width(layout.tileWidth),
                            )
                        }
                    }
                }
                if (entries.isNotEmpty())
                    item("prism/results/tracks-heading") {
                        PrismSectionTitle(stringResource(R.string.prism_results_songs))
                    }
            }
            if (category == SearchResultCategory.PLAYLISTS && selectedCollection == null) {
                if (discoveryLoading)
                    item("prism/results/playlist-loading") {
                        PrismResultLoading(stringResource(R.string.prism_results_searching))
                    }
                if (!discoveryLoading && discoveredPlaylists.isEmpty())
                    item("prism/results/playlist-empty") {
                        PrismStatus(
                            stringResource(R.string.prism_results_no_playlists),
                            audiusState.playlistError
                                ?: stringResource(R.string.prism_results_playlist_detail),
                        )
                    }
                itemsIndexed(
                    discoveredPlaylists.take(visibleCount).chunked(layout.columns),
                    key = { index, _ -> "prism/results/playlist-row/$index" },
                ) { _, row ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        row.forEach { playlist ->
                            PrismCollectionTile(
                                playlist.name,
                                playlist.creator,
                                playlist.artworkUrl,
                                onClick = {
                                    viewModel.openAudiusPlaylist(playlist)
                                    visibleCount = 50
                                    scope.launch { list.animateScrollToItem(0) }
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(layout.columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            if (category == SearchResultCategory.ALBUMS && activeAlbum == null) {
                if (!loading && albums.isEmpty())
                    item("prism/results/albums-empty") {
                        PrismStatus(
                            stringResource(R.string.prism_results_no_albums),
                            stringResource(R.string.prism_results_album_detail),
                        )
                    }
                itemsIndexed(
                    albums.take(visibleCount).chunked(layout.columns),
                    key = { index, _ -> "prism/results/album-row/$index" },
                ) { _, row ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        row.forEach { album ->
                            PrismCollectionTile(
                                album.title,
                                album.artist,
                                album.artworkUrl,
                                onClick = {
                                    activeAlbum = album.key
                                    visibleCount = 50
                                    lastPageRequest = ""
                                    scope.launch { list.animateScrollToItem(0) }
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(layout.columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            if (showingTracks)
                itemsIndexed(
                    visibleEntries,
                    key = { index, result -> universalResultRowKey(index, result.signalKey()) },
                    contentType = { _, _ -> "prism/recording" },
                ) { index, result ->
                    val key = result.signalKey()
                    val sources = result.variants.map { it.provider to it.sourceId }.toSet()
                    val asset =
                        offlineAssets.firstOrNull { asset ->
                            asset.discoverySources.any { (it.provider to it.sourceId) in sources }
                        }
                    val offline =
                        asset?.takeIf { it.state == OfflineTrackState.AVAILABLE }?.toSourceVariant()
                            ?: result.variants.firstOrNull {
                                it.provider == ProviderId.OPEN_TUNE_OFFLINE
                            }
                    val preferred =
                        viewModel.selectVariant(
                            result,
                            result.variants + listOfNotNull(offline),
                            quality,
                            manual[key],
                        )
                    val fallbacks =
                        (result.variants + listOfNotNull(offline))
                            .filterNot {
                                it.provider == preferred?.provider &&
                                    it.sourceId == preferred?.sourceId
                            }
                            .sortedBy { variant ->
                                when (variant.provider) {
                                    ProviderId.OPEN_TUNE_OFFLINE,
                                    ProviderId.LOCAL -> 0
                                    ProviderId.AUDIUS -> 1
                                    ProviderId.YOUTUBE_MUSIC -> 2
                                    else -> 3
                                }
                            }
                    val active =
                        (current != null &&
                            (result.variants + listOfNotNull(offline)).any {
                                it.sourceId == current.id
                            }) ||
                            (current != null &&
                                (preferred == null || preferred.isCatalogOnly()) &&
                                current.title.equals(result.identity.title, true) &&
                                result.identity.artists.any { artist ->
                                    current.artists.any { it.name.equals(artist.name, true) }
                                })
                    val pending = downloading == key || asset?.state == OfflineTrackState.PENDING
                    val canDownload =
                        offline == null &&
                            !pending &&
                            result.variants.any {
                                when (it.provider) {
                                    ProviderId.LOCAL,
                                    ProviderId.OPEN_TUNE_OFFLINE -> false
                                    ProviderId.AUDIUS -> it.downloadable == true
                                    else -> true
                                }
                            }
                    PrismRecordingRow(
                        result = result,
                        preferred = preferred,
                        active = active,
                        playing = playing,
                        resolving = resolving == key,
                        downloaded = offline != null,
                        downloading = pending,
                        modifier = Modifier.animateItem(),
                        onVersions = { dialog = key },
                        onPlay =
                            if (
                                player != null &&
                                    (active || preferred != null || fallbacks.isNotEmpty())
                            ) {
                                {
                                    if (active) player.player.togglePlayPause()
                                    else play(result, listOfNotNull(preferred) + fallbacks)
                                }
                            } else null,
                        onDownload =
                            if (canDownload) {
                                {
                                    if (downloading == null)
                                        scope.launch {
                                            downloading = key
                                            try {
                                                viewModel
                                                    .enqueueOffline(result)
                                                    .onSuccess {
                                                        Toast.makeText(
                                                                context,
                                                                R.string
                                                                    .catalog_offline_download_started,
                                                                Toast.LENGTH_SHORT,
                                                            )
                                                            .show()
                                                    }
                                                    .onFailure {
                                                        Toast.makeText(
                                                                context,
                                                                it.message
                                                                    ?: context.getString(
                                                                        R.string
                                                                            .catalog_source_unplayable
                                                                    ),
                                                                Toast.LENGTH_SHORT,
                                                            )
                                                            .show()
                                                    }
                                            } finally {
                                                downloading = null
                                            }
                                        }
                                }
                            } else null,
                    )
                }

            if (
                sameQuery &&
                    selectedCollection == null &&
                    category != SearchResultCategory.PLAYLISTS &&
                    state.isLoadingMore
            )
                item("prism/results/more-loading") {
                    PrismResultLoading(stringResource(R.string.prism_results_loading))
                }
            else if (visibleCount < availableCount)
                item("prism/results/more-local") {
                    TextButton(
                        onClick = { visibleCount += 50 },
                        modifier = Modifier.padding(horizontal = 20.dp),
                    ) {
                        Text(stringResource(R.string.prism_results_more))
                    }
                }
            else if (
                sameQuery &&
                    selectedCollection == null &&
                    activeAlbum == null &&
                    category != SearchResultCategory.PLAYLISTS &&
                    state.result.hasMore &&
                    entries.isNotEmpty()
            )
                item("prism/results/more-remote") {
                    TextButton(
                        onClick = viewModel::loadMore,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    ) {
                        Text(stringResource(R.string.prism_results_more))
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
            onClearManual = {
                manual.remove(selected.signalKey())
                dialog = null
            },
            onDismiss = { dialog = null },
        )
    }
}

/** Includes version traits and album so alternate recordings cannot share choices. */
private fun UnifiedTrackSearchResult.signalKey(): String =
    identity.isrc?.takeIf { it.isNotBlank() }?.let { "isrc:${it.uppercase(Locale.ROOT)}" }
        ?: listOf(
                identity.title.trim().lowercase(Locale.ROOT),
                identity.artists.joinToString(",") { it.name.trim().lowercase(Locale.ROOT) },
                identity.durationMs ?: -1L,
                identity.album?.canonicalId ?: identity.album?.title.orEmpty(),
                identity.version.traits.map { it.name }.sorted().joinToString(","),
                identity.version.label.orEmpty().trim().lowercase(Locale.ROOT),
            )
            .joinToString("|")

private fun signalProvider(provider: ProviderId): String =
    when (provider) {
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

internal fun sourceFailureLabel(
    error: com.arturo254.opentune.sources.search.UnifiedSearchError?
): String {
    val type = error?.type.orEmpty()
    val details = error?.message.orEmpty()
    return when {
        "TimeoutException" in type -> "No respondió antes del límite de espera"
        "UnknownHostException" in type -> "Sin conexión con el proveedor"
        "SocketTimeoutException" in type -> "El servidor tardó demasiado"
        "unauthorized" in details.lowercase() ||
            "http 401" in details.lowercase() ||
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
            quality.sampleRateHz
                ?.takeIf { it > 0 }
                ?.let {
                    add(
                        if (it % 1000 == 0) "${it / 1000} kHz"
                        else String.format(Locale.US, "%.1f kHz", it / 1000.0)
                    )
                }
            quality.bitrateBitsPerSecond?.takeIf { it > 0 }?.let { add("${it / 1000} kbps") }
        }
        .takeIf { it.isNotEmpty() }
        ?.joinToString(" · ")
}

internal fun universalResultRowKey(index: Int, identity: String): String =
    "universal-result:$index:$identity"
