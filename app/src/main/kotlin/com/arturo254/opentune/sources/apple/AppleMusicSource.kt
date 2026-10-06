package com.arturo254.opentune.sources.apple

import com.arturo254.opentune.catalog.CatalogTrackReference
import com.arturo254.opentune.core.model.AudioQualityInfo
import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.sources.SourceCapabilities
import com.arturo254.opentune.sources.SourceCapability
import com.arturo254.opentune.sources.TrackSearchPage
import com.arturo254.opentune.sources.TrackSearchRequest
import com.arturo254.opentune.sources.TrackSearchSource
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

data class AppleMusicTrackPage(
    val items: List<AppleMusicTrack>,
    val limit: Int,
    val offset: Int,
    val hasMore: Boolean,
)

data class AppleMusicTrack(
    val id: String,
    val title: String,
    val artistName: String,
    val albumName: String? = null,
    val durationMs: Long? = null,
    val isrc: String? = null,
    val explicit: Boolean? = null,
    val artworkUrl: String? = null,
    val albumId: String? = null,
)

interface AppleMusicCatalogGateway {
    suspend fun searchTracksPage(
        query: String,
        limit: Int,
        offset: Int,
    ): Result<AppleMusicTrackPage>
}

@Singleton
class DefaultAppleMusicCatalogGateway
@Inject
constructor() : AppleMusicCatalogGateway {
    private val json = Json { ignoreUnknownKeys = true }
    private val tokenMutex = Mutex()

    @Volatile
    private var token: String? = null

    @Volatile
    private var tokenExpiryMs: Long = 0L

    private val client =
        OkHttpClient
            .Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .callTimeout(35, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

    override suspend fun searchTracksPage(
        query: String,
        limit: Int,
        offset: Int,
    ): Result<AppleMusicTrackPage> =
        withContext(Dispatchers.IO) {
            runCatching {
                val normalizedLimit = limit.coerceIn(1, MAX_PAGE_SIZE)
                val root =
                    requestCatalog(
                        query = query,
                        limit = normalizedLimit,
                        offset = offset,
                        retryWithFreshToken = true,
                    )
                val songs = root["results"]
                    ?.let { runCatching { it.jsonObject }.getOrNull() }
                    ?.get("songs")
                    ?.let { runCatching { it.jsonObject }.getOrNull() }

                val rawItems =
                    songs
                        ?.get("data")
                        ?.let { runCatching { it.jsonArray }.getOrNull() }
                        ?: JsonArray(emptyList())

                val next = songs?.stringValue("next")
                AppleMusicTrackPage(
                    items = rawItems.mapNotNull(::parseSong),
                    limit = normalizedLimit,
                    offset = offset,
                    hasMore = !next.isNullOrBlank() || rawItems.size >= normalizedLimit,
                )
            }
        }

    private fun requestCatalog(
        query: String,
        limit: Int,
        offset: Int,
        retryWithFreshToken: Boolean,
    ): JsonObject {
        val developerToken =
            kotlinx.coroutines.runBlocking {
                developerToken()
            }

        val url =
            (API_BASE + STOREFRONT + "/search")
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("term", query)
                .addQueryParameter("types", "songs")
                .addQueryParameter("limit", limit.toString())
                .addQueryParameter("offset", offset.toString())
                .addQueryParameter("include", "albums,artists")
                .addQueryParameter(
                    "extend",
                    "artistUrl,editorialArtwork,trackCount,upc",
                ).build()

        val request =
            Request
                .Builder()
                .url(url)
                .header("Authorization", "Bearer $developerToken")
                .header("Origin", "https://music.apple.com")
                .header("Referer", "https://music.apple.com/")
                .header("User-Agent", USER_AGENT)
                .get()
                .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 401 && retryWithFreshToken) {
                token = null
                tokenExpiryMs = 0L
                return requestCatalog(
                    query = query,
                    limit = limit,
                    offset = offset,
                    retryWithFreshToken = false,
                )
            }

            if (!response.isSuccessful) {
                error("Apple Music catalog HTTP " + response.code)
            }

            return json.parseToJsonElement(response.body.string()).jsonObject
        }
    }

    private suspend fun developerToken(): String =
        tokenMutex.withLock {
            val now = System.currentTimeMillis()
            token
                ?.takeIf { tokenExpiryMs > now + TOKEN_EXPIRY_SKEW_MS }
                ?.let { return@withLock it }

            val fresh = fetchDeveloperToken()
            token = fresh
            tokenExpiryMs = parseJwtExpiry(fresh) ?: (now + FALLBACK_TOKEN_TTL_MS)
            fresh
        }

    private fun fetchDeveloperToken(): String {
        val page =
            executeText(
                Request
                    .Builder()
                    .url("https://music.apple.com/us/browse")
                    .header("User-Agent", USER_AGENT)
                    .get()
                    .build(),
            )

        extractToken(page)?.let { return it }

        val bundlePaths =
            ASSET_SCRIPT_REGEX
                .findAll(page)
                .mapNotNull { match -> match.groupValues.getOrNull(1) }
                .filterNot { it.contains("-legacy", ignoreCase = true) }
                .distinct()
                .take(MAX_BUNDLES_TO_SCAN)
                .toList()

        bundlePaths.forEach { path ->
            val bundleUrl =
                if (path.startsWith("http://") || path.startsWith("https://")) {
                    path
                } else {
                    "https://music.apple.com" + path
                }

            val body =
                runCatching {
                    executeText(
                        Request
                            .Builder()
                            .url(bundleUrl)
                            .header("User-Agent", USER_AGENT)
                            .get()
                            .build(),
                    )
                }.getOrNull()
                    ?: return@forEach

            extractToken(body)?.let { return it }
        }

        error("Apple Music developer token was not found")
    }

    private fun executeText(request: Request): String =
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Apple Music bootstrap HTTP " + response.code)
            }
            response.body.string()
        }

    private fun extractToken(text: String): String? {
        DEV_TOKEN_PARAM_REGEX
            .find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.takeIf(::looksLikeJwt)
            ?.let { return it }

        JWT_REGEX
            .find(text)
            ?.value
            ?.takeIf(::looksLikeJwt)
            ?.let { return it }

        return null
    }

    private fun looksLikeJwt(value: String): Boolean =
        value.count { it == '.' } == 2 &&
            value.length > 100

    private fun parseJwtExpiry(jwt: String): Long? =
        runCatching {
            val payload = jwt.split('.')[1]
            val padding = "=".repeat((4 - payload.length % 4) % 4)
            val decoded =
                Base64
                    .getUrlDecoder()
                    .decode(payload + padding)
                    .toString(StandardCharsets.UTF_8)
            val root = json.parseToJsonElement(decoded).jsonObject
            root["exp"]
                ?.jsonPrimitive
                ?.longOrNull
                ?.times(1_000L)
                ?.minus(TOKEN_EXPIRY_SKEW_MS)
        }.getOrNull()

    private fun parseSong(element: kotlinx.serialization.json.JsonElement): AppleMusicTrack? {
        val song = element.jsonObject
        val id = song.stringValue("id")?.takeIf(String::isNotBlank) ?: return null
        val attributes =
            song["attributes"]
                ?.let { runCatching { it.jsonObject }.getOrNull() }
                ?: return null
        val title = attributes.stringValue("name")?.trim().orEmpty()
        if (title.isBlank()) return null

        val albumRelationship =
            song["relationships"]
                ?.let { runCatching { it.jsonObject }.getOrNull() }
                ?.get("albums")
                ?.let { runCatching { it.jsonObject }.getOrNull() }
                ?.get("data")
                ?.let { runCatching { it.jsonArray }.getOrNull() }
                ?.firstOrNull()
                ?.let { runCatching { it.jsonObject }.getOrNull() }

        val artwork =
            attributes["artwork"]
                ?.let { runCatching { it.jsonObject }.getOrNull() }

        return AppleMusicTrack(
            id = id,
            title = title,
            artistName = attributes.stringValue("artistName").orEmpty(),
            albumName = attributes.stringValue("albumName"),
            durationMs =
                attributes.longValue("durationInMillis")
                    ?: attributes.intValue("durationInMillis")?.toLong(),
            isrc = attributes.stringValue("isrc")?.uppercase(),
            explicit = attributes.stringValue("contentRating")?.equals("explicit", true),
            artworkUrl = artwork?.let(::artworkUrl),
            albumId = albumRelationship?.stringValue("id"),
        )
    }

    private fun artworkUrl(artwork: JsonObject): String? {
        val template = artwork.stringValue("url") ?: return null
        val width = artwork.intValue("width") ?: DEFAULT_ARTWORK_SIZE
        val height = artwork.intValue("height") ?: DEFAULT_ARTWORK_SIZE
        val size = maxOf(width, height, DEFAULT_ARTWORK_SIZE).coerceAtMost(MAX_ARTWORK_SIZE)

        return template
            .replace("{w}", size.toString())
            .replace("{h}", size.toString())
            .replace("{f}", "jpg")
    }

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
        private const val API_BASE = "https://amp-api.music.apple.com/v1/catalog/"
        private const val STOREFRONT = "us"
        private const val MAX_PAGE_SIZE = 25
        private const val MAX_BUNDLES_TO_SCAN = 6
        private const val DEFAULT_ARTWORK_SIZE = 1200
        private const val MAX_ARTWORK_SIZE = 3000
        private const val TOKEN_EXPIRY_SKEW_MS = 5L * 60L * 1_000L
        private const val FALLBACK_TOKEN_TTL_MS = 12L * 60L * 60L * 1_000L
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Mobile Safari/537.36"

        private val DEV_TOKEN_PARAM_REGEX =
            Regex("""devToken=([A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+)""")
        private val JWT_REGEX =
            Regex(
                """eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+""",
            )
        private val ASSET_SCRIPT_REGEX =
            Regex("""src=["']([^"']*/assets/[^"']+\.js)["']""")
    }
}

