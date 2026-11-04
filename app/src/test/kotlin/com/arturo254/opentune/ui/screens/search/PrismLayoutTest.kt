package com.arturo254.opentune.ui.screens.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrismLayoutTest {
    @Test
    fun phoneAndTabletUseDifferentCollectionDensities() {
        assertEquals(2, prismLayout(360f, 800f, 1f).columns)
        assertEquals(4, prismLayout(800f, 900f, 1f).columns)
        assertEquals(4, prismLayout(1120f, 900f, 1f).columns)
    }

    @Test
    fun enlargedTextGetsMoreSpaceInsteadOfMoreColumns() {
        assertEquals(1, prismLayout(360f, 800f, 1.5f).columns)
        for (width in listOf(240f, 360f, 600f, 800f, 1120f)) {
            val normal = prismLayout(width, 800f, 1f)
            val enlarged = prismLayout(width, 800f, 2f)
            assertTrue(enlarged.columns <= normal.columns)
            assertTrue(enlarged.tileWidth >= normal.tileWidth)
        }
    }

    @Test
    fun collectionRowsFitTheirMeasuredWindow() {
        for (width in listOf(240f, 320f, 360f, 600f, 800f, 1120f)) {
            for (fontScale in listOf(1f, 1.5f, 2f)) {
                val layout = prismLayout(width, 700f, fontScale)
                val rowWidth = layout.tileWidth.value * layout.columns + 12f * (layout.columns - 1)
                assertTrue(layout.columns in 1..4)
                assertTrue(layout.tileWidth.value > 0f)
                assertTrue(
                    "Overflow at width=$width, fontScale=$fontScale",
                    rowWidth <= width - 48f + .01f,
                )
            }
        }
    }

    @Test
    fun shortWindowsRequestCompactDecorationWithoutChangingColumnDensity() {
        val portrait = prismLayout(800f, 900f, 1f)
        val shortWindow = prismLayout(800f, 360f, 1f)
        assertFalse(portrait.compactHeight)
        assertTrue(shortWindow.compactHeight)
        assertEquals(portrait.columns, shortWindow.columns)
        assertEquals(portrait.tileWidth, shortWindow.tileWidth)
    }
}
