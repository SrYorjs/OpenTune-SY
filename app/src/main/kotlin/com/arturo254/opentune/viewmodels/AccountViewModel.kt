package com.arturo254.opentune.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.AlbumItem
import com.arturo254.opentune.innertube.models.ArtistItem
import com.arturo254.opentune.innertube.models.PlaylistItem
import com.arturo254.opentune.innertube.utils.completed
import com.arturo254.opentune.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import javax.inject.Inject

enum class AccountContentType {
    PLAYLISTS, ALBUMS, ARTISTS
}

@HiltViewModel
class AccountViewModel @Inject constructor() : ViewModel() {
    val playlists = MutableStateFlow<List<PlaylistItem>?>(null)
    val albums = MutableStateFlow<List<AlbumItem>?>(null)
    val artists = MutableStateFlow<List<ArtistItem>?>(null)

    val selectedContentType = MutableStateFlow(AccountContentType.PLAYLISTS)

    val isLoading = MutableStateFlow(true)
    val failed = MutableStateFlow(false)
    private var loadJob: Job? = null

    init { refresh() }

    fun refresh() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            isLoading.value = true
            failed.value = false
            try {
                YouTube.library("FEmusic_liked_playlists").completed().onSuccess {
                    playlists.value = it.items.filterIsInstance<PlaylistItem>().filterNot { it.id == "SE" }
                }.onFailure { handleFailure(it) }
                YouTube.library("FEmusic_liked_albums").completed().onSuccess {
                    albums.value = it.items.filterIsInstance<AlbumItem>()
                }.onFailure { handleFailure(it) }
                YouTube.library("FEmusic_library_corpus_artists").completed().onSuccess {
                    artists.value = it.items.filterIsInstance<ArtistItem>()
                }.onFailure { handleFailure(it) }
            } finally {
                isLoading.value = false
            }
        }
    }

    private fun handleFailure(error: Throwable) {
        if (error is CancellationException) throw error
        failed.value = true
        reportException(error)
    }

    fun setSelectedContentType(contentType: AccountContentType) {
        selectedContentType.value = contentType
    }
}
