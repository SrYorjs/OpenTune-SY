package com.arturo254.opentune.sources.soundcloud

import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.sources.SourceCapability
import com.arturo254.opentune.sources.TrackSearchRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundCloudMusicSourceTest {
    @Test
    fun soundCloudTrackMapsToUniversalVariant() {
        val variant =
            SoundCloudTrack(
                id = "123",
                title = "Track",
                artists = listOf("Artist"),
                albumName = "Release",
                durationMs = 201_000L,
                isrc = "USAAA2600010",
                artworkUrl = "https://i1.sndcdn.com/artworks-test-t500x500.jpg",
                streamable = true,
                access = "playable",
            ).toSourceVariant()

        assertEquals(ProviderId.SOUNDCLOUD, variant.provider)
        assertEquals("123", variant.sourceId)
        assertEquals("Track", variant.title)
        assertEquals(listOf("Artist"), variant.artists)
        assertEquals("Release", variant.albumName)
        assertEquals(201_000L, variant.durationMs)
        assertEquals("USAAA2600010", variant.isrc)
        assertEquals(false, variant.quality?.lossless)
        assertNull(variant.explicit)
    }

    @Test
    fun soundCloudDeclaresDirectStreamWithoutPretendingOfflineDownload() {
        val source = SoundCloudMusicSource(FakeCatalogGateway())

        assertTrue(source.supports(SourceCapability.TRACK_SEARCH))
        assertTrue(source.supports(SourceCapability.TRACK_METADATA))
        assertTrue(source.supports(SourceCapability.ARTISTS))
        assertTrue(source.supports(SourceCapability.PLAYLISTS))
        assertTrue(source.supports(SourceCapability.STREAM))
        assertTrue(source.supports(SourceCapability.STATIC_ARTWORK))
        assertFalse(source.supports(SourceCapability.OPEN_TUNE_MANAGED_DOWNLOAD))
        assertFalse(source.supports(SourceCapability.PROVIDER_MANAGED_OFFLINE))
    }

    @Test
    fun searchFiltersNonPlayableTracksAndUsesOpaquePagination() =
        runBlocking {
            val gateway =
                FakeCatalogGateway(
                    page =
                        SoundCloudTrackPage(
                            items =
                                listOf(
                                    track("1"),
                                    track("2", streamable = false),
                                    track("3", access = "blocked"),
                                ),
                            total = 4,
                            limit = 3,
                            offset = 0,
                        ),
                )
            val source = SoundCloudMusicSource(gateway)

            val first =
                source
                    .searchTracks(
                        TrackSearchRequest(
                            query = "Track",
                            limit = 100,
                        ),
                    ).getOrThrow()

            assertEquals(50, gateway.lastLimit)
            assertEquals(0, gateway.lastOffset)
            assertEquals(listOf("1"), first.items.map { it.sourceId })
            assertEquals("offset:50", first.continuationToken)

            gateway.page =
                SoundCloudTrackPage(
                    items = listOf(track("4")),
                    total = 51,
                    limit = 50,
                    offset = 50,
                )

            val second =
                source
                    .searchTracks(
                        TrackSearchRequest(
                            query = "Track",
                            continuationToken = first.continuationToken,
                        ),
                    ).getOrThrow()

            assertEquals(50, gateway.lastOffset)
            assertEquals("4", second.items.single().sourceId)
            assertNull(second.continuationToken)
        }

    @Test
    fun invalidContinuationFailsBeforeGatewayCall() =
        runBlocking {
            val gateway = FakeCatalogGateway()
            val source = SoundCloudMusicSource(gateway)

            val result =
                source.searchTracks(
                    TrackSearchRequest(
                        query = "Track",
                        continuationToken = "bad",
                    ),
                )

            assertTrue(result.isFailure)
            assertEquals(0, gateway.calls)
        }

    @Test
    fun soundCloudVariantBuildsCatalogFallbackReference() {
        val reference = track("42").toSourceVariant().toSoundCloudCatalogReference()

        assertEquals("soundcloud:42", reference.cacheId)
        assertEquals("Track", reference.title)
        assertEquals(listOf("Artist"), reference.artists)
        assertEquals(180_000L, reference.durationMs)
    }

    private fun track(
        id: String,
        streamable: Boolean = true,
        access: String? = "playable",
    ): SoundCloudTrack =
        SoundCloudTrack(
            id = id,
            title = "Track",
            artists = listOf("Artist"),
            durationMs = 180_000L,
            streamable = streamable,
            access = access,
        )

    private class FakeCatalogGateway(
        var page: SoundCloudTrackPage =
            SoundCloudTrackPage(
                items =
                    listOf(
                        SoundCloudTrack(
                            id = "default",
                            title = "Track",
                            artists = listOf("Artist"),
                            durationMs = 180_000L,
                            streamable = true,
                            access = "playable",
                        ),
                    ),
                total = 1,
                limit = 50,
                offset = 0,
            ),
    ) : SoundCloudCatalogGateway {
        var calls = 0
        var lastLimit: Int? = null
        var lastOffset: Int? = null

        override suspend fun searchTracksPage(
            query: String,
            limit: Int,
            offset: Int,
        ): Result<SoundCloudTrackPage> {
            calls++
            lastLimit = limit
            lastOffset = offset
            return Result.success(page)
        }
    }
}
