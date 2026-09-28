package com.arturo254.opentune.deezer

import com.arturo254.opentune.catalog.CatalogTrackReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Read-only Deezer catalog client.
 *
 * It is used for discovery/search metadata only. Playback is resolved through
 * OpenTune's existing YouTube Music engine by CatalogPlaybackResolver.
 */
object Deezer {
    private const val BASE_URL = "https://api.deezer.com"

    private val client =
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

    suspend fun searchTracks(
        query: String,
        limit: Int = 40,
    ): Result<List<DeezerTrack>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url =
                    (BASE_URL + "/search")
                        .toHttpUrl()
                        .newBuilder()
                        .addQueryParameter("q", query.trim())
                        .addQueryParameter("limit", limit.coerceIn(1, 50).toString())
                        .addQueryParameter("strict", "on")
                        .build()

                requestTracks(url.toString())
            }
        }

    suspend fun trendingTracks(limit: Int = 24): Result<List<DeezerTrack>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url =
                    (BASE_URL + "/chart/0/tracks")
                        .toHttpUrl()
                        .newBuilder()
                        .addQueryParameter("limit", limit.coerceIn(1, 50).toString())
                        .build()

                requestTracks(url.toString())
            }
        }

    private fun requestTracks(url: String): List<DeezerTrack> {
        val request =
            Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "OpenTune-SY/3")
                .get()
                .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Deezer HTTP " + response.code)
            }

            val root = JSONObject(response.body.string())
            root.optJSONObject("error")?.let { error ->
                error(error.optString("message", "Deezer catalog error"))
            }

            return parseTracks(root.optJSONArray("data") ?: JSONArray())
        }
    }

    private fun parseTracks(data: JSONArray): List<DeezerTrack> =
        buildList {
            for (index in 0 until data.length()) {
                val json = data.optJSONObject(index) ?: continue
                DeezerTrack.fromJson(json)?.let(::add)
            }
        }
}

data class DeezerTrack(
    val id: String,
    val title: String,
    val artistName: String,
    val artistId: String?,
    val albumId: String?,
    val albumName: String?,
    val durationSeconds: Int,
    val explicit: Boolean,
    val artworkUrl: String?,
    val link: String?,
) {
    fun toCatalogReference(): CatalogTrackReference =
        CatalogTrackReference(
            cacheId = "deezer:" + id,
            title = title,
            artists = listOf(artistName),
            durationMs = durationSeconds.coerceAtLeast(0) * 1000L,
            thumbnailUrl = artworkUrl,
            albumId = albumId?.let { "deezer-album:" + it },
            albumName = albumName,
            explicit = explicit,
        )

    companion object {
        fun fromJson(json: JSONObject): DeezerTrack? {
            val id = json.optString("id").takeIf { it.isNotBlank() } ?: return null
            val title =
                json.optString("title")
                    .takeIf { it.isNotBlank() }
                    ?: json.optString("title_short").takeIf { it.isNotBlank() }
                    ?: return null

            val artist = json.optJSONObject("artist")
            val album = json.optJSONObject("album")

            val artistName =
                artist?.optString("name")
                    ?.takeIf { it.isNotBlank() }
                    ?: return null

            val artwork =
                album?.optString("cover_xl")
                    ?.takeIf { it.isNotBlank() }
                    ?: album?.optString("cover_big")
                        ?.takeIf { it.isNotBlank() }
                    ?: album?.optString("cover_medium")
                        ?.takeIf { it.isNotBlank() }

            return DeezerTrack(
                id = id,
                title = title,
                artistName = artistName,
                artistId = artist?.optString("id")?.takeIf { it.isNotBlank() },
                albumId = album?.optString("id")?.takeIf { it.isNotBlank() },
                albumName = album?.optString("title")?.takeIf { it.isNotBlank() },
                durationSeconds = json.optInt("duration", 0),
                explicit = json.optBoolean("explicit_lyrics", false),
                artworkUrl = artwork,
                link = json.optString("link").takeIf { it.isNotBlank() },
            )
        }
    }
}
