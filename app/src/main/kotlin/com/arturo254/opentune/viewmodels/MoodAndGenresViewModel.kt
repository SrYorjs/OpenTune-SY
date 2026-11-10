/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.arturo254.opentune.viewmodels

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.YTItem
import com.arturo254.opentune.innertube.pages.MoodAndGenres
import com.arturo254.opentune.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.launch
import javax.inject.Inject

private object MoodCoverCache {
    val covers = mutableStateMapOf<String, List<String>>()
    val requested = java.util.Collections.synchronizedSet(mutableSetOf<String>())
}

fun coverKey(browseId: String, params: String?): String = browseId + "|" + params.orEmpty()

@HiltViewModel
class MoodAndGenresViewModel
@Inject
constructor() : ViewModel() {
    val moodAndGenres = MutableStateFlow<List<MoodAndGenres.Item>?>(null)
    val covers = MoodCoverCache.covers
    private val gate = Semaphore(3)

    val isLoading = MutableStateFlow(false)
    val failed = MutableStateFlow(false)

    init { refresh() }

    fun refresh() {
        if (isLoading.value) return
        isLoading.value = true
        failed.value = false
        viewModelScope.launch {
            YouTube
                .explore()
                .onSuccess {
                    moodAndGenres.value = it.moodAndGenres
                }.onFailure {
                    failed.value = true
                    reportException(it)
                }
            isLoading.value = false
        }
    }

    fun requestCovers(browseId: String, params: String?) {
        val key = coverKey(browseId, params)
        if (!MoodCoverCache.requested.add(key)) return
        viewModelScope.launch(Dispatchers.IO) {
            gate.withPermit {
                YouTube.browse(browseId, params)
                    .onSuccess { result ->
                        val urls = result.items
                            .asSequence()
                            .flatMap { it.items.asSequence() }
                            .mapNotNull(YTItem::thumbnail)
                            .distinct()
                            .take(2)
                            .toList()
                        if (urls.isNotEmpty()) MoodCoverCache.covers[key] = urls
                    }
                    .onFailure { MoodCoverCache.requested.remove(key) }
            }
        }
    }

}
