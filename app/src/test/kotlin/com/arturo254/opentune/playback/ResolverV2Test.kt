package com.arturo254.opentune.playback

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ResolverV2Test {
    @Test fun songSpecificErrorsDoNotBlockTheFollowingSong() {
        assertFalse(resolverShouldBackoff(java.io.IOException("HTTP 502")))
        assertFalse(resolverShouldBackoff(java.io.IOException("HTTP 403")))
        assertFalse(resolverShouldBackoff(java.net.SocketTimeoutException()))
        assertTrue(resolverShouldBackoff(java.net.ConnectException()))
        assertTrue(resolverShouldBackoff(java.io.IOException("HTTP 429")))
    }
    @Test fun youtubeFailuresDoNotBlockIndependentProvidersAndExpire() {
        var now = 1000L
        val backoff = ResolverProviderBackoff { now }
        val youtube = resolverProviderScope("resolve/youtube/Mcj75l2gJcY", emptyMap())
        assertEquals("youtube", youtube)
        backoff.block(youtube)
        assertTrue(backoff.isBlocked(resolverProviderScope("search", mapOf("provider" to "youtube"))))
        assertTrue(backoff.isBlocked(resolverProviderScope("search", mapOf("provider" to "youtube_music"))))
        assertFalse(backoff.isBlocked(resolverProviderScope("resolve/audius/fixture", emptyMap())))
        assertFalse(backoff.isBlocked(resolverProviderScope("search", mapOf("provider" to "peertube"))))
        now += 15_000L
        assertFalse(backoff.isBlocked(youtube))
        backoff.block("audius")
        backoff.reset()
        assertFalse(backoff.isBlocked("audius"))
    }
    @Test fun failureDiagnosticsExposeOnlyStatusOrCategory() {
        for (code in listOf(401, 403, 404, 429, 502)) {
            assertEquals("http_$code", resolverFailureCategory(java.io.IOException("HTTP $code")))
        }
        assertEquals("timeout", resolverFailureCategory(java.net.SocketTimeoutException("not-for-logs")))
        assertEquals("transport", resolverFailureCategory(java.io.IOException("https://invalid.test/?token=not-for-logs")))
    }
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
