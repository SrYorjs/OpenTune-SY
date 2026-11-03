package com.arturo254.opentune.ui.screens

import org.junit.Assert.*
import org.junit.Test

class PulseLayoutTest {
    @Test fun compactAndExpandedWindowsReflowTracks() {
        assertEquals(1, pulseLayout(360f, 720f, 1f).columns)
        assertEquals(2, pulseLayout(800f, 360f, 1f).columns)
        assertEquals(3, pulseLayout(1120f, 900f, 1f).columns)
        assertTrue(pulseLayout(800f, 360f, 1f).compactHeight)
        assertFalse(pulseLayout(360f, 720f, 1f).compactHeight)
    }
    @Test fun accessibilityTextKeepsEnoughRoomForEachTrack() {
        assertEquals(1, pulseLayout(800f, 600f, 2f).columns)
        assertEquals(2, pulseLayout(1120f, 700f, 1.5f).columns)
    }
    @Test fun carouselCoversStayWithinPhoneAndTabletViewports() {
        for (width in listOf(240f, 320f, 360f, 600f, 800f, 1120f)) {
            val layout = pulseLayout(width, 700f, 1f)
            assertTrue(layout.coverWidth.value <= width - 40f)
            assertTrue(layout.coverWidth.value in 80f..210f)
        }
        assertTrue(pulseLayout(0f, 0f, 1f).compactHeight)
    }
}
