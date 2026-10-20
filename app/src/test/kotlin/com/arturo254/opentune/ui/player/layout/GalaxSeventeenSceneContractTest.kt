package com.arturo254.opentune.ui.player.layout

import com.arturo254.opentune.constants.PlayerDesignStyle
import com.arturo254.opentune.ui.player.v9.originalPlayerStyleConfig
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GalaxSeventeenSceneContractTest {
    private val root = listOf(
        File("src/main/kotlin"),
        File("app/src/main/kotlin"),
    ).firstOrNull(File::isDirectory) ?: error("Missing source root")

    private fun source(path: String): String =
        File(root, "com/arturo254/opentune/$path").readText()

    @Test
    fun everyDesignHasAnIndependentBackgroundSource() {
        assertEquals(17, PlayerDesignStyle.entries.size)
        for (design in PlayerDesignStyle.entries) {
            for (landscape in listOf(false, true)) {
                val route = resolvePlayerLayout(design, landscape)
                assertFalse(
                    "Shared album wallpaper is still enabled for $design",
                    route.hasSharedBackdrop,
                )
                assertEquals(design, route.style)
            }
        }
    }

    @Test
    fun appleMusicAndGalaxThemeCannotShareTheSameSceneRoute() {
        val apple = resolvePlayerLayout(PlayerDesignStyle.V8, false)
        val galax = resolvePlayerLayout(PlayerDesignStyle.V16, false)
        val vinyl = resolvePlayerLayout(PlayerDesignStyle.V17, false)
        assertEquals(PlayerLayoutFamily.APPLE_MUSIC, apple.family)
        assertEquals(PlayerLayoutFamily.GALAX_THEME, galax.family)
        assertEquals(PlayerLayoutFamily.VINJAX, vinyl.family)
        assertNotEquals(apple.family, galax.family)
        assertNotEquals(apple.family, vinyl.family)
        assertNotEquals(galax.family, vinyl.family)
    }

    @Test
    fun originalGalaxIdentityStaysBoundToOldAnimatedScene() {
        val player = source("ui/player/Player.kt")
        val routing = source("ui/player/layout/PlayerLayout.kt")
        assertTrue(player.contains("immersivePortrait = {"))
        assertTrue(player.contains("immersiveLandscape = {"))
        assertTrue(player.contains("V8PlayerScene("))
        assertTrue(routing.contains("PlayerLayoutFamily.GALAX_THEME -> immersivePortrait()"))
        assertTrue(routing.contains("PlayerLayoutFamily.GALAX_THEME -> immersiveLandscape()"))
        assertTrue(routing.contains("PlayerLayoutFamily.APPLE_MUSIC -> applePortrait()"))
        assertTrue(routing.contains("PlayerLayoutFamily.VINJAX -> vinylPortrait()"))
    }

    @Test
    fun rebuiltAppleMusicContainsItsOwnLayoutAndControls() {
        val apple = source("ui/player/v8/AppleMusicPlayerScene.kt")
        assertTrue(apple.contains("fun AppleMusicPlayerScene("))
        assertTrue(apple.contains("AppleMusicArtwork("))
        assertTrue(apple.contains("AppleMusicControls("))
        assertTrue(apple.contains("onVolumeChange"))
        assertTrue(apple.contains("actions::playPause"))
        assertTrue(apple.contains("actions::previous"))
        assertTrue(apple.contains("actions::next"))
        assertTrue(apple.contains("onLyrics"))
        assertTrue(apple.contains("onQueue"))
        assertFalse(apple.contains("V8PlayerBackdrop("))
        assertFalse(apple.contains("GalaxPlayerSceneOverlay("))
    }

    @Test
    fun vinJaxIsARealTurntableRatherThanAnArtworkBadge() {
        val vinyl = source("ui/player/v17/VinJaxPlayerScene.kt")
        assertTrue(vinyl.contains("fun VinJaxTurntable("))
        assertTrue(vinyl.contains("artwork: @Composable BoxScope.() -> Unit"))
        assertTrue(vinyl.contains(".clip(CircleShape)"))
        assertTrue(vinyl.contains("discRotation.animateTo("))
        assertTrue(vinyl.contains("isPlaying && !reducedMotion"))
        assertTrue(vinyl.contains("val fraction = progress.coerceIn(0f, 1f)"))
        assertTrue(vinyl.contains("val pivot = Offset("))
        assertTrue(vinyl.contains("val needle = Offset("))
        assertTrue(vinyl.contains("val arm = Path().apply"))
        assertTrue(vinyl.contains("drawPath(arm"))
    }

    @Test
    fun classicModernMinimalCinemaExpressiveHaveDistinctLooks() {
        val legacy = source("ui/player/layout/GalaxLegacyStyleScene.kt")
        for (value in listOf(
            "CLÁSICO", "MODERNO", "MÍNIMO", "CINEMA", "EXPRESIVO",
        )) assertTrue(legacy.contains(value))
        assertTrue(legacy.contains("LegacyArtworkFrame("))
        assertTrue(legacy.contains("LegacyControlPanel("))
        assertTrue(legacy.contains("GalaxLegacyBackdrop("))
        assertTrue(legacy.contains("PlayerDesignStyle.V1 ->"))
        assertTrue(legacy.contains("PlayerDesignStyle.V2 ->"))
        assertTrue(legacy.contains("PlayerDesignStyle.V3 ->"))
        assertTrue(legacy.contains("PlayerDesignStyle.V4 ->"))
        assertTrue(legacy.contains("PlayerDesignStyle.V6 ->"))
    }

    @Test
    fun playerStudioIsARealDestinationAndPersistsTheSelectedStyle() {
        val nav = source("ui/screens/NavigationBuilder.kt")
        val appearance = source("ui/screens/settings/AppearanceSettings.kt")
        val studio = source("ui/screens/settings/GalaxPlayerStyleStudioScreen.kt")
        assertTrue(nav.contains("composable(\"settings/appearance/player_studio\")"))
        assertTrue(appearance.contains("navController.navigate(\"settings/appearance/player_studio\")"))
        assertTrue(studio.contains("PlayerDesignStyleKey"))
        assertTrue(studio.contains("PlayerDesignStyle.entries"))
        assertTrue(studio.contains("onSelected(style)"))
        assertTrue(studio.contains("VinJax"))
        assertTrue(studio.contains("GalaxTheme"))
    }

    @Test
    fun collectionRedesignPreservesNavigationAndData() {
        val media = source("ui/screens/GalaxMediaHeader.kt")
        val library = source("ui/screens/library/LibraryMixScreen.kt")
        val hero = source("ui/screens/library/GalaxLibraryDestinationHero.kt")
        val albums = source("ui/screens/library/LibraryAlbumsScreen.kt")
        val artists = source("ui/screens/library/LibraryArtistsScreen.kt")
        val playlists = source("ui/screens/library/LibraryPlaylistsScreen.kt")
        assertTrue(media.contains("GalaxMediaCoverStage("))
        assertTrue(media.contains("GalaxCoverMosaic("))
        assertTrue(hero.contains("GalaxLibraryDestination.ALBUMS"))
        assertTrue(hero.contains("GalaxLibraryDestination.ARTISTS"))
        assertTrue(hero.contains("GalaxLibraryDestination.PLAYLISTS"))
        assertTrue(albums.contains("GalaxLibraryDestinationHero("))
        assertTrue(artists.contains("GalaxLibraryDestinationHero("))
        assertTrue(playlists.contains("GalaxLibraryDestinationHero("))
        assertTrue(library.contains("GalaxMediaHeader("))
    }

    @Test
    fun vinylControlsMaintainIndependentPersistedDesignName() {
        assertTrue(originalPlayerStyleConfig(PlayerDesignStyle.V17).name.contains("VINJAX"))
        assertEquals(PlayerDesignStyle.V17, PlayerDesignStyle.valueOf("V17"))
        assertEquals(PlayerDesignStyle.V16, PlayerDesignStyle.valueOf("V16"))
        val prefs = source("ui/player/preferences/PlayerPresentationSelectors.kt")
        assertTrue(prefs.contains("PlayerDesignStyle.V8"))
        assertTrue(prefs.contains("PlayerDesignStyle.V16"))
        assertTrue(prefs.contains("PlayerDesignStyle.V17"))
    }
}
