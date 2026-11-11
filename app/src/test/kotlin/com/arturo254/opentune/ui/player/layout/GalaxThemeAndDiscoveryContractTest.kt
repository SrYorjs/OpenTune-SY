package com.arturo254.opentune.ui.player.layout

import java.io.File
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
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
    fun originalThemeIsActiveAndKeepsTheGlassDesignSystem() {
        val host = source("MainActivityThemeHost.kt")
        assertTrue(host.contains("import com.arturo254.opentune.foundation.theme.OriginalTheme"))
        assertTrue(host.contains("OriginalTheme(pureBlack = pureBlack)"))
        assertTrue(host.indexOf("OriginalTheme(pureBlack") < host.indexOf("ProvideOpenTuneDesignSystem("))
        assertTrue(host.contains("artworkRadiusDp = artworkCornerRadius"))
        assertTrue(host.contains("reduceMotion = reduceMotion"))
        assertTrue(host.contains("setSystemBarAppearance(ForceDarkTheme)"))
        assertFalse(host.contains("OpenTuneTheme("))
    }

    @Test
    fun originalBinaryResourcesMatchThePinnedSource() {
        val project = listOf(File(".."), File(".")).first { File(it, "visual-foundation").isDirectory }
        val manifest = kotlinx.serialization.json.Json.parseToJsonElement(
            File(project, "docs/inventario-02/trasplante.json").readText(),
        ).jsonObject
        org.junit.Assert.assertEquals("f8a3593fd0c37edca3da9012c0eb4bc37e907cc7", manifest.getValue("revision").jsonPrimitive.content)
        val binaries = manifest.getValue("files").jsonArray.filter {
            it.jsonObject.getValue("adaptation").jsonPrimitive.content == "byte_identical"
        }
        org.junit.Assert.assertEquals(74, binaries.size)
        binaries.forEach { entry ->
            val item = entry.jsonObject
            val path = item.getValue("destination").jsonPrimitive.content
            val digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(File(project, path).readBytes()).joinToString("") { "%02x".format(it.toInt() and 0xff) }
            org.junit.Assert.assertEquals(path, item.getValue("source_sha256").jsonPrimitive.content, digest)
        }
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
        val controls = source("ui/player/GalaxStudioPlayer/StudioTransport.kt")
        assertTrue(player.contains("StudioVisualScene("))
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
        val names = listOf(
            "NEBULA GARDEN", "EVENT HORIZON", "CIRCUIT BREAK",
            "MAGNETIC TAPE", "POLAR SILK", "WHITE NOISE", "ION REACTOR",
        )
        val catalog = source("ui/player/GalaxStudioPlayer/StudioStyleCatalog.kt")
        names.forEach { name -> assertTrue(catalog.contains(name)) }
        for (version in 9..15) {
            val scene = source("ui/player/GalaxStudioPlayer/StudioThemeV$version.kt")
            assertTrue(scene.contains("fun StudioThemeV$version("))
            assertTrue(scene.contains("Canvas("))
            assertTrue(scene.contains("controls()"))
            assertFalse(scene.contains(".cloudy("))
            assertFalse(scene.contains("GALAXOT  /"))
        }
        val dispatch = source("ui/player/GalaxStudioPlayer/StudioVisualScene.kt")
        assertTrue(dispatch.contains("StudioThemeV15("))
        assertTrue(dispatch.contains("StudioThemeV9("))
    }

    @Test
    fun searchNeverReintroducesProviderSwitching() {
        val search = source("ui/screens/search/UniversalSearchScreen.kt")
        val experience = source("ui/screens/search/UniversalVersionQualityDialog.kt")
        assertTrue(search.contains("OrbitResultsHero("))
        assertTrue(search.contains("OrbitResultsCategories("))
        assertFalse(search.contains("SignalQuality("))
        assertTrue(search.contains("state.result.tracks"))
        assertTrue(search.contains("originalRecordingOptions("))
        assertTrue(search.contains("viewModel.loadMore()"))
        val categories = source("ui/screens/search/PrismResultComponents.kt")
            .substringAfter("fun PrismResultCategories(").substringBefore("@Composable")
        assertFalse(categories.contains("ProviderId."))
        assertTrue(search.contains("itemsIndexed("))
        assertFalse(search.contains("viewModel.openAudiusArtist(artist)"))
        assertFalse(search.contains("SearchResultCategory.ARTISTS"))
        assertTrue(search.contains("viewModel.openAudiusPlaylist(playlist)"))
        assertTrue(experience.contains("AudioSelectionMode.AUTOMATIC"))
        assertTrue(experience.contains("AudioSelectionMode.BEST_QUALITY"))
        assertTrue(experience.contains("AudioSelectionMode.DATA_SAVER"))
    }

    @Test
    fun discoveryRetainsGenreNavigationAndSearchLaunch() {
        val main = source("ui/screens/PrismDiscoveryScreen.kt")
        val experience = source("ui/screens/search/PrismSearchDesign.kt")
        assertTrue(main.contains("PrismScreenFrame("))
        assertTrue(main.contains("PrismGenreTile("))
        assertTrue(main.contains("onClick = onOpenSearch"))
        assertTrue(main.contains("youtube_browse/"))
        assertTrue(main.contains("itemsIndexed("))
        assertTrue(main.contains("galaxDiscoveryGenreRowKey("))
        assertTrue(experience.contains("fun PrismGenreTile("))
    }

    @Test
    fun bottomNavigationIsLeftUntouchedAndGlassChoicesArePreserved() {
        val main = source("ui/screens/GalaxMainPages.kt")
        val liquid = source("core/designsystem/liquid/GalaxLiquidModel.kt")
        val player = source("ui/player/Player.kt")
        assertTrue(main.contains("HorizontalPager("))
        assertTrue(main.contains("state = pager"))
        assertTrue(liquid.contains("LocalGalaxLiquidVariant"))
        assertTrue(player.contains("LocalGalaxLiquidBackdrop"))
        assertTrue(player.contains("MiniPlayer("))
    }
}
