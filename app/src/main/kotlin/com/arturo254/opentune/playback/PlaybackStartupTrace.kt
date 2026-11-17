package com.arturo254.opentune.playback

import java.net.InetSocketAddress
import java.net.Proxy
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Handshake
import okhttp3.Protocol
import okhttp3.Response
import timber.log.Timber

internal class PlaybackStartupClock(private val now: () -> Long = System::nanoTime) {
    private val starts = LinkedHashMap<String, Long>()
    private val selections = HashSet<String>()

    @Synchronized fun begin(id: String) {
        starts[id] = now()
        selections.add(id)
        while (starts.size > 64) {
            val oldest = starts.keys.first()
            starts.remove(oldest)
            selections.remove(oldest)
        }
    }

    @Synchronized fun transition(id: String) {
        if (!selections.remove(id)) {
            begin(id)
            selections.remove(id)
        }
    }

    @Synchronized fun elapsed(id: String): Long? = starts[id]?.let { (now() - it) / 1_000_000L }
}

internal object PlaybackStartupTrace {
    private val clock = PlaybackStartupClock()
    fun begin(id: String) { clock.begin(id); mark(id, "selection") }
    fun transition(id: String) {
        clock.transition(id)
        mark(id, "transition")
    }
    fun mark(id: String?, stage: String) { record(id, stage, clock.elapsed(id.orEmpty())) }
    fun record(id: String?, stage: String, elapsedMs: Long?) {
        val safe = safePlaybackIdentity(id)
        Timber.tag("PlaybackStartup").i("id=%s provider=%s stage=%s elapsed_ms=%s", safe.first, safe.second, stage, elapsedMs ?: "unavailable")
    }
}

internal fun safePlaybackIdentity(id: String?): Pair<String, String> = when {
    id?.matches(Regex("[A-Za-z0-9_-]{11}")) == true -> id to "youtube"
    id?.matches(Regex("audius:[A-Za-z0-9_-]{1,80}")) == true -> id.substringAfter(':') to "audius"
    id?.startsWith("peertube:") == true && id.substringAfterLast(':').matches(Regex("[A-Fa-f0-9-]{36}")) -> id.substringAfterLast(':') to "peertube"
    id?.startsWith("offline:") == true || id?.startsWith("local:") == true -> "redacted" to "local"
    else -> "redacted" to "unknown"
}

internal class PlaybackHttpTimingListener : EventListener() {
    private var started = 0L
    private var id: String? = null
    private var dns = 0L
    private var connect = 0L
    private var tls = 0L
    override fun callStart(call: Call) {
        started = System.nanoTime()
        val parts = call.request().url.pathSegments
        val operation = parts.indexOfLast { it == "resolve" || it == "stream" || it == "audio" }
        if (operation >= 0) {
            val provider = parts.getOrNull(operation + 1)
            id = when (provider) {
                "youtube" -> parts.getOrNull(operation + 2)
                "audius", "peertube" -> provider + ":" + parts.getOrNull(operation + 2)
                else -> provider?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) }
            }
        }
    }
    override fun dnsStart(call: Call, domainName: String) { dns = System.nanoTime() }
    override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<java.net.InetAddress>) { duration("dns", dns) }
    override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) { connect = System.nanoTime() }
    override fun secureConnectStart(call: Call) { tls = System.nanoTime() }
    override fun secureConnectEnd(call: Call, handshake: Handshake?) { duration("tls", tls) }
    override fun connectEnd(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy, protocol: Protocol?) { duration("connect", connect) }
    override fun connectionAcquired(call: Call, connection: okhttp3.Connection) { duration(if (connect == 0L) "connection_reused" else "connection_acquired", started) }
    override fun responseHeadersEnd(call: Call, response: Response) { duration("http_headers", started) }
    private fun duration(stage: String, start: Long) { PlaybackStartupTrace.record(id, stage, (System.nanoTime() - start) / 1_000_000L) }
}
