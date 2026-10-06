package com.arturo254.opentune.sources.tidal

import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.sources.SourceCapability
import com.arturo254.opentune.sources.TrackSearchRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TidalMusicSourceTest {
    @Test
    fun tidalTrackMapsToUniversalVariantWithIsrcAndQuality() {
        val variant =
            TidalTrack(
                id = "123",
                title = "Song",
                version = "Remastered",
                artists = listOf("Artist"),
                albumId = "album-1",
                albumTitle = "Album",
                coverId = "aa-bb-cc",
                durationSeconds = 210,
                isrc = "USAAA2600001",
                explicit = true,
                audioQuality = "HI_RES_LOSSLESS",
            ).toSourceVariant()

        assertEquals(ProviderId.TIDAL, variant.provider)
        assertEquals("123", variant.sourceId)
        assertEquals("Song (Remastered)", variant.title)
        assertEquals(listOf("Artist"), variant.artists)
        assertEquals("album-1", variant.albumId)
        assertEquals("Album", variant.albumName)
        assertEquals(210_000L, variant.durationMs)
        assertEquals("USAAA2600001", variant.isrc)
        assertEquals(true, variant.explicit)
        assertEquals(true, variant.quality?.lossless)
        assertEquals("FLAC", variant.quality?.codec)
        assertTrue(variant.artworkUrl.orEmpty().contains("resources.tidal.com/images/aa/bb/cc"))
    }

    @Test
    fun tidalDeclaresCatalogQualityButNotDirectStreamOrManagedDownload() {
        val source = TidalMusicSource(FakeGateway())

        assertTrue(source.supports(SourceCapability.TRACK_SEARCH))
        assertTrue(source.supports(SourceCapability.TRACK_METADATA))
        assertTrue(source.supports(SourceCapability.AUDIO_QUALITY))
        assertFalse(source.supports(SourceCapability.STREAM))
        assertFalse(source.supports(SourceCapability.OPEN_TUNE_MANAGED_DOWNLOAD))
    }

    @Test
    fun tidalPaginationUsesOpaqueOffsetToken() =
        runBlocking {
            val gateway =
                FakeGateway(
                    page =
                        TidalTrackPage(
                            items = listOf(track("1"), track("2")),
                            total = 4,
                            limit = 2,
                            offset = 0,
                        ),
                )
            val source = TidalMusicSource(gateway)

            val first =
                source
                    .searchTracks(
                        TrackSearchRequest(
                            query = "Song",
                            limit = 2,
                        ),
                    ).getOrThrow()

            assertEquals(listOf("1", "2"), first.items.map { it.sourceId })
            assertEquals("offset:2", first.continuationToken)
            assertEquals(0, gateway.lastOffset)

            gateway.page =
                TidalTrackPage(
                    items = listOf(track("3"), track("4")),
                    total = 4,
                    limit = 2,
                    offset = 2,
                )

            val second =
                source
                    .searchTracks(
                        TrackSearchRequest(
                            query = "Song",
                            limit = 2,
                            continuationToken = first.continuationToken,
                        ),
                    ).getOrThrow()

            assertEquals(2, gateway.lastOffset)
            assertEquals(listOf("3", "4"), second.items.map { it.sourceId })
            assertNull(second.continuationToken)
        }

    @Test
    fun invalidTidalContinuationFailsBeforeNetwork() =
        runBlocking {
            val gateway = FakeGateway()
            val source = TidalMusicSource(gateway)

            val result =
                source.searchTracks(
                    TrackSearchRequest(
                        query = "Song",
                        continuationToken = "bad",
                    ),
                )

            assertTrue(result.isFailure)
            assertEquals(0, gateway.calls)
        }

    @Test
    fun tidalVariantBuildsCatalogPlaybackReference() {
        val reference = track("42").toSourceVariant().toTidalCatalogReference()

        assertEquals("tidal:42", reference.cacheId)
        assertEquals("Song", reference.title)
        assertEquals(listOf("Artist"), reference.artists)
        assertEquals(180_000L, reference.durationMs)
    }

    private fun track(id: String): TidalTrack =
        TidalTrack(
            id = id,
            title = "Song",
            artists = listOf("Artist"),
            albumId = "album-$id",
            albumTitle = "Album",
            durationSeconds = 180,
        )

    private class FakeGateway(
        var page: TidalTrackPage =
            TidalTrackPage(
                items = listOf(track("default")),
                total = 1,
                limit = 50,
                offset = 0,
            ),
    ) : TidalCatalogGateway {
        var calls = 0
        var lastOffset: Int? = null

        override suspend fun searchTracksPage(
            query: String,
            limit: Int,
            offset: Int,
        ): Result<TidalTrackPage> {
            calls++
            lastOffset = offset
            return Result.success(page)
        }
    }
}
