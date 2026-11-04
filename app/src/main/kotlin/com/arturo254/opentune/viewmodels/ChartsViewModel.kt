package com.arturo254.opentune.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.pages.ChartsPage
import com.arturo254.opentune.playback.PlayerConnection
import com.arturo254.opentune.sources.deezer.DeezerMusicSource
import com.arturo254.opentune.sources.playback.SourcePlaybackCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ChartsViewModel
@Inject
constructor(
    private val deezer: DeezerMusicSource,
    private val playback: SourcePlaybackCoordinator,
) : ViewModel() {
    private val _catalogChart = MutableStateFlow<List<SourceVariant>>(emptyList())
    val catalogChart = _catalogChart.asStateFlow()

    suspend fun playCatalogTrack(
        connection: PlayerConnection,
        track: SourceVariant,
        title: String,
    ) = playback.play(connection, track, title)

    private val _chartsPage = MutableStateFlow<ChartsPage?>(null)
    val chartsPage = _chartsPage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun loadCharts() {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            YouTube.getChartsPage()
                .onSuccess { page -> _chartsPage.value = page }
                .onFailure { e -> _error.value = "Failed to load charts: ${e.message}" }

            if (_chartsPage.value?.sections.orEmpty().all { it.items.isEmpty() }) {
                // Some regions return a successful page without rankings. Use the existing catalog.
                deezer.trendingTracks(limit = 24).onSuccess { _catalogChart.value = it }
            } else {
                _catalogChart.value = emptyList()
            }
            _isLoading.value = false
        }
    }

    fun loadMore() {
        if (_isLoading.value) return
        viewModelScope.launch {
            _chartsPage.value?.continuation?.let { continuation ->
                _isLoading.value = true
                YouTube.getChartsPage(continuation)
                    .onSuccess { newPage ->
                        _chartsPage.value =
                            _chartsPage.value?.copy(
                                sections = _chartsPage.value?.sections.orEmpty() + newPage.sections,
                                continuation = newPage.continuation,
                            )
                    }
                    .onFailure { e -> _error.value = "Failed to load more: ${e.message}" }
                _isLoading.value = false
            }
        }
    }
}
