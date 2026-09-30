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
import androidx.media3.exoplayer.ExoPlayer
import com.arturo254.jossredconnect.JossRedClient
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.AudioQuality
import com.arturo254.opentune.constants.JossRedMultimediaKey
import com.arturo254.opentune.constants.PlayerStreamClient
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.db.entities.FormatEntity
import com.arturo254.opentune.extensions.SilentHandler
import com.arturo254.opentune.extensions.directorySizeBytes
import com.arturo254.opentune.utils.StreamClientUtils
import com.arturo254.opentune.utils.YTPlayerUtils
import com.arturo254.opentune.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
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
    private val playerProvider: () -> ExoPlayer,
    private val audioQualityProvider: () -> AudioQuality,
    private val preferredStreamClientProvider: () -> PlayerStreamClient,
    private val errorRecoveryControllerProvider: () -> ErrorRecoveryController,
    private val recoverSong: suspend (String, YTPlayerUtils.PlaybackData?) -> Unit,
    private val promptLoginRecovery: (String, String) -> Unit,
) {
    private val connectivityManager: ConnectivityManager =
        context.getSystemService()!!

    private val avoidStreamCodecs: Set<String> by lazy {
        if (deviceSupportsMimeType("audio/opus")) emptySet() else setOf("opus")
    }

    private val mediaOkHttpClient: OkHttpClient by lazy {
        OkHttpClient
            .Builder()
            .proxy(com.arturo254.opentune.innertube.YouTube.streamProxy)
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
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
    }

    fun createDataSourceFactory(): DataSource.Factory =
        ResolvingDataSource.Factory(createCacheDataSource()) { dataSpec ->
            val mediaId = dataSpec.key ?: error("No media id")
            val recovery = errorRecoveryControllerProvider()

            recovery
                .validCachedStream(mediaId)
                ?.let { cached ->
                    scopeProvider().launch(Dispatchers.IO) {
                        recoverSong(mediaId, null)
                    }
                    return@Factory dataSpec
                        .withUri(cached.first.toUri())
                        .subrange(
                            dataSpec.uriPositionOffset,
                            StreamRequestPolicy.boundedLength(dataSpec.length),
                        )
                }

            val requiredCachedLength =
                if (dataSpec.length >= 0L) {
                    dataSpec.length
                } else {
                    val contentLength =
                        runBlocking(Dispatchers.IO) {
                            database.format(mediaId).first()?.contentLength
                        } ?: runCatching {
                            downloadCache
                                .getContentMetadata(mediaId)
                                .get(ContentMetadata.KEY_CONTENT_LENGTH, -1L)
                        }.getOrNull()?.takeIf { it > 0L }
                            ?: runCatching {
                                playerCache
                                    .getContentMetadata(mediaId)
                                    .get(ContentMetadata.KEY_CONTENT_LENGTH, -1L)
                            }.getOrNull()?.takeIf { it > 0L }

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
                            mediaId,
                            dataSpec.position,
                            requiredCachedLength,
                        )

                if (isFullyCached) {
                    scopeProvider().launch(Dispatchers.IO) {
                        recoverSong(mediaId, null)
                    }
                    return@Factory dataSpec
                }
            }

            try {
                val playbackData =
                    runBlocking(Dispatchers.IO) {
                        withTimeout(PLAYBACK_RESOLUTION_TIMEOUT_MS) {
                            YTPlayerUtils.playerResponseForPlayback(
                                mediaId,
                                audioQuality = audioQualityProvider(),
                                connectivityManager = connectivityManager,
                                preferredStreamClient =
                                    preferredStreamClientProvider(),
                                avoidCodecs = avoidStreamCodecs,
                            )
                        }
                    }.getOrElse { throwable ->
                        when (throwable) {
                            is YTPlayerUtils.LoginRequiredForPlaybackException -> {
                                promptLoginRecovery(
                                    mediaId,
                                    throwable.targetUrl,
                                )
                                throw PlaybackException(
                                    context.getString(
                                        R.string.playback_requires_youtube_music_confirmation,
                                    ),
                                    throwable,
                                    PlaybackException.ERROR_CODE_REMOTE_ERROR,
                                )
                            }

                            is PlaybackException -> throw throwable

                            is java.net.ConnectException,
                            is java.net.UnknownHostException,
                            -> throw PlaybackException(
                                context.getString(R.string.error_no_internet),
                                throwable,
                                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                            )

                            is java.net.SocketTimeoutException ->
                                throw PlaybackException(
                                    context.getString(R.string.error_timeout),
                                    throwable,
                                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                                )

                            else ->
                                throw PlaybackException(
                                    context.getString(R.string.error_unknown),
                                    throwable,
                                    PlaybackException.ERROR_CODE_REMOTE_ERROR,
                                )
                        }
                    }

                val nonNullPlayback =
                    requireNotNull(playbackData) {
                        context.getString(R.string.error_unknown)
                    }

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

                return@Factory dataSpec
                    .withUri(streamUrl.toUri())
                    .subrange(
                        dataSpec.uriPositionOffset,
                        StreamRequestPolicy.boundedLength(dataSpec.length),
                    )
            } catch (originalError: Exception) {
                Timber.e(
                    originalError,
                    "YouTube playback error for $mediaId, trying JossRed as fallback",
                )

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
                            "Using JossRed URL as fallback for $mediaId: $alternativeUrl",
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

                        return@Factory dataSpec
                            .withUri(alternativeUrl.toUri())
                            .subrange(
                                dataSpec.uriPositionOffset,
                                StreamRequestPolicy.boundedLength(
                                    dataSpec.length,
                                ),
                            )
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
        }

    fun scheduleNextStreamWarmup() {
        if (connectivityManager.isActiveNetworkMetered) return

        scopeProvider().launch(Dispatchers.IO + SilentHandler) {
            delay(1_200L)

            if (connectivityManager.isActiveNetworkMetered) {
                return@launch
            }

            val player = playerProvider()
            val nextIndex = player.currentMediaItemIndex + 1
            if (nextIndex !in 0 until player.mediaItemCount) {
                return@launch
            }

            val mediaId =
                player
                    .getMediaItemAt(nextIndex)
                    .mediaId
                    .trim()
                    .takeIf { it.isNotBlank() }
                    ?: return@launch

            val recovery = errorRecoveryControllerProvider()
            val now = System.currentTimeMillis()

            if (recovery.hasValidCachedStream(mediaId, now)) {
                return@launch
            }
            if (!recovery.tryBeginWarmup(mediaId)) {
                return@launch
            }

            try {
                val playbackData =
                    withTimeout(8_000L) {
                        YTPlayerUtils.playerResponseForPlayback(
                            mediaId,
                            audioQuality = audioQualityProvider(),
                            connectivityManager = connectivityManager,
                            preferredStreamClient =
                                preferredStreamClientProvider(),
                            avoidCodecs = avoidStreamCodecs,
                        )
                    }.getOrNull() ?: return@launch

                recovery.cacheStream(
                    mediaId = mediaId,
                    url = playbackData.streamUrl,
                    expiresAtMs =
                        System.currentTimeMillis() +
                            playbackData.streamExpiresInSeconds * 1000L,
                )
            } catch (_: Exception) {
            } finally {
                recovery.finishWarmup(mediaId)
            }
        }
    }

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

    private fun createCacheDataSource(): CacheDataSource.Factory =
        CacheDataSource
            .Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(
                CacheDataSource
                    .Factory()
                    .setCache(playerCache)
                    .setUpstreamDataSourceFactory(
                        DefaultDataSource.Factory(
                            context,
                            OkHttpDataSource.Factory(mediaOkHttpClient),
                        ),
                    ).setFlags(FLAG_IGNORE_CACHE_ON_ERROR),
            ).setCacheWriteDataSinkFactory(null)
            .setFlags(FLAG_IGNORE_CACHE_ON_ERROR)

    private fun persistResolvedFormat(
        mediaId: String,
        playback: YTPlayerUtils.PlaybackData,
    ) {
        val format = playback.format
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

        database.query {
            upsert(
                FormatEntity(
                    id = mediaId,
                    itag = format.itag,
                    mimeType = format.mimeType.split(";")[0],
                    codecs =
                        format.mimeType
                            .split("codecs=")[1]
                            .removeSurrounding("""),
                    bitrate = format.bitrate,
                    sampleRate = format.audioSampleRate,
                    contentLength = format.contentLength!!,
                    loudnessDb = loudnessDb,
                    perceptualLoudnessDb = perceptualLoudnessDb,
                    playbackUrl =
                        playback
                            .playbackTracking
                            ?.videostatsPlaybackUrl
                            ?.baseUrl,
                ),
            )
        }
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

    private companion object {
        const val PLAYBACK_RESOLUTION_TIMEOUT_MS = 20_000L
    }
}
