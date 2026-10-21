package com.arturo254.opentune.ui.player.layout

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalaxMainPlayerRestoreTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Kotlin sources not found")

    private fun source(path: String): String =
        File(root, "com/arturo254/opentune/$path").readText()

    @Test
    fun classicMainLayoutsDoNotAddGalaxDecoration() {
        val portrait = source("ui/player/layout/GalaxPlayerClassicScene.kt")
        val landscape = source("ui/player/layout/GalaxPlayerLandscapeScene.kt")
        assertTrue(portrait.contains("Spacer(Modifier.height(30.dp))"))
        assertFalse(portrait.contains("GalaxOrbitField("))
        assertFalse(portrait.contains("GalaxPlayerControlDock("))
        assertFalse(landscape.contains("GalaxOrbitField("))
        assertTrue(landscape.contains("Spacer(Modifier.weight(1f))"))
    }

    @Test
    fun allSevenMainStyleWrappersAreTransparent() {
        (1..7).forEach { version ->
            val scene = source("ui/player/GalaxStudioPlayer/StudioThemeV$version.kt")
            assertTrue(scene.contains("fun StudioThemeV$version("))
            assertTrue(scene.contains("    content()"))
            assertFalse(scene.contains("Canvas("))
            assertFalse(scene.contains("GalaxOrbitField("))
        }
    }

    @Test
    fun mainV8HasItsOwnBackdropWithoutChangingV16() {
        val player = source("ui/player/Player.kt")
        val scene = source("ui/player/v8/V8PlayerScene.kt")
        val backdrop = source("ui/player/v8/MainV8PlayerBackdrop.kt")
        val v16 = source("ui/player/GalaxStudioPlayer/StudioThemeV16.kt")
        assertTrue(player.contains("legacyMain = true"))
        assertTrue(scene.contains("if (legacyMain) {"))
        assertTrue(scene.contains("MainV8PlayerBackdrop("))
        assertTrue(backdrop.contains("fun MainV8PlayerBackdrop("))
        assertTrue(backdrop.contains(".cloudy(radius = cloudyRadius)"))
        assertTrue(v16.contains("V8PlayerScene("))
        assertFalse(v16.contains("legacyMain = true"))
    }

    @Test
    fun mainV5AndV7DontCarryModernGalaxLayers() {
        val player = source("ui/player/Player.kt")
        val v7 = source("ui/player/v7/V7PlayerScene.kt")
        assertFalse(player.contains("intensity = .27f"))
        assertFalse(v7.contains("GalaxPlayerControlDock("))
        assertTrue(v7.contains("controls()"))
    }
}
