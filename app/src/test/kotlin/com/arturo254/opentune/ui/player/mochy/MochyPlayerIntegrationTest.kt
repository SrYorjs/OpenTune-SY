package com.arturo254.opentune.ui.player.mochy

import com.arturo254.opentune.constants.PlayerDesignStyle
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MochyPlayerIntegrationTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .first(File::isDirectory)

    private fun source(name: String) =
        File(root, "com/arturo254/opentune/$name").readText()

    @Test
    fun seventeenNativeStylesRemainAvailableAndTenMochyStylesAreIndependent() {
        assertEquals(17, PlayerDesignStyle.entries.size)
        assertEquals(10, MochyPlayerStyle.selectable.size)
        assertEquals(11, MochyPlayerStyle.entries.size)
        assertTrue(MochyPlayerStyle.NONE !in MochyPlayerStyle.selectable)
    }

    @Test
    fun realPlayerControlsRemainBoundToTheExistingMediaEngine() {
        val player = source("ui/player/Player.kt")
        val scene = source("ui/player/mochy/MochyPlayerScene.kt")
        assertTrue(player.contains("MochyPlayerScene("))
        assertTrue(player.contains("playerActions::playPause"))
        assertTrue(player.contains("playerActions::toggleFavorite"))
        assertTrue(player.contains("queueSheetState.expandSoft()"))
        assertTrue(player.contains("lyricsSheetState.expandSoft()"))
        assertTrue(scene.contains("onSeek("))
        assertTrue(scene.contains("onPlayPause"))
        assertTrue(scene.contains("AsyncImage("))
    }

    @Test
    fun originalMiniBarAndLiquidGlassRemainIndependentFromTheStudio() {
        val player = source("ui/player/Player.kt")
        val mini = source("ui/player/MiniPlayer.kt")
        val chrome = source("MainActivityChrome.kt")
        assertTrue(player.contains("MiniPlayer("))
        assertTrue(player.contains("dockMergeProgress"))
        assertTrue(mini.contains(".playerVisualGlassEffect("))
        assertTrue(chrome.contains("FloatingNavigationToolbar("))
        assertFalse(mini.contains("MochyPlayerStyle"))
        assertFalse(chrome.contains("MochyPlayerStyle"))
    }

    @Test
    fun selectorIncludesBothFamiliesWithoutRemappingOriginalPreferences() {
        val player = source("ui/player/Player.kt")
        val picker = source("ui/player/mochy/MochyPlayerStylePicker.kt")
        val prefs = source("constants/PreferenceKeys.kt")
        assertTrue(prefs.contains("val MochyPlayerStyleKey = stringPreferencesKey("))
        assertTrue(player.contains("PlayerDesignStyleKey"))
        assertTrue(picker.contains("GalaxPlayerStyleGallery("))
        assertTrue(picker.contains("MochyPlayerStyle.selectable.chunked(2)"))
        assertTrue(picker.contains("onMochy(MochyPlayerStyle.NONE)"))
    }
}
