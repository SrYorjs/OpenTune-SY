package com.arturo254.opentune.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arturo254.opentune.constants.HideExplicitKey
import com.arturo254.opentune.constants.HideVideoKey
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.filterExplicit
import com.arturo254.opentune.innertube.models.filterVideo
import com.arturo254.opentune.innertube.pages.ExplorePage
import com.arturo254.opentune.utils.dataStore
import com.arturo254.opentune.utils.get
import com.arturo254.opentune.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(@ApplicationContext val context: Context, val database: MusicDatabase) : ViewModel() {
    val explorePage = MutableStateFlow<ExplorePage?>(null)
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
                val page = YouTube.explore().getOrThrow()
                val history = database.allArtistsByPlayTime().first()
                val artists = history.withIndex().associate { it.value.id to it.index }
                val favourites = history.filter { it.artist.bookmarkedAt != null }.withIndex().associate { it.value.id to it.index }
                explorePage.value = page.copy(newReleaseAlbums = page.newReleaseAlbums.sortedBy { album ->
                    album.artists.orEmpty().mapNotNull { it.id }.firstNotNullOfOrNull { favourites[it] ?: artists[it] } ?: Int.MAX_VALUE
                }.filterExplicit(context.dataStore.get(HideExplicitKey, false)).filterVideo(context.dataStore.get(HideVideoKey, false)))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                failed.value = true
                reportException(error)
            } finally {
                isLoading.value = false
            }
        }
    }
}
