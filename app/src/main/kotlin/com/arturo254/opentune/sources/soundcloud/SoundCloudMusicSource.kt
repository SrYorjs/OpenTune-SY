package com.arturo254.opentune.sources.soundcloud

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
import com.arturo254.opentune.catalog.CatalogTrackReference
import com.arturo254.opentune.core.model.AudioQualityInfo
import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.sources.SourceCapabilities
import com.arturo254.opentune.sources.SourceCapability
import com.arturo254.opentune.sources.TrackSearchPage
import com.arturo254.opentune.sources.TrackSearchRequest
import com.arturo254.opentune.sources.TrackSearchSource
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

data class SoundCloudTrackPage(
    val items: List<SoundCloudTrack>,
    val total: Int?,
    val limit: Int,
    val offset: Int,
)

data class SoundCloudTrack(
    val id: String,
    val title: String,
    val artists: List<String>,
    val albumName: String? = null,
    val durationMs: Long? = null,
    val isrc: String? = null,
    val explicit: Boolean? = null,
    val artworkUrl: String? = null,
    val permalinkUrl: String? = null,
    val streamable: Boolean = true,
    val access: String? = null,
)

interface SoundCloudCatalogGateway {
    suspend fun searchTracksPage(
        query: String,
        limit: Int,
        offset: Int,
    ): Result<SoundCloudTrackPage>
}

interface SoundCloudStreamGateway {
    suspend fun resolveStream(trackId: String): Result<SoundCloudResolvedStream>
}

data class SoundCloudResolvedStream(
    val url: String,
    val mimeType: String?,
    val bitrateBitsPerSecond: Int?,
    val protocol: String,
)

