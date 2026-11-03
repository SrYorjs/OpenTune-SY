package com.arturo254.opentune.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arturo254.opentune.sources.audius.AudiusArtist
import com.arturo254.opentune.sources.audius.AudiusPlaylist
import com.arturo254.opentune.sources.audius.AudiusCollectionType
import com.arturo254.opentune.sources.audius.AudiusSelectedCollection
import com.arturo254.opentune.sources.audius.AudiusDiscoveryRepository
import com.arturo254.opentune.sources.search.UnifiedSearchRepository
import com.arturo254.opentune.sources.search.UnifiedSearchRequest
import com.arturo254.opentune.sources.search.UnifiedSearchResult
import com.arturo254.opentune.sources.search.UnifiedTrackSearchResult
import com.arturo254.opentune.sources.offline.OfflineDownloadCoordinator
import com.arturo254.opentune.sources.offline.OfflineTrackAsset
import com.arturo254.opentune.sources.offline.OfflineTrackRegistry
import com.arturo254.opentune.sources.playback.SourcePlaybackCoordinator
import com.arturo254.opentune.playback.PlayerConnection
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.sources.resolution.AudioSelectionMode
import com.arturo254.opentune.sources.resolution.SourceVariantKey
import com.arturo254.opentune.sources.resolution.UniversalVersionQualityPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
@HiltViewModel
class UniversalSearchViewModel
@Inject
constructor(
    private val repository: UnifiedSearchRepository,
    private val sourcePlaybackCoordinator: SourcePlaybackCoordinator,
    private val offlineDownloadCoordinator: OfflineDownloadCoordinator,
    private val offlineTrackRegistry: OfflineTrackRegistry,
    private val versionQualityPolicy: UniversalVersionQualityPolicy,
    private val audiusDiscovery: AudiusDiscoveryRepository,
) : ViewModel() {
    val query = MutableStateFlow("")
    val offlineAssets = offlineTrackRegistry.assets

    private val _state = MutableStateFlow(UniversalSearchUiState())
    val state: StateFlow<UniversalSearchUiState> = _state.asStateFlow()
    private val _audiusState = MutableStateFlow(AudiusDiscoveryUiState())
    val audiusState: StateFlow<AudiusDiscoveryUiState> = _audiusState.asStateFlow()
    private var collectionJob: Job? = null

    private var loadMoreJob: Job? = null

    init {
        viewModelScope.launch {
            query
                .debounce(250)
                .distinctUntilChanged()
                .collectLatest { rawQuery ->
                    loadMoreJob?.cancel()
                    collectionJob?.cancel()
                    coroutineScope {
                        launch { performSearch(rawQuery) }
                        launch { performAudiusSearch(rawQuery) }
                    }
                }
        }

    }

    fun selectVariant(
        result: UnifiedTrackSearchResult,
        variants: List<SourceVariant>,
        mode: AudioSelectionMode,
        manualVariant: SourceVariantKey?,
    ): SourceVariant? =
        versionQualityPolicy.choose(
            identity = result.identity,
            variants = variants,
            mode = mode,
            manualVariant = manualVariant,
        )

    fun relatedVersions(result: UnifiedTrackSearchResult): List<UnifiedTrackSearchResult> =
        versionQualityPolicy.relatedVersions(
            selected = result,
            results = state.value.result.tracks,
        )

    suspend fun playVariant(
        connection: PlayerConnection,
        variant: SourceVariant,
        queueTitle: String,
    ): Boolean =
        sourcePlaybackCoordinator.play(
            connection = connection,
            variant = variant,
            queueTitle = queueTitle,
        )

    suspend fun playFirstAvailable(
        connection: PlayerConnection,
        variants: List<SourceVariant>,
        queueTitle: String,
    ): Boolean =
        sourcePlaybackCoordinator.playFirstAvailable(
            connection = connection,
            variants = variants,
            queueTitle = queueTitle,
        )

    suspend fun playSearchResults(
        connection: PlayerConnection,
        result: UnifiedTrackSearchResult,
        searchResult: UnifiedSearchResult,
        variants: List<SourceVariant>,
        queueTitle: String,
    ): Boolean =
        sourcePlaybackCoordinator.playSearchResults(
            connection = connection,
            result = searchResult,
            selected = result,
            preferredVariants = variants,
            queueTitle = queueTitle,
        )

    fun openAudiusArtist(artist: AudiusArtist) {
        openAudiusCollection(
            AudiusSelectedCollection(
                type = AudiusCollectionType.ARTIST,
                id = artist.id,
                title = artist.name,
                subtitle = artist.handle.takeIf(String::isNotBlank)?.let { "@$it" }.orEmpty(),
                artworkUrl = artist.artworkUrl,
                description = artist.bio,
            ),
        )
    }

    fun openAudiusPlaylist(playlist: AudiusPlaylist) {
        openAudiusCollection(
            AudiusSelectedCollection(
                type = AudiusCollectionType.PLAYLIST,
                id = playlist.id,
                title = playlist.name,
                subtitle = playlist.creator,
                artworkUrl = playlist.artworkUrl,
                description = playlist.description,
            ),
        )
    }

    fun closeAudiusCollection() {
        collectionJob?.cancel()
        _audiusState.update {
            it.copy(selected = null, tracks = emptyList(), loadingCollection = false,
                collectionError = null, hasMoreTracks = false, nextOffset = 0)
        }
    }

    private fun openAudiusCollection(collection: AudiusSelectedCollection) {
        collectionJob?.cancel()
        _audiusState.update {
            it.copy(selected = collection, tracks = emptyList(), loadingCollection = true,
                collectionError = null, hasMoreTracks = false, nextOffset = 0)
        }
        collectionJob = viewModelScope.launch {
            val result = when (collection.type) {
                AudiusCollectionType.ARTIST -> audiusDiscovery.artistTracks(collection.id, 0)
                AudiusCollectionType.PLAYLIST -> audiusDiscovery.playlistTracks(collection.id)
            }
            _audiusState.update { current ->
                if (current.selected != collection) current else result.fold(
                    onSuccess = { page ->
                        current.copy(
                            tracks = page.tracks,
                            loadingCollection = false,
                            hasMoreTracks = page.hasMore,
                            nextOffset = 50,
                        )
                    },
                    onFailure = { error ->
                        current.copy(loadingCollection = false, collectionError = error.message)
                    },
                )
            }
        }
    }

    fun loadMoreAudiusTracks() {
        val snapshot = _audiusState.value
        val selected = snapshot.selected ?: return
        if (selected.type != AudiusCollectionType.ARTIST ||
            !snapshot.hasMoreTracks || snapshot.loadingCollection
        ) return
        collectionJob?.cancel()
        _audiusState.update { it.copy(loadingCollection = true) }
        collectionJob = viewModelScope.launch {
            val page = audiusDiscovery.artistTracks(selected.id, snapshot.nextOffset)
            _audiusState.update { current ->
                if (current.selected != selected) current else page.fold(
                    onSuccess = {
                        current.copy(
                            tracks = (current.tracks + it.tracks)
                                .distinctBy { variant -> variant.sourceId },
                            hasMoreTracks = it.hasMore,
                            nextOffset = snapshot.nextOffset + 50,
                            loadingCollection = false,
                            collectionError = null,
                        )
                    },
                    onFailure = {
                        current.copy(loadingCollection = false,
                            collectionError = it.message)
                    },
                )
            }
        }
    }

    suspend fun playAudiusCollection(
        connection: PlayerConnection,
        tracks: List<SourceVariant>,
        startIndex: Int,
        title: String,
    ): Boolean = sourcePlaybackCoordinator.playCollection(
        connection = connection,
        tracks = tracks,
        startIndex = startIndex,
        queueTitle = title,
    )

    private suspend fun performAudiusSearch(rawQuery: String) {
        val normalized = rawQuery.trim()
        if (normalized.isEmpty()) {
            _audiusState.value = AudiusDiscoveryUiState()
            return
        }
        _audiusState.value = AudiusDiscoveryUiState(query = normalized, loading = true)
        val playlists = audiusDiscovery.searchPlaylists(normalized)
        if (query.value.trim() != normalized) return
        _audiusState.update {
            it.copy(
                playlists = playlists.getOrDefault(emptyList()),
                loading = false,
                playlistError = playlists.exceptionOrNull()?.message,
            )
        }
    }

    fun retrySearch() {
        val currentQuery = query.value.trim()
        if (currentQuery.isEmpty()) return
        loadMoreJob?.cancel()
        viewModelScope.launch {
            coroutineScope {
                launch { performSearch(currentQuery) }
                launch { performAudiusSearch(currentQuery) }
            }
        }
    }

    suspend fun enqueueOffline(result: UnifiedTrackSearchResult): Result<OfflineTrackAsset> =
        offlineDownloadCoordinator.enqueue(
            identity = result.identity,
            variants = result.variants,
        )

    fun loadMore() {
        val snapshot = _state.value
        if (
            snapshot.query.isBlank() ||
            snapshot.isLoading ||
            snapshot.isLoadingMore ||
            !snapshot.result.hasMore
        ) {
            return
        }
        if (loadMoreJob?.isActive == true) return

        val requestedQuery = snapshot.query
        val previousResult = snapshot.result

        loadMoreJob =
            viewModelScope.launch {
                _state.update { current ->
                    if (current.query == requestedQuery) {
                        current.copy(isLoadingMore = true)
                    } else {
                        current
                    }
                }

                runCatching {
                    repository.loadMore(
                        request = UnifiedSearchRequest(query = requestedQuery),
                        previous = previousResult,
                    )
                }.onSuccess { result ->
                    _state.update { current ->
                        if (current.query == requestedQuery) {
                            current.copy(
                                isLoadingMore = false,
                                result = result,
                                error = null,
                            )
                        } else {
                            current
                        }
                    }
                }.onFailure { throwable ->
                    _state.update { current ->
                        if (current.query == requestedQuery) {
                            current.copy(
                                isLoadingMore = false,
                                error = throwable,
                            )
                        } else {
                            current
                        }
                    }
                }
            }
    }

    private suspend fun performSearch(rawQuery: String) {
        val normalizedQuery = rawQuery.trim()
        if (normalizedQuery.isEmpty()) {
            _state.value = UniversalSearchUiState()
            return
        }

        _state.value =
            UniversalSearchUiState(
                query = normalizedQuery,
                isLoading = true,
            )

        runCatching {
            repository.search(
                UnifiedSearchRequest(query = normalizedQuery),
                onPartial = { result ->
                    if (query.value.trim() == normalizedQuery) {
                        _state.value = UniversalSearchUiState(query = normalizedQuery, result = result, isLoading = true)
                    }
                },
            )
        }.onSuccess { result ->
            if (query.value.trim() == normalizedQuery) {
                _state.value =
                    UniversalSearchUiState(
                        query = normalizedQuery,
                        result = result,
                    )
            }
        }.onFailure { throwable ->
            if (throwable is kotlinx.coroutines.CancellationException) throw throwable
            if (query.value.trim() == normalizedQuery) {
                _state.value =
                    UniversalSearchUiState(
                        query = normalizedQuery,
                        error = throwable,
                    )
            }
        }
    }
}

data class UniversalSearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val result: UnifiedSearchResult = UnifiedSearchResult(query = ""),
    val error: Throwable? = null,
)

data class AudiusDiscoveryUiState(
    val query: String = "",
    val loading: Boolean = false,
    val artists: List<AudiusArtist> = emptyList(),
    val playlists: List<AudiusPlaylist> = emptyList(),
    val artistError: String? = null,
    val playlistError: String? = null,
    val selected: AudiusSelectedCollection? = null,
    val tracks: List<SourceVariant> = emptyList(),
    val loadingCollection: Boolean = false,
    val collectionError: String? = null,
    val hasMoreTracks: Boolean = false,
    val nextOffset: Int = 0,
)
