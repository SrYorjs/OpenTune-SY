package com.arturo254.opentune.ui.screens

import org.junit.Assert.*
import org.junit.Test

class OrbitLayoutTest {
    @Test
    fun phoneTabletAndLandscapeUseAvailableSpace() {
        assertEquals(1, orbitLayout(360f, 720f, 1f).columns)
        assertFalse(orbitLayout(360f, 720f, 1f).sideBySide)
        assertEquals(2, orbitLayout(800f, 360f, 1f).columns)
        assertTrue(orbitLayout(800f, 360f, 1f).sideBySide)
        assertEquals(3, orbitLayout(1120f, 900f, 1f).columns)
    }

    @Test
    fun largeTextReturnsToOneColumnWithoutShrinkingText() {
        val layout = orbitLayout(800f, 600f, 2f)
        assertEquals(1, layout.columns)
        assertFalse(layout.sideBySide)
    }

    @Test
    fun artFitsNarrowAndShortWindowsIncludingTransientZeroSize() {
        for (width in listOf(0f, 240f, 320f, 600f, 1120f)) {
            for (height in listOf(0f, 180f, 360f, 720f, 1200f)) {
                val layout = orbitLayout(width, height, 1f)
                assertTrue(layout.artworkDp >= 0f)
                assertTrue(layout.artworkDp <= width)
                assertTrue(layout.artworkDp <= height * .34f)
                assertTrue(layout.artworkDp <= 260f)
            }
        }
        assertTrue(orbitLayout(360f, 240f, 1f).artworkDp < orbitLayout(360f, 800f, 1f).artworkDp)
    }
}
