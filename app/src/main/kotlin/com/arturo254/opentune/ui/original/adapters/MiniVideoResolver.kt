package com.arturo254.opentune.ui.original.adapters

import com.arturo254.opentune.innertube.NewPipeUtils
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.YouTubeClient
import com.arturo254.opentune.innertube.models.response.PlayerResponse
import com.arturo254.opentune.ui.original.utils.*
import com.arturo254.opentune.utils.StreamClientUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.concurrent.TimeUnit

private const val VideoLog = "MiniVideoResolver"

/** The same fallback order as the reference video path; this never invokes the audio resolver. */
internal val videoClients = listOf(YouTubeClient.VISIONOS, YouTubeClient.WEB_REMIX,
    YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER, YouTubeClient.ANDROID_VR_1_61_48, YouTubeClient.WEB)

internal suspend fun <T : Any> firstPlayableVideoClient(
    clients: List<YouTubeClient>, resolve: suspend (YouTubeClient) -> T?,
): T {
    for (client in clients) {
        try {
            Timber.tag(VideoLog).d("Trying client=%s", client.clientName)
            resolve(client)?.let { return it }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            // Do not log signed stream URLs, cookies, visitorData or tokens from exceptions.
            Timber.tag(VideoLog).w("Client=%s failed type=%s", client.clientName, failure.javaClass.simpleName)
        }
    }
    throw IllegalStateException("No playable video after ${clients.size} clients")
}

internal fun PlayerResponse.StreamingData.Format.ByteRange.videoRange(): LongRange? {
    val first = start.toLongOrNull() ?: return null
    val last = end.toLongOrNull() ?: return null
    return if (first >= 0 && last >= first) first..last else null
}

/** Reject early/truncated ranges, not just successful responses for byte zero. */
internal fun videoTailServed(code: Int, contentRange: String?, offset: Long, length: Long?): Boolean {
    if (code == 416 && length == null) return true // unknown length, smaller than the probe
    if (code != 206) return false
    val range = Regex("bytes (\\d+)-(\\d+)/(\\d+|\\*)").matchEntire(contentRange.orEmpty()) ?: return false
    val first = range.groupValues[1].toLongOrNull() ?: return false
    val last = range.groupValues[2].toLongOrNull() ?: return false
    val total = range.groupValues[3].toLongOrNull()
    return first == offset && last >= offset && (length == null || total == length)
}

object MiniVideoResolver {
    suspend fun resolveVideoStreams(videoId: String): Result<VideoStreams> = withContext(Dispatchers.IO) {
        try {
            if (videoId.startsWith("peertube:")) return@withContext Result.success(
                com.arturo254.opentune.sources.peertube.PeerTubeClient.streams(videoId))
            if (YouTube.cookie == null) {
                YouTube.visitorData().onSuccess { YouTube.visitorData = it }
                    .onFailure { Timber.tag(VideoLog).w("visitorData refresh failed type=%s", it.javaClass.simpleName) }
            }
            val timestamp by lazy {
                NewPipeUtils.getSignatureTimestamp(videoId)
                    .onFailure { Timber.tag(VideoLog).w("Signature timestamp unavailable type=%s", it.javaClass.simpleName) }
                    .getOrNull()
            }
            Result.success(firstPlayableVideoClient(videoClients) { client -> videoStreamsFrom(client, videoId) { timestamp } })
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Timber.tag(VideoLog).e("Resolution failed videoId=%s type=%s", videoId, failure.javaClass.simpleName)
            com.arturo254.opentune.playback.OwnResolverClient.resolveV2("youtube",videoId,"video")?.let { stream ->
                val track = VideoTrack(-1, stream.url, stream.mimeType, stream.codecs.orEmpty(),
                    stream.width ?: -1, stream.height ?: -1, stream.fps ?: -1, stream.bitrate ?: -1, null, null, stream.length)
                return@withContext Result.success(VideoStreams(videoId,listOf(track),emptyMap(),
                    System.currentTimeMillis() + stream.expires * 1000L, 0L))
            }
            Result.failure(failure)
        }
    }

