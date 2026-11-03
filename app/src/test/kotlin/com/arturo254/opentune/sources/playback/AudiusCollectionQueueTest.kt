package com.arturo254.opentune.sources.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudiusCollectionQueueTest {
    @Test
    fun selectedTrackStartsTheQueueWithoutResolvingAllMediaItems() {
        val tracks = (0 until 75).map { "track-$it" }
        val ordered = rotateAudiusTracks(tracks, 20)

        assertEquals(75, ordered.size)
        assertEquals("track-20", ordered.first())
        assertEquals("track-19", ordered.last())
        assertEquals("track-69", ordered[49])
    }

    @Test
    fun queuePagesAreLimitedAndInvalidSelectionsAreRejected() {
        assertEquals(50, audiusQueuePageEnd(1, 75))
        assertEquals(75, audiusQueuePageEnd(50, 75))
        assertEquals(0, audiusQueuePageEnd(1, 0))
        assertTrue(rotateAudiusTracks(listOf("one"), 3).isEmpty())
    }
}