@Singleton
class SoundCloudWebClient
@Inject
constructor() : SoundCloudCatalogGateway, SoundCloudStreamGateway {
    private val json = Json { ignoreUnknownKeys = true }
    private val clientIdMutex = Mutex()

    @Volatile
    private var clientId: String? = null

    @Volatile
    private var clientIdExpiresAtMs: Long = 0L

    private val client =
        OkHttpClient
            .Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .callTimeout(35, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

    override suspend fun searchTracksPage(
        query: String,
        limit: Int,
        offset: Int,
    ): Result<SoundCloudTrackPage> =
        withContext(Dispatchers.IO) {
            runCatching {
                val normalizedLimit = limit.coerceIn(1, MAX_PAGE_SIZE)
                val root =
                    apiGet(
                        path = "search/tracks",
                        params =
                            listOf(
                                "q" to query,
                                "limit" to normalizedLimit.toString(),
                                "offset" to offset.toString(),
                                "access" to "playable",
                            ),
                    )

                val rawItems = root.arrayOrEmpty("collection")
                SoundCloudTrackPage(
                    items = rawItems.mapNotNull(::parseTrack),
                    total = root.intValue("total_results"),
                    limit = normalizedLimit,
                    offset = offset,
                )
            }
        }

    override suspend fun resolveStream(trackId: String): Result<SoundCloudResolvedStream> =
        withContext(Dispatchers.IO) {
            runCatching {
                var track = apiGet(path = "tracks/$trackId")
                if (track["streamable"]?.jsonPrimitive?.booleanOrNull == false) {
                    error("SoundCloud track is not streamable")
                }
                val access = track.stringValue("access")
                if (access != null && access != "playable") {
                    error("SoundCloud track is not playable")
                }

                var lastError: Throwable? = null
                val candidates = rankTranscodings(track)
                if (candidates.isEmpty()) {
                    error("SoundCloud returned no supported full stream")
                }

                candidates.take(MAX_STREAM_CANDIDATES).forEach { candidate ->
                    try {
                        return@runCatching resolveTranscoding(
                            candidate = candidate,
                            track = track,
                            allowRefresh = true,
                        )
                    } catch (error: Throwable) {
                        lastError = error
                        if (
                            error is SoundCloudAuthorizationException
                        ) {
                            invalidateClientId()
                            track = apiGet(path = "tracks/$trackId")
                        }
                    }
                }

                throw lastError ?: IllegalStateException("SoundCloud stream resolution failed")
            }
        }

    private suspend fun apiGet(
        path: String,
        params: List<Pair<String, String>> = emptyList(),
        retryAuth: Boolean = true,
    ): JsonObject {
        val id = ensureClientId()
        val builder =
            (API_BASE + path)
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("client_id", id)
        params.forEach { (name, value) ->
            builder.addQueryParameter(name, value)
        }

        val request =
            Request
                .Builder()
                .url(builder.build())
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .get()
                .build()

        client.newCall(request).execute().use { response ->
            if ((response.code == 401 || response.code == 403) && retryAuth) {
                invalidateClientId()
                return apiGet(
                    path = path,
                    params = params,
                    retryAuth = false,
                )
            }

            if (!response.isSuccessful) {
                error("SoundCloud API HTTP " + response.code)
            }

            return json.parseToJsonElement(response.body.string()).jsonObject
        }
    }

    private suspend fun ensureClientId(): String =
        clientIdMutex.withLock {
            val now = System.currentTimeMillis()
            clientId
                ?.takeIf { clientIdExpiresAtMs > now + CLIENT_ID_EXPIRY_SKEW_MS }
                ?.let { return@withLock it }

            val fresh = fetchClientId()
            clientId = fresh
            clientIdExpiresAtMs = now + CLIENT_ID_TTL_MS
            fresh
        }

    private fun invalidateClientId() {
        clientId = null
        clientIdExpiresAtMs = 0L
    }

    private fun fetchClientId(): String {
        val home =
            executeText(
                Request
                    .Builder()
                    .url("https://soundcloud.com/")
                    .header("User-Agent", USER_AGENT)
                    .get()
                    .build(),
            )

        DIRECT_CLIENT_ID_REGEX
            .find(home)
            ?.groupValues
            ?.getOrNull(1)
            ?.takeIf { it.length == 32 }
            ?.let { return it }

        val bundles =
            SCRIPT_REGEX
                .findAll(home)
                .mapNotNull { match -> match.groupValues.getOrNull(1) }
                .distinct()
                .toList()
                .takeLast(MAX_BUNDLES_TO_SCAN)
                .asReversed()

        bundles.forEach { bundleUrl ->
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

            BUNDLE_CLIENT_ID_REGEX
                .find(body)
                ?.groupValues
                ?.getOrNull(1)
                ?.takeIf { it.length == 32 }
                ?.let { return it }

            QUERY_CLIENT_ID_REGEX
                .find(body)
                ?.groupValues
                ?.getOrNull(1)
                ?.takeIf { it.length == 32 }
                ?.let { return it }
        }

        error("Could not discover SoundCloud client_id")
    }

    private fun executeText(request: Request): String =
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("SoundCloud bootstrap HTTP " + response.code)
            }
            response.body.string()
        }

    private suspend fun resolveTranscoding(
        candidate: SoundCloudTranscoding,
        track: JsonObject,
        allowRefresh: Boolean,
    ): SoundCloudResolvedStream {
        val id = ensureClientId()
        val endpoint = candidate.url.toHttpUrl()
        val builder = endpoint.newBuilder()
        builder.setQueryParameter("client_id", id)
        track.stringValue("track_authorization")
            ?.takeIf(String::isNotBlank)
            ?.let { builder.setQueryParameter("track_authorization", it) }

        val request =
            Request
                .Builder()
                .url(builder.build())
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .get()
                .build()

        client.newCall(request).execute().use { response ->
            if (
                (response.code == 401 || response.code == 403) &&
                allowRefresh
            ) {
                throw SoundCloudAuthorizationException()
            }

            if (!response.isSuccessful) {
                error("SoundCloud stream endpoint HTTP " + response.code)
            }

            val payload = json.parseToJsonElement(response.body.string()).jsonObject
            val streamUrl =
                payload.stringValue("url")
                    ?.takeIf(String::isNotBlank)
                    ?: error("SoundCloud returned no stream URL")

            return SoundCloudResolvedStream(
                url = streamUrl,
                mimeType = candidate.mimeType,
                bitrateBitsPerSecond = candidate.bitrateBitsPerSecond,
                protocol = candidate.protocol,
            )
        }
    }

    private fun rankTranscodings(track: JsonObject): List<SoundCloudTranscoding> {
        val transcodings =
            track["media"]
                ?.let { runCatching { it.jsonObject }.getOrNull() }
                ?.arrayOrEmpty("transcodings")
                .orEmpty()

        return transcodings
            .mapNotNull { element ->
                val item = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
                val url = item.stringValue("url") ?: return@mapNotNull null
                val format =
                    item["format"]
                        ?.let { runCatching { it.jsonObject }.getOrNull() }
                        ?: return@mapNotNull null
                val protocol = format.stringValue("protocol")?.lowercase() ?: return@mapNotNull null
                if (protocol != "progressive" && protocol != "hls") return@mapNotNull null

                val preset = item.stringValue("preset").orEmpty()
                if (
                    item["snipped"]?.jsonPrimitive?.booleanOrNull == true ||
                    preset.contains("preview", ignoreCase = true)
                ) {
                    return@mapNotNull null
                }

                val mime = format.stringValue("mime_type")
                val bitrate =
                    BITRATE_REGEX
                        .find(preset)
                        ?.groupValues
                        ?.getOrNull(1)
                        ?.toIntOrNull()
                        ?.times(1_000)

                SoundCloudTranscoding(
                    url = url,
                    protocol = protocol,
                    mimeType = mime,
                    preset = preset,
                    quality = item.stringValue("quality"),
                    bitrateBitsPerSecond = bitrate,
                )
            }.sortedByDescending { transcoding ->
                var score = 0
                val mime = transcoding.mimeType.orEmpty().lowercase()
                val preset = transcoding.preset.lowercase()
                if ("aac" in mime || "mp4" in mime || preset.startsWith("aac_")) score += 60
                if ("opus" in mime || preset.startsWith("opus_")) score += 40
                if ("mpeg" in mime || "mp3" in mime) score += 20
                if (transcoding.protocol == "progressive") score += 8
                if (transcoding.quality.equals("hq", ignoreCase = true)) score += 5
                score += ((transcoding.bitrateBitsPerSecond ?: 0) / 100_000)
                score
            }
    }

    private fun parseTrack(element: kotlinx.serialization.json.JsonElement): SoundCloudTrack? {
        val item = runCatching { element.jsonObject }.getOrNull() ?: return null
        val id =
            item.longValue("id")?.toString()
                ?: item.stringValue("id")
                ?: return null
        val title = item.stringValue("title")?.trim().orEmpty()
        if (title.isBlank()) return null

        val publisher =
            item["publisher_metadata"]
                ?.let { runCatching { it.jsonObject }.getOrNull() }
        val user =
            item["user"]
                ?.let { runCatching { it.jsonObject }.getOrNull() }

        val artist =
            publisher?.stringValue("artist")
                ?: item.stringValue("metadata_artist")
                ?: user?.stringValue("username")
                ?: ""

        val artwork =
            item.stringValue("artwork_url")
                ?.let(::highResolutionArtwork)
                ?: user
                    ?.stringValue("avatar_url")
                    ?.let(::highResolutionArtwork)

        return SoundCloudTrack(
            id = id,
            title = title,
            artists = listOf(artist).filter(String::isNotBlank),
            albumName =
                publisher?.stringValue("album_title")
                    ?: publisher?.stringValue("release_title"),
            durationMs =
                item.longValue("full_duration")
                    ?: item.longValue("duration")
                    ?: item.intValue("duration")?.toLong(),
            isrc =
                (
                    publisher?.stringValue("isrc")
                        ?: item.stringValue("isrc")
                )?.uppercase(),
            explicit = null,
            artworkUrl = artwork,
            permalinkUrl = item.stringValue("permalink_url"),
            streamable = item["streamable"]?.jsonPrimitive?.booleanOrNull ?: true,
            access = item.stringValue("access"),
        )
    }

    private fun highResolutionArtwork(url: String): String =
        url.replace("-large.", "-t500x500.")

    private fun JsonObject.arrayOrEmpty(name: String): JsonArray =
        this[name]
            ?.let { runCatching { it.jsonArray }.getOrNull() }
            ?: JsonArray(emptyList())

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

    private data class SoundCloudTranscoding(
        val url: String,
        val protocol: String,
        val mimeType: String?,
        val preset: String,
        val quality: String?,
        val bitrateBitsPerSecond: Int?,
    )

    private class SoundCloudAuthorizationException : IllegalStateException()

    companion object {
        private const val API_BASE = "https://api-v2.soundcloud.com/"
        private const val MAX_PAGE_SIZE = 50
        private const val MAX_BUNDLES_TO_SCAN = 8
        private const val MAX_STREAM_CANDIDATES = 6
        private const val CLIENT_ID_TTL_MS = 24L * 60L * 60L * 1_000L
        private const val CLIENT_ID_EXPIRY_SKEW_MS = 60_000L
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Mobile Safari/537.36"

        private val DIRECT_CLIENT_ID_REGEX =
            Regex("""["']?client_id["']?\s*[:=]\s*["']([A-Za-z0-9]{32})["']""")
        private val BUNDLE_CLIENT_ID_REGEX =
            Regex("""["']?client_id["']?\s*[:=]\s*["']([A-Za-z0-9]{32})["']""")
        private val QUERY_CLIENT_ID_REGEX =
            Regex("""client_id=([A-Za-z0-9]{32})""")
        private val SCRIPT_REGEX =
            Regex("""src=["'](https://[^"']*sndcdn\.com[^"']*\.js)["']""")
        private val BITRATE_REGEX =
            Regex("""_(\d+)k""", RegexOption.IGNORE_CASE)
    }
}

