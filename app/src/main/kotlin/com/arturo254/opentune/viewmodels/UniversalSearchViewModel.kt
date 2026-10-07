package com.arturo254.opentune.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arturo254.opentune.sources.search.UnifiedSearchRepository
import com.arturo254.opentune.sources.search.UnifiedSearchRequest
import com.arturo254.opentune.sources.search.UnifiedSearchResult
import com.arturo254.opentune.sources.search.UnifiedTrackSearchResult
import com.arturo254.opentune.sources.offline.OfflineDownloadCoordinator
import com.arturo254.opentune.sources.offline.OfflineTrackAsset
import com.arturo254.opentune.sources.offline.OfflineTrackRegistry
import com.arturo254.opentune.sources.offline.OfflineTrackState
import com.arturo254.opentune.sources.soundcloud.SoundCloudPlaybackResolver
import com.arturo254.opentune.core.model.SourceVariant
import androidx.media3.common.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
@HiltViewModel
class UniversalSearchViewModel
@Inject
constructor(
    private val repository: UnifiedSearchRepository,
    private val soundCloudPlaybackResolver: SoundCloudPlaybackResolver,
    private val offlineDownloadCoordinator: OfflineDownloadCoordinator,
    private val offlineTrackRegistry: OfflineTrackRegistry,
) : ViewModel() {
    val query = MutableStateFlow("")

    private val _state = MutableStateFlow(UniversalSearchUiState())
    val state: StateFlow<UniversalSearchUiState> = _state.asStateFlow()

    private var loadMoreJob: Job? = null

    init {
        viewModelScope.launch {
            query
                .debounce(250)
                .distinctUntilChanged()
                .collectLatest { rawQuery ->
                    loadMoreJob?.cancel()
                    performSearch(rawQuery)
                }
        }

        viewModelScope.launch {
            offlineTrackRegistry.assets
                .map { assets ->
                    assets
                        .filter { it.state == OfflineTrackState.AVAILABLE }
                        .map { it.mediaId }
                        .toSet()
                }
                .distinctUntilChanged()
                .drop(1)
                .collect {
                    val currentQuery = query.value.trim()
                    if (currentQuery.isNotEmpty()) {
                        performSearch(currentQuery)
                    }
                }
        }
    }

    suspend fun resolveSoundCloud(variant: SourceVariant): MediaItem? =
        soundCloudPlaybackResolver.resolveToMediaItem(variant)

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
