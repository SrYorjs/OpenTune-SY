/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.arturo254.opentune.playback

import android.content.Context
import android.media.MediaCodecList
import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.media3.database.DatabaseProvider
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.YouTubeClient
import com.arturo254.opentune.constants.AudioQuality
import com.arturo254.opentune.constants.AudioQualityKey
import com.arturo254.opentune.constants.PlayerStreamClient
import com.arturo254.opentune.constants.PlayerStreamClientKey
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.db.entities.FormatEntity
import com.arturo254.opentune.db.entities.LyricsEntity
import com.arturo254.opentune.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.arturo254.opentune.db.entities.SongEntity
import com.arturo254.opentune.di.DownloadCache
import com.arturo254.opentune.di.PlayerCache
import com.arturo254.opentune.lyrics.LyricsHelper
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.sources.offline.OfflineTrackAsset
import com.arturo254.opentune.sources.offline.OfflineTrackRegistry
import com.arturo254.opentune.utils.YTPlayerUtils
import timber.log.Timber
import com.arturo254.opentune.utils.StreamClientUtils
import com.arturo254.opentune.utils.enumPreference
import com.arturo254.opentune.constants.NetworkMeteredKey
import com.arturo254.opentune.utils.dataStore
import com.arturo254.opentune.utils.get
import com.arturo254.opentune.ui.player.fetchCanvasArtworkForPlayback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadUtil
@Inject
constructor(
    @ApplicationContext private val context: Context,
    val database: MusicDatabase,
    val databaseProvider: DatabaseProvider,
    @DownloadCache val downloadCache: Cache,
    @PlayerCache val playerCache: Cache,
    private val lyricsHelper: LyricsHelper,
    private val offlineAssets: OfflineMediaAssetStore,
    private val offlineTrackRegistry: OfflineTrackRegistry,
) {
    private val connectivityManager = context.getSystemService<ConnectivityManager>()!!
    private val audioQuality by enumPreference(context, AudioQualityKey, AudioQuality.AUTO)
    private val preferredStreamClient by enumPreference(context, PlayerStreamClientKey, PlayerStreamClient.ANDROID_VR)
    private val songUrlCache = HashMap<String, Pair<String, Long>>()
    private val avoidStreamCodecs: Set<String> by lazy {
        if (deviceSupportsMimeType("audio/opus")) emptySet() else setOf("opus")
    }
    private val mediaDispatcher =
        Dispatcher().apply {
            maxRequests = 32
            maxRequestsPerHost = 12
        }

    private val downloadExecutor = Executors.newFixedThreadPool(4)

    private val mediaOkHttpClient: OkHttpClient by lazy {
        OkHttpClient
            .Builder()
            .dispatcher(mediaDispatcher)
            .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .proxy(YouTube.streamProxy)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val request = chain.request()
                val host = request.url.host
                val isYouTubeMediaHost =
                    host.endsWith("googlevideo.com") ||
                            host.endsWith("googleusercontent.com") ||
                            host.endsWith("youtube.com") ||
                            host.endsWith("youtube-nocookie.com") ||
                            host.endsWith("ytimg.com")

                if (!isYouTubeMediaHost) return@addInterceptor chain.proceed(request)

                val clientParam = request.url.queryParameter("c")?.trim().orEmpty()

                val userAgent = StreamClientUtils.resolveUserAgent(clientParam)
                val originReferer = StreamClientUtils.resolveOriginReferer(clientParam)

                val builder = request.newBuilder().header("User-Agent", userAgent)
                originReferer.origin?.let { builder.header("Origin", it) }
                originReferer.referer?.let { builder.header("Referer", it) }

                chain.proceed(builder.build())
            }.build()
    }

    val downloads = MutableStateFlow<Map<String, Download>>(emptyMap())

    private val dataSourceFactory =
        ResolvingDataSource.Factory(
            CacheDataSource
                .Factory()
                .setCache(playerCache)
                .setUpstreamDataSourceFactory(
                    OkHttpDataSource.Factory(
                        mediaOkHttpClient,
                    ),
                ),
        ) { dataSpec ->
            val mediaId = dataSpec.key ?: error("No media id")
            val length = if (dataSpec.length >= 0) dataSpec.length else 1
            if (playerCache.cacheSpace > 500 * 1024 * 1024L) {
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                    playerCache.keys.shuffled().take(10).forEach { key ->
                        playerCache.getCachedSpans(key).sumOf { it.length }
                    }
                }
            }
            if (playerCache.isCached(mediaId, dataSpec.position, length)) {
                CoroutineScope(Dispatchers.IO).launch { ensureLyricsSavedByLookup(mediaId) }
                return@Factory dataSpec
            }
            songUrlCache[mediaId]?.takeIf { it.second > System.currentTimeMillis() }?.let {
                CoroutineScope(Dispatchers.IO).launch { ensureLyricsSavedByLookup(mediaId) }
                return@Factory dataSpec.withUri(it.first.toUri())
            }
            val playbackData = runBlocking(Dispatchers.IO) {
                val networkMeteredPref = context.dataStore.get(NetworkMeteredKey, true)
                YTPlayerUtils.playerResponseForPlayback(
                    mediaId,
                    audioQuality = audioQuality,
                    preferredStreamClient = preferredStreamClient,
                    connectivityManager = connectivityManager,
                    networkMetered = networkMeteredPref,
                    avoidCodecs = avoidStreamCodecs,
                )
            }.getOrThrow()
            val format = playbackData.format

            database.query {
                upsert(
                    FormatEntity(
                        id = mediaId,
                        itag = format.itag,
                        mimeType = format.mimeType.split(";")[0],
                        codecs = format.mimeType.split("codecs=")[1].removeSurrounding("\""),
                        bitrate = format.bitrate,
                        sampleRate = format.audioSampleRate,
                        contentLength = format.contentLength!!,
                        loudnessDb = playbackData.audioConfig?.loudnessDb,
                        perceptualLoudnessDb = playbackData.audioConfig?.perceptualLoudnessDb,
                        playbackUrl = playbackData.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                    ),
                )

                val now = LocalDateTime.now()
                val existing = getSongByIdBlocking(mediaId)?.song

                val updatedSong = if (existing != null) {
                    if (existing.dateDownload == null) existing.copy(dateDownload = now) else existing
                } else {
                    SongEntity(
                        id = mediaId,
                        title = playbackData.videoDetails?.title ?: "Unknown",
                        duration = playbackData.videoDetails?.lengthSeconds?.toIntOrNull() ?: 0,
                        thumbnailUrl = playbackData.videoDetails?.thumbnail?.thumbnails?.lastOrNull()?.url,
                        dateDownload = now
                    )
                }

                upsert(updatedSong)

                // Aseguramos que la letra quede guardada localmente para poder
                // verla sin conexión una vez que la canción ya está descargada.
                CoroutineScope(Dispatchers.IO).launch {
                    ensureLyricsSaved(
                        mediaId = mediaId,
                        title = updatedSong.title,
                        artist = playbackData.videoDetails?.author.orEmpty(),
                        album = null,
                        duration = updatedSong.duration,
                    )
                }
            }

            val streamUrl = playbackData.streamUrl

            songUrlCache[mediaId] = streamUrl to (System.currentTimeMillis() + (playbackData.streamExpiresInSeconds * 1000L))
            dataSpec.withUri(streamUrl.toUri())
        }

    val downloadNotificationHelper =
        DownloadNotificationHelper(context, ExoDownloadService.CHANNEL_ID)

    val downloadManager: DownloadManager =
        DownloadManager(
            context,
            databaseProvider,
            downloadCache,
            dataSourceFactory,
            downloadExecutor
        ).apply {
            maxParallelDownloads = 4
            addListener(
                object : DownloadManager.Listener {
                    override fun onDownloadChanged(
                        downloadManager: DownloadManager,
                        download: Download,
                        finalException: Exception?,
                    ) {
                        downloads.update { map ->
                            map.toMutableMap().apply {
                                set(download.request.id, download)
                            }
                        }

                        when (download.state) {
                            Download.STATE_COMPLETED ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    offlineTrackRegistry.markAvailable(download.request.id)
                                    ensureOfflineBundleByLookup(download.request.id)
                                }

                            Download.STATE_FAILED ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    offlineTrackRegistry.markFailed(download.request.id)
                                }

                            Download.STATE_REMOVING ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    offlineTrackRegistry.remove(download.request.id)
                                }
                        }
                    }
                }
            )
        }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val result = mutableMapOf<String, Download>()
            val cursor = downloadManager.downloadIndex.getDownloads()
            while (cursor.moveToNext()) {
                val download = cursor.download
                result[download.request.id] = download

                when (download.state) {
                    Download.STATE_COMPLETED -> {
                        if (offlineTrackRegistry.findByMediaId(download.request.id) == null) {
                            val song = database.getSongByIdBlocking(download.request.id)
                            offlineTrackRegistry.adoptLegacy(
                                mediaId = download.request.id,
                                title = song?.song?.title ?: download.request.id,
                                artists = song?.artists?.map { it.name }.orEmpty(),
                                albumId = song?.album?.id ?: song?.song?.albumId,
                                albumName = song?.album?.title ?: song?.song?.albumName,
                                durationMs =
                                    song?.song?.duration
                                        ?.takeIf { it >= 0 }
                                        ?.toLong()
                                        ?.times(1_000L),
                                artworkUrl = song?.song?.thumbnailUrl,
                            )
                        } else {
                            offlineTrackRegistry.markAvailable(download.request.id)
                        }
                    }

                    Download.STATE_FAILED ->
                        offlineTrackRegistry.markFailed(download.request.id)

                    Download.STATE_REMOVING ->
                        offlineTrackRegistry.remove(download.request.id)
                }
            }

            val indexedIds = result.keys
            offlineTrackRegistry
                .snapshot()
                .filter { asset -> asset.mediaId !in indexedIds }
                .forEach { asset ->
                    offlineTrackRegistry.remove(asset.mediaId)
                }

            downloads.value = result
        }
    }

    suspend fun enqueueOfflineDownload(
        asset: OfflineTrackAsset,
        metadata: MediaMetadata,
    ) {
        database.query {
            insert(metadata)
        }

        offlineTrackRegistry.registerPending(asset)

        val request =
            DownloadRequest
                .Builder(
                    asset.mediaId,
                    asset.mediaId.toUri(),
                ).setCustomCacheKey(asset.mediaId)
                .setData(metadata.title.toByteArray())
                .build()

        try {
            DownloadService.sendAddDownload(
                context,
                ExoDownloadService::class.java,
                request,
                false,
            )
        } catch (error: Throwable) {
            offlineTrackRegistry.markFailed(asset.mediaId)
            throw error
        }
    }

    fun getDownload(songId: String): Flow<Download?> = downloads.map { it[songId] }

    /**
     * Igual que [ensureLyricsSaved], pero para los casos en que la canción
     * se resuelve desde caché (ya se había reproducido antes) y no tenemos
     * a mano el `playbackData` de YouTube. Busca el título/artista en la
     * base de datos local, donde ya deberían estar guardados.
     */
    private suspend fun ensureLyricsSavedByLookup(mediaId: String) {
        val existing = database.getLyricsById(mediaId)
        if (existing != null) {
            if (existing.lyrics != LYRICS_NOT_FOUND) {
                offlineAssets.saveLyrics(mediaId, existing.lyrics)
            }
            return
        }

        val song = database.getSongByIdBlocking(mediaId) ?: run {
            Timber.tag("DownloadUtil").w("No se encontró la canción $mediaId en la BD, no se puede buscar letra todavía")
            return
        }

        ensureLyricsSaved(
            mediaId = mediaId,
            title = song.song.title,
            artist = song.artists.joinToString { it.name },
            album = null,
            duration = song.song.duration,
        )
    }

    /**
     * Busca la letra de una canción descargada y la guarda en la base de datos
     * local (tabla `lyrics`) para que quede disponible sin conexión.
     * No hace nada si ya existe una letra guardada (encontrada o no encontrada),
     * para no repetir peticiones de red cada vez que se re-descarga el mismo tema.
     */
    private suspend fun ensureLyricsSaved(
        mediaId: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
    ) {
        val existing = database.getLyricsById(mediaId)
        if (existing != null) {
            if (existing.lyrics != LYRICS_NOT_FOUND) {
                offlineAssets.saveLyrics(mediaId, existing.lyrics)
            }
            return
        }

        val mediaMetadata = MediaMetadata(
            id = mediaId,
            title = title,
            artists = listOf(MediaMetadata.Artist(id = null, name = artist)),
            duration = duration,
            album = album?.let { MediaMetadata.Album(id = "", title = it) },
        )

        val lyrics = runCatching {
            lyricsHelper.getLyrics(mediaMetadata)
        }.getOrElse {
            Timber.tag("DownloadUtil").w(it, "No se pudo obtener la letra para $mediaId")
            LYRICS_NOT_FOUND
        }

        database.query {
            upsert(LyricsEntity(id = mediaId, lyrics = lyrics))
        }

        if (lyrics != LYRICS_NOT_FOUND) {
            offlineAssets.saveLyrics(mediaId, lyrics)
        }
    }

    private suspend fun ensureOfflineBundleByLookup(mediaId: String) {
        val song = database.getSongByIdBlocking(mediaId) ?: return
        val title = song.song.title
        val artists = song.artists.joinToString { it.name }
        val album = song.album?.title ?: song.song.albumName

        coroutineScope {
            val lyricsJob =
                async(Dispatchers.IO) {
                    ensureLyricsSaved(
                        mediaId = mediaId,
                        title = title,
                        artist = artists,
                        album = album,
                        duration = song.song.duration,
                    )
                }

            val coverJob =
                async(Dispatchers.IO) {
                    offlineAssets.cacheCover(
                        mediaId = mediaId,
                        url = song.song.thumbnailUrl,
                    )
                }

            val canvasJob =
                async(Dispatchers.IO) {
                    val canvas =
                        runCatching {
                            fetchCanvasArtworkForPlayback(
                                songTitleRaw = title,
                                artistNameRaw = artists,
                                albumName = album,
                            )
                        }.getOrNull()

                    offlineAssets.cacheAnimatedArtwork(
                        mediaId = mediaId,
                        url = canvas?.preferredAnimationUrl,
                    )
                }

            awaitAll(lyricsJob, coverJob, canvasJob)
        }
    }

    fun getOfflineCoverUri(mediaId: String): String? =
        offlineAssets.getCoverUri(mediaId)

    fun getOfflineAnimationUri(mediaId: String): String? =
        offlineAssets.getAnimationUri(mediaId)

    private fun deviceSupportsMimeType(mimeType: String): Boolean {
        return runCatching {
            val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
            codecList.codecInfos.any { info ->
                !info.isEncoder && info.supportedTypes.any { it.equals(mimeType, ignoreCase = true) }
            }
        }.getOrDefault(false)
    }
}