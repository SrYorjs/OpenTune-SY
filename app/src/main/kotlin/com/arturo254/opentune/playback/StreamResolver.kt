package com.arturo254.opentune.playback

import android.content.Context
import android.media.MediaCodecList
import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.arturo254.jossredconnect.JossRedClient
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.AudioQuality
import com.arturo254.opentune.constants.JossRedMultimediaKey
import com.arturo254.opentune.constants.PlayerStreamClient
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.db.entities.FormatEntity
import com.arturo254.opentune.extensions.directorySizeBytes
import com.arturo254.opentune.utils.StreamClientUtils
import com.arturo254.opentune.utils.YTPlayerUtils
import com.arturo254.opentune.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

internal class StreamResolver(
    private val context: Context,
    private val database: MusicDatabase,
    private val playerCache: Cache,
    private val downloadCache: Cache,
    private val scopeProvider: () -> CoroutineScope,
    private val audioQualityProvider: () -> AudioQuality,
    private val preferredStreamClientProvider: () -> PlayerStreamClient,
    private val errorRecoveryControllerProvider: () -> ErrorRecoveryController,
    private val recoverSong: suspend (String, YTPlayerUtils.PlaybackData?) -> Unit,
    private val promptLoginRecovery: (String, String) -> Unit,
) {
    private val resolutionRequests = PlaybackResolutionRequests()

    fun onMediaItemTransition(mediaId: String?) {
        resolutionRequests.cancelExcept(mediaId)
    }

    fun cancelResolutions() {
        resolutionRequests.cancelExcept(null)
    }

    private fun playerCacheKey(mediaId: String) = audioPlayerCacheKey(playerCache, mediaId)

    private fun rememberPlayerCacheKey(mediaId: String, playback: YTPlayerUtils.PlaybackData) =
        rememberAudioPlayerCacheKey(playerCache, mediaId, playback)

    private val connectivityManager: ConnectivityManager =
        context.getSystemService<ConnectivityManager>()!!

    private val avoidStreamCodecs: Set<String> by lazy {
        if (deviceSupportsMimeType("audio/opus")) emptySet() else setOf("opus")
    }

    private val mediaOkHttpClient: OkHttpClient by lazy {
        playbackHttpClient(com.arturo254.opentune.innertube.YouTube.streamProxy, OwnResolverClient.credentialInterceptor())
    }

    fun createDataSourceFactory(): DataSource.Factory =
        ResolvingDataSource.Factory(createCacheDataSource()) { dataSpec ->
            if (dataSpec.uri.scheme?.lowercase() in setOf("file", "content", "android.resource")) {
                return@Factory dataSpec
            }
            if (StreamRequestPolicy.isDirectHttpStream(dataSpec.uri.toString())) {
                return@Factory dataSpec
            }
            val mediaId = dataSpec.key ?: error("No media id")
            val recovery = errorRecoveryControllerProvider()

            recovery
                .validCachedStream(mediaId)
                ?.takeIf { playerCacheKey(mediaId) != mediaId }
                ?.let { cached ->
                    scopeProvider().launch(Dispatchers.IO) {
                        recoverSong(mediaId, null)
                    }
                    return@Factory StreamRequestPolicy.resolveDataSpec(dataSpec, cached.first.toUri())
                }

            val requiredCachedLength =
                if (dataSpec.length >= 0L) {
                    dataSpec.length
                } else {
                    val cacheStarted = System.nanoTime()
                    val contentLength = cachedAudioLength(downloadCache, playerCache, mediaId) {
                        runBlocking(Dispatchers.IO) { database.format(mediaId).first()?.contentLength }
                    }
                    PlaybackStartupTrace.record(mediaId, "cache_lookup", (System.nanoTime() - cacheStarted) / 1_000_000L)

                    contentLength?.let {
                        (it - dataSpec.position).takeIf { remaining ->
                            remaining > 0L
                        }
                    }
                }

            if (requiredCachedLength != null) {
                val isFullyCached =
                    downloadCache.isCached(
                        mediaId,
                        dataSpec.position,
                        requiredCachedLength,
                    ) ||
                        playerCache.isCached(
                            playerCacheKey(mediaId),
                            dataSpec.position,
                            requiredCachedLength,
                        )

                if (isFullyCached) {
                    PlaybackStartupTrace.mark(mediaId, "cache_complete")
                    scopeProvider().launch(Dispatchers.IO) {
                        recoverSong(mediaId, null)
                    }
                    return@Factory dataSpec
                }
            }

            if (mediaId.startsWith("offline:")) {
                throw java.io.FileNotFoundException(
                    "El audio descargado no está completo o no está disponible"
                )
            }

            val resolutionStarted = System.nanoTime()
            try {
                val playbackData =
                    resolutionRequests.resolve(mediaId, scopeProvider().coroutineContext[kotlinx.coroutines.Job]) {
                        YTPlayerUtils.playerResponseForPlayback(
                            mediaId, audioQuality = audioQualityProvider(), connectivityManager = connectivityManager,
                            preferredStreamClient = preferredStreamClientProvider(), avoidCodecs = avoidStreamCodecs,
                        )
                    }.getOrElse { throwable ->
                        if (throwable is YTPlayerUtils.LoginRequiredForPlaybackException &&
                            !PlaybackGateClassifier.requiresAgeVerification(throwable.message)) {
                            promptLoginRecovery(mediaId, throwable.targetUrl)
                        }
                        throw PlaybackResolutionDataSourceException(mediaId, throwable, dataSpec, (System.nanoTime() - resolutionStarted) / 1_000_000L)
                    }

                val nonNullPlayback =
                    requireNotNull(playbackData) {
                        context.getString(R.string.error_unknown)
                    }

                rememberPlayerCacheKey(mediaId, nonNullPlayback)
                persistResolvedFormat(mediaId, nonNullPlayback)

                scopeProvider().launch(Dispatchers.IO) {
                    recoverSong(mediaId, nonNullPlayback)
                }

                val streamUrl = nonNullPlayback.streamUrl
                recovery.cacheStream(
                    mediaId = mediaId,
                    url = streamUrl,
                    expiresAtMs =
                        System.currentTimeMillis() +
                            nonNullPlayback.streamExpiresInSeconds * 1000L,
                )

                return@Factory StreamRequestPolicy.resolveDataSpec(dataSpec, streamUrl.toUri())
            } catch (originalError: Exception) {
                if (originalError is PlaybackLoadCancelledException) throw originalError
                val diagnostic = playbackFailureDiagnostic(mediaId, originalError)
                Timber.tag("PlaybackDiagnostic").w("id=%s provider=%s stage=%s root=%s category=%s",
                    diagnostic.mediaId, diagnostic.provider, diagnostic.stage, diagnostic.rootType, diagnostic.category)
                if (diagnostic.category == "timeout" || diagnostic.cancelled) throw originalError

                val useAlternativeSource =
                    runBlocking(Dispatchers.IO) {
                        try {
                            context.dataStore.data.first()[JossRedMultimediaKey]
                                ?: false
                        } catch (error: Exception) {
                            Timber.e(
                                error,
                                "Error reading JossRed preference, defaulting to false",
                            )
                            false
                        }
                    }

                if (!useAlternativeSource) {
                    Timber.d(
                        "JossRed fallback disabled by user preference",
                    )
                    throw originalError
                }

                try {
                    val alternativeUrl =
                        runCatching {
                            runBlocking(Dispatchers.IO) {
                                withTimeout(5_000L) {
                                    JossRedClient.getStreamingUrl(mediaId)
                                }
                            }
                        }.getOrNull()

                    if (alternativeUrl == null) {
                        Timber.w(
                            "JossRed returned null URL for $mediaId, throwing original error",
                        )
                        throw originalError
                    }

                    val client =
                        OkHttpClient
                            .Builder()
                            .connectTimeout(
                                2,
                                java.util.concurrent.TimeUnit.SECONDS,
                            ).readTimeout(
                                2,
                                java.util.concurrent.TimeUnit.SECONDS,
                            ).build()

                    val request =
                        Request
                            .Builder()
                            .url(alternativeUrl)
                            .head()
                            .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            Timber.w(
                                "JossRed URL unreachable for $mediaId (HTTP ${response.code}), throwing original error",
                            )
                            throw originalError
                        }

                        Timber.i(
                            "Using JossRed fallback for $mediaId",
                        )

                        recovery.cacheStream(
                            mediaId = mediaId,
                            url = alternativeUrl,
                            expiresAtMs =
                                System.currentTimeMillis() + 300_000L,
                        )

                        scopeProvider().launch(Dispatchers.IO) {
                            try {
                                recoverSong(mediaId, null)
                            } catch (recoverError: Exception) {
                                Timber.e(
                                    recoverError,
                                    "Error recovering song from JossRed fallback",
                                )
                            }
                        }

                        return@Factory StreamRequestPolicy.resolveDataSpec(dataSpec, alternativeUrl.toUri())
                    }
                } catch (fallbackError: Exception) {
                    when (fallbackError) {
                        is JossRedClient.JossRedException ->
                            Timber.w(
                                "JossRed error for $mediaId: ${fallbackError.message}, throwing original error",
                            )

                        is TimeoutCancellationException ->
                            Timber.w(
                                "JossRed timeout for $mediaId, throwing original error",
                            )

                        else ->
                            Timber.e(
                                fallbackError,
                                "JossRed unexpected error for $mediaId, throwing original error",
                            )
                    }

                    throw originalError
                }
            }
        }.let { factory -> DataSource.Factory { PlaybackDiagnosticDataSource(factory.createDataSource()) } }

    suspend fun trimPlayerCacheToBytes(limitBytes: Long) {
        if (limitBytes <= 0L) return

        withContext(Dispatchers.IO) {
            val cacheDir = context.filesDir.resolve("exoplayer")
            val currentSpace =
                runCatching { playerCache.cacheSpace }.getOrNull() ?: 0L
            var totalBytes =
                if (currentSpace > 0L) {
                    currentSpace
                } else {
                    cacheDir.directorySizeBytes()
                }

            if (totalBytes <= limitBytes) return@withContext

            data class Candidate(
                val key: String,
                val lastTouchTimestamp: Long,
                val sizeBytes: Long,
            )

            val candidates =
                runCatching {
                    playerCache.keys
                        .mapNotNull { key ->
                            runCatching {
                                val spans =
                                    playerCache.getCachedSpans(key)
                                if (spans.isEmpty()) {
                                    return@runCatching null
                                }

                                Candidate(
                                    key = key,
                                    lastTouchTimestamp =
                                        spans.minOf {
                                            it.lastTouchTimestamp
                                        },
                                    sizeBytes =
                                        spans.sumOf { it.length },
                                )
                            }.getOrNull()
                        }.sortedBy { it.lastTouchTimestamp }
                }.getOrNull().orEmpty()

            for (candidate in candidates) {
                if (totalBytes <= limitBytes) break
                runCatching {
                    playerCache.removeResource(candidate.key)
                }
                totalBytes -= candidate.sizeBytes.coerceAtLeast(0L)
            }
        }
    }

    private fun createCacheDataSource(): DataSource.Factory = playbackAudioCacheFactory(
        playerCache, downloadCache, DefaultDataSource.Factory(context, OkHttpDataSource.Factory(mediaOkHttpClient)),
    ).let { cache -> DataSource.Factory { PlaybackDiagnosticDataSource(cache.createDataSource(), "cache_open", false) } }

    private fun persistResolvedFormat(
        mediaId: String,
        playback: YTPlayerUtils.PlaybackData,
    ) {
        val entity = persistablePlaybackFormat(mediaId, playback) ?: return
        val loudnessDb = playback.audioConfig?.loudnessDb
        val perceptualLoudnessDb =
            playback.audioConfig?.perceptualLoudnessDb

        Timber
            .tag("AudioNormalization")
            .d(
                "Storing format for $mediaId with loudnessDb: $loudnessDb, perceptualLoudnessDb: $perceptualLoudnessDb",
            )

        if (loudnessDb == null && perceptualLoudnessDb == null) {
            Timber
                .tag("AudioNormalization")
                .w(
                    "No loudness data available from YouTube for video: $mediaId",
                )
        }

        database.query { upsert(entity) }
    }

    private fun deviceSupportsMimeType(mimeType: String): Boolean =
        runCatching {
            val codecList =
                MediaCodecList(MediaCodecList.ALL_CODECS)

            codecList.codecInfos.any { info ->
                !info.isEncoder &&
                    info.supportedTypes.any {
                        it.equals(mimeType, ignoreCase = true)
                    }
            }
        }.getOrDefault(false)

}

