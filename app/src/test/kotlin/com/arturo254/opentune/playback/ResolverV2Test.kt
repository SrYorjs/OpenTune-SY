package com.arturo254.opentune.playback

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ResolverV2Test {
    @Test fun actualV2SearchContractDoesNotCreateAnotherYoutubeCatalog() {
        val raw = """{"results":[{"provider":"youtube","id":"abcdefghijk","title":"Exact title","artist":"Exact artist","durationSeconds":123.5,"kind":"video","thumbnail":null},{"provider":"audius","id":"abc","title":"Other","artist":"Other","kind":"audio"}],"partial":false}"""
        val rows = parseResolverSearch(raw,"youtube")!!
        assertEquals(1,rows.size)
        assertEquals("youtube_music",rows.single().provider.value)
        assertEquals(123500L,rows.single().durationMs)
        assertTrue(parseResolverSearch("""{"results":[{"provider":"audius","id":"abc","title":"Title"}],"partial":false}""","audius")!!.isEmpty())
        assertNull(parseResolverSearch("""{"results":[],"partial":true}""","audius"))
    }
    @Test fun resolveValidatesIdentityAndKeepsUnavailableDataExplicit() {
        val raw = """{"provider":"audius","id":"abc","kind":"audio","mimeType":"audio/mpeg","bitrate":null,"contentLength":null,"expiresInSeconds":240,"streamPath":"https://attacker.invalid"}"""
        val stream = parseResolverStream(raw,"audius","abc","audio","https://resolver.invalid/v2/stream/audius/abc")!!
        assertEquals("https://resolver.invalid/v2/stream/audius/abc",stream.url)
        assertNull(stream.width)
        assertNull(stream.bitrate)
        assertNull(parseResolverStream(raw,"audius","different","audio","https://resolver.invalid"))
        assertNull(parseResolverStream(raw,"audius","abc","video","https://resolver.invalid"))
    }
    @Test fun fallbackIsConsumedOnceAndFailureDoesNotLoop() = runBlocking {
        var calls = 0
        StreamFallbacks.register("test:a") { calls++; null }
        assertTrue(StreamFallbacks.available("test:a"))
        assertNull(StreamFallbacks.resolve("test:a"))
        assertNull(StreamFallbacks.resolve("test:a"))
        assertEquals(1,calls)
        assertFalse(StreamFallbacks.available("test:a"))
    }
}
