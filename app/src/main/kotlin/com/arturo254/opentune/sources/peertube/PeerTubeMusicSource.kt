package com.arturo254.opentune.sources.peertube

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import com.arturo254.opentune.BuildConfig
import com.arturo254.opentune.core.model.*
import com.arturo254.opentune.extensions.ExtraIsMusicVideo
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.sources.*
import com.arturo254.opentune.ui.original.utils.VideoStreams
import com.arturo254.opentune.ui.original.utils.VideoTrack
import com.arturo254.opentune.playback.videoCacheKey
import com.arturo254.opentune.utils.awaitText
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object PeerTubeClient {
    private val json = Json { ignoreUnknownKeys = true }
    private val http = OkHttpClient.Builder().connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS).callTimeout(10, TimeUnit.SECONDS).build()
    private val lock = Mutex()
    private val details = LinkedHashMap<String, Pair<Long, JsonObject>>()
    private val root get() = requireNotNull(BuildConfig.PEERTUBE_URL.toHttpUrlOrNull())
        .also { require(it.isHttps && it.username.isEmpty() && it.password.isEmpty()) }

    private suspend fun get(path: String, query: Map<String, String> = emptyMap()): JsonObject {
        val url = root.newBuilder().addPathSegments(path)
            .apply { query.forEach { (k, v) -> addQueryParameter(k, v) } }.build()
        return json.parseToJsonElement(http.newCall(Request.Builder().url(url).build()).awaitText()).jsonObject
    }

    suspend fun search(request: TrackSearchRequest): TrackSearchPage {
        val offset = request.continuationToken?.toIntOrNull()?.also { require(it >= 0) } ?: 0
        val count = request.limit.coerceIn(1, 50)
        val response = get("api/v1/search/videos", mapOf("search" to request.query, "start" to "$offset", "count" to "$count", "nsfw" to "false"))
        val entries = response["data"]?.jsonArray.orEmpty()
        val items = entries.mapNotNull { parsePeerTubeVariant(it.jsonObject, root.toString()) }
        return TrackSearchPage(items, if (entries.size == count) "${offset + count}" else null)
    }

    private suspend fun detail(sourceId: String): JsonObject = lock.withLock {
        details[sourceId]?.takeIf { it.first > System.currentTimeMillis() }?.let { return@withLock it.second }
        val uuid = sourceId.substringAfterLast(':')
        require(sourceId.substringBeforeLast(':') == "${root.host}:${root.port}" && uuid.matches(Regex("[a-fA-F0-9-]{36}")))
        val value = get("api/v1/videos/$uuid")
        if (details.size >= 32) details.remove(details.keys.first())
        details[sourceId] = (System.currentTimeMillis() + 120_000L) to value
        value
    }

    suspend fun streams(mediaId: String): VideoStreams {
        val sourceId = mediaId.removePrefix("peertube:")
        val data = detail(sourceId)
        val duration = requireNotNull(data.long("duration")) * 1000L
        require(duration > 0)
        val files = data["files"]?.jsonArray.orEmpty().toList() +
            data["streamingPlaylists"]?.jsonArray.orEmpty().flatMap { it.jsonObject["files"]?.jsonArray.orEmpty() }
        val tracks = files.mapNotNull { element ->
            val file = element.jsonObject
            val url = file.text("fileUrl")?.toHttpUrlOrNull()?.takeIf { it.isHttps && it.encodedPath.endsWith(".mp4") } ?: return@mapNotNull null
            if ((file["hasAudio"] as? JsonPrimitive)?.booleanOrNull != true || (file["hasVideo"] as? JsonPrimitive)?.booleanOrNull != true) return@mapNotNull null
            val width = file.long("width")?.toInt() ?: return@mapNotNull null
            val height = file.long("height")?.toInt() ?: return@mapNotNull null
            val size = file.long("size") ?: return@mapNotNull null
            val fps = file.long("fps")?.toInt() ?: return@mapNotNull null
            val id = file.long("id")?.toInt() ?: return@mapNotNull null
            VideoTrack(id, url.toString(), "video/mp4", "", width, height, fps,
                (size * 8000L / duration).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), null, null, size)
        }.distinctBy { it.itag }.sortedBy { it.shortSide }
        require(tracks.isNotEmpty()) { "No playable public MP4" }
        return VideoStreams("peertube:$sourceId", tracks, emptyMap(), System.currentTimeMillis() + 120_000L, duration)
    }

    suspend fun downloadable(sourceId: String): Boolean =
        (detail(sourceId)["downloadEnabled"] as? JsonPrimitive)?.booleanOrNull == true
}

