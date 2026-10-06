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
        searchTracksPage(
            query = query,
            limit = limit,
        ).map(DeezerTrackPage::items)

    suspend fun searchTracksPage(
        query: String,
        limit: Int = 40,
        index: Int = 0,
    ): Result<DeezerTrackPage> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(index >= 0) { "Deezer search index cannot be negative" }

                val safeLimit = limit.coerceIn(1, 50)
                val url =
                    (BASE_URL + "/search")
                        .toHttpUrl()
                        .newBuilder()
                        .addQueryParameter("q", query.trim())
                        .addQueryParameter("limit", safeLimit.toString())
                        .addQueryParameter("index", index.toString())
                        .addQueryParameter("strict", "on")
                        .build()

                requestTrackPage(
                    url = url.toString(),
                    index = index,
                )
            }
        }

    suspend fun trackDetails(trackId: String): Result<DeezerTrack> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request =
                    Request.Builder()
                        .url(BASE_URL + "/track/" + trackId)
                        .header("Accept", "application/json")
                        .header("User-Agent", "OpenTune-SY/3")
                        .get()
                        .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        error("Deezer track HTTP " + response.code)
                    }
                    val json = JSONObject(response.body.string())
                    json.optJSONObject("error")?.let { apiError ->
                        error(apiError.optString("message", "Deezer track error"))
                    }
                    DeezerTrack.fromJson(json) ?: error("Invalid Deezer track")
                }
            }
        }

    suspend fun findAudioFeatures(
        title: String,
        artist: String,
        durationSeconds: Int,
    ): Result<DeezerTrack> =
        withContext(Dispatchers.IO) {
            runCatching {
                val candidates =
                    searchTracks(
                        query = listOf(title, artist).filter(String::isNotBlank).joinToString(" "),
                        limit = 8,
                    ).getOrThrow()

                val best =
                    candidates.maxByOrNull { candidate ->
                        val titleScore =
                            when {
                                candidate.title.equals(title, ignoreCase = true) -> 0.62
                                candidate.title.contains(title, ignoreCase = true) ||
                                    title.contains(candidate.title, ignoreCase = true) -> 0.46
                                else -> tokenOverlap(candidate.title, title) * 0.40
                            }
                        val artistScore =
                            when {
                                candidate.artistName.equals(artist, ignoreCase = true) -> 0.25
                                candidate.artistName.contains(artist, ignoreCase = true) ||
                                    artist.contains(candidate.artistName, ignoreCase = true) -> 0.19
                                else -> tokenOverlap(candidate.artistName, artist) * 0.15
                            }
                        val durationScore =
                            if (durationSeconds > 0 && candidate.durationSeconds > 0) {
                                when (kotlin.math.abs(durationSeconds - candidate.durationSeconds)) {
                                    in 0..2 -> 0.13
                                    in 3..5 -> 0.10
                                    in 6..10 -> 0.05
                                    else -> 0.0
                                }
                            } else {
                                0.0
                            }
                        titleScore + artistScore + durationScore
                    } ?: error("No Deezer candidate")

                trackDetails(best.id).getOrThrow()
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

    private fun requestTrackPage(
        url: String,
        index: Int,
    ): DeezerTrackPage {
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
            root.optJSONObject("error")?.let { apiError ->
                error(apiError.optString("message", "Deezer catalog error"))
            }

            val tracks = parseTracks(root.optJSONArray("data") ?: JSONArray())
            val total = root.optInt("total", index + tracks.size)
            val nextIndex =
                (index + tracks.size)
                    .takeIf { tracks.isNotEmpty() && it < total }

            return DeezerTrackPage(
                items = tracks,
                nextIndex = nextIndex,
            )
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

    private fun tokenOverlap(
        leftRaw: String,
        rightRaw: String,
    ): Double {
        fun normalize(value: String): Set<String> =
            value
                .lowercase()
                .replace(Regex("""[^\p{L}\p{N}]+"""), " ")
                .trim()
                .split(Regex("""\s+"""))
                .filter(String::isNotBlank)
                .toSet()

        val left = normalize(leftRaw)
        val right = normalize(rightRaw)
        if (left.isEmpty() || right.isEmpty()) return 0.0
        return left.intersect(right).size.toDouble() / left.union(right).size.toDouble()
    }

    private fun parseTracks(data: JSONArray): List<DeezerTrack> =
        buildList {
            for (index in 0 until data.length()) {
                val json = data.optJSONObject(index) ?: continue
                DeezerTrack.fromJson(json)?.let(::add)
            }
        }
}

data class DeezerTrackPage(
    val items: List<DeezerTrack>,
    val nextIndex: Int? = null,
)

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
    val bpm: Float? = null,
    val gainDb: Float? = null,
    val isrc: String? = null,
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
                bpm =
                    json.optDouble("bpm", Double.NaN)
                        .takeIf { !it.isNaN() && it > 0.0 }
                        ?.toFloat(),
                gainDb =
                    json.optDouble("gain", Double.NaN)
                        .takeIf { !it.isNaN() }
                        ?.toFloat(),
                isrc = json.optString("isrc").takeIf { it.isNotBlank() },
            )
        }
    }
}
