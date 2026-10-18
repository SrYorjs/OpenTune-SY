package com.arturo254.opentune.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arturo254.opentune.constants.HideExplicitKey
import com.arturo254.opentune.constants.HideVideoKey
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.pages.BrowseResult
import com.arturo254.opentune.utils.dataStore
import com.arturo254.opentune.utils.get
import com.arturo254.opentune.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class YouTubeBrowseViewModel @Inject constructor(@ApplicationContext val context: Context, savedStateHandle: SavedStateHandle) : ViewModel() {
    private val browseId = savedStateHandle.get<String>("browseId").orEmpty()
    private val params = savedStateHandle.get<String>("params")
    val result = MutableStateFlow<BrowseResult?>(null)
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
                require(browseId.isNotBlank())
                result.value = YouTube.browse(browseId, params).getOrThrow()
                    .filterExplicit(context.dataStore.get(HideExplicitKey, false))
                    .filterVideo(context.dataStore.get(HideVideoKey, false))
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
