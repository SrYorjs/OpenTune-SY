package com.arturo254.opentune.ui.component

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalaxAdaptiveDockMotionTest {
    @Test
    fun scrollDownMergesAfterThresholdAndScrollUpUnfoldsWithHysteresis() {
        val motion = GalaxDockScrollAccumulator()
        assertFalse(motion.advance(-18f, 64f))
        assertFalse(motion.advance(-21f, 64f))
        assertTrue(motion.advance(-30f, 64f))
        assertTrue(motion.advance(12f, 64f))
        assertFalse(motion.advance(65f, 64f))
    }

    @Test
    fun smallTremorsDoNotToggleDockAndResetRemovesPreviousPageState() {
        val motion = GalaxDockScrollAccumulator()
        repeat(10) {
            assertFalse(motion.advance(-0.2f, 64f))
        }
        assertTrue(motion.advance(-75f, 64f))
        motion.reset()
        assertFalse(motion.advance(-5f, 64f))
        assertFalse(motion.advance(Float.NaN, 64f))
        assertFalse(motion.advance(-800f, 0f))
    }

    @Test
    fun appShellRetainsSingleGlassEngineAndBothNavigationEntryPoints() {
        val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
            .firstOrNull(File::isDirectory) ?: error("Kotlin sources missing")
        fun source(name: String) = File(root, "com/arturo254/opentune/$name").readText()
        val shell = source("MainActivityScaffold.kt")
        val player = source("ui/player/Player.kt")
        val navigation = source("MainActivityChrome.kt")
        val toolbar = source("ui/component/FloatingNavigationToolbar.kt")
        val mini = source("ui/player/MiniPlayer.kt")
        assertTrue(shell.contains(".nestedScroll(dockMotion.nestedScrollConnection)"))
        assertTrue(shell.contains("collapsedDockNavigation = {"))
        assertTrue(shell.contains("GalaxCompactNavigationOrb("))
        assertTrue(player.contains("dockMergeProgress = merge"))
        assertTrue(player.contains("GalaxCompactMiniPlayer("))
        assertTrue(mini.contains("1f - dockMergeProgress"))
        assertTrue(navigation.contains("selectedRoute = selectedNavigationRoute"))
        assertTrue(toolbar.contains("if (unifiedDock) Modifier"))
        assertTrue(shell.contains("LocalGalaxLiquidVariant provides liquidVariant"))
        assertTrue(shell.contains("LocalGalaxLiquidTuning provides liquidTuning"))
    }
}
