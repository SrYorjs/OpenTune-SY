package com.arturo254.opentune.sources.navidrome

import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.sources.playback.SourcePlaybackRoute
import com.arturo254.opentune.sources.playback.playbackRoute
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class NavidromeSourceTest {
    @Test
    fun flacSearchResultPreservesOriginalQualityAndProvider() {
        val payload = Json.parseToJsonElement(
            """{"id":"f93d","title":"Galax Symphony","artist":"Composer",
                "album":"Night Sky","albumId":"album-7","duration":247,
                "coverArt":"cover-7","suffix":"flac","contentType":"audio/flac",
                "bitRate":920,"size":27188271}""",
        )
        val song = parseNavidromeSong(payload)!!
        val variant = song.toVariant("https://example.com/cover.jpg")

        assertEquals(ProviderId.NAVIDROME, variant.provider)
        assertEquals(SourcePlaybackRoute.NAVIDROME_DIRECT, variant.playbackRoute())
        assertEquals(247_000L, variant.durationMs)
        assertEquals("album-7", variant.albumId)
        assertEquals("audio/flac", variant.quality?.mimeType)
        assertEquals("FLAC", variant.quality?.codec)
        assertEquals(920_000, variant.quality?.bitrateBitsPerSecond)
        assertEquals(true, variant.quality?.lossless)
        assertTrue(variant.downloadable == true)
    }

    @Test
    fun mp3AndUnknownBitrateDoNotAdvertiseLosslessAudio() {
        val payload = Json.parseToJsonElement(
            """{"id":"2","title":"Song","artist":"Band","suffix":"mp3"}""",
        )
        val variant = parseNavidromeSong(payload)!!.toVariant(null)
        assertEquals("audio/mpeg", variant.quality?.mimeType)
        assertEquals(false, variant.quality?.lossless)
        assertEquals(null, variant.quality?.bitrateBitsPerSecond)
    }

    @Test
    fun malformedTracksFailClosed() {
        val missingTitle = Json.parseToJsonElement("""{"id":"2","artist":"Band"}""")
        assertEquals(null, parseNavidromeSong(missingTitle))
    }

    @Test
    fun connectionRequiresHttpsAndRejectsEmbeddedSecrets() {
        assertEquals("https://music.example.com", validateNavidromeUrl("https://music.example.com/"))
        assertEquals("https://music.example.com/music", validateNavidromeUrl("https://music.example.com/music"))
        assertThrows(IllegalArgumentException::class.java) {
            validateNavidromeUrl("http://music.example.com")
        }
        assertThrows(IllegalArgumentException::class.java) {
            validateNavidromeUrl("https://user:password@music.example.com")
        }
        assertThrows(IllegalArgumentException::class.java) {
            validateNavidromeUrl("https://music.example.com?password=secret")
        }
    }
}
