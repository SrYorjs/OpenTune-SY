package com.arturo254.opentune.ui.player.performance

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Static invariants; profile jank and TalkBack focus ordering on a phone. */
class PlayerPerformanceAccessibilityIntegrationTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Kotlin source root missing")
    private fun source(path: String) =
        File(root, "com/arturo254/opentune/$path").readText()

    @Test fun visualPaletteStateReusesSnapshotAndBoundsMemoryWithoutFlushes() {
        val visual = source("ui/player/visual/PlayerVisualState.kt")
        val cache = source("ui/player/visual/PlayerPaletteCache.kt")
        assertTrue(visual.contains("PlayerPaletteCache<PlayerPaletteCacheKey, List<Color>>()"))
        assertTrue(visual.contains("return remember(artworkUrl, canvasArtwork, gradientColors)"))
        assertTrue(visual.contains("remember(colors.surface, colors.surfaceVariant)"))
        assertFalse(visual.contains("paletteCache.clear()"))
        assertTrue(cache.contains("LinkedHashMap<K, V>"))
        assertTrue(cache.contains("size > maxEntries"))
    }

    @Test fun aodTimeoutUsesOneCancelableDeadlineNotHundredMillisecondPolling() {
        val host = source("ui/player/host/PlayerContentLifecycle.kt")
        assertTrue(host.contains("LaunchedEffect(aodAutoTimeoutSeconds, isExpanded, lastInteractionTime, isAodActive)"))
        assertTrue(host.contains("playerAodRemainingDelayMs("))
        assertTrue(host.contains("delay(remaining)"))
        assertTrue(host.contains("latestOnTimeout()"))
        assertFalse(host.contains("while (isActive)"))
        assertFalse(host.contains("delay(100L)"))
        assertTrue(host.contains("rememberUpdatedState(onAodTimeout)"))
    }

    @Test fun liquidGlassActionsExposeLocalizedTalkBackLabelsWithoutTouchGeometryChanges() {
        val glass = source("ui/component/LiquidGlassControls.kt")
        val v8 = source("ui/player/v8/V8PlayerComponents.kt")
        assertTrue(glass.contains("accessibilityLabel: String? = null"))
        assertTrue(glass.contains("Modifier.semantics { contentDescription = accessibilityLabel }"))
        assertTrue(glass.contains("role = Role.Button"))
        assertEquals(9, Regex("""accessibilityLabel =""").findAll(v8).count())
        listOf(
            "player_a11y_previous", "player_a11y_pause", "player_a11y_like",
            "player_a11y_unlike", "player_a11y_repeat",
        ).forEach { key ->
            assertTrue(v8.contains("R.string.$key"))
        }
        assertTrue(v8.contains("size = 72.dp"))
        assertTrue(v8.contains("size = 58.dp"))
        assertTrue(v8.contains("size = 44.dp"))
        for (style in listOf("v2/V2PlayerComponents.kt",
            "v5/V5PlayerComponents.kt", "v7/V7PlayerComponents.kt")) {
            val file = source("ui/player/$style")
            assertTrue(file.contains("stringResource(if (liked) R.string.player_a11y_unlike else R.string.player_a11y_like)"))
            assertTrue(file.contains("contentDescription = stringResource("))
        }
    }
}
