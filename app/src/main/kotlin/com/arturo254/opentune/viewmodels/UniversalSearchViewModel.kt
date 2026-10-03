package com.arturo254.opentune.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arturo254.opentune.sources.search.UnifiedSearchRepository
import com.arturo254.opentune.sources.search.UnifiedSearchRequest
import com.arturo254.opentune.sources.search.UnifiedSearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
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
    }

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
