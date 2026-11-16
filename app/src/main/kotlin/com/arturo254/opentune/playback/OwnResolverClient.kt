package com.arturo254.opentune.playback

import com.arturo254.opentune.BuildConfig
import com.arturo254.opentune.constants.AudioQuality
import com.arturo254.opentune.innertube.models.response.PlayerResponse
import com.arturo254.opentune.utils.YTPlayerUtils
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import com.arturo254.opentune.utils.awaitText
import com.arturo254.opentune.core.model.SourceVariant
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Cliente del servidor personal "OpenTune Resolver" (carpeta opentune-resolver/).
 *
 * El servidor resuelve el stream con yt-dlp y lo retransmite por /audio/{id}, de modo que el
 * teléfono nunca habla directamente con googlevideo. Si no hay URL/clave configuradas o el
 * servidor no responde, [resolve] devuelve null y la app usa su resolución normal en el dispositivo.
 */
object OwnResolverClient {
    private const val TAG = "OwnResolver"
    private const val UNREACHABLE_BACKOFF_MS = 45_000L

    @Volatile private var base: HttpUrl? = null
    @Volatile private var apiKey: String = ""
    @Volatile private var blockedUntilMs = 0L

    private val http = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS) // yt-dlp puede tardar unos segundos en resolver
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Si los ajustes están vacíos se usan los valores incluidos en el APK (local.properties). */
    fun configure(url: String, key: String) {
        val effectiveUrl = url.ifBlank { BuildConfig.OWN_RESOLVER_URL }
        val effectiveKey = key.ifBlank { BuildConfig.OWN_RESOLVER_KEY }
        val trimmed = effectiveUrl.trim().trimEnd('/')
        base = if (trimmed.isEmpty()) null else {
            val withScheme = if ("://" in trimmed) trimmed else "http://$trimmed"
            withScheme.toHttpUrlOrNull()?.takeIf { it.isHttps && it.username.isEmpty() && it.password.isEmpty() }
        }
        apiKey = effectiveKey.trim()
        blockedUntilMs = 0L
        v2Backoff.reset()
        Timber.tag(TAG).i("Configured=%s", isConfigured())
    }

    fun credentialInterceptor() = ResolverCredentialInterceptor({ base }, { apiKey })

    fun isConfigured(): Boolean = base != null && apiKey.isNotEmpty()

    fun isUsable(): Boolean = isConfigured() && System.currentTimeMillis() >= blockedUntilMs


    private val v2Backoff = ResolverProviderBackoff()

    private fun v2Request(path: String, parameters: Map<String,String>): Request? {
        val root = base ?: return null
        if (!isConfigured() || v2Backoff.isBlocked(resolverProviderScope(path, parameters))) return null
        val url = root.newBuilder().addPathSegments("v2/" + path)
            .apply { parameters.forEach { (name,value) -> addQueryParameter(name,value) } }.build()
        return Request.Builder().url(url).header("X-API-Key",apiKey).build()
    }

    private suspend fun getV2(path: String, parameters: Map<String,String>): String? {
        val request = v2Request(path,parameters) ?: return null
        return try { http.newCall(request).awaitText() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            Timber.tag(TAG).w("stage=resolver operation=%s category=%s", path.substringBefore('/'), resolverFailureCategory(error))
            v2Backoff.block(resolverProviderScope(path, parameters))
            null
        }
    }

    suspend fun searchV2(provider: String, query: String, kind: String, limit: Int): List<SourceVariant>? {
        if (query.trim().length < 2) return null
        val raw = getV2("search",mapOf("query" to query,"provider" to provider,"kind" to kind,"limit" to limit.coerceIn(1,25).toString())) ?: return null
        return runCatching { parseResolverSearch(raw,provider) }.getOrNull()
    }

    private fun streamV2Url(provider: String, id: String, kind: String, quality: String): String? =
        base?.newBuilder()?.addPathSegments("v2/stream")?.addPathSegment(provider)?.addPathSegment(id)
            ?.addQueryParameter("kind",kind)?.addQueryParameter("quality",quality)?.build()?.toString()

    suspend fun resolveV2(provider: String, id: String, kind: String = "audio", quality: String = "auto"): ResolverStream? {
        if (!id.matches(Regex("[A-Za-z0-9_-]{1,80}")) || provider !in setOf("youtube","audius")) return null
        val raw = getV2("resolve/$provider/$id",mapOf("kind" to kind,"quality" to quality)) ?: return null
        val url = streamV2Url(provider,id,kind,quality) ?: return null
        return runCatching { parseResolverStream(raw,provider,id,kind,url) }.getOrNull()
    }

    private fun qualityParam(q: AudioQuality) = when (q) {
        AudioQuality.AUTO -> "auto"
        AudioQuality.LOW -> "low"
        AudioQuality.HIGH -> "high"
        AudioQuality.HIGHEST -> "highest"
    }

    suspend fun resolve(
        videoId: String,
        quality: AudioQuality,
        avoidCodecs: Set<String> = emptySet(),
    ): YTPlayerUtils.PlaybackData? {
        val root = base ?: return null
        if (!isUsable()) return null
        val q = qualityParam(quality)
        resolveV2("youtube",videoId,"audio",q)?.takeIf { avoidCodecs.isEmpty() || it.codecs != null }?.let { resolved ->
            if (avoidCodecs.none { resolved.codecs?.contains(it,ignoreCase=true) == true }) {
                val format = PlayerResponse.StreamingData.Format(
                    itag = -1, url = resolved.url,
                    mimeType = resolved.mimeType + (resolved.codecs?.let { "; codecs=\"$it\"" } ?: ""),
                    bitrate = resolved.bitrate ?: androidx.media3.common.Format.NO_VALUE,
                    width = null, height = null, contentLength = resolved.length, quality = q,
                    fps = null, qualityLabel = null, averageBitrate = resolved.bitrate,
                    audioQuality = null, approxDurationMs = null, audioSampleRate = null, audioChannels = null,
                    loudnessDb = null, lastModified = null, signatureCipher = null, cipher = null)

                return YTPlayerUtils.PlaybackData(null,null,null,format,resolved.url,resolved.expires)
            }
        }

        val resolveUrl = root.newBuilder()
            .addPathSegments("resolve/$videoId")
            .addQueryParameter("quality", q)
            .build()

        val json = try {
            JSONObject(http.newCall(Request.Builder().url(resolveUrl).header("X-API-Key", apiKey).build()).awaitText())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: IOException) {
            Timber.tag(TAG).w("stage=resolver operation=legacy category=%s", resolverFailureCategory(e))
            blockedUntilMs = System.currentTimeMillis() + UNREACHABLE_BACKOFF_MS
            return null
        } catch (e: Exception) {
            Timber.tag(TAG).w("Invalid server response type=%s", e.javaClass.simpleName)
            return null
        }

        val codecs = json.optString("codecs")
        if (avoidCodecs.any { codecs.contains(it, ignoreCase = true) }) return null

        val audioUrl = root.newBuilder()
            .addPathSegments("audio/$videoId")
            .addQueryParameter("quality", q)
            .build()
            .toString()

        val sampleRate = json.optInt("sampleRate", 0).takeIf { it > 0 }
        val format = PlayerResponse.StreamingData.Format(
            itag = json.optInt("itag"),
            url = audioUrl,
            // "codecs=" es necesario: StreamResolver.persistResolvedFormat lo separa por ese texto
            mimeType = "${json.getString("mimeType")}; codecs=\"$codecs\"",
            bitrate = json.getInt("bitrate"),
            width = null,
            height = null,
            contentLength = json.getLong("contentLength"),
            quality = "tiny",
            fps = null,
            qualityLabel = null,
            averageBitrate = json.getInt("bitrate"),
            audioQuality = null,
            approxDurationMs = json.optInt("durationSeconds", 0).takeIf { it > 0 }?.let { (it * 1000L).toString() },
            audioSampleRate = sampleRate,
            audioChannels = 2,
            loudnessDb = null,
            lastModified = null,
            signatureCipher = null,
            cipher = null,
        )

        Timber.tag(TAG).i("Resolved $videoId via own server: ${format.mimeType} @ ${format.bitrate}bps")
        return YTPlayerUtils.PlaybackData(
            audioConfig = null,
            videoDetails = null,
            playbackTracking = null,
            format = format,
            streamUrl = audioUrl,
            streamExpiresInSeconds = json.optInt("expiresInSeconds", 3600),
        )
    }
}

internal fun resolverFailureCategory(error: Exception): String =
    Regex("^HTTP ([1-5][0-9]{2})$").matchEntire(error.message.orEmpty())
        ?.groupValues?.get(1)?.let { "http_$it" }
        ?: if (error is java.net.SocketTimeoutException || error is java.io.InterruptedIOException) "timeout" else "transport"

internal fun resolverProviderScope(path: String, parameters: Map<String, String>): String =
    (if (path.startsWith("resolve/")) path.split('/').getOrNull(1) else parameters["provider"])
        ?.let { if (it == "youtube_music") "youtube" else it } ?: "all"

internal class ResolverProviderBackoff(private val now: () -> Long = System::currentTimeMillis) {
    private val blockedUntil = java.util.concurrent.ConcurrentHashMap<String, Long>()
    fun isBlocked(provider: String): Boolean = now() < (blockedUntil[provider] ?: 0L)
    fun block(provider: String) { blockedUntil[provider] = now() + 15_000L }
    fun reset() { blockedUntil.clear() }
}
