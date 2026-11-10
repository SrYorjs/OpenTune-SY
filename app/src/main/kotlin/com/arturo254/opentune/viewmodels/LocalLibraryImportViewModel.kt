package com.arturo254.opentune.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arturo254.opentune.sources.local.LocalAudioImportResult
import com.arturo254.opentune.sources.local.LocalAudioImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LocalLibraryImportViewModel @Inject constructor(
    private val importer: LocalAudioImporter,
) : ViewModel() {
    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running

    private val _result = MutableStateFlow<LocalAudioImportResult?>(null)
    val result: StateFlow<LocalAudioImportResult?> = _result

    fun import(uris: List<Uri>) {
        if (_running.value || uris.isEmpty()) return
        viewModelScope.launch {
            _running.value = true
            try {
                _result.value = importer.import(uris)
            } finally {
                _running.value = false
            }
        }
    }

    private val _removed = MutableStateFlow<Int?>(null)
    val removed: StateFlow<Int?> = _removed

    fun removeImported(ids: List<String>) {
        if (_running.value || ids.isEmpty()) return
        viewModelScope.launch {
            _running.value = true
            try {
                _removed.value = importer.removeImported(ids)
            } finally {
                _running.value = false
            }
        }
    }

    fun consumeRemoved() {
        _removed.value = null
    }

    fun consumeResult() {
        _result.value = null
    }
}