@Singleton
class SoundCloudMusicSource
@Inject
constructor(
    private val gateway: SoundCloudCatalogGateway,
) : TrackSearchSource {
    override val id: ProviderId = ProviderId.SOUNDCLOUD
    override val displayName: String = "SoundCloud"

    override val capabilities: SourceCapabilities =
        SourceCapabilities.of(
            SourceCapability.TRACK_SEARCH,
            SourceCapability.TRACK_METADATA,
            SourceCapability.ARTISTS,
            SourceCapability.PLAYLISTS,
            SourceCapability.STREAM,
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
                        .takeIf { next ->
                            page.items.isNotEmpty() &&
                                (
                                    page.total == null ||
                                        next < page.total
                                )
                        }

                TrackSearchPage(
                    items =
                        page.items
                            .asSequence()
                            .filter { it.streamable }
                            .filter { it.access == null || it.access == "playable" }
                            .map(SoundCloudTrack::toSourceVariant)
                            .toList(),
                    continuationToken = nextOffset?.let(::encodeContinuation),
                )
            }
    }

    private fun decodeContinuation(token: String?): Int {
        if (token == null) return 0
        require(token.startsWith(CONTINUATION_PREFIX)) {
            "Invalid SoundCloud continuation token"
        }

        return token
            .removePrefix(CONTINUATION_PREFIX)
            .toIntOrNull()
            ?.takeIf { it >= 0 }
            ?: throw IllegalArgumentException("Invalid SoundCloud continuation offset")
    }

    private fun encodeContinuation(offset: Int): String = CONTINUATION_PREFIX + offset

    companion object {
        private const val CONTINUATION_PREFIX = "offset:"
        private const val MAX_PAGE_SIZE = 50
    }
}

