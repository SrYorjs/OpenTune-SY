package com.arturo254.opentune.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.YTItem
import com.arturo254.opentune.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowseViewModel @Inject constructor(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val browseId = savedStateHandle.get<String>("browseId")
    val items = MutableStateFlow<List<YTItem>?>(null)
    val title = MutableStateFlow<String?>(null)
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
                if (browseId == null) items.value = emptyList() else {
                    val result = YouTube.browse(browseId, null).getOrThrow()
                    title.value = result.title
                    items.value = result.items.flatMap { it.items }.distinctBy { it.id }
                }
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
