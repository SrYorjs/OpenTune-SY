package com.arturo254.opentune.playback

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueSelectionSafetyTest {
    @Test
    fun pendingQueueLoadCannotReplaceNewSelection() {
        val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
            .firstOrNull(File::isDirectory) ?: error("Source root missing")
        val source = File(root, "com/arturo254/opentune/playback/QueueController.kt").readText()
        assertTrue(source.contains("activePlayQueueJob?.cancel()"))
        assertTrue(source.contains("if (currentQueue !== queue) return@launch"))
    }
}
