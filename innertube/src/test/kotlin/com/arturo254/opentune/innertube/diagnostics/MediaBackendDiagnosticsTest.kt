package com.arturo254.opentune.innertube.diagnostics

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaBackendDiagnosticsTest {
    @Test
    fun validatesOnlyPublicVideoIdentifiers() {
        assertEquals("jNQXAC9IVRw", validatedPublicVideoId(" jNQXAC9IVRw "))
        assertThrows(IllegalArgumentException::class.java) {
            validatedPublicVideoId("https://youtube.com/watch?v=jNQXAC9IVRw")
        }
        assertThrows(IllegalArgumentException::class.java) {
            validatedPublicVideoId("not-an-id")
        }
    }

    @Test
    fun pipedRequiresHttpsAndRefusesEmbeddedCredentials() {
        assertEquals(
            "https://pipedapi.kavin.rocks",
            validatedPipedBaseUrl("https://pipedapi.kavin.rocks/"),
        )
        for (invalid in listOf(
            "http://pipedapi.kavin.rocks",
            "https://user:secret@pipedapi.kavin.rocks",
            "https://pipedapi.kavin.rocks/?token=secret",
            "https://pipedapi.kavin.rocks/path/../internal",
        )) {
            assertThrows(IllegalArgumentException::class.java) {
                validatedPipedBaseUrl(invalid)
            }
        }
    }

    @Test
    fun streamParserKeepsOnlySafeMetadataWithoutStreamUrls() {
        val root = Json.parseToJsonElement(
            """{"title":"Public Video","duration":123,
                "audioStreams":[
                  {"format":"M4A","codec":"mp4a.40.2","mimeType":"audio/mp4",
                   "url":"https://private.example/stream?token=SECRET"},
                  {"format":"WEBMA","codec":"opus","mimeType":"audio/webm",
                   "url":"https://private.example/stream?cookie=SENSITIVE"}
                ]}""",
        ).jsonObject
        val result = parsePipedStreamResponse("jNQXAC9IVRw", root)

        assertEquals("Piped", result.engine)
        assertTrue(result.hasMetadata)
        assertEquals(2, result.audioFormats)
        assertEquals("OK", result.status)
        assertEquals(2, result.formats.size)
        assertTrue(result.formats.first().contains("M4A"))
        assertFalse(result.toString().contains("SECRET"))
        assertFalse(result.toString().contains("SENSITIVE"))
        assertFalse(result.toString().contains("private.example"))
    }

    @Test
    fun parserReportsMetadataWithoutAudioAsSeparateFailure() {
        val root = Json.parseToJsonElement(
            """{"title":"Public Video","audioStreams":[]}""",
        ).jsonObject
        val result = parsePipedStreamResponse("jNQXAC9IVRw", root)
        assertTrue(result.hasMetadata)
        assertEquals(0, result.audioFormats)
        assertEquals("SIN_FORMATOS_AUDIO", result.status)
    }

    @Test
    fun searchParserHandlesMusicFiltersAndInvalidResponses() {
        assertEquals(
            listOf("music_songs", "music_artists", "music_albums", "music_playlists"),
            pipedMusicFilters,
        )
        val page = Json.parseToJsonElement(
            """{"items":[{"title":"A"},{"title":"B"}],"nextpage":"sample"}""",
        ).jsonObject
        assertEquals(2, parsePipedSearchResponse("music_songs", page).results)
        assertEquals("OK", parsePipedSearchResponse("music_songs", page).status)
        val invalid = Json.parseToJsonElement("""{"message":"unavailable"}""").jsonObject
        assertEquals(
            "RESPUESTA_INESPERADA",
            parsePipedSearchResponse("music_songs", invalid).status,
        )
    }
}
