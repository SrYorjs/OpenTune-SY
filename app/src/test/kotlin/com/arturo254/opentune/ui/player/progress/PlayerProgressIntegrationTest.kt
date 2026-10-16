package com.arturo254.opentune.ui.player.progress

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks wiring, not device-level touch, screenshot or Media3 tests. */
class PlayerProgressIntegrationTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Android sources missing")
    private fun source(path: String) =
        File(root, "com/arturo254/opentune/$path").readText()

    @Test fun fullMiniAndV8SlidersShareOneProgressCalculator() {
        val player = source("ui/player/Player.kt")
        val components = source("ui/player/PlayerComponents.kt")
        val mini = source("ui/player/MiniPlayerComponents.kt")
        val state = source("ui/player/state/PlayerState.kt")
        assertTrue(player.contains("rememberPlayerSeekSession("))
        assertTrue(player.contains("seekSession.settle("))
        assertTrue(player.contains("seekSession.finish {"))
        assertTrue(player.contains("playerActions.seekTo(targetPosition, displayedTrackId = mediaMetadata?.id)"))
        assertTrue(components.contains("value = progress.sliderValue"))
        assertTrue(components.contains("valueRange = progress.sliderRange"))
        assertTrue(components.contains("val remaining = progress.remainingMs"))
        assertTrue(mini.contains("progressFraction = playerState.progressFraction"))
        assertTrue(mini.contains("progress = { progressFraction }"))
        assertTrue(state.contains("calculatePlayerProgress(positionMs, durationMs, pendingSeekMs)"))
    }

    @Test fun oneExistingTickerAndCrossfadeOwnerArePreserved() {
        val player = source("ui/player/Player.kt")
        val progress = source("ui/player/progress/PlayerProgress.kt")
        val actions = source("ui/player/actions/PlayerActions.kt")
        assertTrue(player.contains("delay(100)"))
        assertTrue(progress.contains("PLAYER_SEEK_SETTLE_TOLERANCE_MS = 1_500L"))
        assertFalse(progress.contains("LaunchedEffect("))
        assertFalse(progress.contains("PlayerConnection("))
        assertFalse(progress.contains("import androidx.media3.common.Player"))
        assertTrue(actions.contains("backend.advanceCrossfadeTarget()"))
        assertTrue(actions.contains("backend.seekTo(positionMs)"))
    }

    @Test fun existingVisualSliderStylesAndGlassTrackStillRender() {
        val c = source("ui/player/PlayerComponents.kt")
        val p = source("ui/player/Player.kt")
        assertTrue(c.contains("fun StyledPlaybackSlider("))
        assertTrue(c.contains("SliderStyle.Wavy"))
        assertTrue(c.contains("SliderStyle.Circular"))
        assertTrue(c.contains("SliderStyle.Thick"))
        assertTrue(c.contains("SliderStyle.Simple"))
        assertTrue(source("ui/player/v8/V8PlayerComponents.kt").contains("fun GlassTrack("))
        assertTrue(p.contains("v5OverlayGestures("))
        assertTrue(source("ui/player/v5/V5PlayerContent.kt").contains("fun Modifier.v5OverlayGestures("))
        assertTrue(p.contains("onSeekToPositionMs = updatedOnSliderValueChange"))
    }
}
