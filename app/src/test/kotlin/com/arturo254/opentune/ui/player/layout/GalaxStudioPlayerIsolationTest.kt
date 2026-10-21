package com.arturo254.opentune.ui.player.layout

import com.arturo254.opentune.constants.PlayerDesignStyle
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalaxStudioPlayerIsolationTest {
    private val sourceRoot = listOf(
        File("src/main/kotlin"),
        File("app/src/main/kotlin"),
    ).firstOrNull(File::isDirectory) ?: error("Source root unavailable")

    private fun source(path: String): String =
        File(sourceRoot,"com/arturo254/opentune/$path").readText()

    private fun theme(version: Int): String =
        source("ui/player/GalaxStudioPlayer/StudioThemeV$version.kt")

    @Test
    fun allSeventeenThemesUseSeparateKotlinSources() {
        assertEquals(17,PlayerDesignStyle.entries.size)
        for (version in 1..7) {
            assertTrue(theme(version).contains("fun StudioThemeV$version("))
        }
        for (version in 9..15) {
            assertTrue(theme(version).contains("fun StudioThemeV$version("))
        }
        assertTrue(source("ui/player/GalaxStudioPlayer/StudioThemeV8Apple.kt")
            .contains("fun AppleMusicPlayerScene("))
        assertTrue(source("ui/player/GalaxStudioPlayer/StudioThemeV16.kt")
            .contains("fun StudioThemeV16("))
        assertTrue(source("ui/player/GalaxStudioPlayer/StudioThemeV17VinJax.kt")
            .contains("fun VinJaxPlayerScene("))
    }

    @Test
    fun liquidGlassEngineAndSettingsStillBelongToOriginalModules() {
        val liquid = source("core/designsystem/liquid/GalaxLiquidModel.kt")
        val chrome = source("core/designsystem/liquid/GalaxLiquidChrome.kt")
        val player = source("ui/player/Player.kt")
        assertTrue(liquid.contains("LocalGalaxLiquidVariant"))
        assertTrue(chrome.contains("LocalGalaxLiquidTuning"))
        assertTrue(chrome.contains("GalaxLiquidVariant.DISABLED"))
        assertTrue(player.contains("LocalGalaxLiquidBackdrop provides"))
        assertTrue(player.contains("playerArtworkBackdrop"))
        assertTrue(player.contains("PlayerArtworkAtmosphere("))
        assertTrue(player.contains("if (activePlayerGlass)"))
    }

    @Test
    fun oldClassicLayoutsUseOriginalEngineInsteadOfTemporaryOverlay() {
        val player = source("ui/player/Player.kt")
        assertTrue(player.contains("GalaxPlayerClassicScene("))
        assertTrue(player.contains("GalaxPlayerLandscapeScene("))
        assertFalse(player.contains("GalaxLegacyStyleScene("))
        val scenes = source("ui/player/layout/GalaxPlayerClassicScene.kt")
        assertFalse(scenes.contains("        GalaxPlayerSceneMasthead("))
    }

    @Test
    fun appleMusicSharesGalaxThemeArtworkBackdropButNotItsDock() {
        val apple = source("ui/player/GalaxStudioPlayer/StudioThemeV8Apple.kt")
        val galax = source("ui/player/GalaxStudioPlayer/StudioThemeV16.kt")
        val origin = source("ui/player/v8/V8PlayerScene.kt")
        assertTrue(apple.contains("V8PlayerScene("))
        assertTrue(galax.contains("V8PlayerScene("))
        assertTrue(apple.contains("bareControls = true"))
        assertTrue(apple.contains("AppleMusicControls("))
        assertTrue(origin.contains("V8PlayerBackdrop("))
        assertTrue(origin.contains("if (isLandscape || bareControls)"))
        assertFalse(origin.contains("        GalaxPlayerSceneOverlay("))
    }

    @Test
    fun vinJaxAlbumArtRotatesInsideTheRecordAndItsNeedleMoves() {
        val vinyl = source("ui/player/GalaxStudioPlayer/StudioThemeV17VinJax.kt")
        assertTrue(vinyl.contains("while (isPlaying && !reducedMotion)"))
        assertTrue(vinyl.contains("rotation.animateTo("))
        assertTrue(vinyl.contains("rotationZ=rotation"))
        assertTrue(vinyl.contains(".clip(CircleShape)"))
        assertTrue(vinyl.contains("content=artwork"))
        assertTrue(vinyl.contains("needlePosition"))
        assertTrue(vinyl.contains("if(isPlaying) 0f else 1f"))
        assertTrue(vinyl.contains("val pivot=Offset("))
        assertTrue(vinyl.contains("drawPath(path"))
        assertFalse(vinyl.contains("33⅓ RPM"))
    }

    @Test
    fun sevenOriginalExperimentalStylesHaveDifferentCompositions() {
        val dispatch = source("ui/player/GalaxStudioPlayer/StudioVisualScene.kt")
        val catalog = source("ui/player/GalaxStudioPlayer/StudioStyleCatalog.kt")
        val titles=listOf(
            "NEBULA GARDEN","EVENT HORIZON","CIRCUIT BREAK",
            "MAGNETIC TAPE","POLAR SILK","WHITE NOISE","ION REACTOR",
        )
        titles.forEach { assertTrue(catalog.contains(it)) }
        for (version in 9..15) {
            val scene=theme(version)
            assertTrue(scene.contains("Canvas(Modifier.fillMaxSize())"))
            assertTrue(scene.contains("content=artwork"))
            assertTrue(scene.contains("controls()"))
            assertTrue(dispatch.contains("PlayerDesignStyle.V$version -> StudioThemeV$version"))
            assertFalse(scene.contains("GALAXOT  /"))
        }
        assertTrue(theme(11).contains("val spacing=24.dp.toPx()"))
        assertTrue(theme(13).contains("cubicTo("))
        assertTrue(theme(15).contains("state.progress.coerceIn"))
    }

    @Test
    fun newCollectionNavigationPreservesOldFilteringAndMusicRoutes() {
        val library = source("ui/screens/library/LibraryScreen.kt")
        val portal = source("ui/screens/library/GalaxLibraryOrbitNavigator.kt")
        val media = source("ui/screens/GalaxMediaHeader.kt")
        assertTrue(library.contains("CollectionTabs("))
        assertTrue(library.contains("filterContent()"))
        assertTrue(library.contains("DropdownMenu("))
        assertTrue(library.contains("PlaylistMenu("))
        assertTrue(portal.contains("LibraryFilter.PLAYLISTS"))
        assertTrue(portal.contains("LibraryFilter.ALBUMS"))
        assertTrue(portal.contains("LibraryFilter.ARTISTS"))
        assertTrue(portal.contains("LibraryFilter.SONGS"))
        assertTrue(media.contains("GalaxMediaCoverStage("))
        assertTrue(media.contains("actions()"))
        assertTrue(media.contains("supporting()"))
    }

    @Test
    fun studioSelectionPersistsAndLegacyDesignValuesRemainReadable() {
        val navigation=source("ui/screens/NavigationBuilder.kt")
        val appearance=source("ui/screens/settings/AppearanceSettings.kt")
        val studio=source("ui/screens/settings/GalaxPlayerStyleStudioScreen.kt")
        val preferences=source("constants/PreferenceKeys.kt")
        assertTrue(navigation.contains("settings/appearance/player_studio"))
        assertTrue(appearance.contains("settings/appearance/player_studio"))
        assertTrue(studio.contains("PlayerDesignStyleKey"))
        assertTrue(studio.contains("PlayerDesignStyle.entries"))
        assertTrue(preferences.contains("playerDesignStyle"))
        assertTrue(preferences.contains("V16"))
        assertTrue(preferences.contains("V17"))
        assertTrue(source("ui/player/GalaxStudioPlayer/StudioTransport.kt")
            .contains("actions::playPause"))
        assertTrue(source("ui/player/GalaxStudioPlayer/StudioTransport.kt")
            .contains("actions::toggleFavorite"))
        assertTrue(source("ui/player/Player.kt").contains("rememberPlayerPresentationPreferences()"))
    }
}
