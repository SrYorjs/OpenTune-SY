package com.arturo254.opentune.ui.screens

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalaxHomePlayerRedesignTest {
    private val root = listOf(
        File("src/main/kotlin"),
        File("app/src/main/kotlin"),
    ).firstOrNull(File::isDirectory) ?: error("Source root not found")

    private fun source(path: String): String =
        File(root, "com/arturo254/opentune/$path").readText()

    @Test
    fun orbitalCanvasHonorsReducedMotionAndPause() {
        val orbit = source("core/designsystem/component/GalaxOrbitField.kt")
        assertTrue(orbit.contains("galaxMotion.reducedMotion"))
        assertTrue(orbit.contains("active && !reducedMotion"))
        assertTrue(orbit.contains("rememberInfiniteTransition"))
        assertTrue(orbit.contains("drawArc("))
        assertFalse(orbit.contains(".blur("))
        assertFalse(orbit.contains("cloudy("))
    }

    @Test
    fun homeHasActualEditorialChaptersAndExpandableDiscovery() {
        val home = source("ui/screens/HomeScreen.kt")
        val chapters = source("ui/screens/GalaxHomeZones.kt")
        val control = source("ui/screens/GalaxHomeCatalogControl.kt")
        assertTrue(home.contains("GalaxHomeOrbitHeading("))
        assertTrue(home.contains("GalaxHomeChapterDivider("))
        assertTrue(home.contains("GalaxHomeZoneHeader("))
        assertTrue(home.contains("GalaxHomeCatalogControl("))
        assertTrue(home.contains("showAllRecommendations by rememberSaveable"))
        assertTrue(home.contains("homePage?.continuation != null"))
        assertTrue(home.contains("if (showAllRecommendations) Int.MAX_VALUE else 3"))
        assertTrue(chapters.contains("enum class GalaxHomeZone"))
        assertTrue(control.contains("onClick = onToggle"))
    }

    @Test
    fun listeningOrbitRetainsRealPlaybackAndContextMenu() {
        val home = source("ui/screens/HomeScreen.kt")
        val mosaic = source("ui/screens/GalaxHomeExperience.kt")
        assertTrue(mosaic.contains("GalaxOrbitField("))
        assertTrue(mosaic.contains("onPlay(song)"))
        assertTrue(mosaic.contains("onLongPress(song)"))
        assertTrue(home.contains("ListQueue("))
        assertTrue(home.contains("orbitSongs.map { it.toMediaItem() }"))
        assertTrue(home.contains("onDismiss = menuState::dismiss"))
    }

    @Test
    fun playerStylesUseNewScenesWithoutReplacingPlaybackEngine() {
        val player = source("ui/player/Player.kt")
        val classic = source("ui/player/layout/GalaxPlayerClassicScene.kt")
        val v7 = source("ui/player/v7/V7PlayerScene.kt")
        val v8 = source("ui/player/v8/V8PlayerScene.kt")
        val overlay = source("ui/player/visual/GalaxPlayerSceneOverlay.kt")
        assertTrue(player.contains("GalaxPlayerClassicScene("))
        assertTrue(player.contains("PlayerLayout("))
        assertTrue(player.contains("queueSheetState"))
        assertTrue(player.contains("rememberPlayerSeekSession"))
        assertTrue(classic.contains("controls: @Composable ColumnScope.() -> Unit"))
        assertTrue(classic.contains("artwork: @Composable BoxScope.() -> Unit"))
        assertTrue(v7.contains("GalaxPlayerSceneOverlay("))
        assertTrue(v8.contains("GalaxPlayerSceneOverlay("))
        assertTrue(overlay.contains("GalaxOrbitField("))
    }

    @Test
    fun existingBottomNavigationAndGlassChoicesStayAvailable() {
        val pages = source("ui/screens/GalaxMainPages.kt")
        val liquid = source("core/designsystem/liquid/GalaxLiquidModel.kt")
        val host = source("ui/player/host/PlayerHost.kt")
        assertTrue(pages.contains("HorizontalPager(state = pager"))
        assertTrue(liquid.contains("LocalGalaxLiquidVariant"))
        assertTrue(host.contains("BottomSheetPlayer("))
    }
}