    private suspend fun videoStreamsFrom(client: YouTubeClient, videoId: String, timestamp: () -> Int?): VideoStreams? {
        val response = YouTube.player(videoId, client = client,
            signatureTimestamp = if (client.useSignatureTimestamp) timestamp() else null).getOrThrow()
        Timber.tag(VideoLog).d("Client=%s playabilityStatus=%s", client.clientName, response.playabilityStatus.status)
        if (response.playabilityStatus.status != "OK") return null
        val data = response.streamingData ?: return null
        val duration = data.adaptiveFormats.firstNotNullOfOrNull { it.approxDurationMs?.toLongOrNull() }
            ?: response.videoDetails?.lengthSeconds?.toLongOrNull()?.times(1000L) ?: return null
        val expires = data.expiresInSeconds ?: return null
        val headers = buildMap {
            put("User-Agent", client.userAgent)
            put("Accept", "*/*")
            put("Accept-Language", "en-US,en;q=0.9")
            val origin = when (client.clientName) {
                "WEB_REMIX" -> "https://music.youtube.com"
                "WEB_CREATOR" -> "https://studio.youtube.com"
                else -> "https://www.youtube.com"
            }
            put("Origin", origin); put("Referer", "$origin/")
        }
        fun trackOf(format: PlayerResponse.StreamingData.Format): VideoTrack? {
            // Existing NewPipe handles direct n URLs AND cipher signatures/throttling transforms.
            val resolved = NewPipeUtils.getStreamUrl(format, videoId, client)
                .onFailure { Timber.tag(VideoLog).w("URL transform client=%s itag=%d failed type=%s",
                    client.clientName, format.itag, it.javaClass.simpleName) }.getOrNull() ?: return null
            return VideoTrack(format.itag, StreamClientUtils.patchClientVersion(resolved, client.clientVersion),
                format.containerType(), format.codecs(), format.width ?: return null, format.height ?: return null,
                format.fps ?: 30, format.bitrate, format.initRange?.videoRange(), format.indexRange?.videoRange(), format.contentLength)
        }
        val tracks = pickVideoFamily(data.adaptiveFormats).mapNotNull(::trackOf)
        if (tracks.isEmpty()) return null
        val probe = tracks.minBy { kotlin.math.abs(it.shortSide - 720) }
        if (!validateTail(probe, headers, client)) return null
        val sharp = pickSharpFamily(data.adaptiveFormats, tracks.last().shortSide).mapNotNull(::trackOf)
        Timber.tag(VideoLog).d("Resolved videoId=%s client=%s tracks=%d dash=%s", videoId, client.clientName, tracks.size,
            tracks.all { it.chunked })
        return VideoStreams(videoId, tracks, headers, System.currentTimeMillis() + expires * 1000L, duration, sharp)
    }

    private fun validateTail(track: VideoTrack, headers: Map<String, String>, client: YouTubeClient): Boolean {
        val length = track.contentLength?.takeIf { it > 0 }
        val offset = length?.minus(1) ?: 1_048_576L
        val request = Request.Builder().url(track.url).get().apply {
            headers.forEach { (name, value) -> header(name, value) }
            header("Range", "bytes=$offset-$offset")
        }.build()
        // Same proxy choice as the dedicated video session, not StreamResolver/OwnResolverClient.
        val http = OkHttpClient.Builder().followAppProxy().connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS).callTimeout(12, TimeUnit.SECONDS).build()
        return http.newCall(request).execute().use { response ->
            val valid = videoTailServed(response.code, response.header("Content-Range"), offset, length) &&
                (response.code == 416 || response.body?.source()?.readByteArray(1)?.isNotEmpty() == true)
            Timber.tag(VideoLog).d("Tail probe client=%s itag=%d status=%d valid=%s", client.clientName, track.itag, response.code, valid)
            valid
        }
    }
}
