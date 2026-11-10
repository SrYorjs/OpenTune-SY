package com.arturo254.opentune.ui.visual.player

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OriginalExpandedPlayerIntegrationTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("No existe la ruta del proyecto")

    private fun source(path: String) =
        File(root, "com/arturo254/opentune/$path").readText()

    @Test
    fun originalPlayerIsActiveInsideTheExistingBottomSheet() {
        val player = source("ui/player/Player.kt")
        val host = source("ui/player/host/PlayerHost.kt")
        assertTrue(host.contains("BottomSheetPlayer("))
        assertTrue(player.contains("OriginalExpandedPlayer("))
        assertTrue(player.contains("useOriginalExpandedPlayer"))
        assertTrue(player.contains("onLyrics = { lyricsSheetState.expandSoft() }"))
        assertTrue(player.contains("onQueue = { queueSheetState.expandSoft() }"))
        assertTrue(player.contains("player = playerConnection.player"))
        assertTrue(player.contains("actions = playerActions"))
    }

    @Test
    fun motionAndTransportAreSourceBacked() {
        val main = source("ui/visual/player/OriginalExpandedPlayer.kt")
        val carousel = source("ui/visual/player/SongCarousel.kt")
        val control = source("ui/visual/player/OriginalPlayerControls.kt")
        val seek = source("ui/visual/player/PlayerProgressSlider.kt")
        val handle = source("ui/visual/player/PlayerHandle.kt")
        assertTrue(main.contains("CarouselPictures("))
        assertTrue(main.contains("CarouselCrossfade("))
        assertTrue(main.contains("CoverReflection("))
        assertTrue(main.contains("OriginalPlayerTransportRow("))
        assertTrue(main.contains("OriginalPlayerProgressSlider("))
        assertTrue(main.contains("PlayerHandle("))
        assertTrue(carousel.contains("fun startDrag()"))
        assertTrue(carousel.contains("fun dragTo("))
        assertTrue(carousel.contains("fun release("))
        assertTrue(control.contains("AnimatedPressScalePlayPauseButton("))
        assertTrue(control.contains("AnimatedPressScaleSkipButton("))
        assertTrue(control.contains("PlaybackParameters("))
        assertTrue(seek.contains("fun OriginalPlayerProgressSlider("))
        assertTrue(handle.contains("rememberOnLightBehind("))
    }

    @Test
    fun queueAndLyricsCardsRemainInteractive() {
        val main = source("ui/visual/player/OriginalExpandedPlayer.kt")
        val cards = source("ui/visual/player/OriginalPlayerBottomCards.kt")
        assertTrue(main.contains("onOpenLyrics = onLyrics"))
        assertTrue(main.contains("onOpenQueue = onQueue"))
        assertTrue(cards.contains("detectTapGestures("))
        assertTrue(cards.contains("awaitEachGesture"))
        assertTrue(cards.contains("flip.animateTo("))
        assertTrue(main.contains("actions::toggleShuffle"))
        assertTrue(main.contains("actions::toggleRepeat"))
        assertTrue(main.contains("actions::toggleFavorite"))
    }

    @Test
    fun originalPlayerHasNoAlternateMusicService() {
        val main = source("ui/visual/player/OriginalExpandedPlayer.kt")
        val controls = source("ui/visual/player/OriginalPlayerControls.kt")
        assertFalse(main.contains("ExoPlayer.Builder("))
        assertFalse(main.contains("MusicService("))
        assertFalse(controls.contains("ExoPlayer.Builder("))
        assertTrue(main.contains("actions.seekTo("))
        assertTrue(main.contains("actions::next"))
        assertTrue(main.contains("actions::previous"))
        assertTrue(main.contains("onVolumeChange = onVolumeChange"))
    }
}
