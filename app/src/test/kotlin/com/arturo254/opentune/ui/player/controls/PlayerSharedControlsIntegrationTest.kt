package com.arturo254.opentune.ui.player.controls

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Architectural guard only; device tests still verify touch targets, haptics,
 * Glass surfaces, disabled state and V1–V8 sizing.
 */
class PlayerSharedControlsIntegrationTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Android Kotlin sources missing")
    private fun source(p: String) =
        File(root, "com/arturo254/opentune/$p").readText()

    @Test fun allPlayerVersionsUseSharedPlayAndSkipGlyphs() {
        val controls = source("ui/player/PlayerComponents.kt")
        val v2 = source("ui/player/v2/V2PlayerComponents.kt")
        val v7 = source("ui/player/v7/V7PlayerComponents.kt")
        val allVersions = controls + v2 + v7
        assertEquals(6, Regex("""PlayerSharedPlaybackContent\(""").findAll(allVersions).count())
        assertEquals(14, Regex("""PlayerSharedSkipIcon\(""").findAll(allVersions).count())
        assertTrue(controls.contains("playerPlaybackIconRes(playbackState, playing)"))
        assertTrue(controls.contains("playerPlaybackIconRes(playbackState, isPlaying)"))
        assertTrue(controls.contains("playerFavoriteIconRes(currentSongLiked"))
        assertTrue(controls.contains("playerRepeatIconRes(repeatMode)"))
        assertTrue(controls.contains("LiquidGlassIconButton("))
        assertTrue(controls.contains("V2PlayerTransport("))
        assertTrue(v2.contains("FilledTonalIconButton("))
        assertTrue(controls.contains("ResizableIconButton("))
    }

    @Test fun allSharedSecondaryActionsRouteThroughPlayerActions() {
        val controls = source("ui/player/PlayerComponents.kt")
        val actions = source("ui/player/actions/PlayerActions.kt")
        assertFalse(controls.contains("playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled"))
        assertFalse(controls.contains("playerConnection.player.toggleRepeatMode()"))
        assertEquals(4, Regex("""playerActions.toggleShuffle\(\)""").findAll(controls).count())
        assertEquals(5, Regex("""playerActions.toggleRepeat\(\)""").findAll(controls).count())
        assertTrue(actions.contains("fun toggleShuffle() = backend.toggleShuffle()"))
        assertTrue(actions.contains("fun toggleRepeat() = backend.toggleRepeat()"))
        assertTrue(actions.contains("player.shuffleModeEnabled = !player.shuffleModeEnabled"))
        assertTrue(actions.contains("player.toggleRepeatMode()"))
        assertTrue(controls.contains("sharePlayerTrack(context, mediaMetadata.id)"))
    }

    @Test fun miniPlayerUsesTheSameGlyphRulesAndOwnSurface() {
        val mini = source("ui/player/MiniPlayerComponents.kt")
        val surface = source("ui/player/MiniPlayer.kt")
        assertTrue(mini.contains("playerPlaybackIconRes("))
        assertTrue(mini.contains("playerFavoriteIconRes("))
        assertTrue(mini.contains("playerSkipIconRes("))
        assertTrue(mini.contains("playerActions.playPause()"))
        assertTrue(surface.contains("playerVisualGlassEffect("))
        assertTrue(surface.contains(".clip(miniPlayerShape)"))
    }

    @Test fun sharedControlKitDoesNotOwnThePlayerOrItsStyles() {
        val kit = source("ui/player/controls/PlayerSharedControls.kt")
        assertFalse(kit.contains("MusicService("))
        assertFalse(kit.contains("ExoPlayer.Builder("))
        assertFalse(kit.contains("PlayerConnection("))
        assertFalse(kit.contains("PlayerLayout("))
        assertFalse(kit.contains("rememberPreference("))
        assertTrue(kit.contains("CircularWavyProgressIndicator("))
    }
}