@Singleton
class AppleMusicSource
@Inject
constructor(
    private val gateway: AppleMusicCatalogGateway,
) : TrackSearchSource {
    override val id: ProviderId = ProviderId.APPLE_MUSIC
    override val displayName: String = "Apple Music"

    override val capabilities: SourceCapabilities =
        SourceCapabilities.of(
            SourceCapability.TRACK_SEARCH,
            SourceCapability.TRACK_METADATA,
            SourceCapability.ALBUMS,
            SourceCapability.ARTISTS,
            SourceCapability.PLAYLISTS,
            SourceCapability.STATIC_ARTWORK,
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
                TrackSearchPage(
                    items = page.items.map(AppleMusicTrack::toSourceVariant),
                    continuationToken =
                        if (page.hasMore && page.items.isNotEmpty()) {
                            encodeContinuation(page.offset + page.limit)
                        } else {
                            null
                        },
                )
            }
    }

    private fun decodeContinuation(token: String?): Int {
        if (token == null) return 0
        require(token.startsWith(CONTINUATION_PREFIX)) {
            "Invalid Apple Music continuation token"
        }

        return token
            .removePrefix(CONTINUATION_PREFIX)
            .toIntOrNull()
            ?.takeIf { it >= 0 }
            ?: throw IllegalArgumentException("Invalid Apple Music continuation offset")
    }

    private fun encodeContinuation(offset: Int): String = CONTINUATION_PREFIX + offset

    companion object {
        private const val CONTINUATION_PREFIX = "offset:"
        private const val MAX_PAGE_SIZE = 25
    }
}

internal fun AppleMusicTrack.toSourceVariant(): SourceVariant =
    SourceVariant(
        provider = ProviderId.APPLE_MUSIC,
        sourceId = id,
        title = title,
        artists =
            artistName
                .split(',', '&')
                .map(String::trim)
                .filter(String::isNotBlank)
                .ifEmpty { listOf(artistName).filter(String::isNotBlank) },
        albumId = albumId,
        albumName = albumName,
        durationMs = durationMs?.takeIf { it > 0L },
        isrc = isrc?.takeIf(String::isNotBlank),
        explicit = explicit,
        artworkUrl = artworkUrl,
        quality = AudioQualityInfo(lossless = null),
    )

internal fun SourceVariant.toAppleMusicCatalogReference(): CatalogTrackReference {
    require(provider == ProviderId.APPLE_MUSIC) {
        "Only Apple Music variants can be converted to Apple Music catalog references"
    }

    return CatalogTrackReference(
        cacheId = "apple-music:" + sourceId,
        title = title,
        artists = artists,
        durationMs = durationMs ?: 0L,
        thumbnailUrl = artworkUrl,
        albumId = albumId?.let { "apple-music-album:" + it },
        albumName = albumName,
        explicit = explicit ?: false,
    )
}
