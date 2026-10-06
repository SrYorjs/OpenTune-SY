package com.arturo254.opentune.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.sources.TrackSearchRequest
import com.arturo254.opentune.sources.spotify.SpotifyMusicSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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

data class SpotifySearchState(
    val tracks: List<SourceVariant> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val isConnected: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SpotifySearchViewModel
@Inject
constructor(
    private val spotifySource: SpotifyMusicSource,
) : ViewModel() {
    val query = MutableStateFlow("")

    val state: StateFlow<SpotifySearchState> =
        query
            .debounce(180)
            .distinctUntilChanged()
            .flatMapLatest { rawQuery ->
                flow {
                    emit(SpotifySearchState(loading = true, isConnected = true))

                    val normalizedQuery = rawQuery.trim()
                    if (normalizedQuery.isEmpty()) {
                        emit(
                            SpotifySearchState(
                                isConnected = true,
                                loading = false,
                            ),
                        )
                        return@flow
                    }

                    spotifySource
                        .searchTracks(
                            TrackSearchRequest(
                                query = normalizedQuery,
                                limit = 50,
                            ),
                        ).fold(
                            onSuccess = { page ->
                                emit(
                                    SpotifySearchState(
                                        tracks = page.items,
                                        isConnected = true,
                                    ),
                                )
                            },
                            onFailure = { throwable ->
                                emit(
                                    SpotifySearchState(
                                        error = throwable.message,
                                        isConnected = true,
                                    ),
                                )
                            },
                        )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SpotifySearchState(
                    loading = false,
                    isConnected = true,
                ),
            )
}
