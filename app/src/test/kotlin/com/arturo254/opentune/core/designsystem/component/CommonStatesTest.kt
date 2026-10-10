package com.arturo254.opentune.core.designsystem.component

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class CommonStatesTest {
    private fun source(name: String): String {
        val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
            .firstOrNull(File::isDirectory) ?: error("Source root missing")
        return File(root, "com/arturo254/opentune/core/designsystem/component/$name.kt").readText()
    }

    @Test
    fun allFourStatesArePureVisualComponents() {
        val s = source("UiStateComponents")
        listOf("fun OpenTuneLoadingState(", "fun OpenTuneEmptyState(",
            "fun OpenTuneErrorState(", "fun OpenTuneShimmerState(",
            "fun OpenTuneListShimmer(").forEach { assertTrue(s.contains(it)) }
        assertTrue(s.contains("onRetry: (() -> Unit)?"))
        assertTrue(s.contains("ShimmerHost("))
        listOf("MusicService", "ViewModel", "SourceResolver", "rememberPreference(")
            .forEach { assertTrue(!s.contains(it)) }
    }
}
