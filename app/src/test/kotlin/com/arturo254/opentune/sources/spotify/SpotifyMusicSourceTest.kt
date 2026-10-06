package com.arturo254.opentune.sources.spotify

import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.sources.SourceCapability
import com.arturo254.opentune.sources.TrackSearchRequest
import com.arturo254.opentune.spotify.models.SpotifyImage
import com.arturo254.opentune.spotify.models.SpotifyPaging
import com.arturo254.opentune.spotify.models.SpotifySimpleAlbum
import com.arturo254.opentune.spotify.models.SpotifySimpleArtist
import com.arturo254.opentune.spotify.models.SpotifyTrack
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyMusicSourceTest {
    @Test
    fun spotifyTrackMapsToProviderNeutralVariant() {
        val track =
            SpotifyTrack(
                id = "spotify-track-1",
                name = "Song",
                artists =
                    listOf(
                        SpotifySimpleArtist(id = "artist-1", name = "Artist A"),
                        SpotifySimpleArtist(id = "artist-2", name = "Artist B"),
                    ),
                album =
                    SpotifySimpleAlbum(
                        id = "album-1",
                        name = "Album",
                        images =
                            listOf(
                                SpotifyImage(
                                    url = "https://example.test/large.jpg",
                                    width = 640,
                                    height = 640,
                                ),
                                SpotifyImage(
                                    url = "https://example.test/medium.jpg",
                                    width = 300,
                                    height = 300,
                                ),
                            ),
                    ),
                durationMs = 215_000,
                explicit = true,
            )

        val variant = track.toSpotifySourceVariant()

        assertEquals(ProviderId.SPOTIFY, variant.provider)
        assertEquals("spotify-track-1", variant.sourceId)
        assertEquals("Song", variant.title)
        assertEquals(listOf("Artist A", "Artist B"), variant.artists)
        assertEquals("album-1", variant.albumId)
        assertEquals("Album", variant.albumName)
        assertEquals(215_000L, variant.durationMs)
        assertEquals(true, variant.explicit)
        assertEquals("https://example.test/medium.jpg", variant.artworkUrl)
        assertNull(variant.quality)
        assertNull(variant.isrc)
    }

    @Test
    fun spotifyDeclaresCatalogCapabilitiesWithoutPretendingToStream() {
        val source = SpotifyMusicSource(FakeGateway())

        assertTrue(source.supports(SourceCapability.TRACK_SEARCH))
        assertTrue(source.supports(SourceCapability.TRACK_METADATA))
        assertTrue(source.supports(SourceCapability.ALBUMS))
        assertTrue(source.supports(SourceCapability.ARTISTS))
        assertTrue(source.supports(SourceCapability.PLAYLISTS))
        assertTrue(source.supports(SourceCapability.ACCOUNT_LIBRARY))
        assertTrue(source.supports(SourceCapability.STATIC_ARTWORK))
        assertFalse(source.supports(SourceCapability.STREAM))
        assertFalse(source.supports(SourceCapability.OPEN_TUNE_MANAGED_DOWNLOAD))
        assertFalse(source.supports(SourceCapability.AUDIO_QUALITY))
    }

    @Test
    fun searchUsesOpaqueOffsetContinuationAndFiltersSpotifyLocalTracks() =
        runBlocking {
            val gateway =
                FakeGateway(
                    page =
                        SpotifyPaging(
                            items =
                                listOf(
                                    track(id = "1"),
                                    track(id = "local", isLocal = true),
                                ),
                            total = 3,
                            limit = 2,
                            offset = 0,
                        ),
                )
            val source = SpotifyMusicSource(gateway)

            val first =
                source
                    .searchTracks(
                        TrackSearchRequest(
                            query = "Song",
                            limit = 80,
                        ),
                    ).getOrThrow()

            assertEquals("Song", gateway.lastQuery)
            assertEquals(50, gateway.lastLimit)
            assertEquals(0, gateway.lastOffset)
            assertEquals(listOf("1"), first.items.map { it.sourceId })
            assertEquals("offset:2", first.continuationToken)

            gateway.page =
                SpotifyPaging(
                    items = listOf(track(id = "3")),
                    total = 3,
                    limit = 50,
                    offset = 2,
                )

            val second =
                source
                    .searchTracks(
                        TrackSearchRequest(
                            query = "Song",
                            continuationToken = first.continuationToken,
                        ),
                    ).getOrThrow()

            assertEquals(2, gateway.lastOffset)
            assertEquals("3", second.items.single().sourceId)
            assertNull(second.continuationToken)
        }

    @Test
    fun invalidContinuationFailsBeforeGatewayCall() =
        runBlocking {
            val gateway = FakeGateway()
            val source = SpotifyMusicSource(gateway)

            val result =
                source.searchTracks(
                    TrackSearchRequest(
                        query = "Song",
                        continuationToken = "bad-token",
                    ),
                )

            assertTrue(result.isFailure)
            assertEquals(0, gateway.calls)
        }

    @Test
    fun spotifyVariantBuildsCatalogPlaybackReference() {
        val reference = track(id = "42").toSpotifySourceVariant().toSpotifyCatalogReference()

        assertEquals("spotify:42", reference.cacheId)
        assertEquals("spotify-album:album-1", reference.albumId)
        assertEquals("Song", reference.title)
        assertEquals(listOf("Artist"), reference.artists)
        assertEquals(180_000L, reference.durationMs)
    }

    private fun track(
        id: String,
        isLocal: Boolean = false,
    ): SpotifyTrack =
        SpotifyTrack(
            id = id,
            name = "Song",
            artists = listOf(SpotifySimpleArtist(id = "artist-1", name = "Artist")),
            album =
                SpotifySimpleAlbum(
                    id = "album-1",
                    name = "Album",
                    images =
                        listOf(
                            SpotifyImage(
                                url = "https://example.test/$id.jpg",
                                width = 300,
                                height = 300,
                            ),
                        ),
                ),
            durationMs = 180_000,
            explicit = false,
            isLocal = isLocal,
        )

    private class FakeGateway(
        var page: SpotifyPaging<SpotifyTrack> =
            SpotifyPaging(
                items = listOf(
                    SpotifyTrack(
                        id = "default",
                        name = "Default",
                    ),
                ),
                total = 1,
                limit = 50,
                offset = 0,
            ),
    ) : SpotifyCatalogGateway {
        var calls: Int = 0
        var lastQuery: String? = null
        var lastLimit: Int? = null
        var lastOffset: Int? = null

        override suspend fun searchTracksPage(
            query: String,
            limit: Int,
            offset: Int,
        ): Result<SpotifyPaging<SpotifyTrack>> {
            calls++
            lastQuery = query
            lastLimit = limit
            lastOffset = offset
            return Result.success(page)
        }
    }
}
