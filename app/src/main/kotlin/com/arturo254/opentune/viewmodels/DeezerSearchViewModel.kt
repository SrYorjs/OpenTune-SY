package com.arturo254.opentune.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arturo254.opentune.deezer.Deezer
import com.arturo254.opentune.deezer.DeezerTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DeezerSearchState(
    val tracks: List<DeezerTrack> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val showingTrending: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class DeezerSearchViewModel
@Inject
constructor() : ViewModel() {
    val query = MutableStateFlow("")

    val state: StateFlow<DeezerSearchState> =
        query
            .debounce(140)
            .distinctUntilChanged()
            .flatMapLatest { rawQuery ->
                flow {
                    emit(
                        DeezerSearchState(
                            loading = true,
                            showingTrending = rawQuery.isBlank(),
                        )
                    )

                    val result =
                        if (rawQuery.isBlank()) {
                            Deezer.trendingTracks(limit = 30)
                        } else {
                            Deezer.searchTracks(
                                query = rawQuery.trim(),
                                limit = 42,
                            )
                        }

                    result.fold(
                        onSuccess = { tracks ->
                            emit(
                                DeezerSearchState(
                                    tracks = tracks,
                                    showingTrending = rawQuery.isBlank(),
                                )
                            )
                        },
                        onFailure = { throwable ->
                            emit(
                                DeezerSearchState(
                                    error = throwable.message,
                                    showingTrending = rawQuery.isBlank(),
                                )
                            )
                        },
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = DeezerSearchState(loading = true),
            )
}
