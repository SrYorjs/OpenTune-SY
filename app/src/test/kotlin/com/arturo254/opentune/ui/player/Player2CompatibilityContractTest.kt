package com.arturo254.opentune.ui.player

import com.arturo254.opentune.constants.PlayerDesignStyle
import com.arturo254.opentune.constants.PlayerBackgroundStyle
import com.arturo254.opentune.playback.QueueWindowPlanner
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Source-contract tests for integration boundaries not executable in a plain
 * JVM without a Media3 service/Compose scene. They are NOT playback E2E tests.
 */
class Player2CompatibilityContractTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("app main Kotlin sources not found")
    private fun source(path: String) = File(root, "com/arturo254/opentune/$path").readText()

    @Test
    fun allPersistedStylesRemainAvailableIncludingV4AndV6() {
        assertEquals(
            listOf("V1", "V2", "V3", "V4", "V5", "V6", "V7", "V8"),
            PlayerDesignStyle.entries.map { it.name },
        )
        assertNotNull(PlayerBackgroundStyle.DEFAULT)
        assertEquals(PlayerDesignStyle.V8, PlayerDesignStyle.valueOf("V8"))
    }

    @Test
    fun preferenceNamesAndDefaultsHaveNotBeenRenamed() {
        val keys = source("constants/PreferenceKeys.kt")
        val host = source("ui/player/Player.kt")
        val queue = source("ui/player/Queue.kt")
        val mini = source("ui/player/MiniPlayer.kt")
        listOf(
            "stringPreferencesKey(\"playerDesignStyle\")",
            "stringPreferencesKey(\"playerBackgroundStyle\")",
            "booleanPreferencesKey(\"queueEditLock\")",
            "booleanPreferencesKey(\"autoLoadMore\")",
            "booleanPreferencesKey(\"swipeThumbnail\")",
            "floatPreferencesKey(\"swipeSensitivity\")",
        ).forEach { assertTrue("Preference contract missing: $it", keys.contains(it)) }
        assertTrue(host.contains("defaultValue = PlayerDesignStyle.V8"))
        assertTrue(queue.contains("defaultValue = PlayerDesignStyle.V8"))
        assertTrue(queue.contains("rememberPreference(QueueEditLockKey, defaultValue = true)"))
        assertTrue(queue.contains("rememberPreference(AutoLoadMoreKey, defaultValue = true)"))
        assertTrue(mini.contains("SwipeThumbnailKey"))
    }

    @Test
    fun transportControlsAndTogetherSkipAreStillConnected() {
        val playback = source("playback/PlayerConnection.kt")
        val playerControls = source("ui/player/PlayerComponents.kt")
        val miniControls = source("ui/player/MiniPlayerComponents.kt")
        assertTrue(playback.contains("ControlAction.SkipNext"))
        assertTrue(playback.contains("ControlAction.SkipPrevious"))
        assertTrue(playback.contains("TogetherRole.Guest"))
        assertTrue(playback.contains("player.seekToNext()"))
        assertTrue(playback.contains("player.seekToPrevious()"))
        // Phase 4.3 routes all visual controls through PlayerActions.
        assertTrue(playerControls.contains("playerActions.playPause()"))
        assertTrue(playerControls.contains("playerActions::next"))
        assertTrue(playerControls.contains("playerActions::previous"))
        val actions = source("ui/player/actions/PlayerActions.kt")
        assertTrue(actions.contains("connection.seekToNext()"))
        assertTrue(actions.contains("connection.seekToPrevious()"))
        assertTrue(miniControls.contains("MiniPlayerSwipePolicy.shouldChangeSong("))
        assertTrue(miniControls.contains("MiniPlayerSwipePolicy.canConsumeDrag("))
    }

    @Test
    fun playerQueuePreservesLockShuffleAndReorderPaths() {
        val queue = source("ui/player/Queue.kt")
        assertTrue(queue.contains("effectiveLocked = locked || togetherForcesLock"))
        assertTrue(queue.contains("rememberReorderableLazyListState("))
        val actions = source("ui/player/queue/QueueActions.kt")
        assertTrue(queue.contains("queueActions.reorder("))
        assertTrue(queue.contains("queueActions::toggleRepeat"))
        assertTrue(queue.contains("queueActions::toggleShuffle"))
        assertTrue(queue.contains("queueActions.removeAt("))
        assertTrue(queue.contains("queueActions.restore("))
        assertTrue(actions.contains("player.moveMediaItem("))
        assertTrue(actions.contains("playerActions.toggleRepeat()"))
        assertTrue(actions.contains("playerActions.toggleShuffle()"))
        assertTrue(queue.contains("AutoLoadMoreKey"))
        assertNotNull(QueueWindowPlanner.plan(1, 0))
    }

    @Test
    fun criticalVisualIntegrationPointsStillExist() {
        val host = source("ui/player/Player.kt")
        val controls = source("ui/player/PlayerComponents.kt")
        assertTrue(host.contains("fun BottomSheetPlayer("))
        assertTrue(host.contains("V7PlayerScene("))
        assertTrue(source("ui/player/v7/V7PlayerScene.kt").contains("V7PlayerBackdrop("))
        assertTrue(host.contains("V8PlayerBackdrop("))
        // Phase 4.7: the Canvas model is now owned by the visual engine,
        // while Player keeps consuming its current Canvas snapshot.
        val visual = source("ui/player/visual/PlayerVisualState.kt")
        assertTrue(visual.contains("CanvasArtwork("))
        assertTrue(host.contains("val canvasArtwork = playerVisuals.canvasArtwork"))
        assertTrue(host.contains("PlayerLyricsPanel("))
        assertTrue(controls.contains("fun V8PlayerControlsContent("))
        assertTrue(controls.contains("fun PlayerPlaybackControls("))
    }
}
