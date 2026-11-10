package com.arturo254.opentune.ui.visual.studio

import com.arturo254.opentune.constants.PlayerDesignStyle
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OriginalStudioSelectorTest {
    private val root by lazy {
        listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
            .firstOrNull(File::isDirectory) ?: error("Falta la carpeta Kotlin principal")
    }

    private fun source(relative: String): String =
        File(root, "com/arturo254/opentune/$relative").readText()

    @Test
    fun catalogueContainsEveryOriginalStyleAndEveryExistingVariant() {
        val names = StudioPlayerStyle.selectorStyles.map { it.name }
        assertEquals(27, names.size)
        assertEquals(27, names.distinct().size)
        assertTrue(names.containsAll(
            listOf(
                "FLAGSHIP", "CLASSIC", "TURNTABLE", "VIDEO",
                "IPOD", "BEDSIDE_TILES", "BEDSIDE", "POSTER", "TIMELINE", "LYRICS",
            )
        ))
        assertEquals(
            PlayerDesignStyle.entries.toSet(),
            StudioPlayerStyle.selectorStyles.mapNotNull { it.legacy }.toSet(),
        )
        assertEquals(24, StudioPlayerStyle.selectorColumns.size)
        assertEquals(
            listOf(StudioPlayerStyle.FLAGSHIP, StudioPlayerStyle.CLASSIC),
            StudioPlayerStyle.selectorColumns.first(),
        )
    }

    @Test
    fun onlyStylesWithRealRenderersCanBeSelected() {
        assertTrue(StudioPlayerStyle.FLAGSHIP.selectable)
        assertTrue(StudioPlayerStyle.CLASSIC.selectable)
        assertTrue(StudioPlayerStyle.TURNTABLE.selectable)
        assertTrue(StudioPlayerStyle.TIMELINE.selectable)
        assertTrue(StudioPlayerStyle.IPOD.selectable)
        assertFalse(StudioPlayerStyle.VIDEO.selectable)
        assertFalse(StudioPlayerStyle.LYRICS.selectable)
        assertFalse(StudioPlayerStyle.POSTER.selectable)
        assertFalse(StudioPlayerStyle.BEDSIDE.selectable)
        assertFalse(StudioPlayerStyle.BEDSIDE_TILES.selectable)
        PlayerDesignStyle.entries.forEach { style ->
            val item = StudioPlayerStyle.entries.first { it.legacy == style }
            assertTrue(item.selectable)
            assertEquals(style.name, item.selectorName)
        }
    }

    @Test
    fun storedSelectionUsesStableNamesAndSafeFallback() {
        assertEquals(StudioPlayerStyle.TIMELINE, StudioPlayerStyle.fromStored("TIMELINE"))
        assertEquals(StudioPlayerStyle.V1, StudioPlayerStyle.fromStored("V1"))
        assertEquals(StudioPlayerStyle.V17, StudioPlayerStyle.fromStored("V17"))
        assertEquals(StudioPlayerStyle.FLAGSHIP, StudioPlayerStyle.fromStored(null))
        assertEquals(StudioPlayerStyle.FLAGSHIP, StudioPlayerStyle.fromStored("UNSUPPORTED"))
    }

    @Test
    fun originalSelectorControlsRealPlayerLayouts() {
        val player = source("ui/player/Player.kt")
        val selector = source("ui/visual/studio/PlayerSelector.kt")
        val timeline = source("ui/visual/studio/OriginalTimelinePlayer.kt")
        val actions = source("ui/visual/studio/OriginalStyleGestures.kt")
        val ipod = source("ui/visual/ipod/OriginalIpodPlayer.kt")
        val wheel = source("ui/visual/ipod/IpodClickWheel.kt")
        val library = source("ui/visual/ipod/IpodLibraryViewModel.kt")
        val menu = source("ui/menu/PlayerMenu.kt")
        assertTrue(player.contains("PlayerSelectorOverlay("))
        assertTrue(player.contains("studioSheetLayer(studio)"))
        assertTrue(player.contains("renderNativePlayer(style, Modifier.fillMaxSize(), true)"))
        assertTrue(player.contains("renderLegacyPlayer(style.legacy)"))
        assertTrue(player.contains("studio.settleOn(index, restyled, velocity)"))
        assertTrue(menu.contains("onOpenStyles"))
        assertTrue(selector.contains("studio.page.animateTo("))
        assertTrue(selector.contains("studio.rows(tappedColumn).animateTo("))
        assertTrue(timeline.contains("OriginalTimelineRidingTimes("))
        assertTrue(timeline.contains("OriginalStyleTransportCircles("))
        assertTrue(actions.contains("carousel.release("))
        assertTrue(actions.contains("actions::next"))
        assertTrue(actions.contains("actions::previous"))
        assertTrue(player.contains("OriginalIpodPlayer("))
        assertTrue(ipod.contains("IpodClickWheel("))
        assertTrue(ipod.contains("IpodCoverFlow("))
        assertTrue(ipod.contains("IpodNowPlaying("))
        assertTrue(wheel.contains("fun IpodClickWheel("))
        assertTrue(library.contains("database.playlists("))
        assertTrue(library.contains("database.artistSongs("))
        assertTrue(library.contains("database.albumWithSongs("))
        assertFalse(library.contains("YouTube.album("))
        assertFalse(timeline.contains("ExoPlayer.Builder("))
    }
}
