package com.arturo254.opentune.playback

import com.arturo254.opentune.utils.awaitText
import com.arturo254.opentune.ui.original.utils.VideoTrack
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.lang.reflect.Proxy
import java.io.IOException
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class ProviderNetworkTest {
    private fun authenticated(request: Request, root: String): Request {
        var sent: Request? = null
        val chain = Proxy.newProxyInstance(Interceptor.Chain::class.java.classLoader, arrayOf(Interceptor.Chain::class.java)) { _, method, args ->
            when (method.name) {
                "request" -> request
                "proceed" -> {
                    sent = args!![0] as Request
                    Response.Builder().request(sent!!).protocol(Protocol.HTTP_1_1).code(200).message("OK").body("".toResponseBody()).build()
                }
                else -> throw UnsupportedOperationException(method.name)
            }
        } as Interceptor.Chain
        ResolverCredentialInterceptor({ root.toHttpUrl() }, { "test-only-key" }).intercept(chain).close()
        return sent!!
    }

    @Test fun resolverKeyIsHeaderOnlyAndRemovedAtEveryExternalHop() {
        val root = "https://resolver.example/private/"
        val local = authenticated(Request.Builder().url(root + "audio/id?key=legacy-value&quality=high").build(), root)
        assertEquals("test-only-key",local.header("X-API-Key"))
        assertNull(local.url.queryParameter("key"))
        assertEquals("high",local.url.queryParameter("quality"))
        for (url in listOf("https://cdn.example/audio", "https://resolver.example.attacker.test/private/audio/id", "http://resolver.example/private/audio/id", "https://resolver.example:8443/private/audio/id")) {
            val sent = authenticated(local.newBuilder().url(url).build(), root)
            assertNull(url, sent.header("X-API-Key"))
        }
        val redirect = authenticated(local.newBuilder().url("https://cdn.example/file?key=test-only-key&variant=720").build(),root)
        assertNull(redirect.url.queryParameter("key"))
        assertEquals("720",redirect.url.queryParameter("variant"))
    }

    @Test fun actualHttpResponsesAndCancellationPropagateWithoutExposingUrls() = runBlocking {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
        server.createContext("/") { exchange ->
            val code = exchange.requestURI.path.substringAfter('/').toIntOrNull() ?: 200
            val data = "response".toByteArray()
            exchange.sendResponseHeaders(code,data.size.toLong())
            exchange.responseBody.use { it.write(data) }
        }
        server.start()
        val http = OkHttpClient()
        try {
            for (code in listOf(200,206)) {
                val request = Request.Builder().url("http://127.0.0.1:${server.address.port}/$code").header("Range","bytes=0-7").build()
                assertEquals("response",http.newCall(request).awaitText())
            }
            for (code in listOf(401,403,404,429,502)) {
                try {
                    http.newCall(Request.Builder().url("http://127.0.0.1:${server.address.port}/$code").build()).awaitText()
                    fail("HTTP failure ignored")
                } catch (e: IOException) { assertEquals("HTTP $code",e.message) }
            }
        } finally { server.stop(0); http.connectionPool.evictAll(); http.dispatcher.executorService.shutdown() }
    }

    @Test fun videoKeysSurviveSignedUrlRefreshAndIsolateProviderAndVariant() {
        val track = VideoTrack(136,"https://cdn.example/file?expire=1","video/mp4","avc1",1280,720,30,1000000,null,null,10000)
        assertEquals(videoCacheKey("youtube:abc",track),videoCacheKey("youtube:abc",track.copy(url="https://cdn.example/new?expire=2")))
        assertNotEquals(videoCacheKey("youtube:abc",track),videoCacheKey("peertube:abc",track))
        assertNotEquals(videoCacheKey("youtube:abc",track),videoCacheKey("youtube:abc",track.copy(itag=137,height=1080)))
    }

    @Test fun cancellingOrTimingOutAbortsTheUnderlyingHttpCall() = runBlocking {
        val executor = java.util.concurrent.Executors.newCachedThreadPool()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1",0),0)
        server.executor = executor
        server.createContext("/") { exchange ->
            try { Thread.sleep(1000) } catch (_: InterruptedException) { }
            runCatching { exchange.sendResponseHeaders(200,1); exchange.responseBody.use { it.write(1) } }
            exchange.close()
        }
        server.start()
        val http = OkHttpClient.Builder().callTimeout(150,java.util.concurrent.TimeUnit.MILLISECONDS).build()
        try {
            val request = Request.Builder().url("http://127.0.0.1:${server.address.port}/").build()
            val cancelled = http.newCall(request)
            val job = launch { cancelled.awaitText() }
            delay(30)
            job.cancelAndJoin()
            assertTrue(cancelled.isCanceled())
            try { http.newCall(request).awaitText(); fail("timeout ignored") } catch (_: IOException) { }
        } finally { server.stop(0); executor.shutdownNow(); http.connectionPool.evictAll(); http.dispatcher.executorService.shutdown() }
    }
}
