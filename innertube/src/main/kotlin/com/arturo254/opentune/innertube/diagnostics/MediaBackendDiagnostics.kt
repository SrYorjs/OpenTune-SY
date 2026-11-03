package com.arturo254.opentune.innertube.diagnostics

import com.arturo254.opentune.innertube.NewPipeUtils
import com.arturo254.opentune.innertube.PlaybackAuthState
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.YouTubeClient
import java.net.URI
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.schabi.newpipe.extractor.stream.StreamInfo

/**
 * Experimental, read-only diagnostic. Never returns or logs cookies, API keys,
 * auth headers, signed stream URLs, proxy URLs, or raw exception messages.
 *
 * This does NOT alter playback, downloading, user authentication, or source ranking.
 */
data class AudioBackendObservation(
    val engine: String,
    val videoId: String,
    val hasMetadata: Boolean,
    val audioFormats: Int,
    val formats: List<String>,
    val status: String,
    val accessCheck: String = "NO_PROBADO",
)

data class PipedSearchObservation(
    val filter: String,
    val results: Int,
    val status: String,
)

data class BackendComparisonReport(
    val pipedHost: String,
    val observations: List<AudioBackendObservation>,
    val search: List<PipedSearchObservation>,
)

internal val pipedMusicFilters =
    listOf("music_songs", "music_artists", "music_albums", "music_playlists")

internal fun validatedPipedBaseUrl(address: String): String {
    val parsed = try {
        URI(address.trim())
    } catch (_: Exception) {
        throw IllegalArgumentException("Introduce una URL HTTPS válida de una API Piped")
    }
    require(
        parsed.scheme.equals("https", ignoreCase = true) &&
            !parsed.host.isNullOrBlank() &&
            parsed.userInfo == null &&
            parsed.query == null &&
            parsed.fragment == null &&
            !parsed.path.orEmpty().contains("..")
    ) { "La instancia Piped debe usar HTTPS sin credenciales ni parámetros" }
    return parsed.toString().trimEnd('/')
}

internal fun validatedPublicVideoId(id: String): String {
    val value = id.trim()
    require(value.matches(Regex("[A-Za-z0-9_-]{11}"))) {
        "Introduce identificadores públicos de YouTube de 11 caracteres"
    }
    return value
}

internal fun parsePipedStreamResponse(videoId: String, root: JsonObject): AudioBackendObservation {
    val formats = (root["audioStreams"] as? JsonArray).orEmpty()
        .mapNotNull { it as? JsonObject }
    val labels = formats.map { item ->
        listOfNotNull(
            item.stringValue("format"),
            item.stringValue("codec"),
            item.stringValue("mimeType"),
        ).filter(String::isNotBlank).distinct().joinToString(" · ")
    }.filter(String::isNotBlank).distinct().take(6)
    val hasMetadata = !root.stringValue("title").isNullOrBlank() ||
        root.longValue("duration")?.let { it > 0 } == true
    return AudioBackendObservation(
        engine = "Piped",
        videoId = videoId,
        hasMetadata = hasMetadata,
        audioFormats = formats.size,
        formats = labels,
        status = when {
            !hasMetadata && formats.isEmpty() -> "RESPUESTA_SIN_METADATOS"
            formats.isEmpty() -> "SIN_FORMATOS_AUDIO"
            else -> "OK"
        },
    )
}

internal fun parsePipedSearchResponse(filter: String, root: JsonObject): PipedSearchObservation {
    val items = root["items"] as? JsonArray
    return PipedSearchObservation(
        filter = filter,
        results = items?.size ?: 0,
        status = if (items == null) "RESPUESTA_INESPERADA" else "OK",
    )
}

private fun JsonObject.stringValue(name: String) =
    (get(name) as? JsonPrimitive)?.contentOrNull

private fun JsonObject.longValue(name: String) = stringValue(name)?.toLongOrNull()

