package com.arturo254.opentune.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arturo254.opentune.audius.Audius
import com.arturo254.opentune.audius.AudiusTrack
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

data class AudiusSearchState(
    val tracks: List<AudiusTrack> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val showingTrending: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class AudiusSearchViewModel
@Inject
constructor() : ViewModel() {
    val query = MutableStateFlow("")

    val state: StateFlow<AudiusSearchState> =
        query
            .debounce(180)
            .distinctUntilChanged()
            .flatMapLatest { rawQuery ->
                flow {
                    emit(
                        AudiusSearchState(
                            loading = true,
                            showingTrending = rawQuery.isBlank(),
                        )
                    )

                    val result =
                        if (rawQuery.isBlank()) {
                            Audius.trendingTracks(limit = 32)
                        } else {
                            Audius.searchTracks(
                                query = rawQuery.trim(),
                                limit = 32,
                            )
                        }

                    result.fold(
                        onSuccess = { tracks ->
                            emit(
                                AudiusSearchState(
                                    tracks = tracks,
                                    loading = false,
                                    showingTrending = rawQuery.isBlank(),
                                )
                            )
                        },
                        onFailure = { throwable ->
                            emit(
                                AudiusSearchState(
                                    loading = false,
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
                initialValue = AudiusSearchState(loading = true),
            )
}
