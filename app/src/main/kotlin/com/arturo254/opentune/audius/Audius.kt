package com.arturo254.opentune.audius

import com.arturo254.opentune.models.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Lightweight Audius REST client.
 *
 * Audius gives us a second, direct streaming source that does not need the
 * YouTube player-response resolution path. Media IDs are prefixed so the
 * playback service can route them without confusing them with YouTube IDs.
 */
object Audius {
    private const val BASE_URL = "https://api.audius.co/v1"
    private const val MEDIA_PREFIX = "audius:"

    private val client =
        OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

    fun mediaId(trackId: String): String = MEDIA_PREFIX + trackId

    fun isMediaId(mediaId: String): Boolean = mediaId.startsWith(MEDIA_PREFIX)

    fun rawTrackId(mediaId: String): String =
        mediaId.removePrefix(MEDIA_PREFIX)

    fun streamUrl(mediaId: String): String =
        (BASE_URL + "/tracks/" + rawTrackId(mediaId) + "/stream")
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("app_name", "OpenTune-SY")
            .build()
            .toString()

    suspend fun searchTracks(
        query: String,
        limit: Int = 30,
    ): Result<List<AudiusTrack>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url =
                    (BASE_URL + "/tracks/search")
                        .toHttpUrl()
                        .newBuilder()
                        .addQueryParameter("query", query)
                        .addQueryParameter("app_name", "OpenTune-SY")
                        .addQueryParameter("limit", limit.coerceIn(1, 50).toString())
                        .addQueryParameter("sort_method", "relevant")
                        .build()

                requestTracks(url.toString())
            }
        }

    suspend fun trendingTracks(limit: Int = 30): Result<List<AudiusTrack>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url =
                    (BASE_URL + "/tracks/trending")
                        .toHttpUrl()
                        .newBuilder()
                        .addQueryParameter("app_name", "OpenTune-SY")
                        .addQueryParameter("limit", limit.coerceIn(1, 50).toString())
                        .addQueryParameter("time", "week")
                        .build()

                requestTracks(url.toString())
            }
        }

    private fun requestTracks(url: String): List<AudiusTrack> {
        val request =
            Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "OpenTune-SY/1.0")
                .get()
                .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Audius HTTP " + response.code)
            }

            val body = response.body.string()
            val root = JSONObject(body)
            val data = root.optJSONArray("data") ?: JSONArray()

            return buildList {
                for (index in 0 until data.length()) {
                    val json = data.optJSONObject(index) ?: continue
                    AudiusTrack.fromJson(json)?.let(::add)
                }
            }
        }
    }
}

data class AudiusTrack(
    val id: String,
    val title: String,
    val artistName: String,
    val artistId: String?,
    val durationSeconds: Int,
    val artworkUrl: String?,
    val genre: String?,
    val mood: String?,
    val playCount: Long,
    val permalink: String?,
) {
    fun toMediaMetadata(): MediaMetadata =
        MediaMetadata(
            id = Audius.mediaId(id),
            title = title,
            artists =
                listOf(
                    MediaMetadata.Artist(
                        id = artistId?.let { "audius-user:" + it },
                        name = artistName,
                        thumbnailUrl = null,
                    )
                ),
            duration = durationSeconds,
            thumbnailUrl = artworkUrl,
            album = null,
            explicit = false,
        )

    companion object {
        fun fromJson(json: JSONObject): AudiusTrack? {
            val id = json.optString("id").takeIf { it.isNotBlank() } ?: return null
            val title = json.optString("title").takeIf { it.isNotBlank() } ?: return null

            val gated =
                json.optBoolean("is_stream_gated", false) ||
                    json.optBoolean("isStreamGated", false)
            if (gated) return null

            val user = json.optJSONObject("user")
            val artistName =
                user?.optString("name")
                    ?.takeIf { it.isNotBlank() }
                    ?: user?.optString("handle")
                        ?.takeIf { it.isNotBlank() }
                    ?: "Audius Artist"
            val artistId =
                user?.optString("id")
                    ?.takeIf { it.isNotBlank() }

            val artwork = json.optJSONObject("artwork")
            val artworkUrl =
                artwork?.optString("480x480")
                    ?.takeIf { it.isNotBlank() }
                    ?: artwork?.optString("_480x480")
                        ?.takeIf { it.isNotBlank() }
                    ?: artwork?.optString("1000x1000")
                        ?.takeIf { it.isNotBlank() }
                    ?: artwork?.optString("_1000x1000")
                        ?.takeIf { it.isNotBlank() }
                    ?: artwork?.optString("150x150")
                        ?.takeIf { it.isNotBlank() }

            return AudiusTrack(
                id = id,
                title = title,
                artistName = artistName,
                artistId = artistId,
                durationSeconds = json.optInt("duration", MediaMetadata.UNKNOWN_DURATION),
                artworkUrl = artworkUrl,
                genre = json.optString("genre").takeIf { it.isNotBlank() },
                mood = json.optString("mood").takeIf { it.isNotBlank() },
                playCount =
                    when {
                        json.has("play_count") -> json.optLong("play_count", 0L)
                        json.has("playCount") -> json.optLong("playCount", 0L)
                        else -> 0L
                    },
                permalink = json.optString("permalink").takeIf { it.isNotBlank() },
            )
        }
    }
}
