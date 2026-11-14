package com.arturo254.opentune.ui.original.adapters

import androidx.compose.runtime.staticCompositionLocalOf
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.YouTubeClient
import com.arturo254.opentune.ui.original.utils.VideoStreams
import com.arturo254.opentune.ui.original.utils.VideoTrack
import com.arturo254.opentune.ui.original.utils.codecs
import com.arturo254.opentune.ui.original.utils.containerType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

val LocalOriginalPlayerConnection = staticCompositionLocalOf<OriginalPlayerConnection?> { null }

fun OriginalPlayerConnection.togglePlayPause() = player.togglePlayPause()

fun OkHttpClient.Builder.followAppProxy(): OkHttpClient.Builder = proxy(YouTube.proxy)

object MiniVideoResolver {
    suspend fun resolveVideoStreams(videoId: String): Result<VideoStreams> = withContext(Dispatchers.IO) {
        runCatching {
            val client = YouTubeClient.IOS
            val response = YouTube.player(videoId, client = client).getOrThrow()
            val data = requireNotNull(response.streamingData)
            val formats = (data.adaptiveFormats.orEmpty() + data.formats.orEmpty()).filter {
                !it.isAudio && it.url != null && (it.width ?: 0) > 0 && (it.height ?: 0) > 0
            }
            val tracks = formats.map { format ->
                VideoTrack(format.itag, requireNotNull(format.url), format.containerType(), format.codecs(),
                    requireNotNull(format.width), requireNotNull(format.height), format.fps ?: 30,
                    format.bitrate, null, null, format.contentLength)
            }.sortedBy { it.shortSide }
            require(tracks.isNotEmpty()) { "No directly playable video formats for $videoId" }
            VideoStreams(videoId, tracks, mapOf("User-Agent" to client.userAgent),
                System.currentTimeMillis() + requireNotNull(data.expiresInSeconds) * 1000L,
                requireNotNull(response.videoDetails?.lengthSeconds).toLong() * 1000L)
        }
    }
}
