package com.arturo254.opentune.ui.player.layout

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalaxThemeAndDiscoveryContractTest {
    private val root = listOf(
        File("src/main/kotlin"),
        File("app/src/main/kotlin"),
    ).firstOrNull(File::isDirectory) ?: error("Source root was not found")

    private fun source(relative: String): String =
        File(root, "com/arturo254/opentune/$relative").readText()

    @Test
    fun albumColorExtractionFollowsCurrentMediaAndCancelsStaleResults() {
        val host = source("MainActivityThemeHost.kt")
        assertTrue(host.contains("currentMediaMetadata.collectLatest"))
        assertTrue(host.contains("Dispatchers.IO"))
        assertTrue(host.contains(".allowHardware(false)"))
        assertTrue(host.contains(".extractThemeColor()"))
        assertTrue(host.contains("artworkThemeActive = true"))
        assertTrue(host.contains("artworkThemeActive = false"))
        assertTrue(host.contains("themeColor = customThemeColor"))
    }

    @Test
    fun applicationRestoresUsersColorWhenNoTrackIsAvailable() {
        val host = source("MainActivityThemeHost.kt")
        val prefs = source("constants/PreferenceKeys.kt")
        assertTrue(host.contains("ArtworkReactiveThemeKey"))
        assertTrue(prefs.contains("artworkReactiveTheme"))
        assertTrue(host.contains("if (!artworkReactiveTheme) return@LaunchedEffect"))
        assertTrue(host.contains("song == null"))
        assertTrue(host.contains("artwork == null"))
        assertTrue(host.contains("seedPalette = if (!artworkThemeActive) customThemeSeedPalette else null"))
        assertTrue(host.contains("customThemeColor"))
    }

    @Test
    fun destinationThemeDoesNotOverrideAlbumColorWithStaticGalaxyBlue() {
        val source = source("core/designsystem/theme/GalaxScreenStyle.kt")
        assertTrue(source.contains("val blue = scheme.primary"))
        assertTrue(source.contains("val violet = scheme.tertiary"))
        assertTrue(source.contains("val original = MaterialTheme.colorScheme"))
        assertFalse(source.contains("val blue = if (dark) GalaxScreenStyle.blue"))
        assertFalse(source.contains("val original = if (immersive) galaxColorScheme"))
    }

    @Test
    fun galleryContainsExactlyThePersistentPlayerStyleUniverse() {
        val gallery = source("ui/player/preferences/GalaxPlayerStyleGallery.kt")
        val labels = source("ui/player/preferences/PlayerPresentationSelectors.kt")
        val appearance = source("ui/screens/settings/AppearanceSettings.kt")
        assertTrue(gallery.contains("items(PlayerDesignStyle.entries"))
        assertTrue(gallery.contains("onSelected(style)"))
        assertTrue(gallery.contains("role = Role.RadioButton"))
        assertTrue(appearance.contains("settings/appearance/player_studio"))
        assertTrue(source("ui/screens/settings/GalaxPlayerStyleStudioScreen.kt").contains("GalaxPlayerStyleGallery("))
        assertTrue(appearance.contains("artworkReactiveTheme"))
        assertTrue(labels.contains("PlayerDesignStyle.V9"))
        assertTrue(labels.contains("PlayerDesignStyle.V15"))
    }

    @Test
    fun newLayoutsPreservePlaybackSeekLyricsAndQueue() {
        val player = source("ui/player/Player.kt")
        val controls = source("ui/player/v9/GalaxOriginalPlayerControls.kt")
        assertTrue(player.contains("GalaxOriginalPlayerScene("))
        assertTrue(player.contains("GalaxOriginalPlayerControls("))
        assertTrue(player.contains("onSeek = updatedOnSliderValueChange"))
        assertTrue(player.contains("onSeekFinished = updatedOnSliderValueChangeFinished"))
        assertTrue(player.contains("onQueue = { queueSheetState.expandSoft() }"))
        assertTrue(player.contains("onLyrics = { lyricsSheetState.expandSoft() }"))
        assertTrue(controls.contains("actions::playPause"))
        assertTrue(controls.contains("actions::next"))
        assertTrue(controls.contains("actions::previous"))
        assertTrue(controls.contains("actions::toggleShuffle"))
        assertTrue(controls.contains("actions::toggleRepeat"))
        assertTrue(controls.contains("actions::toggleFavorite"))
    }

    @Test
    fun sevenOriginalScenesHaveDifferentArtworkAndBackgroundTreatments() {
        val scenes = source("ui/player/v9/GalaxOriginalPlayerScenes.kt")
        val names = listOf(
            "NEBULA DRIFT",
            "STELLAR RINGS",
            "CYBERGRID",
            "VINYL ORBIT",
            "AURORA FLOW",
            "MONOLITH",
            "QUANTUM PULSE",
        )
        names.forEach { name -> assertTrue(scenes.contains(name)) }
        assertTrue(scenes.contains("RoundedCornerShape(0.dp)"))
        assertTrue(scenes.contains("CircleShape"))
        assertTrue(scenes.contains("drawArc("))
        assertTrue(scenes.contains("drawCircle("))
        assertTrue(scenes.contains("GalaxOrbitField("))
        assertTrue(scenes.contains("GalaxOriginalControlSurface("))
        assertFalse(scenes.contains(".cloudy("))
        assertFalse(scenes.contains(".blur("))
    }

    @Test
    fun searchNeverReintroducesProviderSwitching() {
        val search = source("ui/screens/search/UniversalSearchScreen.kt")
        val experience = source("ui/screens/search/GalaxSearchExperience.kt")
        assertTrue(search.contains("GalaxSearchEditorialHero("))
        assertTrue(search.contains("GalaxSearchQualityBar("))
        assertTrue(search.contains("state.result.tracks"))
        assertTrue(search.contains("viewModel.selectVariant"))
        assertTrue(search.contains("viewModel.loadMore()"))
        assertTrue(experience.contains("TODOS"))
        assertTrue(experience.contains("AudioSelectionMode.AUTOMATIC"))
        assertTrue(experience.contains("AudioSelectionMode.BEST_QUALITY"))
        assertTrue(experience.contains("AudioSelectionMode.DATA_SAVER"))
    }

    @Test
    fun discoveryRetainsGenreNavigationAndSearchLaunch() {
        val main = source("ui/screens/GalaxMainPages.kt")
        val experience = source("ui/screens/GalaxDiscoverExperience.kt")
        assertTrue(main.contains("GalaxDiscoverPortal("))
        assertTrue(main.contains("GalaxGenreSurface("))
        assertTrue(main.contains("onSearch = onOpenSearch"))
        assertTrue(main.contains("youtube_browse/"))
        assertTrue(main.contains("GalaxDiscoverEditorialHeading("))
        assertTrue(experience.contains("role = Role.Button"))
    }

    @Test
    fun bottomNavigationIsLeftUntouchedAndGlassChoicesArePreserved() {
        val main = source("ui/screens/GalaxMainPages.kt")
        val liquid = source("core/designsystem/liquid/GalaxLiquidModel.kt")
        val player = source("ui/player/Player.kt")
        assertTrue(main.contains("HorizontalPager(state = pager"))
        assertTrue(liquid.contains("LocalGalaxLiquidVariant"))
        assertTrue(player.contains("LocalGalaxLiquidBackdrop"))
        assertTrue(player.contains("MiniPlayer("))
    }
}
