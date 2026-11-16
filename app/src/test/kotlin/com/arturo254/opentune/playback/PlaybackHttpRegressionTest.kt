package com.arturo254.opentune.playback

import android.app.Application
import android.net.Uri
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.*
import androidx.media3.datasource.cache.*
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.extractor.DefaultExtractorInput
import androidx.media3.extractor.mp3.Mp3Extractor
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.exoplayer.upstream.Loader
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import com.arturo254.opentune.utils.awaitText
import com.sun.net.httpserver.HttpsConfigurator
import com.sun.net.httpserver.HttpsServer
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE, application = Application::class)
@ConscryptMode(ConscryptMode.Mode.OFF)
class PlaybackHttpRegressionTest {
    private val key = "unit-only-key"
    private val id = "Mcj75l2gJcY"
    private val payload = ByteArray(1_048_707) { (it % 251).toByte() }
    private lateinit var server: HttpsServer
    private lateinit var client: OkHttpClient
    private lateinit var root: HttpUrl
    private val executor = Executors.newFixedThreadPool(2)
    private val requests = ConcurrentLinkedQueue<Triple<String, String?, String?>>()
    private var ignoreRange = false
    private var failureStatus: Int? = null

    @Before fun start() {
        val certificate = HeldCertificate.Builder().commonName("unit-local")
            .addSubjectAlternativeName("localhost").addSubjectAlternativeName("127.0.0.1").build()
        val serverTls = HandshakeCertificates.Builder().heldCertificate(certificate).build()
        val clientTls = HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
        server = HttpsServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.httpsConfigurator = HttpsConfigurator(serverTls.sslContext())
        server.executor = executor
        root = "https://localhost:${server.address.port}/private/".toHttpUrl()
        client = playbackHttpClient(null, ResolverCredentialInterceptor({ root }, { key })).newBuilder()
            .sslSocketFactory(clientTls.sslSocketFactory(), clientTls.trustManager).build()
        server.createContext("/") { exchange ->
            val path = exchange.requestURI.path
            val authentication = exchange.requestHeaders.getFirst("X-API-Key")
            val range = exchange.requestHeaders.getFirst("Range")
            requests.add(Triple(path, authentication, range))
            if (path.endsWith("redirect")) {
                exchange.responseHeaders.add("Location", "https://127.0.0.1:${server.address.port}/external")
                exchange.sendResponseHeaders(302, -1)
            } else if (path.endsWith("slow")) {
                Thread.sleep(5000)
                exchange.sendResponseHeaders(200, -1)
            } else if (path.contains("resolve")) {
                val body = """{"provider":"youtube","id":"$id","kind":"audio","mimeType":"audio/mp4","bitrate":96000,"contentLength":${payload.size},"expiresInSeconds":240}""".toByteArray()
                exchange.sendResponseHeaders(if (authentication == key) 200 else 401, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            } else if (failureStatus != null) {
                exchange.sendResponseHeaders(failureStatus!!, -1)
            } else if (path.contains("stream") && authentication != key) {
                exchange.sendResponseHeaders(401, -1)
            } else {
                val match = range?.let { Regex("bytes=(\\d+)-(\\d*)").matchEntire(it) }
                val first = if (ignoreRange) 0 else match?.groupValues?.get(1)?.toInt() ?: 0
                val last = if (ignoreRange) payload.lastIndex else match?.groupValues?.get(2)?.toIntOrNull()?.coerceAtMost(payload.lastIndex) ?: payload.lastIndex
                exchange.responseHeaders.add("Content-Type", "audio/mp4")
                if (first >= payload.size) {
                    exchange.responseHeaders.add("Content-Range", "bytes */${payload.size}")
                    exchange.sendResponseHeaders(416, -1)
                } else {
                    val status = if (!ignoreRange && match != null) 206 else 200
                    if (status == 206) exchange.responseHeaders.add("Content-Range", "bytes $first-$last/${payload.size}")
                    exchange.sendResponseHeaders(status, (last - first + 1).toLong())
                    exchange.responseBody.use { it.write(payload, first, last - first + 1) }
                }
            }
            exchange.close()
        }
        server.start()
    }

    @After fun stop() {
        server.stop(0)
        executor.shutdownNow()
        client.connectionPool.evictAll()
        client.dispatcher.executorService.shutdownNow()
    }

    private fun uri(path: String = "v2/stream/youtube/$id?kind=audio") = Uri.parse(root.resolve(path).toString())
    private fun source() = OkHttpDataSource.Factory(client).createDataSource()
    private fun read(source: DataSource, spec: DataSpec): Pair<Long, ByteArray> {
        try {
            val length = source.open(spec)
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = source.read(buffer, 0, buffer.size)
                if (count == C.RESULT_END_OF_INPUT) break
                output.write(buffer, 0, count)
            }
            return length to output.toByteArray()
        } finally { source.close() }
    }

