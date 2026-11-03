package com.arturo254.opentune.sources.playback

import androidx.media3.common.MediaItem
import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudiusCollectionQueueTest {
    @Test
    fun startsWithSelectedTrackAndDefersRemainingSongs() = runBlocking {
        val tracks = (0 until 75).map { id ->
            SourceVariant(
                provider = ProviderId.AUDIUS,
                sourceId = "track-$id",
                title = "Track $id",
                artists = listOf("Artist"),
            )
        }
        val resolved = mutableListOf<String>()
        val queue = AudiusCollectionQueue(
            title = "Lista",
            tracks = tracks,
            startIndex = 20,
            resolver = { track ->
                resolved.add(track.sourceId)
                MediaItem.Builder().setMediaId(track.sourceId).setUri("https://example.org/audio").build()
            },
        )

        assertEquals(listOf("track-20"), queue.getInitialStatus().items.map { it.mediaId })
        assertEquals(listOf("track-20"), resolved)
        assertTrue(queue.hasNextPage())

        val firstPage = queue.nextPage()
        assertEquals(49, firstPage.size)
        assertEquals("track-21", firstPage.first().mediaId)
        assertEquals("track-19", firstPage.last().mediaId)

        val secondPage = queue.nextPage()
        assertEquals(25, secondPage.size)
        assertEquals("track-0", secondPage.first().mediaId)
        assertEquals("track-19", secondPage.last().mediaId)
        assertFalse(queue.hasNextPage())
        assertTrue(queue.nextPage().isEmpty())
    }
}
