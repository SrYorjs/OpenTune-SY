package com.arturo254.opentune.ui.player.panels

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalaxPhaseSixRegressionTest {
    private val root = listOf(
        File("src/main/kotlin"),
        File("app/src/main/kotlin"),
    ).firstOrNull(File::isDirectory) ?: error("Missing source root")

    private fun source(relative: String): String =
        File(root, "com/arturo254/opentune/$relative").readText()

    @Test
    fun equalizerShowsActualLevelsAndRetainsAllPresetActions() {
        val menu = source("ui/menu/PlayerMenu.kt")
        val header = source("ui/player/panels/GalaxEqStudioHeader.kt")
        val curve = source("ui/player/panels/GalaxEqualizerCurve.kt")
        assertTrue(menu.contains("GalaxEqStudioHeader("))
        assertTrue(menu.contains("levels = bandLevelsMb"))
        assertTrue(menu.contains("playerConnection.service.applyEqFlatPreset()"))
        assertTrue(menu.contains("playerConnection.service.applySystemEqPreset("))
        assertTrue(menu.contains("playerConnection.service.applyEqProfile("))
        assertTrue(header.contains("levels.average()"))
        assertTrue(curve.contains("drawPath(fill, areaBrush)"))
    }

    @Test
    fun equalizerGainUsesMillibelsAndStableLocaleFormatting() {
        assertEquals("+3.0 dB", equalizerGainLabel(300))
        assertEquals("-12.5 dB", equalizerGainLabel(-1250))
        assertEquals("+0.0 dB", equalizerGainLabel(0))
    }

    @Test
    fun downloadFiltersUseRealMedia3StatesAndKeepQueueManager() {
        val panel = source("ui/screens/downloads/GalaxDownloadOverview.kt")
        val screen = source("ui/screens/DownloadQueueScreen.kt")
        assertTrue(panel.contains("Download.STATE_DOWNLOADING"))
        assertTrue(panel.contains("Download.STATE_QUEUED"))
        assertTrue(panel.contains("Download.STATE_RESTARTING"))
        assertTrue(panel.contains("Download.STATE_STOPPED"))
        assertTrue(panel.contains("Download.STATE_FAILED"))
        assertTrue(panel.contains("fun filterDownloadQueue("))
        assertTrue(panel.contains("it.download.percentDownloaded in 0f..100f"))
        assertTrue(screen.contains("GalaxDownloadOverview("))
        assertTrue(screen.contains("GalaxDownloadFilters("))
        assertTrue(screen.contains("filterDownloadQueue(downloads, filter)"))
        assertTrue(screen.contains("viewModel.pauseAll()"))
        assertTrue(screen.contains("viewModel.resumeAll()"))
        assertTrue(screen.contains("viewModel.removeDownload("))
    }

    @Test
    fun destructiveDownloadQueueClearRequiresExplicitApproval() {
        val screen = source("ui/screens/DownloadQueueScreen.kt")
        assertTrue(screen.contains("var confirmClear by rememberSaveable"))
        assertTrue(screen.contains("if (confirmClear && downloads.isNotEmpty())"))
        assertTrue(screen.contains("onClick = { confirmClear = true }"))
        assertTrue(screen.contains("viewModel.removeAll()"))
        assertTrue(screen.contains("confirmClear = false"))
        assertTrue(screen.contains("R.string.galax_downloads_clear_confirm"))
    }

    @Test
    fun universalQualityDialogRetainsMultisourceSelection() {
        val picker = source("ui/screens/search/UniversalVersionQualityDialog.kt")
        val header = source("ui/screens/search/GalaxQualitySelectionHeader.kt")
        assertTrue(picker.contains("GalaxQualitySelectionHeader("))
        assertTrue(picker.contains("recordings.flatMap { it.variants }"))
        assertTrue(picker.contains("onQualityModeChange(mode)"))
        assertTrue(picker.contains("onSelectVariant(recording, variant)"))
        assertTrue(picker.contains("onClearManual"))
        assertTrue(picker.contains("AudioSelectionMode.BEST_QUALITY"))
        assertTrue(picker.contains("AudioSelectionMode.DATA_SAVER"))
        assertTrue(header.contains("galax_quality_versions"))
        assertFalse(header.contains("LOSSLESS GUARANTEED"))
    }

    @Test
    fun queueAndLyricsRetainTheirOriginalGestureImplementations() {
        val queue = source("ui/player/Queue.kt")
        val lyrics = source("ui/player/LyricsScreen.kt")
        assertTrue(queue.contains("GalaxQueueJourneyRail("))
        assertTrue(queue.contains("GalaxPanelTimelineHeader("))
        assertTrue(queue.contains("ReorderableItem("))
        assertTrue(queue.contains("queueActions.removeAt("))
        assertTrue(lyrics.contains("GalaxPanelTimelineHeader("))
        assertTrue(lyrics.contains("LyricsV2("))
        assertTrue(lyrics.contains("GalaxLyricsControls("))
    }

    @Test
    fun liquidGlassEngineAndStudioThemesRemainUnchangedByPartSix() {
        val player = source("ui/player/Player.kt")
        val panels = source("ui/player/panels/GalaxPanelTimelineHeader.kt")
        assertTrue(player.contains("LocalGalaxLiquidBackdrop provides"))
        assertTrue(source("core/designsystem/liquid/GalaxLiquidModel.kt").contains("LocalGalaxLiquidVariant"))
        assertFalse(panels.contains("LocalGalaxLiquidVariant"))
    }
}
