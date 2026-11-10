package com.arturo254.opentune.ui.visual.library

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arturo254.opentune.R
import com.arturo254.opentune.viewmodels.LocalLibraryImportViewModel

data class LibraryImportUi(
    val running: Boolean,
    val openPicker: () -> Unit,
)

@Composable
fun rememberLibraryImportUi(
    viewModel: LocalLibraryImportViewModel = hiltViewModel(),
): LibraryImportUi {
    val context = LocalContext.current
    val running by viewModel.running.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        viewModel.import(uris)
    }
    LaunchedEffect(result) {
        result?.let { finished ->
            Toast.makeText(
                context,
                context.getString(
                    R.string.visual_library_import_result,
                    finished.imported,
                    finished.existing,
                    finished.failed,
                ),
                Toast.LENGTH_LONG,
            ).show()
            viewModel.consumeResult()
        }
    }
    return LibraryImportUi(running) {
        if (!running) launcher.launch(arrayOf("audio/*"))
    }
}
