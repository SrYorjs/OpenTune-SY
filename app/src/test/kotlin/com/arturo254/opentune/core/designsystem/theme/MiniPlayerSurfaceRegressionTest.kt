package com.arturo254.opentune.core.designsystem.theme

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniPlayerSurfaceRegressionTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Android source tree is missing")
    private fun source(path: String): String =
        File(root, "com/arturo254/opentune/$path").readText()

    @Test
    fun originalPillKeepsItsExactSourceShapeAndGlass() {
        val pill = source("ui/visual/player/OriginalMiniPlayer.kt")
        assertTrue(pill.contains("RoundedCornerShape(20.dp)"))
        assertTrue(pill.contains("height(64.dp)"))
        assertTrue(pill.contains(".dropShadow(PillShape, ChromeShadow)"))
        assertTrue(pill.contains("FrostedChromeGlass("))
        assertTrue(pill.contains("if (!pillFrame.merged)"))
    }

    @Test
    fun glassRootCapturesContentWithoutCapturingItsConsumers() {
        val host = source("MainActivityScaffold.kt")
        val mini = source("ui/player/MiniPlayer.kt")
        val glass = source("ui/visual/component/GlassKit.kt")
        assertTrue(host.contains("LocalOriginalGlassState provides originalGlassState"))
        assertTrue(host.contains(".glassRoot(originalGlassState)"))
        assertTrue(mini.contains("OriginalMiniPlayer("))
        assertTrue(glass.contains("RenderNode"))
        assertTrue(glass.contains("GlassPillBackground("))
    }
}