internal fun JsonObject.text(key: String) = (get(key) as? JsonPrimitive)?.contentOrNull
internal fun JsonObject.long(key: String) = (get(key) as? JsonPrimitive)?.longOrNull

internal fun parsePeerTubeVariant(item: JsonObject, base: String): SourceVariant? {
    val root = base.toHttpUrlOrNull()?.takeIf { it.isHttps } ?: return null
    val uuid = item.text("uuid") ?: return null
    val title = item.text("name") ?: return null
    val channel = item["channel"] as? JsonObject ?: return null
    val artist = channel.text("displayName") ?: channel.text("name") ?: return null
    if ((item["isLive"] as? JsonPrimitive)?.booleanOrNull == true) return null
    return SourceVariant(ProviderId.PEERTUBE, "${root.host}:${root.port}:$uuid", title, listOf(artist),
        durationMs = item.long("duration")?.times(1000L),
        artworkUrl = item.text("thumbnailPath")?.let { root.resolve(it)?.toString() },
        version = TrackVersion(setOf(TrackVersionTrait.MUSIC_VIDEO)),
        downloadable = (item["downloadEnabled"] as? JsonPrimitive)?.booleanOrNull == true)
}

@Singleton
class PeerTubeMusicSource @Inject constructor() : TrackSearchSource, StreamMusicSource, ManagedDownloadMusicSource {
    override val id = ProviderId.PEERTUBE
    override val displayName = "PeerTube"
    override val capabilities = SourceCapabilities.of(SourceCapability.TRACK_SEARCH, SourceCapability.TRACK_METADATA,
        SourceCapability.STREAM, SourceCapability.STATIC_ARTWORK, SourceCapability.OPEN_TUNE_MANAGED_DOWNLOAD)

    override suspend fun searchTracks(request: TrackSearchRequest): Result<TrackSearchPage> = try {
        Result.success(PeerTubeClient.search(request))
    } catch (cancelled: CancellationException) { throw cancelled } catch (error: Exception) { Result.failure(error) }

    override suspend fun resolvePlayback(variant: SourceVariant): MediaItem? {
        if (variant.provider != id) return null
        val streams = PeerTubeClient.streams("peertube:${variant.sourceId}")
        val track = streams.trackFor(720) ?: return null
        val metadata = MediaMetadata(streams.videoId, variant.title,
            variant.artists.map { MediaMetadata.Artist(null, it) },
            duration = (streams.durationMs / 1000L).toInt(), thumbnailUrl = variant.artworkUrl)
        return MediaItem.Builder().setMediaId(streams.videoId).setUri(track.url).setMimeType(track.mimeType)
            .setCustomCacheKey(videoCacheKey(streams.videoId, track)).setTag(metadata)
            .setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setTitle(variant.title)
                .setArtist(variant.artists.joinToString()).setArtworkUri(variant.artworkUrl?.toUri())
                .setExtras(Bundle().apply { putBoolean(ExtraIsMusicVideo, true) }).build()).build()
    }

    override suspend fun resolveManagedDownload(variant: SourceVariant): ManagedDownloadResource? {
        if (variant.provider != id || !PeerTubeClient.downloadable(variant.sourceId)) return null
        return PeerTubeClient.streams("peertube:${variant.sourceId}").trackFor(720)?.let { ManagedDownloadResource(it.url) }
    }
}
