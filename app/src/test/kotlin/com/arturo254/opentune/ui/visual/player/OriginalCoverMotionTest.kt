package com.arturo254.opentune.ui.visual.player

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OriginalCoverMotionTest {
    @Test
    fun beginsAtMiniArtworkCenter() {
        val placement = originalCoverPlacement(
            progress = 0f,
            fullWidth = 320.dp,
            fullCenterX = 180.dp,
            fullCenterY = 260.dp,
            miniCenterX = 56.dp,
            miniCenterY = 32.dp,
            growLate = true,
        )
        assertEquals(48f / 320f, placement.scale, 0.0001f)
        assertEquals(-124f, placement.translationX.value, 0.0001f)
        assertEquals(-228f, placement.translationY.value, 0.0001f)
    }

    @Test
    fun finishesWithExactExpandedDimensionsAndPosition() {
        val placement = originalCoverPlacement(
            progress = 1f,
            fullWidth = 360.dp,
            fullCenterX = 180.dp,
            fullCenterY = 250.dp,
            miniCenterX = 64.dp,
            miniCenterY = 32.dp,
            growLate = true,
        )
        assertEquals(1f, placement.scale, 0.0001f)
        assertEquals(0f, placement.translationX.value, 0.0001f)
        assertEquals(0f, placement.translationY.value, 0.0001f)
    }

    @Test
    fun lateGrowthUsesMochyTravelBeforeExpansion() {
        val before = originalCoverPlacement(
            progress = 0.5f,
            fullWidth = 320.dp,
            fullCenterX = 180.dp,
            fullCenterY = 270.dp,
            miniCenterX = 55.dp,
            miniCenterY = 32.dp,
            growLate = true,
        )
        val after = originalCoverPlacement(
            progress = 0.9f,
            fullWidth = 320.dp,
            fullCenterX = 180.dp,
            fullCenterY = 270.dp,
            miniCenterX = 55.dp,
            miniCenterY = 32.dp,
            growLate = true,
        )
        assertEquals(48f / 320f, before.scale, 0.0001f)
        assertTrue(after.scale > before.scale)
        assertTrue(after.scale < 1f)
        assertTrue(kotlin.math.abs(after.translationY.value) < kotlin.math.abs(before.translationY.value))
    }
}