@Singleton
class SoundCloudPlaybackResolver
@Inject
constructor(
    private val gateway: SoundCloudStreamGateway,
) {
    suspend fun resolveToMediaItem(variant: SourceVariant): MediaItem? {
        require(variant.provider == ProviderId.SOUNDCLOUD) {
            "Only SoundCloud variants can be resolved by SoundCloudPlaybackResolver"
        }

        val stream = gateway.resolveStream(variant.sourceId).getOrNull() ?: return null
        val metadata = variant.toOpenTuneMetadata()

        return MediaItem
            .Builder()
            .setMediaId(variant.sourceId)
            .setUri(stream.url)
            .setCustomCacheKey("soundcloud:" + variant.sourceId)
            .setTag(metadata)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata
                    .Builder()
                    .setTitle(variant.title)
                    .setArtist(variant.artists.joinToString())
                    .setSubtitle(variant.artists.joinToString())
                    .setAlbumTitle(variant.albumName)
                    .setArtworkUri(variant.artworkUrl?.toUri())
                    .setMediaType(MEDIA_TYPE_MUSIC)
                    .build(),
            ).build()
    }
}

internal fun SoundCloudTrack.toSourceVariant(): SourceVariant =
    SourceVariant(
        provider = ProviderId.SOUNDCLOUD,
        sourceId = id,
        title = title,
        artists = artists,
        albumName = albumName,
        durationMs = durationMs?.takeIf { it > 0L },
        isrc = isrc?.takeIf(String::isNotBlank),
        explicit = explicit,
        artworkUrl = artworkUrl,
        quality =
            AudioQualityInfo(
                lossless = false,
            ),
    )

internal fun SourceVariant.toSoundCloudCatalogReference(): CatalogTrackReference {
    require(provider == ProviderId.SOUNDCLOUD) {
        "Only SoundCloud variants can be converted to SoundCloud catalog references"
    }

    return CatalogTrackReference(
        cacheId = "soundcloud:" + sourceId,
        title = title,
        artists = artists,
        durationMs = durationMs ?: 0L,
        thumbnailUrl = artworkUrl,
        albumId = albumId?.let { "soundcloud-album:" + it },
        albumName = albumName,
        explicit = explicit ?: false,
    )
}

private fun SourceVariant.toOpenTuneMetadata(): MediaMetadata {
    val durationSeconds =
        durationMs
            ?.takeIf { it >= 0L }
            ?.div(1_000L)
            ?.coerceAtMost(Int.MAX_VALUE.toLong())
            ?.toInt()
            ?: MediaMetadata.UNKNOWN_DURATION

    return MediaMetadata(
        id = sourceId,
        title = title,
        artists =
            artists.map { artist ->
                MediaMetadata.Artist(
                    id = null,
                    name = artist,
                )
            },
        duration = durationSeconds,
        thumbnailUrl = artworkUrl,
        album =
            albumName?.let { title ->
                MediaMetadata.Album(
                    id = albumId,
                    title = title,
                )
            },
        explicit = explicit ?: false,
    )
}
