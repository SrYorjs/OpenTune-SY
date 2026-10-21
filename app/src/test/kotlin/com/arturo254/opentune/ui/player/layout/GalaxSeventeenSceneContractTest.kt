package com.arturo254.opentune.ui.player.layout

import com.arturo254.opentune.constants.PlayerDesignStyle
import com.arturo254.opentune.ui.player.GalaxStudioPlayer.originalPlayerStyleConfig
import com.arturo254.opentune.ui.player.preferences.playerStyleCategoryLabel
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
                assertEquals(
                    "Classic background ownership for $design",
                    design in setOf(PlayerDesignStyle.V1, PlayerDesignStyle.V2,
                        PlayerDesignStyle.V3, PlayerDesignStyle.V4, PlayerDesignStyle.V6),
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
        assertTrue(player.contains("StudioThemeV16("))
        assertTrue(routing.contains("PlayerLayoutFamily.GALAX_THEME -> immersivePortrait"))
        assertTrue(routing.contains("PlayerLayoutFamily.GALAX_THEME -> immersiveLandscape"))
        assertTrue(routing.contains("PlayerLayoutFamily.APPLE_MUSIC -> applePortrait"))
        assertTrue(routing.contains("PlayerLayoutFamily.VINJAX -> vinylPortrait"))
    }

    @Test
    fun rebuiltAppleMusicContainsItsOwnLayoutAndControls() {
        val apple = source("ui/player/GalaxStudioPlayer/StudioThemeV8Apple.kt")
        assertTrue(apple.contains("fun AppleMusicPlayerScene("))
        assertTrue(apple.contains("V8PlayerScene("))
        assertTrue(apple.contains("bareControls = true"))
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
        val vinyl = source("ui/player/GalaxStudioPlayer/StudioThemeV17VinJax.kt")
        assertTrue(vinyl.contains("fun VinJaxTurntable("))
        assertTrue(vinyl.contains("artwork: @Composable BoxScope.() -> Unit"))
        assertTrue(vinyl.contains(".clip(CircleShape)"))
        assertTrue(vinyl.contains("rotation.animateTo("))
        assertTrue(vinyl.contains("isPlaying && !reducedMotion"))
        assertTrue(vinyl.contains("progress.coerceIn(0f,1f)"))
        assertTrue(vinyl.contains("val pivot=Offset("))
        assertTrue(vinyl.contains("val needle=Offset("))
        assertTrue(vinyl.contains("val path=Path().apply"))
        assertTrue(vinyl.contains("drawPath(path"))
    }

    @Test
    fun classicModernMinimalCinemaExpressiveHaveDistinctLooks() {
        val player = source("ui/player/Player.kt")
        val routing = source("ui/player/layout/PlayerLayout.kt")
        assertTrue(player.contains("GalaxPlayerClassicScene("))
        assertTrue(player.contains("GalaxPlayerLandscapeScene("))
        for (version in 1..7) {
            val path = "ui/player/GalaxStudioPlayer/StudioThemeV$version.kt"
            val scene = source(path)
            assertTrue(scene.contains("fun StudioThemeV$version("))
            assertTrue(routing.contains("StudioThemeV$version(content)"))
        }
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
        assertTrue(studio.contains("VINJAX"))
        assertTrue(studio.contains("GALAXTHEME"))
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
    fun galleryIdentifiesGalaxThemeAndVinJaxWithoutGenericOverrides() {
        assertEquals("GALAXTHEME", playerStyleCategoryLabel(PlayerDesignStyle.V16))
        assertEquals("VINJAX", playerStyleCategoryLabel(PlayerDesignStyle.V17))
        (9..15).forEach { version ->
            assertEquals("ORIGINAL GALAXOT", playerStyleCategoryLabel(PlayerDesignStyle.valueOf("V$version")))
        }
        assertEquals("ESTILO CLÁSICO", playerStyleCategoryLabel(PlayerDesignStyle.V8))
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
