package com.arturo254.opentune.lyrics

import android.content.Context
import com.arturo254.opentune.constants.EnableNetEaseLyricsKey
import com.arturo254.opentune.utils.dataStore
import com.arturo254.opentune.utils.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Synced-LRC fallback backed by NetEase Cloud Music's public web endpoints.
 *
 * Only synchronized line lyrics are accepted here. Plain text is deliberately
 * rejected so this provider improves timing quality rather than competing with
 * the existing plain-lyrics fallbacks.
 */
object NetEaseLyricsProvider : LyricsProvider {
    override val name: String = "NetEase LRC"

    private const val BASE_URL = "https://music.163.com"
    private val timestampRegex = Regex("""\[\d{1,2}:\d{2}(?:\.\d{1,3})?]""")

    private val client =
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(7, TimeUnit.SECONDS)
            .callTimeout(9, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableNetEaseLyricsKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
    ): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val candidate =
                    searchCandidates(title, artist, duration)
                        .maxByOrNull { it.score }
                        ?.takeIf { it.score >= 0.44 }
                        ?: error("No sufficiently accurate NetEase match")

                val lyrics = fetchLyrics(candidate.id)
                if (!timestampRegex.containsMatchIn(lyrics)) {
                    error("NetEase result is not synchronized LRC")
                }
                lyrics
            }
        }

    private fun searchCandidates(
        title: String,
        artist: String,
        duration: Int,
    ): List<Candidate> {
        val query = listOf(title, artist).filter(String::isNotBlank).joinToString(" ")
        val url =
            (BASE_URL + "/api/search/get")
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("s", query)
                .addQueryParameter("type", "1")
                .addQueryParameter("limit", "10")
                .addQueryParameter("offset", "0")
                .build()

        val request =
            Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("Referer", "https://music.163.com/")
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126 Mobile Safari/537.36"
                )
                .get()
                .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("NetEase search HTTP " + response.code)
            val root = JSONObject(response.body.string())
            val songs = root.optJSONObject("result")?.optJSONArray("songs") ?: JSONArray()

            return buildList {
                for (index in 0 until songs.length()) {
                    val song = songs.optJSONObject(index) ?: continue
                    val songId = song.optString("id").takeIf(String::isNotBlank) ?: continue
                    val candidateTitle = song.optString("name")
                    val artists =
                        (song.optJSONArray("artists") ?: song.optJSONArray("ar") ?: JSONArray())
                            .let(::artistNames)
                    val durationMs =
                        when {
                            song.has("duration") -> song.optLong("duration", 0L)
                            song.has("dt") -> song.optLong("dt", 0L)
                            else -> 0L
                        }

                    add(
                        Candidate(
                            id = songId,
                            score =
                                matchScore(
                                    expectedTitle = title,
                                    expectedArtist = artist,
                                    expectedDurationSec = duration,
                                    candidateTitle = candidateTitle,
                                    candidateArtist = artists.joinToString(" "),
                                    candidateDurationMs = durationMs,
                                ),
                        )
                    )
                }
            }
        }
    }

    private fun fetchLyrics(songId: String): String {
        val url =
            (BASE_URL + "/api/song/lyric")
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("id", songId)
                .addQueryParameter("lv", "-1")
                .addQueryParameter("kv", "-1")
                .addQueryParameter("tv", "-1")
                .addQueryParameter("yv", "-1")
                .build()

        val request =
            Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("Referer", "https://music.163.com/")
                .header("User-Agent", "OpenTune-SY/3")
                .get()
                .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("NetEase lyric HTTP " + response.code)
            val root = JSONObject(response.body.string())
            if (root.optBoolean("nolyric", false) || root.optBoolean("uncollected", false)) {
                error("Lyrics unavailable")
            }
            val wordSynced =
                root
                    .optJSONObject("yrc")
                    ?.optString("lyric")
                    ?.takeIf(String::isNotBlank)
                    ?.let(::convertYrcToEnhancedLrc)
                    ?.takeIf(String::isNotBlank)

            if (wordSynced != null) return wordSynced

            return root
                .optJSONObject("lrc")
                ?.optString("lyric")
                ?.takeIf(String::isNotBlank)
                ?: error("Empty NetEase lyrics")
        }
    }

    /**
     * NetEase YRC uses millisecond line/word ranges:
     * [lineStart,lineDuration](wordStart,wordDuration,0)word...
     *
     * OpenTune already understands enhanced LRC with <mm:ss.xxx> word markers,
     * so convert instead of throwing away the most accurate timing data.
     */
    private fun convertYrcToEnhancedLrc(yrc: String): String {
        val lineRegex = Regex("""^\[(\d+),(\d+)](.*)$""")
        val wordRegex = Regex("""\((\d+),(\d+),\d+\)([^()]*)""")

        return yrc
            .lineSequence()
            .mapNotNull { rawLine ->
                val line = rawLine.trim()
                if (line.isBlank() || line.startsWith("{")) return@mapNotNull null

                val lineMatch = lineRegex.matchEntire(line) ?: return@mapNotNull null
                val lineStart = lineMatch.groupValues[1].toLongOrNull() ?: return@mapNotNull null
                val body = lineMatch.groupValues[3]
                val words = wordRegex.findAll(body).toList()
                if (words.isEmpty()) return@mapNotNull null

                buildString {
                    append(formatLineTime(lineStart))
                    words.forEach { match ->
                        val rawWordStart = match.groupValues[1].toLongOrNull() ?: lineStart
                        val wordStart =
                            if (rawWordStart + 1_000L < lineStart) {
                                lineStart + rawWordStart
                            } else {
                                rawWordStart
                            }
                        val text = match.groupValues[3]
                        if (text.isNotEmpty()) {
                            append(formatWordTime(wordStart))
                            append(text)
                        }
                    }
                }.takeIf { it.length > 12 }
            }
            .joinToString("\n")
    }

    private fun formatLineTime(ms: Long): String =
        "[" + formatTimestamp(ms) + "]"

    private fun formatWordTime(ms: Long): String =
        "<" + formatTimestamp(ms) + ">"

    private fun formatTimestamp(ms: Long): String {
        val safe = ms.coerceAtLeast(0L)
        val minutes = safe / 60_000L
        val seconds = (safe % 60_000L) / 1_000L
        val millis = safe % 1_000L
        return String.format(Locale.ROOT, "%02d:%02d.%03d", minutes, seconds, millis)
    }

    private fun artistNames(array: JSONArray): List<String> =
        buildList {
            for (index in 0 until array.length()) {
                val value = array.optJSONObject(index)?.optString("name")
                if (!value.isNullOrBlank()) add(value)
            }
        }

    private fun matchScore(
        expectedTitle: String,
        expectedArtist: String,
        expectedDurationSec: Int,
        candidateTitle: String,
        candidateArtist: String,
        candidateDurationMs: Long,
    ): Double {
        val wantedTitle = normalize(expectedTitle)
        val foundTitle = normalize(candidateTitle)
        val wantedArtist = normalize(expectedArtist)
        val foundArtist = normalize(candidateArtist)

        var score = 0.0

        score +=
            when {
                wantedTitle == foundTitle -> 0.58
                foundTitle.contains(wantedTitle) || wantedTitle.contains(foundTitle) -> 0.44
                tokenOverlap(wantedTitle, foundTitle) >= 0.65 -> 0.34
                else -> 0.0
            }

        score +=
            when {
                wantedArtist == foundArtist -> 0.27
                foundArtist.contains(wantedArtist) || wantedArtist.contains(foundArtist) -> 0.22
                tokenOverlap(wantedArtist, foundArtist) >= 0.5 -> 0.15
                else -> 0.0
            }

        if (expectedDurationSec > 0 && candidateDurationMs > 0L) {
            val diff = abs(candidateDurationMs / 1000L - expectedDurationSec)
            score +=
                when {
                    diff <= 2L -> 0.15
                    diff <= 5L -> 0.11
                    diff <= 10L -> 0.06
                    else -> 0.0
                }
        }

        return score.coerceIn(0.0, 1.0)
    }

    private fun normalize(value: String): String =
        value
            .lowercase(Locale.ROOT)
            .replace(Regex("""\([^)]*(?:feat|ft|official|video|audio|remaster)[^)]*\)"""), " ")
            .replace(Regex("""[^\p{L}\p{N}]+"""), " ")
            .trim()
            .replace(Regex("""\s+"""), " ")

    private fun tokenOverlap(a: String, b: String): Double {
        val left = a.split(' ').filter(String::isNotBlank).toSet()
        val right = b.split(' ').filter(String::isNotBlank).toSet()
        if (left.isEmpty() || right.isEmpty()) return 0.0
        return left.intersect(right).size.toDouble() / left.union(right).size.toDouble()
    }

    private data class Candidate(
        val id: String,
        val score: Double,
    )
}
