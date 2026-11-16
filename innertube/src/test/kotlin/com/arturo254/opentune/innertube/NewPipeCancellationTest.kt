package com.arturo254.opentune.innertube

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.*
import org.junit.Test

class NewPipeCancellationTest {
    @Test fun cancellingExtractionCancelsTheActualCallDuringBodyRead() = runBlocking {
        val ready = CountDownLatch(1)
        val release = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.executor = executor
        server.createContext("/") { exchange ->
            exchange.sendResponseHeaders(200, 1024)
            exchange.responseBody.write(1)
            exchange.responseBody.flush()
            ready.countDown()
            release.await(5, TimeUnit.SECONDS)
            exchange.close()
        }
        server.start()
        val client = OkHttpClient()
        val call = client.newCall(Request.Builder().url("http://127.0.0.1:${server.address.port}/").build())
        try {
            val extraction = launch(Dispatchers.IO) {
                NewPipeUtils.cancellable { executeNewPipeCall(call) { it.body.bytes() } }
            }
            assertTrue(withContext(Dispatchers.IO) { ready.await(3, TimeUnit.SECONDS) })
            withTimeout(2000) { extraction.cancelAndJoin() }
            assertTrue(call.isCanceled())
            assertEquals("next", NewPipeUtils.cancellable { "next" })
        } finally {
            release.countDown()
            server.stop(0)
            executor.shutdownNow()
            client.connectionPool.evictAll()
            client.dispatcher.executorService.shutdownNow()
        }
    }
}
