package com.arturo254.opentune.sources.tidal

import com.arturo254.opentune.catalog.CatalogTrackReference
import com.arturo254.opentune.core.model.AudioQualityInfo
import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.sources.SourceCapabilities
import com.arturo254.opentune.sources.SourceCapability
import com.arturo254.opentune.sources.TrackSearchPage
import com.arturo254.opentune.sources.TrackSearchRequest
import com.arturo254.opentune.sources.TrackSearchSource
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

data class TidalTrackPage(
    val items: List<TidalTrack>,
    val total: Int,
    val limit: Int,
    val offset: Int,
)

data class TidalTrack(
    val id: String,
    val title: String,
    val version: String? = null,
    val artists: List<String>,
    val albumId: String? = null,
    val albumTitle: String? = null,
    val coverId: String? = null,
    val durationSeconds: Long? = null,
    val isrc: String? = null,
    val explicit: Boolean? = null,
    val audioQuality: String? = null,
)

interface TidalCatalogGateway {
    suspend fun searchTracksPage(
        query: String,
        limit: Int,
        offset: Int,
    ): Result<TidalTrackPage>
}

@Singleton
class DefaultTidalCatalogGateway
@Inject
constructor() : TidalCatalogGateway {
    private val json = Json { ignoreUnknownKeys = true }

    private val client =
        OkHttpClient
            .Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

    override suspend fun searchTracksPage(
        query: String,
        limit: Int,
        offset: Int,
    ): Result<TidalTrackPage> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url =
                    BASE_URL
                        .toHttpUrl()
                        .newBuilder()
                        .addPathSegments("search/tracks")
                        .addQueryParameter("countryCode", COUNTRY_CODE)
                        .addQueryParameter("locale", LOCALE)
                        .addQueryParameter("deviceType", DEVICE_TYPE)
                        .addQueryParameter("query", query)
                        .addQueryParameter("limit", limit.toString())
                        .addQueryParameter("offset", offset.toString())
                        .build()

                val request =
                    Request
                        .Builder()
                        .url(url)
                        .header("Accept", "application/json")
                        .header("User-Agent", USER_AGENT)
                        .header("x-tidal-token", PUBLIC_TOKEN)
                        .get()
                        .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        error("TIDAL catalog HTTP " + response.code)
                    }

                    val body = response.body.string()
                    val root = json.parseToJsonElement(body).jsonObject
                    val rawItems = root.arrayOrEmpty("items")
                    val tracks = rawItems.mapNotNull(::parseTrack)
                    val total =
                        root.intValue("totalNumberOfItems")
                            ?: root.intValue("total")
                            ?: (offset + rawItems.size)

                    TidalTrackPage(
                        items = tracks,
                        total = total,
                        limit = limit,
                        offset = offset,
                    )
                }
            }
        }

    private fun parseTrack(element: kotlinx.serialization.json.JsonElement): TidalTrack? {
        val item = element.jsonObject
        val id =
            item.stringValue("id")
                ?: item.longValue("id")?.toString()
                ?: return null
        val title = item.stringValue("title")?.trim().orEmpty()
        if (title.isBlank()) return null

        val artists =
            item.arrayOrEmpty("artists")
                .mapNotNull { artist ->
                    runCatching {
                        artist.jsonObject.stringValue("name")
                    }.getOrNull()
                }.filterNotNull()
                .filter(String::isNotBlank)

        val album = item["album"]?.let { runCatching { it.jsonObject }.getOrNull() }

        return TidalTrack(
            id = id,
            title = title,
            version = item.stringValue("version"),
            artists =
                artists.ifEmpty {
                    listOfNotNull(
                        item["artist"]
                            ?.let { runCatching { it.jsonObject }.getOrNull() }
                            ?.stringValue("name"),
                    )
                },
            albumId =
                album?.stringValue("id")
                    ?: album?.longValue("id")?.toString(),
            albumTitle =
                album?.stringValue("title")
                    ?: album?.stringValue("name"),
            coverId = album?.stringValue("cover"),
            durationSeconds =
                item.longValue("duration")
                    ?: item.intValue("duration")?.toLong(),
            isrc = item.stringValue("isrc")?.uppercase(),
            explicit =
                item["explicit"]
                    ?.jsonPrimitive
                    ?.contentOrNull
                    ?.toBooleanStrictOrNull(),
            audioQuality = item.stringValue("audioQuality"),
        )
    }

    private fun JsonObject.arrayOrEmpty(name: String): JsonArray =
        this[name]?.let {
            runCatching { it.jsonArray }.getOrNull()
        } ?: JsonArray(emptyList())

    private fun JsonObject.stringValue(name: String): String? =
        this[name]
            ?.jsonPrimitive
            ?.contentOrNull

    private fun JsonObject.intValue(name: String): Int? =
        this[name]
            ?.jsonPrimitive
            ?.intOrNull

    private fun JsonObject.longValue(name: String): Long? =
        this[name]
            ?.jsonPrimitive
            ?.longOrNull

    companion object {
        private const val BASE_URL = "https://tidal.com/v1/"
        private const val PUBLIC_TOKEN = "49YxDN9a2aFV6RTG"
        private const val COUNTRY_CODE = "US"
        private const val LOCALE = "en_US"
        private const val DEVICE_TYPE = "BROWSER"
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Mobile Safari/537.36"
    }
}

