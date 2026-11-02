package com.arturo254.opentune.sources.audius

import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.sources.ManagedDownloadResource
import com.arturo254.opentune.sources.SourceCapability
import com.arturo254.opentune.sources.TrackSearchRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudiusMusicSourceTest {
    @Test
    fun searchUsesPagesOfFiftyAndFiltersGatedTracks() = runBlocking {
        val gateway = FakeAudiusGateway()
        gateway.page = AudiusTrackPage(
            items = (1..50).map { id ->
                track(id.toString(), streamGated = id == 2)
            },
            limit = 50,
            offset = 0,
        )
        val source = AudiusMusicSource(gateway)
        val first = source.searchTracks(TrackSearchRequest("galaxot", 80)).getOrThrow()

        assertEquals(50, gateway.lastLimit)
        assertEquals(0, gateway.lastOffset)
        assertEquals(49, first.items.size)
        assertFalse(first.items.any { it.sourceId == "2" })
        assertEquals("audius-offset:50", first.continuationToken)

        gateway.page = AudiusTrackPage(listOf(track("51")), 50, 50)
        val next = source.searchTracks(
            TrackSearchRequest("galaxot", continuationToken = first.continuationToken),
        ).getOrThrow()
        assertEquals("51", next.items.single().sourceId)
        assertEquals(50, gateway.lastOffset)
        assertNull(next.continuationToken)
    }

    @Test
    fun trackParsingDistinguishesStreamingAndDownloads() {
        val json = Json.parseToJsonElement(
            """{"id":"7eP5n","title":"Test Track","user":{"name":"Artist"},"duration":187,
                "artwork":{"480x480":"https://example.org/cover.jpg"},
                "is_streamable":true,"is_downloadable":true}"""
        )
        val track = parseAudiusTrack(json)!!
        assertEquals("7eP5n", track.id)
        assertEquals("Artist", track.artist)
        assertEquals(187_000L, track.durationMs)
        assertEquals("https://example.org/cover.jpg", track.artworkUrl)
        assertTrue(track.downloadable)
        assertFalse(track.downloadGated)
        assertEquals(ProviderId.AUDIUS, track.toSourceVariant().provider)
    }

    @Test
    fun gatedAndNonDownloadableTracksCannotBeSavedOffline() = runBlocking {
        val gateway = FakeAudiusGateway()
        val source = AudiusMusicSource(gateway)
        gateway.track = track("one", downloadable = false)
        assertNull(source.resolveManagedDownload(gateway.track.toSourceVariant()))

        gateway.track = track("two", downloadable = true, downloadGated = true)
        assertNull(source.resolveManagedDownload(gateway.track.toSourceVariant()))

        gateway.track = track("three", downloadable = true)
        assertEquals(
            ManagedDownloadResource("https://example.org/tracks/three/download"),
            source.resolveManagedDownload(gateway.track.toSourceVariant()),
        )
    }

    @Test
    fun audiusDeclaresSearchStreamingAndAuthorizedDownload() {
        val source = AudiusMusicSource(FakeAudiusGateway())
        assertTrue(source.supports(SourceCapability.TRACK_SEARCH))
        assertTrue(source.supports(SourceCapability.STREAM))
        assertTrue(source.supports(SourceCapability.OPEN_TUNE_MANAGED_DOWNLOAD))
        assertFalse(source.supports(SourceCapability.PROVIDER_MANAGED_OFFLINE))
        assertFalse(source.supports(SourceCapability.ARTISTS))
        assertFalse(source.supports(SourceCapability.PLAYLISTS))
    }

    @Test
    fun invalidContinuationDoesNotCallNetwork() = runBlocking {
        val gateway = FakeAudiusGateway()
        val result = AudiusMusicSource(gateway).searchTracks(
            TrackSearchRequest("anything", continuationToken = "wrong"),
        )
        assertTrue(result.isFailure)
        assertEquals(0, gateway.searchCalls)
    }

    private fun track(
        id: String,
        downloadable: Boolean = true,
        streamGated: Boolean = false,
        downloadGated: Boolean = false,
    ): AudiusTrack = AudiusTrack(
        id = id,
        title = "Track",
        artist = "Artist",
        durationMs = 180_000L,
        artworkUrl = null,
        isrc = null,
        downloadable = downloadable,
        streamable = true,
        streamGated = streamGated,
        downloadGated = downloadGated,
        explicit = null,
    )

    private class FakeAudiusGateway : AudiusCatalogGateway {
        var searchCalls = 0
        var lastLimit = 0
        var lastOffset = 0
        var page = AudiusTrackPage(emptyList(), 50, 0)
        var track = AudiusTrack(
            id = "example",
            title = "Track",
            artist = "Artist",
            durationMs = null,
            artworkUrl = null,
            isrc = null,
            downloadable = false,
            streamable = true,
            streamGated = false,
            downloadGated = false,
            explicit = null,
        )

        override suspend fun searchTracks(
            query: String,
            limit: Int,
            offset: Int,
        ): Result<AudiusTrackPage> {
            searchCalls++
            lastLimit = limit
            lastOffset = offset
            return Result.success(page)
        }

        override suspend fun getTrack(id: String): Result<AudiusTrack> = Result.success(track)
        override fun streamUrl(id: String): String = "https://example.org/tracks/$id/stream"
        override fun downloadUrl(id: String): String = "https://example.org/tracks/$id/download"
    }
}
