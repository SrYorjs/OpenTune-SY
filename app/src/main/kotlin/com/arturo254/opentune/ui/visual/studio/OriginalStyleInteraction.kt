package com.arturo254.opentune.ui.visual.studio

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.input.pointer.PointerInputScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

val LocalStyleInteractive = compositionLocalOf<() -> Boolean> { { true } }

suspend fun PointerInputScope.whileStyleInteractive(
    interactive: () -> Boolean,
    detect: suspend PointerInputScope.() -> Unit,
): Unit = coroutineScope {
    var listening: kotlinx.coroutines.Job? = null
    snapshotFlow { interactive() }.collect { enabled ->
        listening?.cancelAndJoin()
        listening = if (enabled) launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            this@whileStyleInteractive.detect()
        } else null
    }
}
