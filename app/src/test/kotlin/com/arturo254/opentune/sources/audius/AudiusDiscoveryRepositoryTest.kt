package com.arturo254.opentune.sources.audius

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class AudiusDiscoveryRepositoryTest {
    @Test
    fun parsesRealArtistProfileFields() {
        val json = Json.parseToJsonElement(
            """{"id":"7eP5n","name":"Galax Artist","handle":"galax",
                "track_count":42,"bio":"Artist biography",
                "profile_picture":{"480x480":"https://example.org/avatar.jpg"}}""",
        )
        val artist = parseAudiusArtist(json)!!
        assertEquals("7eP5n", artist.id)
        assertEquals("Galax Artist", artist.name)
        assertEquals("galax", artist.handle)
        assertEquals(42, artist.trackCount)
        assertEquals("https://example.org/avatar.jpg", artist.artworkUrl)
    }

    @Test
    fun parsesPlaylistsWithArtistArtworkAndTrackCount() {
        val json = Json.parseToJsonElement(
            """{"id":"D7KyD","playlist_name":"Galactic Mix",
                "description":"Electronic music","track_count":12,
                "user":{"name":"Playlist Creator"},
                "artwork":{"_480x480":"https://example.org/playlist.jpg"}}""",
        )
        val playlist = parseAudiusPlaylist(json)!!
        assertEquals("D7KyD", playlist.id)
        assertEquals("Galactic Mix", playlist.name)
        assertEquals("Playlist Creator", playlist.creator)
        assertEquals(12, playlist.trackCount)
        assertEquals("https://example.org/playlist.jpg", playlist.artworkUrl)
    }

    @Test
    fun alternativePlaylistPayloadsAreSupported() {
        val json = Json.parseToJsonElement(
            """{"playlist_id":"D7KyD","playlistName":"Alternate",
                "user":{"name":"Curator"}}""",
        )
        assertEquals("Alternate", parseAudiusPlaylist(json)?.name)
    }

    @Test
    fun unidentifiableProfilesAreIgnored() {
        assertNull(parseAudiusArtist(Json.parseToJsonElement("""{"name":"Nameless"}""")))
        assertNull(parseAudiusPlaylist(Json.parseToJsonElement("""{"id":"7eP5n"}""")))
    }

    @Test
    fun rejectsUnsafeIdsBeforeBuildingApiPaths() {
        assertEquals("7eP5n", validatedAudiusId("7eP5n"))
        assertThrows(IllegalArgumentException::class.java) {
            validatedAudiusId("../tracks")
        }
    }
}
