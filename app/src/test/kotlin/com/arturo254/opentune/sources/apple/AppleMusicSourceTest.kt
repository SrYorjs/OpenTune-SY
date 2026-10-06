package com.arturo254.opentune.sources.apple

import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.sources.SourceCapability
import com.arturo254.opentune.sources.TrackSearchRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppleMusicSourceTest {
    @Test
    fun appleTrackMapsToUniversalVariantWithIsrc() {
        val variant =
            AppleMusicTrack(
                id = "100",
                title = "Song",
                artistName = "Artist A, Artist B",
                albumName = "Album",
                durationMs = 201_000L,
                isrc = "USAAA2600002",
                explicit = true,
                artworkUrl = "https://example.test/art.jpg",
                albumId = "album-100",
            ).toSourceVariant()

        assertEquals(ProviderId.APPLE_MUSIC, variant.provider)
        assertEquals("100", variant.sourceId)
        assertEquals("Song", variant.title)
        assertEquals(listOf("Artist A", "Artist B"), variant.artists)
        assertEquals("USAAA2600002", variant.isrc)
        assertEquals(201_000L, variant.durationMs)
        assertEquals("album-100", variant.albumId)
        assertEquals("Album", variant.albumName)
    }

    @Test
    fun appleDeclaresCatalogCapabilitiesWithoutFakeStream() {
        val source = AppleMusicSource(FakeGateway())

        assertTrue(source.supports(SourceCapability.TRACK_SEARCH))
        assertTrue(source.supports(SourceCapability.TRACK_METADATA))
        assertTrue(source.supports(SourceCapability.ALBUMS))
        assertTrue(source.supports(SourceCapability.ARTISTS))
        assertTrue(source.supports(SourceCapability.PLAYLISTS))
        assertFalse(source.supports(SourceCapability.STREAM))
        assertFalse(source.supports(SourceCapability.OPEN_TUNE_MANAGED_DOWNLOAD))
    }

    @Test
    fun applePaginationUsesOpaqueOffsetToken() =
        runBlocking {
            val gateway =
                FakeGateway(
                    page =
                        AppleMusicTrackPage(
                            items = listOf(track("1"), track("2")),
                            limit = 2,
                            offset = 0,
                            hasMore = true,
                        ),
                )
            val source = AppleMusicSource(gateway)

            val first =
                source
                    .searchTracks(
                        TrackSearchRequest(
                            query = "Song",
                            limit = 2,
                        ),
                    ).getOrThrow()

            assertEquals("offset:2", first.continuationToken)
            assertEquals(0, gateway.lastOffset)

            gateway.page =
                AppleMusicTrackPage(
                    items = listOf(track("3")),
                    limit = 2,
                    offset = 2,
                    hasMore = false,
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
            assertEquals("3", second.items.single().sourceId)
            assertNull(second.continuationToken)
        }

    @Test
    fun invalidAppleContinuationFailsBeforeNetwork() =
        runBlocking {
            val gateway = FakeGateway()
            val source = AppleMusicSource(gateway)

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
    fun appleVariantBuildsCatalogPlaybackReference() {
        val reference = track("42").toSourceVariant().toAppleMusicCatalogReference()

        assertEquals("apple-music:42", reference.cacheId)
        assertEquals("Song", reference.title)
        assertEquals(listOf("Artist"), reference.artists)
        assertEquals(180_000L, reference.durationMs)
    }

    private fun track(id: String): AppleMusicTrack =
        AppleMusicTrack(
            id = id,
            title = "Song",
            artistName = "Artist",
            albumName = "Album",
            durationMs = 180_000L,
        )

    private class FakeGateway(
        var page: AppleMusicTrackPage =
            AppleMusicTrackPage(
                items = listOf(track("default")),
                limit = 25,
                offset = 0,
                hasMore = false,
            ),
    ) : AppleMusicCatalogGateway {
        var calls = 0
        var lastOffset: Int? = null

        override suspend fun searchTracksPage(
            query: String,
            limit: Int,
            offset: Int,
        ): Result<AppleMusicTrackPage> {
            calls++
            lastOffset = offset
            return Result.success(page)
        }
    }
}
