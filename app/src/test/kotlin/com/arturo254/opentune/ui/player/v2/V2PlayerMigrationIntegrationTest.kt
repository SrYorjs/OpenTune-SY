package com.arturo254.opentune.ui.player.v2

import com.arturo254.opentune.constants.PlayerDesignStyle
import com.arturo254.opentune.ui.player.layout.PlayerLayoutFamily
import com.arturo254.opentune.ui.player.layout.resolvePlayerLayout
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V2 migration contracts. Exact device UI rendering, gesture physics and
 * playback state still require phone regression; these are JVM/source tests.
 */
class V2PlayerMigrationIntegrationTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Android Kotlin source root missing")
    private fun source(path: String): String =
        File(root, "com/arturo254/opentune/$path").readText()

    @Test fun v2UsesOneExplicitLayoutRouteWithoutChangingClassicSlots() {
        val layout = source("ui/player/layout/PlayerLayout.kt")
        val player = source("ui/player/Player.kt")
        assertEquals(PlayerLayoutFamily.V2, resolvePlayerLayout(PlayerDesignStyle.V2, false).family)
        assertEquals(PlayerLayoutFamily.V2, resolvePlayerLayout(PlayerDesignStyle.V2, true).family)
        assertEquals(PlayerLayoutFamily.CLASSIC, resolvePlayerLayout(PlayerDesignStyle.V1, false).family)
        assertEquals(PlayerLayoutFamily.CLASSIC, resolvePlayerLayout(PlayerDesignStyle.V3, false).family)
        assertTrue(layout.contains("v2Portrait: @Composable () -> Unit = classicPortrait"))
        assertTrue(layout.contains("v2Landscape: @Composable () -> Unit = classicLandscape"))
        assertTrue(layout.contains("PlayerLayoutFamily.V2 -> v2Portrait()"))
        assertTrue(layout.contains("PlayerLayoutFamily.V2 -> v2Landscape()"))
        assertTrue(player.contains("PlayerLayout("))
        assertTrue(player.contains("classicPortrait = {"))
        assertTrue(player.contains("classicLandscape = {"))
        assertTrue(player.contains("Thumbnail("))
    }

    @Test fun v2TopActionsKeepAsymmetricPillsAndUseSharedIntents() {
        val controls = source("ui/player/PlayerComponents.kt")
        val v2 = source("ui/player/v2/V2PlayerComponents.kt")
        assertTrue(controls.contains("V2PlayerTopActions("))
        assertTrue(controls.contains("onToggleFavorite = playerActions::toggleFavorite"))
        assertTrue(controls.contains("onShare = { sharePlayerTrack(context, mediaMetadata.id) }"))
        assertTrue(v2.contains("topStart = 50.dp, bottomStart = 50.dp"))
        assertTrue(v2.contains("topEnd = 10.dp, bottomEnd = 10.dp"))
        assertTrue(v2.contains("topStart = 10.dp, bottomStart = 10.dp"))
        assertTrue(v2.contains("topEnd = 50.dp, bottomEnd = 50.dp"))
        assertTrue(v2.contains("Arrangement.spacedBy(12.dp)"))
        assertTrue(v2.contains(".size(42.dp)"))
        assertTrue(v2.contains("playerFavoriteIconRes(liked)"))
    }

    @Test fun v2ControlsKeepPressedSpringsAndCallSinglePlayerActions() {
        val controls = source("ui/player/PlayerComponents.kt")
        val v2 = source("ui/player/v2/V2PlayerComponents.kt")
        val actions = source("ui/player/actions/PlayerActions.kt")
        assertTrue(controls.contains("V2PlayerTransport("))
        assertTrue(controls.contains("onPrevious = playerActions::previous"))
        assertTrue(controls.contains("onPlayPause = playerActions::playPause"))
        assertTrue(controls.contains("onNext = playerActions::next"))
        assertTrue(v2.contains("label = \"playWeightV2\""))
        assertTrue(v2.contains("label = \"sideWeightV2\""))
        assertTrue(v2.contains("Spring.DampingRatioLowBouncy"))
        assertTrue(v2.contains("Spring.StiffnessLow"))
        assertTrue(v2.contains("val playButtonHeight = maxW / 6f"))
        assertTrue(v2.contains("val playButtonWidth = playButtonHeight * 1.6f"))
        assertTrue(v2.contains("val sideButtonHeight = playButtonHeight * 0.8f"))
        assertTrue(v2.contains("val sideButtonWidth = sideButtonHeight * 1.3f"))
        assertTrue(v2.contains("PlayerSharedPlaybackContent("))
        assertTrue(v2.contains("PlayerSharedSkipIcon("))
        assertTrue(v2.contains("enabled = canSkipPrevious"))
        assertTrue(v2.contains("enabled = canSkipNext"))
        assertTrue(actions.contains("fun next() = backend.next()"))
        assertFalse(v2.contains("PlayerConnection("))
        assertFalse(v2.contains("LaunchedEffect("))
        assertFalse(v2.contains("ExoPlayer("))
    }

    @Test fun progressSkinAndAudioOwnershipRemainShared() {
        val controls = source("ui/player/PlayerComponents.kt")
        val player = source("ui/player/Player.kt")
        assertTrue(controls.contains("PlayerSlider("))
        assertTrue(controls.contains("PlayerTimeLabel("))
        assertTrue(player.contains("rememberPlayerState("))
        assertTrue(player.contains("rememberVisualSkin("))
        assertTrue(player.contains("rememberPlayerSeekSession("))
        assertTrue(player.contains("PlayerHostBackHandler("))
        assertTrue(player.contains("MiniPlayer("))
    }
}
