package com.arturo254.opentune.sources.youtube

import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.innertube.models.Album
import com.arturo254.opentune.innertube.models.Artist
import com.arturo254.opentune.innertube.models.SongItem
import com.arturo254.opentune.sources.SourceCapability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeMusicSourceTest {
    @Test
    fun songItemMapsToProviderNeutralVariantWithoutInventingQuality() {
        val item =
            SongItem(
                id = "video-id",
                title = "Song",
                artists =
                    listOf(
                        Artist(name = "Artist A", id = "UC1"),
                        Artist(name = "Artist B", id = "UC2"),
                    ),
                album = Album(name = "Album", id = "MPREb_album"),
                duration = 213,
                thumbnail = "https://example.test/cover.jpg",
                explicit = true,
            )

        val variant = item.toYouTubeSourceVariant()

        assertEquals(ProviderId.YOUTUBE_MUSIC, variant.provider)
        assertEquals("video-id", variant.sourceId)
        assertEquals(listOf("Artist A", "Artist B"), variant.artists)
        assertEquals("MPREb_album", variant.albumId)
        assertEquals("Album", variant.albumName)
        assertEquals(213_000L, variant.durationMs)
        assertEquals(true, variant.explicit)
        assertEquals("https://example.test/cover.jpg", variant.artworkUrl)
        assertNull(variant.quality)
        assertTrue(variant.version.traits.isEmpty())
    }

    @Test
    fun youtubeAdapterDeclaresExistingIntegrationCapabilities() {
        val source = YouTubeMusicSource()

        assertTrue(source.supports(SourceCapability.TRACK_SEARCH))
        assertTrue(source.supports(SourceCapability.STREAM))
        assertTrue(source.supports(SourceCapability.OPEN_TUNE_MANAGED_DOWNLOAD))
        assertTrue(source.supports(SourceCapability.AUDIO_QUALITY))
    }
}
