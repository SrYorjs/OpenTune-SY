package com.arturo254.opentune.ui.player

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalaxOSSecondarySurfacesTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Kotlin root missing")
    private fun source(name: String) = File(root, "com/arturo254/opentune/$name").readText()

    @Test
    fun homeShuffleAlwaysHasAsynchronousSavedAndDiscoveryFallbacks() {
        val vm = source("viewmodels/HomeViewModel.kt")
        val screen = source("ui/screens/HomeScreen.kt")
        val actions = source("ui/screens/GalaxHomeActions.kt")
        assertTrue(vm.contains("database.allSongs().first()"))
        assertTrue(vm.contains("galaxShufflePool("))
        assertTrue(vm.contains("ListQueue("))
        assertTrue(vm.contains("filterIsInstance<SongItem>()"))
        assertTrue(vm.contains("playSourceVariant("))
        assertTrue(screen.contains("viewModel.playGalaxShuffle(player)"))
        assertTrue(screen.contains("galax_shuffle_unavailable"))
        assertTrue(actions.contains("loading = isShuffling"))
        assertFalse(screen.contains("onShuffle = {\n                            (quickPicks.orEmpty() + speedDialSongs).randomOrNull()"))
    }

    @Test
    fun menuAndEqualizerKeepRealActionsUnderNewVisualDesign() {
        val menu = source("ui/component/BottomSheetMenu.kt")
        val audio = source("ui/player/panels/GalaxEqualizerCurve.kt")
        val playerMenu = source("ui/menu/PlayerMenu.kt")
        assertTrue(menu.contains("state.content(this)"))
        assertTrue(menu.contains("onDismissRequest ="))
        assertTrue(menu.contains("drawWithCache"))
        assertTrue(audio.contains("val points = levels.mapIndexed"))
        assertTrue(audio.contains("drawPath("))
        assertTrue(audio.contains("strokeBrush"))
        assertTrue(audio.contains("drawWithCache"))
        assertTrue(playerMenu.contains("GalaxEqStudioHeader("))
        assertTrue(playerMenu.contains("levels = bandLevelsMb"))
        assertTrue(playerMenu.contains("previewEqBandLevels("))
        assertTrue(playerMenu.contains("applySystemEqPreset("))
    }

    @Test
    fun queueRedesignKeepsControlActionsAndDragOwnership() {
        val queue = source("ui/player/QueueComponents.kt")
        assertTrue(queue.contains(".bottomSheetDraggable(sheetState)"))
        assertTrue(queue.contains("GALAXOS  /  PLAYBACK"))
        for (action in listOf(
            "onShuffleClick",
            "onRepeatClick",
            "onInfiniteQueueClick",
            "onLockClick",
            "onToggleLike",
            "onMenuClick",
        )) {
            assertTrue(queue.contains(action))
        }
        assertTrue(queue.contains("LiquidGlassIconButton("))
        assertTrue(queue.contains("queue_autoplaying_similar"))
    }
}