@Singleton
class TidalMusicSource
@Inject
constructor(
    private val gateway: TidalCatalogGateway,
) : TrackSearchSource {
    override val id: ProviderId = ProviderId.TIDAL
    override val displayName: String = "TIDAL"

    override val capabilities: SourceCapabilities =
        SourceCapabilities.of(
            SourceCapability.TRACK_SEARCH,
            SourceCapability.TRACK_METADATA,
            SourceCapability.ALBUMS,
            SourceCapability.ARTISTS,
            SourceCapability.PLAYLISTS,
            SourceCapability.STATIC_ARTWORK,
            SourceCapability.AUDIO_QUALITY,
        )

    override suspend fun searchTracks(request: TrackSearchRequest): Result<TrackSearchPage> {
        val query = request.query.trim()
        if (query.isEmpty()) {
            return Result.success(TrackSearchPage(items = emptyList()))
        }

        val offset =
            runCatching { decodeContinuation(request.continuationToken) }
                .getOrElse { return Result.failure(it) }
        val limit = request.limit.coerceAtMost(MAX_PAGE_SIZE)

        return gateway
            .searchTracksPage(
                query = query,
                limit = limit,
                offset = offset,
            ).map { page ->
                val nextOffset =
                    (page.offset + page.limit)
                        .takeIf {
                            page.items.isNotEmpty() &&
                                page.offset + page.limit < page.total
                        }

                TrackSearchPage(
                    items = page.items.map(TidalTrack::toSourceVariant),
                    continuationToken = nextOffset?.let(::encodeContinuation),
                )
            }
    }

    private fun decodeContinuation(token: String?): Int {
        if (token == null) return 0
        require(token.startsWith(CONTINUATION_PREFIX)) {
            "Invalid TIDAL continuation token"
        }

        return token
            .removePrefix(CONTINUATION_PREFIX)
            .toIntOrNull()
            ?.takeIf { it >= 0 }
            ?: throw IllegalArgumentException("Invalid TIDAL continuation offset")
    }

    private fun encodeContinuation(offset: Int): String = CONTINUATION_PREFIX + offset

    companion object {
        private const val CONTINUATION_PREFIX = "offset:"
        private const val MAX_PAGE_SIZE = 50
    }
}

internal fun TidalTrack.toSourceVariant(): SourceVariant {
    val completeTitle =
        version
            ?.trim()
            ?.takeIf { it.isNotBlank() && !title.contains(it, ignoreCase = true) }
            ?.let { "$title ($it)" }
            ?: title

    return SourceVariant(
        provider = ProviderId.TIDAL,
        sourceId = id,
        title = completeTitle,
        artists = artists,
        albumId = albumId,
        albumName = albumTitle,
        durationMs = durationSeconds?.takeIf { it > 0L }?.times(1_000L),
        isrc = isrc?.takeIf(String::isNotBlank),
        explicit = explicit,
        artworkUrl = coverId?.let(::tidalArtworkUrl),
        quality = tidalQuality(audioQuality),
    )
}

private fun tidalQuality(value: String?): AudioQualityInfo? =
    when (value?.trim()?.uppercase()) {
        "HI_RES_LOSSLESS" ->
            AudioQualityInfo(
                lossless = true,
                codec = "FLAC",
            )

        "LOSSLESS" ->
            AudioQualityInfo(
                lossless = true,
                codec = "FLAC",
            )

        "HIGH", "LOW" ->
            AudioQualityInfo(
                lossless = false,
            )

        else -> null
    }

private fun tidalArtworkUrl(coverId: String): String =
    "https://resources.tidal.com/images/" +
        coverId.replace('-', '/') +
        "/1280x1280.jpg"

internal fun SourceVariant.toTidalCatalogReference(): CatalogTrackReference {
    require(provider == ProviderId.TIDAL) {
        "Only TIDAL variants can be converted to TIDAL catalog references"
    }

    return CatalogTrackReference(
        cacheId = "tidal:" + sourceId,
        title = title,
        artists = artists,
        durationMs = durationMs ?: 0L,
        thumbnailUrl = artworkUrl,
        albumId = albumId?.let { "tidal-album:" + it },
        albumName = albumName,
        explicit = explicit ?: false,
    )
}
