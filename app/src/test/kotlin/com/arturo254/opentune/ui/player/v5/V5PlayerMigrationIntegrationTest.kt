package com.arturo254.opentune.ui.player.v5

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Structural migration checks; device gestures and animations need physical tests. */
class V5PlayerMigrationIntegrationTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Android source root missing")
    private fun source(path: String) = File(root, "com/arturo254/opentune/$path").readText()

    @Test fun portraitAndLandscapeBothUseTheExtractedV5ContentAndGestureOwner() {
        val p = source("ui/player/Player.kt")
        val compact = source("ui/player/v5/V5PlayerContent.kt")
        assertEquals(2, Regex("""V5PlayerContent\(""").findAll(p).count())
        assertEquals(2, Regex("""\.v5OverlayGestures\(""").findAll(p).count())
        assertEquals(1, Regex("""V5LandscapeLikeBox\(""").findAll(p).count())
        assertTrue(compact.contains("fun V5PlayerContent("))
        assertTrue(compact.contains("fun V5LandscapeLikeBox("))
        assertTrue(compact.contains("fun Modifier.v5OverlayGestures("))
        assertTrue(compact.contains("rotationZ = 90f"))
        assertTrue(compact.contains("transformOrigin = TransformOrigin(0f, 0f)"))
        assertFalse(p.contains("private fun LittlePlayerContent("))
    }

    @Test fun v5GestureThresholdsAndSeekSessionAreStillOwnedByTheOriginalSystems() {
        val p = source("ui/player/Player.kt")
        val v5 = source("ui/player/v5/V5PlayerContent.kt")
        assertTrue(v5.contains("awaitEachGesture"))
        assertTrue(v5.contains("awaitFirstDown(requireUnconsumed = true)"))
        assertTrue(v5.contains("viewConfiguration.doubleTapTimeoutMillis"))
        assertTrue(v5.contains("viewConfiguration.touchSlop"))
        assertTrue(v5.contains("val isTopSide = upPosition.y < size.height / 2f"))
        assertTrue(v5.contains("val seekAllowedFromDown"))
        assertTrue(v5.contains("val clampedFraction = fraction.coerceIn(0f, 1f)"))
        assertTrue(v5.contains("onSeekToPositionMs(targetMs)"))
        assertTrue(v5.contains("onSeekFinished()"))
        assertTrue(p.contains("seekSession.finish {"))
        assertTrue(p.contains("playerActions.seekTo(targetPosition, displayedTrackId = mediaMetadata?.id)"))
        assertTrue(p.contains("onSkipPrevious = playerActions::previous"))
        assertTrue(p.contains("onSkipNext = playerActions::next"))
        assertEquals(2, Regex("""onToggleLike = playerActions::toggleFavorite""").findAll(p).count())
        assertFalse(v5.contains("PlayerConnection("))
        assertFalse(v5.contains("ExoPlayer("))
    }

    @Test fun v5KeepsItsCompactMetadataAndSpringTransport() {
        val c = source("ui/player/PlayerComponents.kt")
        val v5 = source("ui/player/v5/V5PlayerComponents.kt")
        val compact = source("ui/player/v5/V5PlayerContent.kt")
        assertTrue(c.contains("V5PlayerTopActions("))
        assertTrue(c.contains("V5PlayerTransport("))
        assertTrue(c.contains("onToggleFavorite = playerActions::toggleFavorite"))
        assertTrue(c.contains("onToggleRepeat = playerActions::toggleRepeat"))
        assertTrue(c.contains("onPlayPause = playerActions::playPause"))
        assertTrue(v5.contains("label = \"playSizeV1V5\""))
        assertTrue(v5.contains("label = \"playAlphaV1V5\""))
        assertTrue(v5.contains("label = \"sideScaleV1V5\""))
        assertTrue(v5.contains("label = \"extraScaleV1V5\""))
        assertTrue(v5.contains("targetValue = if (isPlayPressed) 76.dp else 72.dp"))
        assertTrue(v5.contains("ResizableIconButton("))
        assertTrue(v5.contains("playerPlaybackIconRes(playbackState, isPlaying)"))
        assertTrue(compact.contains("val titleSize = (56f * scale).sp"))
        assertTrue(compact.contains("val timeSize = (44f * scale).sp"))
        assertTrue(compact.contains("calculatePlayerProgress(positionMs, durationMs, sliderPosition)"))
        assertTrue(compact.contains("AnimatedContent("))
    }

    @Test fun originalV1AndV3RemainOnTheirUnmodifiedStyleBranches() {
        val c = source("ui/player/PlayerComponents.kt")
        assertTrue(c.contains("PlayerDesignStyle.V1 -> {"))
        assertTrue(c.contains("PlayerDesignStyle.V3 -> {"))
        assertFalse(c.contains("PlayerDesignStyle.V1, PlayerDesignStyle.V5 -> {"))
        assertFalse(c.contains("PlayerDesignStyle.V3, PlayerDesignStyle.V5 -> {"))
    }
}
