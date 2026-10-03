package com.arturo254.opentune.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arturo254.opentune.sources.search.UnifiedSearchRepository
import com.arturo254.opentune.sources.search.UnifiedSearchRequest
import com.arturo254.opentune.sources.search.UnifiedSearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class UniversalSearchViewModel
@Inject
constructor(
    private val repository: UnifiedSearchRepository,
) : ViewModel() {
    val query = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val state =
        query
            .distinctUntilChanged()
            .flatMapLatest { rawQuery ->
                flow {
                    val normalizedQuery = rawQuery.trim()
                    if (normalizedQuery.isEmpty()) {
                        emit(UniversalSearchUiState())
                        return@flow
                    }

                    emit(
                        UniversalSearchUiState(
                            query = normalizedQuery,
                            isLoading = true,
                        ),
                    )

                    runCatching {
                        repository.search(
                            UnifiedSearchRequest(query = normalizedQuery),
                        )
                    }.onSuccess { result ->
                        emit(
                            UniversalSearchUiState(
                                query = normalizedQuery,
                                result = result,
                            ),
                        )
                    }.onFailure { throwable ->
                        emit(
                            UniversalSearchUiState(
                                query = normalizedQuery,
                                error = throwable,
                            ),
                        )
                    }
                }
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                UniversalSearchUiState(),
            )
}

data class UniversalSearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val result: UnifiedSearchResult = UnifiedSearchResult(query = ""),
    val error: Throwable? = null,
)
