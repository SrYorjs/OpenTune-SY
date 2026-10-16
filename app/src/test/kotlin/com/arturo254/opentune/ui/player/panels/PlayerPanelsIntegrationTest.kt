package com.arturo254.opentune.ui.player.panels

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Structural integration contracts; hardware back, menus and lyrics require device checks. */
class PlayerPanelsIntegrationTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Android Kotlin source root missing")
    private fun source(path: String) =
        File(root, "com/arturo254/opentune/$path").readText()

    @Test fun allPlayerAndQueueMenusUseOneNavigationGateway() {
        val player = source("ui/player/Player.kt")
        val controls = source("ui/player/PlayerComponents.kt")
        val queue = source("ui/player/Queue.kt")
        val panel = source("ui/player/panels/PlayerPanelMenus.kt")
        val menu = source("ui/menu/PlayerMenu.kt")

        assertEquals(2, Regex("menuState\\.showPlayerPanelMenu\\(").findAll(player).count())
        assertEquals(6, Regex("menuState\\.showPlayerPanelMenu\\(").findAll(controls).count())
        assertEquals(5, Regex("menuState\\.showPlayerPanelMenu\\(").findAll(queue).count())
        for (file in listOf(player, controls, queue)) {
            assertFalse(Regex("""menuState\.show\s*\{\s*PlayerMenu\(""").containsMatchIn(file))
            assertFalse(file.contains("PlayerMenu("))
        }
        assertTrue(panel.contains("PlayerMenu("))
        assertTrue(panel.contains("ShowMediaInfo("))
        assertTrue(panel.contains("isQueueTrigger = isQueueTrigger"))
        assertTrue(panel.contains("detailsMediaId?.takeIf { it.isNotBlank() }"))
        assertTrue(panel.contains("onDismiss = this@showPlayerPanelMenu::dismiss"))
        assertTrue(queue.contains("detailsMediaId = window.mediaItem.mediaId"))
        assertTrue(menu.contains("PlayerVolumeCard("))
    }

    @Test fun lyricsSheetHasSingleOwnerAndKeepsOldBackPriority() {
        val player = source("ui/player/Player.kt")
        val panels = source("ui/player/panels/PlayerLyricsPanel.kt")
        val host = source("ui/player/host/PlayerContentLifecycle.kt")
        val lyrics = source("ui/player/LyricsScreen.kt")
        val queue = source("ui/player/Queue.kt")

        assertTrue(player.contains("PlayerLyricsPanel("))
        assertTrue(player.contains("onShowLyrics = { lyricsSheetState.expandSoft() }"))
        assertTrue(player.contains("PlayerHostBackHandler("))
        assertFalse(player.contains("LyricsScreen("))
        assertTrue(panels.contains("LyricsScreen("))
        assertTrue(panels.contains("onBackClick = { sheetState.collapseSoft() }"))
        assertTrue(panels.contains("alpha = sheetState.progress.coerceIn(0f, 1f)"))
        assertTrue(host.contains("PlayerHostBackDestination.LYRICS -> lyricsSheet.collapseSoft()"))
        assertTrue(lyrics.contains("LyricsV2("))
        assertTrue(lyrics.contains("Lyrics("))
        assertTrue(lyrics.contains("lyricsHelper.getLyrics(mediaMetadata)"))
        assertTrue(queue.contains("onShowLyrics = onShowLyrics"))
    }

    @Test fun currentFormatBadgesShareMappingAndSourcesRemainIndependent() {
        val controls = source("ui/player/PlayerComponents.kt")
        val queueControls = source("ui/player/QueueComponents.kt")
        val format = source("ui/player/panels/PlayerQualityFormat.kt")
        val player = source("ui/player/Player.kt")
        assertTrue(controls.contains("playerFormatBadgeLabel(currentFormat.mimeType)"))
        assertTrue(controls.contains("playerFormatBadgeLabel(format.mimeType)"))
        assertTrue(queueControls.contains("playerCodecContainer(currentFormat.mimeType)"))
        assertTrue(player.contains("playerConnection.currentFormat.collectAsState("))
        assertTrue(format.contains("not a source-quality selector"))
        assertFalse(format.contains("SourceResolver("))
    }
}
