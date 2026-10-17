package com.arturo254.opentune.ui.player.cleanup

import com.arturo254.opentune.constants.PlayerDesignStyle
import com.arturo254.opentune.ui.player.layout.PlayerLayoutFamily
import com.arturo254.opentune.ui.player.layout.resolvePlayerLayout
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerFinalCleanupRegressionTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Kotlin source root missing")
    private fun source(path: String) =
        File(root, "com/arturo254/opentune/$path").readText()

    @Test fun v8DoesNotReSubscribeToPlaybackOrDuplicateControlsBetweenOrientations() {
        val p = source("ui/player/Player.kt")
        val v8 = source("ui/player/v8/V8PlayerComponents.kt")
        assertEquals(1, Regex("""V8PlayerControlsContent\(""").findAll(p).count())
        assertEquals(2, Regex("""V8PlayerScene\(""").findAll(p).count())
        assertTrue(p.contains("playerActions = playerActions"))
        assertTrue(p.contains("liked = playerState.isLiked"))
        assertTrue(p.contains("shuffleModeEnabled = playerState.shuffleModeEnabled"))
        assertTrue(v8.contains("playerActions: PlayerActions"))
        assertFalse(v8.contains("rememberPlayerActions("))
        assertFalse(v8.contains("playerConnection.currentSong.collectAsState("))
        assertFalse(v8.contains("playerConnection.shuffleModeEnabled.collectAsState("))
        assertFalse(v8.contains("fun V8PlayerBackdrop(") && source("ui/player/PlayerComponents.kt").contains("fun V8PlayerBackdrop("))
        assertTrue(v8.contains("onNext = playerActions::next"))
    }

    @Test fun v1ToV8RoutesAndExistingDataStoreKeysRemainCompatible() {
        val cases = mapOf(
            PlayerDesignStyle.V1 to PlayerLayoutFamily.CLASSIC,
            PlayerDesignStyle.V2 to PlayerLayoutFamily.V2,
            PlayerDesignStyle.V3 to PlayerLayoutFamily.CLASSIC,
            PlayerDesignStyle.V4 to PlayerLayoutFamily.CLASSIC,
            PlayerDesignStyle.V5 to PlayerLayoutFamily.COMPACT,
            PlayerDesignStyle.V6 to PlayerLayoutFamily.CLASSIC,
            PlayerDesignStyle.V7 to PlayerLayoutFamily.CINEMATIC,
            PlayerDesignStyle.V8 to PlayerLayoutFamily.IMMERSIVE,
        )
        cases.forEach { (style, family) ->
            for (landscape in listOf(false, true)) {
                assertEquals(family, resolvePlayerLayout(style, landscape).family)
            }
        }
        val prefs = source("ui/player/preferences/PlayerPresentationPreferences.kt")
        for (key in listOf("PlayerDesignStyleKey", "PlayerBackgroundStyleKey",
            "PlayerButtonsStyleKey", "SliderStyleKey")) {
            assertTrue(prefs.contains("key = $key"))
        }
        val c = source("ui/player/PlayerComponents.kt")
        assertTrue(c.contains("PlayerDesignStyle.V1 -> {"))
        assertTrue(c.contains("PlayerDesignStyle.V3 -> {"))
    }

    @Test fun noExtraEngineOrTimerExistsInTheNewVisualModules() {
        for (path in listOf(
            "ui/player/v2/V2PlayerComponents.kt",
            "ui/player/v5/V5PlayerComponents.kt",
            "ui/player/v7/V7PlayerComponents.kt",
            "ui/player/v8/V8PlayerComponents.kt",
            "ui/player/visual/PlayerPaletteCache.kt",
        )) {
            val src = source(path)
            assertFalse(path, src.contains("ExoPlayer.Builder("))
            assertFalse(path, src.contains("MusicService("))
            assertFalse(path, src.contains("delay(100)"))
            assertFalse(path, src.contains("player.seekToNext()"))
        }
    }
}
