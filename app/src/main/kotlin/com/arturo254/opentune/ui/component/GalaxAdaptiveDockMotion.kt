package com.arturo254.opentune.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * A single scroll signal from the app shell, not a scroll listener registered
 * on individual destinations. Hysteresis prevents micro-scrolls from making
 * the playback/navigation dock flutter between its two optical arrangements.
 */
internal class GalaxDockScrollAccumulator {
    private var travel = 0f
    private var merged = false

    fun advance(scrollY: Float, thresholdPx: Float): Boolean {
        if (!scrollY.isFinite() || !thresholdPx.isFinite() || thresholdPx <= 0f ||
            abs(scrollY) < 0.75f) return merged

        // Compose scrolls upward (negative Y) when the user travels down a list.
        travel = (travel - scrollY).coerceIn(0f, thresholdPx * 1.4f)
        if (!merged && travel >= thresholdPx) merged = true
        if (merged && travel <= thresholdPx * 0.35f) merged = false
        return merged
    }

    fun reset() {
        travel = 0f
        merged = false
    }
}

internal data class GalaxDockMotion(
    val progress: Float,
    val nestedScrollConnection: NestedScrollConnection,
)

/**
 * Merges only while a track and compact bottom navigation are present.
 * Disabling accessibility animations also removes the interpolated frames.
 */
@Composable
internal fun rememberGalaxAdaptiveDockMotion(
    enabled: Boolean,
    resetKey: String,
    reducedMotion: Boolean,
): GalaxDockMotion {
    val accumulator = remember { GalaxDockScrollAccumulator() }
    var merged by remember { mutableStateOf(false) }
    val thresholdPx = with(LocalDensity.current) { 64.dp.toPx() }
    val currentEnabled by rememberUpdatedState(enabled)
    val currentThreshold by rememberUpdatedState(thresholdPx)

    LaunchedEffect(resetKey, enabled) {
        accumulator.reset()
        merged = false
    }

    val nestedConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (!currentEnabled || source != NestedScrollSource.UserInput) {
                    return Offset.Zero
                }
                // Use the actual child movement; allow a pull down at the very
                // top to unfold the dock without requiring another scrollable item.
                val moved = if (abs(consumed.y) >= 0.75f) consumed.y
                    else if (available.y > 0f) available.y else 0f
                val next = accumulator.advance(moved, currentThreshold)
                if (next != merged) merged = next
                return Offset.Zero
            }
        }
    }

    val progress by animateFloatAsState(
        targetValue = if (enabled && merged) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else spring(
            dampingRatio = 0.86f,
            stiffness = 370f,
        ),
        label = "GalaxOT single liquid dock",
    )

    return GalaxDockMotion(progress, nestedConnection)
}
