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
    fun appShellUsesOriginalPhoneAndWideNavigationWithSharedGlass() {
        val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
            .firstOrNull(File::isDirectory) ?: error("Kotlin sources missing")
        fun source(name: String) = File(root, "com/arturo254/opentune/$name").readText()
        val shell = source("MainActivityScaffold.kt")
        val navigation = source("MainActivityChrome.kt")
        val mini = source("ui/player/MiniPlayer.kt")
        val wide = source("ui/visual/component/WideBar.kt")
        assertTrue(shell.contains("LocalOriginalGlassState provides originalGlassState"))
        assertTrue(shell.contains("LocalMiniPillFrame provides"))
        assertTrue(shell.contains(".glassRoot(originalGlassState)"))
        assertTrue(navigation.contains("AppNavigationBar("))
        assertTrue(navigation.contains("WideBarBackground("))
        assertTrue(navigation.contains("WideBarTabs("))
        assertTrue(wide.contains("fun WideBarTabs("))
        assertTrue(mini.contains("OriginalMiniPlayer("))
        assertTrue(shell.contains("LocalPlayerConnection provides playerConnection"))
    }
}