internal fun persistablePlaybackFormat(mediaId: String, playback: YTPlayerUtils.PlaybackData): FormatEntity? {
    val format = playback.format
    val length = format.contentLength?.takeIf { it > 0 } ?: return null
    val codecs = Regex("(?:^|;)\\s*codecs=\"([^\"]+)\"").find(format.mimeType)?.groupValues?.get(1) ?: return null
    return FormatEntity(mediaId, format.itag, format.mimeType.substringBefore(';').trim(), codecs,
        format.bitrate, format.audioSampleRate, length, playback.audioConfig?.loudnessDb,
        playback.audioConfig?.perceptualLoudnessDb, playback.playbackTracking?.videostatsPlaybackUrl?.baseUrl)
}

internal fun audioPlayerCacheKey(cache: Cache, mediaId: String): String =
    cache.getContentMetadata(mediaId).get("opentune.audio.variant", "")?.takeIf { it.isNotEmpty() }
        ?: cache.keys.filter { it.startsWith("audio:youtube:$mediaId:") }
            .maxByOrNull { key -> cache.getCachedSpans(key).maxOfOrNull { it.lastTouchTimestamp } ?: Long.MIN_VALUE }
        ?: mediaId

internal fun rememberAudioPlayerCacheKey(cache: Cache, mediaId: String, playback: YTPlayerUtils.PlaybackData) {
    val format = playback.format
    val key = "audio:youtube:$mediaId:${format.itag}:${format.mimeType}:${format.bitrate}:${format.contentLength}"
    cache.applyContentMetadataMutations(key, androidx.media3.datasource.cache.ContentMetadataMutations().set("opentune.media.id", mediaId))
    cache.applyContentMetadataMutations(mediaId, androidx.media3.datasource.cache.ContentMetadataMutations().set("opentune.audio.variant", key))
}