object MediaBackendDiagnostics {
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(14, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun compare(
        videoIds: List<String>,
        pipedBaseUrl: String,
        searchQuery: String = "lofi",
    ): BackendComparisonReport = supervisorScope {
        val ids = videoIds.take(3).map(::validatedPublicVideoId).distinct()
        require(ids.isNotEmpty()) { "Introduce al menos un identificador de video" }
        val base = validatedPipedBaseUrl(pipedBaseUrl)
        val host = URI(base).host
        val videos = ids.map { id ->
            async {
                listOf(
                    async { observeInnerTube(id) },
                    async { observeNewPipe(id) },
                    async { observePiped(id, base) },
                ).awaitAll()
            }
        }
        val search = pipedMusicFilters.map { filter ->
            async { observePipedSearch(filter, searchQuery.take(100), base) }
        }
        BackendComparisonReport(
            pipedHost = host,
            observations = videos.awaitAll().flatten(),
            search = search.awaitAll(),
        )
    }

    private suspend fun observeInnerTube(id: String): AudioBackendObservation {
        return withTimeoutOrNull(20_000L) {
            try {
                // Explicitly anonymous: no cookie, visitor data, or PO tokens.
                val response = YouTube.player(
                    videoId = id,
                    client = YouTubeClient.WEB_REMIX,
                    setLogin = false,
                    authState = PlaybackAuthState.EMPTY,
                ).getOrThrow()
                val formats = response.streamingData?.adaptiveFormats.orEmpty()
                    .filter { it.isAudio }
                AudioBackendObservation(
                    engine = "InnerTube",
                    videoId = id,
                    hasMetadata = response.videoDetails != null,
                    audioFormats = formats.size,
                    formats = formats.mapNotNull { it.mimeType }
                        .distinct().take(6),
                    status = response.playabilityStatus.status
                        .takeIf { it != "OK" } ?: if (formats.isEmpty()) "SIN_FORMATOS_AUDIO" else "OK",
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                failure("InnerTube", id, e)
            }
        } ?: unavailable("InnerTube", id, "TIMEOUT")
    }

    private suspend fun observeNewPipe(id: String): AudioBackendObservation =
        withContext(Dispatchers.IO) {
            try {
                NewPipeUtils.ensureInitialized()
                val info = StreamInfo.getInfo("https://www.youtube.com/watch?v=$id")
                val streams = info.audioStreams.orEmpty()
                AudioBackendObservation(
                    engine = "NewPipeExtractor",
                    videoId = id,
                    hasMetadata = info.name.isNotBlank() || info.duration > 0,
                    audioFormats = streams.size,
                    formats = streams.map { stream ->
                        listOfNotNull(stream.format?.name, stream.codec)
                            .filter(String::isNotBlank).distinct().joinToString(" · ")
                    }.filter(String::isNotBlank).distinct().take(6),
                    status = if (streams.isNotEmpty()) "OK" else "SIN_FORMATOS_AUDIO",
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                failure("NewPipeExtractor", id, e)
            }
        }

    private suspend fun observePiped(id: String, base: String): AudioBackendObservation =
        withContext(Dispatchers.IO) {
            try {
                val root = getJson("$base/streams/$id")
                parsePipedStreamResponse(id, root)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                failure("Piped", id, e)
            }
        }

    private suspend fun observePipedSearch(
        filter: String,
        query: String,
        base: String,
    ): PipedSearchObservation = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext PipedSearchObservation(
            filter, 0, "CONSULTA_VACIA",
        )
        try {
            val url = "$base/search".toHttpUrl().newBuilder()
                .addQueryParameter("q", query)
                .addQueryParameter("filter", filter)
                .build()
            parsePipedSearchResponse(filter, getJson(url.toString()))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            PipedSearchObservation(filter, 0, safeError(e))
        }
    }

    private fun getJson(url: String): JsonObject =
        client.newCall(
            Request.Builder().get().url(url).header("Accept", "application/json").build(),
        ).execute().use { response ->
            if (!response.isSuccessful) throw PipedHttpError(response.code)
            val body = response.body.string()
            require(body.length <= 2_000_000) { "RESPUESTA_DEMASIADO_GRANDE" }
            json.parseToJsonElement(body).jsonObject
        }

    private fun failure(engine: String, videoId: String, error: Exception) =
        unavailable(engine, videoId, safeError(error))

    private fun unavailable(engine: String, videoId: String, status: String) =
        AudioBackendObservation(
            engine = engine,
            videoId = videoId,
            hasMetadata = false,
            audioFormats = 0,
            formats = emptyList(),
            status = status,
        )

    private fun safeError(error: Exception): String = when (error) {
        is PipedHttpError -> "HTTP_${error.status}"
        else -> "ERROR_${error.javaClass.simpleName.take(45)}"
    }

    private class PipedHttpError(val status: Int) : Exception()
}
