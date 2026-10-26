package com.arturo254.opentune.core.designsystem.component

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaComponentsArchitectureTest {
    private val sourceRoot =
        listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
            .firstOrNull(File::isDirectory)
            ?: error("Android Kotlin source tree not found")
    private val componentDirectory =
        File(sourceRoot, "com/arturo254/opentune/core/designsystem/component")

    @Test
    fun allBaseComponentsUseSemanticDesignSystemWithoutFeatureDependencies() {
        val names = listOf("Artwork", "SongRow", "AlbumCard", "ArtistCard")
        names.forEach { name ->
            val contents = File(componentDirectory, "$name.kt").readText()
            assertTrue("$name must consume design tokens", contents.contains("OpenTuneDesignSystem"))
            assertTrue("$name must be composable", contents.contains("@Composable"))
            listOf("ViewModel", "MusicService", "SourceResolver", "RoomDatabase", "PlayerConnection").forEach { prohibited ->
                assertTrue("$name must not own $prohibited", !contents.contains(prohibited))
            }
        }
    }

    @Test
    fun searchArtworkMigratesWhileKeepingPlaybackAndQualityControls() {
        val contents =
            File(sourceRoot, "com/arturo254/opentune/ui/screens/search/UniversalSearchScreen.kt")
                .readText()
        assertTrue(contents.contains("TransmissionResult("))
        assertTrue(contents.contains("onVersions"))
        assertTrue(contents.contains("signalQuality"))
        assertTrue(contents.contains("resolving"))
    }
}