internal fun playbackHttpClient(proxy: java.net.Proxy?, credentials: okhttp3.Interceptor, transport: OkHttpClient = OwnResolverClient.httpTransport()): OkHttpClient =
        transport.newBuilder()
            .proxy(proxy)
            .addNetworkInterceptor(credentials)
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(PLAYBACK_STREAM_CONNECT_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .callTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val request = chain.request()
                val host = request.url.host
                val isYouTubeMediaHost =
                    host.endsWith("googlevideo.com") ||
                        host.endsWith("googleusercontent.com") ||
                        host.endsWith("youtube.com") ||
                        host.endsWith("youtube-nocookie.com") ||
                        host.endsWith("ytimg.com")

                if (!isYouTubeMediaHost) {
                    return@addInterceptor chain.proceed(request)
                }

                val clientParam =
                    request.url.queryParameter("c")?.trim().orEmpty()
                val userAgent =
                    StreamClientUtils.resolveUserAgent(clientParam)
                val originReferer =
                    StreamClientUtils.resolveOriginReferer(clientParam)

                val builder =
                    request
                        .newBuilder()
                        .header("User-Agent", userAgent)

                originReferer.origin?.let {
                    builder.header("Origin", it)
                }
                originReferer.referer?.let {
                    builder.header("Referer", it)
                }

                chain.proceed(builder.build())
            }.build()
