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
            withScheme.toHttpUrlOrNull()
        }
        apiKey = effectiveKey.trim()
        blockedUntilMs = 0L
        Timber.tag(TAG).i("Configured: ${if (isConfigured()) base.toString() else "disabled"}")
    }

    fun isConfigured(): Boolean = base != null && apiKey.isNotEmpty()

    fun isUsable(): Boolean = isConfigured() && System.currentTimeMillis() >= blockedUntilMs

    private fun qualityParam(q: AudioQuality) = when (q) {
        AudioQuality.AUTO -> "auto"
        AudioQuality.LOW -> "low"
        AudioQuality.HIGH -> "high"
        AudioQuality.HIGHEST -> "highest"
    }

    /** Bloqueante: llamar desde Dispatchers.IO. Devuelve null si no se pudo usar el servidor. */
    fun resolve(
        videoId: String,
        quality: AudioQuality,
        avoidCodecs: Set<String> = emptySet(),
    ): YTPlayerUtils.PlaybackData? {
        val root = base ?: return null
        if (!isUsable()) return null
        val q = qualityParam(quality)

        val resolveUrl = root.newBuilder()
            .addPathSegments("resolve/$videoId")
            .addQueryParameter("quality", q)
            .build()

        val json = try {
            http.newCall(Request.Builder().url(resolveUrl).header("X-API-Key", apiKey).build())
                .execute().use { resp ->
                    if (!resp.isSuccessful) {
                        Timber.tag(TAG).w("resolve $videoId -> HTTP ${resp.code}")
                        return null
                    }
                    JSONObject(resp.body.string())
                }
        } catch (e: IOException) {
            Timber.tag(TAG).w(e, "Server unreachable, pausing for ${UNREACHABLE_BACKOFF_MS / 1000}s")
            blockedUntilMs = System.currentTimeMillis() + UNREACHABLE_BACKOFF_MS
            return null
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Bad response from server")
            return null
        }

        val codecs = json.optString("codecs")
        if (avoidCodecs.any { codecs.contains(it, ignoreCase = true) }) return null

        val audioUrl = root.newBuilder()
            .addPathSegments("audio/$videoId")
            .addQueryParameter("quality", q)
            .addQueryParameter("key", apiKey)
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