    @Test fun actualStreamClientAuthenticatesResolveAndStreamAndPreservesWholeFile() = runBlocking {
        val raw = client.newCall(Request.Builder().url(root.resolve("v2/resolve/youtube/$id?kind=audio")!!).build()).awaitText()
        assertNotNull(parseResolverStream(raw, "youtube", id, "audio", uri().toString()))
        val original = DataSpec.Builder().setUri(id).setKey(id).build()
        val former = original.withUri(uri()).subrange(0, 512 * 1024L)
        assertEquals(512 * 1024, read(source(), former).second.size)
        val resolved = StreamRequestPolicy.resolveDataSpec(original, uri())
        val (length, bytes) = read(source(), resolved)
        assertEquals(payload.size.toLong(), length)
        assertArrayEquals(payload, bytes)
        assertTrue(requests.filter { it.first.contains("resolve") || it.first.contains("stream") }.all { it.second == key })
    }

    @Test fun partialReadsAndIgnoredRangesHonorPositionsAndLength() {
        val spec = DataSpec.Builder().setUri(uri()).setKey(id).setPosition(1031).setLength(1024).build()
        for (ignored in listOf(false, true)) {
            ignoreRange = ignored
            val (length, bytes) = read(source(), spec)
            assertEquals(1024L, length)
            assertArrayEquals(payload.copyOfRange(1031, 2055), bytes)
            assertEquals("bytes=1031-2054", requests.last().third)
        }
    }

    @Test fun endOfFile416IsEmptyButOutOfBounds416IsNotIgnored() {
        assertEquals(0L, read(source(), DataSpec.Builder().setUri(uri()).setPosition(payload.size.toLong()).build()).first)
        try {
            read(source(), DataSpec.Builder().setUri(uri()).setKey(id).setPosition(payload.size + 1L).build())
            fail("416 ignored")
        } catch (error: HttpDataSource.InvalidResponseCodeException) {
            assertEquals(416, error.responseCode)
            assertEquals(416, playbackFailureDiagnostic(id, error).httpCode)
        }
    }

    @Test fun redirectsNeverReceiveResolverAuthenticationOutsideOrigin() {
        read(source(), DataSpec.Builder().setUri(uri("redirect")).build())
        assertEquals(key, requests.first().second)
        assertNull(requests.last().second)
        assertEquals("/external", requests.last().first)
    }

    @Test fun httpFailuresHaveSafeRootStatusAndSelectedId() {
        failureStatus = 403
        try {
            read(PlaybackDiagnosticDataSource(source()), DataSpec.Builder().setUri(uri()).setKey(id).build())
            fail("403 ignored")
        } catch (error: HttpDataSource.InvalidResponseCodeException) {
            val diagnostic = playbackFailureDiagnostic(id, error, PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS)
            assertEquals(id, diagnostic.mediaId)
            assertEquals("youtube", diagnostic.provider)
            assertEquals(403, diagnostic.httpCode)
            assertEquals("http_connection", diagnostic.stage)
            assertNotNull(diagnostic.elapsedMs)
            assertFalse(diagnostic.toString().contains(key))
            assertFalse(diagnostic.toString().contains(root.toString()))
        }
    }

    @Test fun cancelledMetadataRequestAbortsBeforeFallbackAndFollowingTrackWorks() = runBlocking {
        val call = client.newCall(Request.Builder().url(root.resolve("slow")!!).build())
        val result = resolvePlaybackStages(listOf(
            ResolutionStage("resolver_v2", 100) { call.awaitText() },
            ResolutionStage("innertube", 1000) { "next" },
        ))
        assertEquals("next", result)
        assertTrue(call.isCanceled())
    }

