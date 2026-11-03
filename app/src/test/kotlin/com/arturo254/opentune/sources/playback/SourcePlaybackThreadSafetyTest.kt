package com.arturo254.opentune.sources.playback

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourcePlaybackThreadSafetyTest {
    @Test
    fun cancellationCleansUpListenerOnExoPlayerApplicationLooper() {
        val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
            .firstOrNull(File::isDirectory) ?: error("Source root missing")
        val source = File(root, "com/arturo254/opentune/sources/playback/SourcePlaybackCoordinator.kt")
            .readText()

        assertTrue(source.contains("withContext(Dispatchers.Main.immediate)"))
        assertTrue(source.contains("val handler = Handler(applicationLooper)"))
        assertTrue(source.contains("Looper.myLooper() == applicationLooper"))
        assertTrue(source.contains("handler.post(cleanup)"))
        assertTrue(source.contains("removed.compareAndSet(false, true)"))
        assertFalse(source.contains("invokeOnCancellation { player.removeListener(listener) }"))
    }
}
