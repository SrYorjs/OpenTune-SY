package com.arturo254.opentune.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.sources.TrackSearchRequest
import com.arturo254.opentune.sources.apple.AppleMusicSource
import com.arturo254.opentune.sources.tidal.TidalMusicSource
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

data class CatalogProviderSearchState(
    val tracks: List<SourceVariant> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
private fun searchState(
    scope: kotlinx.coroutines.CoroutineScope,
    query: MutableStateFlow<String>,
    search: suspend (TrackSearchRequest) -> Result<com.arturo254.opentune.sources.TrackSearchPage>,
): StateFlow<CatalogProviderSearchState> =
    query
        .debounce(180)
        .distinctUntilChanged()
        .flatMapLatest { rawQuery ->
            flow {
                val normalizedQuery = rawQuery.trim()
                if (normalizedQuery.isEmpty()) {
                    emit(CatalogProviderSearchState())
                    return@flow
                }

                emit(CatalogProviderSearchState(loading = true))
                search(
                    TrackSearchRequest(
                        query = normalizedQuery,
                        limit = 50,
                    ),
                ).fold(
                    onSuccess = { page ->
                        emit(
                            CatalogProviderSearchState(
                                tracks = page.items,
                            ),
                        )
                    },
                    onFailure = { throwable ->
                        emit(
                            CatalogProviderSearchState(
                                error = throwable.message,
                            ),
                        )
                    },
                )
            }
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CatalogProviderSearchState(),
        )

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class TidalSearchViewModel
@Inject
constructor(
    private val source: TidalMusicSource,
) : ViewModel() {
    val query = MutableStateFlow("")

    val state: StateFlow<CatalogProviderSearchState> =
        searchState(
            scope = viewModelScope,
            query = query,
            search = source::searchTracks,
        )
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class AppleMusicSearchViewModel
@Inject
constructor(
    private val source: AppleMusicSource,
) : ViewModel() {
    val query = MutableStateFlow("")

    val state: StateFlow<CatalogProviderSearchState> =
        searchState(
            scope = viewModelScope,
            query = query,
            search = source::searchTracks,
        )
}