    @Test fun cacheRetainsCompleteResourceAcrossSignedUrlRefreshAndReopen() {
        val folder = java.nio.file.Files.createTempDirectory("media-cache-").toFile()
        var player = SimpleCache(folder.resolve("player"), LeastRecentlyUsedCacheEvictor(4L * payload.size))
        var downloads = SimpleCache(folder.resolve("downloads"), NoOpCacheEvictor())
        try {
            val factory = playbackAudioCacheFactory(player, downloads, OkHttpDataSource.Factory(client))
            val spec = DataSpec.Builder().setUri(uri()).setKey(id).build()
            assertEquals(512 * 1024, read(factory.createDataSource(), spec.subrange(0, 512 * 1024L)).second.size)
            assertArrayEquals(payload, read(factory.createDataSource(), spec).second)
            assertEquals("bytes=524288-", requests.last().third)
            val count = requests.size
            player.release()
            downloads.release()
            player = SimpleCache(folder.resolve("player"), LeastRecentlyUsedCacheEvictor(4L * payload.size))
            downloads = SimpleCache(folder.resolve("downloads"), NoOpCacheEvictor())
            val reopened = playbackAudioCacheFactory(player, downloads, OkHttpDataSource.Factory(client))
            failureStatus = 403
            assertArrayEquals(payload, read(reopened.createDataSource(), spec.withUri(uri("v2/stream/youtube/$id?expired=fixture"))).second)
            assertEquals(count, requests.size)
            failureStatus = null
            val another = DataSpec.Builder().setUri(uri()).setKey("audius:$id").build()
            assertArrayEquals(payload, read(reopened.createDataSource(), another).second)
            assertTrue(requests.size > count)
        } finally { player.release(); downloads.release(); folder.deleteRecursively() }
    }

    @Test fun realExtractorsRecognizeGeneratedAacAndMp3WithoutDroppingExistingContainers() {
        for ((file, type) in listOf("tone.m4a" to Mp4Extractor::class.java, "tone.mp3" to Mp3Extractor::class.java)) {
            val data = javaClass.getResourceAsStream("/playback/$file")!!.use { it.readBytes() }
            val source = ByteArrayDataSource(data)
            source.open(DataSpec(Uri.EMPTY))
            try {
                val input = DefaultExtractorInput(source, 0, data.size.toLong())
                val matching = playbackExtractorsFactory().createExtractors().firstOrNull { extractor ->
                    input.resetPeekPosition()
                    try { extractor.sniff(input) } catch (_: java.io.EOFException) { false }
                }
                assertTrue(file, type.isInstance(matching))
            } finally { source.close() }
        }
    }

    @Test fun media3LoaderReceivesAnIoTimeoutInsteadOfUnexpectedCancellation() {
        val mapped = PlaybackResolutionDataSourceException(id, PlaybackResolutionTimeoutException(), DataSpec.Builder().setUri(id).setKey(id).build(), 20_000)
        val seen = AtomicReference<java.io.IOException>()
        val loader = Loader("resolution-regression")
        val load = object : Loader.Loadable {
            override fun cancelLoad() = Unit
            override fun load() { throw mapped }
        }
        loader.startLoading(load, object : Loader.Callback<Loader.Loadable> {
            override fun onLoadCompleted(loadable: Loader.Loadable, elapsedRealtimeMs: Long, loadDurationMs: Long) = Unit
            override fun onLoadCanceled(loadable: Loader.Loadable, elapsedRealtimeMs: Long, loadDurationMs: Long, released: Boolean) = Unit
            override fun onLoadError(loadable: Loader.Loadable, elapsedRealtimeMs: Long, loadDurationMs: Long, error: java.io.IOException, errorCount: Int): Loader.LoadErrorAction {
                seen.set(error)
                return Loader.DONT_RETRY_FATAL
            }
        }, 0)
        try {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (seen.get() == null && System.nanoTime() < deadline) {
                shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(10)
            }
            assertSame(mapped, seen.get())
            assertEquals(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT, mapped.reason)
            val loadInfo = LoadErrorHandlingPolicy.LoadErrorInfo(LoadEventInfo(1L, mapped.dataSpec, 0L), MediaLoadData(C.DATA_TYPE_MEDIA), mapped, 1)
            assertEquals(C.TIME_UNSET, PlaybackLoadErrorHandlingPolicy().getRetryDelayMsFor(loadInfo))
            val diagnostic = playbackFailureDiagnostic(id, mapped)
            assertEquals("timeout", diagnostic.category)
            assertFalse(diagnostic.cancelled)
            assertTrue(playbackFailureDiagnostic("-9l5t3MWss8", mapped).stale)
        } finally { loader.release() }
    }
}
