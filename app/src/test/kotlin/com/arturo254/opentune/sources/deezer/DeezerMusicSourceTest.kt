package com.arturo254.opentune.sources.deezer

import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.deezer.DeezerTrack
import com.arturo254.opentune.deezer.DeezerTrackPage
import com.arturo254.opentune.sources.SourceCapability
import com.arturo254.opentune.sources.TrackSearchRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeezerMusicSourceTest {
    @Test
    fun deezerTrackMapsToProviderNeutralVariantWithoutInventingQuality() {
        val track =
            DeezerTrack(
                id = "3135556",
                title = "Harder Better Faster Stronger",
                artistName = "Daft Punk",
                artistId = "27",
                albumId = "302127",
                albumName = "Discovery",
                durationSeconds = 224,
                explicit = false,
                artworkUrl = "https://example.test/deezer.jpg",
                link = "https://www.deezer.com/track/3135556",
                isrc = "GBDUW0000059",
            )

        val variant = track.toDeezerSourceVariant()

        assertEquals(ProviderId.DEEZER, variant.provider)
        assertEquals("3135556", variant.sourceId)
        assertEquals("Harder Better Faster Stronger", variant.title)
        assertEquals(listOf("Daft Punk"), variant.artists)
        assertEquals("302127", variant.albumId)
        assertEquals("Discovery", variant.albumName)
        assertEquals(224_000L, variant.durationMs)
        assertEquals("GBDUW0000059", variant.isrc)
        assertEquals(false, variant.explicit)
        assertEquals("https://example.test/deezer.jpg", variant.artworkUrl)
        assertNull(variant.quality)
    }

    @Test
    fun deezerDeclaresDiscoveryCapabilitiesWithoutPretendingToStream() {
        val source = DeezerMusicSource(FakeGateway())

        assertTrue(source.supports(SourceCapability.TRACK_SEARCH))
        assertTrue(source.supports(SourceCapability.TRACK_METADATA))
        assertTrue(source.supports(SourceCapability.STATIC_ARTWORK))
        assertFalse(source.supports(SourceCapability.STREAM))
        assertFalse(source.supports(SourceCapability.OPEN_TUNE_MANAGED_DOWNLOAD))
        assertFalse(source.supports(SourceCapability.AUDIO_QUALITY))
    }

    @Test
    fun searchMapsCatalogPageAndKeepsOpaqueContinuation() =
        runBlocking {
            val gateway =
                FakeGateway(
                    searchResult =
                        Result.success(
                            DeezerTrackPage(
                                items = listOf(track(id = "1")),
                                nextIndex = 50,
                            ),
                        ),
                )
            val source = DeezerMusicSource(gateway)

            val first =
                source
                    .searchTracks(
                        TrackSearchRequest(
                            query = "Daft Punk",
                            limit = 80,
                        ),
                    ).getOrThrow()

            assertEquals("Daft Punk", gateway.lastQuery)
            assertEquals(50, gateway.lastLimit)
            assertEquals(0, gateway.lastIndex)
            assertEquals("index:50", first.continuationToken)
            assertEquals(ProviderId.DEEZER, first.items.single().provider)

            gateway.searchResult =
                Result.success(
                    DeezerTrackPage(
                        items = listOf(track(id = "2")),
                        nextIndex = null,
                    ),
                )

            val second =
                source
                    .searchTracks(
                        TrackSearchRequest(
                            query = "Daft Punk",
                            continuationToken = first.continuationToken,
                        ),
                    ).getOrThrow()

            assertEquals(50, gateway.lastIndex)
            assertEquals("2", second.items.single().sourceId)
            assertNull(second.continuationToken)
        }

    @Test
    fun invalidContinuationFailsBeforeCallingCatalog() =
        runBlocking {
            val gateway = FakeGateway()
            val source = DeezerMusicSource(gateway)

            val result =
                source.searchTracks(
                    TrackSearchRequest(
                        query = "Song",
                        continuationToken = "wrong-token",
                    ),
                )

            assertTrue(result.isFailure)
            assertEquals(0, gateway.searchCalls)
        }

    @Test
    fun trendingAlsoReturnsSourceVariants() =
        runBlocking {
            val gateway =
                FakeGateway(
                    trendingResult =
                        Result.success(
                            listOf(
                                track(id = "popular-1"),
                                track(id = "popular-2"),
                            ),
                        ),
                )
            val source = DeezerMusicSource(gateway)

            val result = source.trendingTracks(limit = 30).getOrThrow()

            assertEquals(30, gateway.lastTrendingLimit)
            assertEquals(listOf("popular-1", "popular-2"), result.map { it.sourceId })
            assertTrue(result.all { it.provider == ProviderId.DEEZER })
        }

    @Test
    fun deezerVariantBuildsCatalogReferenceForExistingPlaybackBridge() {
        val variant = track(id = "42").toDeezerSourceVariant()

        val reference = variant.toDeezerCatalogReference()

        assertEquals("deezer:42", reference.cacheId)
        assertEquals("deezer-album:album-1", reference.albumId)
        assertEquals("Song", reference.title)
        assertEquals(listOf("Artist"), reference.artists)
        assertEquals(180_000L, reference.durationMs)
    }

    private fun track(id: String): DeezerTrack =
        DeezerTrack(
            id = id,
            title = "Song",
            artistName = "Artist",
            artistId = "artist-1",
            albumId = "album-1",
            albumName = "Album",
            durationSeconds = 180,
            explicit = false,
            artworkUrl = "https://example.test/$id.jpg",
            link = null,
            isrc = "USAAA0000001",
        )

    private class FakeGateway(
        var searchResult: Result<DeezerTrackPage> =
            Result.success(
                DeezerTrackPage(
                    items = listOf(trackStatic("default")),
                    nextIndex = null,
                ),
            ),
        var trendingResult: Result<List<DeezerTrack>> =
            Result.success(listOf(trackStatic("trending"))),
    ) : DeezerCatalogGateway {
        var searchCalls: Int = 0
        var lastQuery: String? = null
        var lastLimit: Int? = null
        var lastIndex: Int? = null
        var lastTrendingLimit: Int? = null

        override suspend fun searchTracksPage(
            query: String,
            limit: Int,
            index: Int,
        ): Result<DeezerTrackPage> {
            searchCalls++
            lastQuery = query
            lastLimit = limit
            lastIndex = index
            return searchResult
        }

        override suspend fun trendingTracks(limit: Int): Result<List<DeezerTrack>> {
            lastTrendingLimit = limit
            return trendingResult
        }

        companion object {
            private fun trackStatic(id: String): DeezerTrack =
                DeezerTrack(
                    id = id,
                    title = "Song",
                    artistName = "Artist",
                    artistId = null,
                    albumId = "album-1",
                    albumName = "Album",
                    durationSeconds = 180,
                    explicit = false,
                    artworkUrl = null,
                    link = null,
                )
        }
    }
}
