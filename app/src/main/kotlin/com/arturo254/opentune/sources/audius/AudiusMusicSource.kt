package com.arturo254.opentune.sources.audius

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
import androidx.media3.common.MimeTypes
import com.arturo254.opentune.BuildConfig
import com.arturo254.opentune.core.model.AudioQualityInfo
import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.sources.ManagedDownloadMusicSource
import com.arturo254.opentune.sources.ManagedDownloadResource
import com.arturo254.opentune.sources.SourceCapabilities
import com.arturo254.opentune.sources.SourceCapability
import com.arturo254.opentune.sources.StreamMusicSource
import com.arturo254.opentune.sources.TrackSearchPage
import com.arturo254.opentune.sources.TrackSearchRequest
import com.arturo254.opentune.sources.TrackSearchSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl
import java.util.concurrent.TimeUnit

data class AudiusTrack(
    val id: String,
    val title: String,
    val artist: String,
    val durationMs: Long?,
    val artworkUrl: String?,
    val isrc: String?,
    val downloadable: Boolean,
    val streamable: Boolean,
    val streamGated: Boolean,
    val downloadGated: Boolean,
    val explicit: Boolean?,
)

data class AudiusTrackPage(
    val items: List<AudiusTrack>,
    val limit: Int,
    val offset: Int,
)

interface AudiusCatalogGateway {
    suspend fun searchTracks(query: String, limit: Int, offset: Int): Result<AudiusTrackPage>
    suspend fun getTrack(id: String): Result<AudiusTrack>
    fun streamUrl(id: String): String
    fun downloadUrl(id: String): String
}

@Singleton
class AudiusHttpGateway @Inject constructor() : AudiusCatalogGateway {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    override suspend fun searchTracks(
        query: String,
        limit: Int,
        offset: Int,
    ): Result<AudiusTrackPage> = withContext(Dispatchers.IO) {
        runCatching {
            val root = fetch(
                "/tracks/search",
                listOf(
                    "query" to query,
                    "limit" to limit.toString(),
                    "offset" to offset.toString(),
                ),
            )
            val tracks = (root["data"] as? JsonArray).orEmpty()
                .mapNotNull(::parseAudiusTrack)
            AudiusTrackPage(tracks, limit, offset)
        }
    }

    override suspend fun getTrack(id: String): Result<AudiusTrack> =
        withContext(Dispatchers.IO) {
            runCatching {
                val root = fetch("/tracks/${encodeSegment(id)}")
                parseAudiusTrack(root["data"] ?: JsonNull)
                    ?: error("Audius did not return a valid track")
            }
        }

    override fun streamUrl(id: String): String =
        endpoint("/tracks/${encodeSegment(id)}/stream").toString()

    override fun downloadUrl(id: String): String =
        endpoint("/tracks/${encodeSegment(id)}/download").toString()

    private fun fetch(
        path: String,
        parameters: List<Pair<String, String>> = emptyList(),
    ): JsonObject {
        val requestUrl = endpoint(path, parameters)
        val request = Request.Builder()
            .url(requestUrl)
            .header("Accept", "application/json")
            .get()
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Audius API HTTP ${response.code}")
            }
            json.parseToJsonElement(response.body.string()).jsonObject
        }
    }

    private fun endpoint(
        path: String,
        parameters: List<Pair<String, String>> = emptyList(),
    ): HttpUrl {
        val builder = API_BASE.newBuilder().addPathSegments(path.trimStart('/'))
        val key = BuildConfig.AUDIUS_API_KEY.trim()
        if (key.isNotEmpty()) builder.addQueryParameter("api_key", key)
        parameters.forEach { (name, value) -> builder.addQueryParameter(name, value) }
        return builder.build()
    }

    private fun encodeSegment(value: String): String =
        value.takeIf { it.isNotBlank() && it.all { char ->
            char.isLetterOrDigit() || char == '-' || char == '_'
        } } ?: error("Invalid Audius track identifier")

    private companion object {
        val API_BASE: HttpUrl = okhttp3.HttpUrl.Builder()
            .scheme("https")
            .host("api.audius.co")
            .addPathSegment("v1")
            .build()
    }
}

internal fun parseAudiusTrack(element: JsonElement): AudiusTrack? {
    val track = element as? JsonObject ?: return null
    val id = track.string("id")?.takeIf(String::isNotBlank) ?: return null
    val title = track.string("title")?.takeIf(String::isNotBlank) ?: return null
    val user = track["user"] as? JsonObject
    val artist = user?.string("name") ?: track.string("artist_name") ?: return null
    val artwork = track["artwork"] as? JsonObject
    val streamable = track.boolean("is_streamable")
        ?: track.boolean("isStreamable")
        ?: true
    val streamGated = track.boolean("is_stream_gated") == true ||
        track.boolean("isStreamGated") == true ||
        track.hasGate("stream_conditions", "streamConditions")
    return AudiusTrack(
        id = id,
        title = title,
        artist = artist,
        durationMs = track.long("duration")?.takeIf { it > 0L }?.times(1_000L),
        artworkUrl = artwork?.string("480x480") ?: artwork?.string("_480x480")
            ?: artwork?.string("1000x1000") ?: artwork?.string("_1000x1000")
            ?: artwork?.string("150x150") ?: artwork?.string("_150x150"),
        isrc = track.string("isrc"),
        downloadable = track.boolean("is_downloadable")
            ?: track.boolean("isDownloadable")
            ?: track.boolean("downloadable")
            ?: false,
        streamable = streamable,
        streamGated = streamGated,
        downloadGated = track.hasGate("download_conditions", "downloadConditions"),
        explicit = track.boolean("is_explicit") ?: track.boolean("explicit"),
    )
}

private fun JsonObject.string(key: String): String? =
    (get(key) as? JsonPrimitive)?.contentOrNull

private fun JsonObject.boolean(key: String): Boolean? =
    (get(key) as? JsonPrimitive)?.let { it.booleanOrNull ?: when (it.contentOrNull?.lowercase()) {
        "true" -> true
        "false" -> false
        else -> null
    } }

private fun JsonObject.long(key: String): Long? =
    (get(key) as? JsonPrimitive)?.contentOrNull?.toLongOrNull()

private fun JsonObject.hasGate(vararg names: String): Boolean =
    names.any { name -> get(name)?.let { it != JsonNull } == true }

internal fun AudiusTrack.toSourceVariant(): SourceVariant =
    SourceVariant(
        provider = ProviderId.AUDIUS,
        sourceId = id,
        title = title,
        artists = listOf(artist),
        durationMs = durationMs,
        isrc = isrc,
        explicit = explicit,
        artworkUrl = artworkUrl,
        quality = AudioQualityInfo(mimeType = MimeTypes.AUDIO_MPEG, lossless = false),
        downloadable = downloadable && !downloadGated && streamable && !streamGated,
    )

@Singleton
class AudiusMusicSource @Inject constructor(
    private val gateway: AudiusCatalogGateway,
) : TrackSearchSource, StreamMusicSource, ManagedDownloadMusicSource {
    override val id = ProviderId.AUDIUS
    override val displayName = "Audius"
    override val capabilities = SourceCapabilities.of(
        SourceCapability.TRACK_SEARCH,
        SourceCapability.TRACK_METADATA,
        SourceCapability.ARTISTS,
        SourceCapability.PLAYLISTS,
        SourceCapability.STREAM,
        SourceCapability.OPEN_TUNE_MANAGED_DOWNLOAD,
        SourceCapability.STATIC_ARTWORK,
        SourceCapability.AUDIO_QUALITY,
    )

    override suspend fun searchTracks(request: TrackSearchRequest): Result<TrackSearchPage> {
        val query = request.query.trim()
        if (query.isEmpty()) return Result.success(TrackSearchPage(emptyList()))
        val offset = runCatching {
            if (request.continuationToken == null) 0 else {
                require(request.continuationToken.startsWith(CONTINUATION_PREFIX))
                request.continuationToken.removePrefix(CONTINUATION_PREFIX)
                    .toInt().also { require(it >= 0) }
            }
        }.getOrElse { return Result.failure(it) }
        val limit = request.limit.coerceIn(1, MAX_PAGE_SIZE)
        return gateway.searchTracks(query, limit, offset).map { page ->
            TrackSearchPage(
                items = page.items
                    .filter { it.streamable && !it.streamGated }
                    .map(AudiusTrack::toSourceVariant),
                continuationToken = if (page.items.size >= limit) {
                    CONTINUATION_PREFIX + (offset + page.items.size)
                } else null,
            )
        }
    }

    override suspend fun resolvePlayback(variant: SourceVariant): MediaItem? {
        if (variant.provider != ProviderId.AUDIUS) return null
        val artist = variant.artists.joinToString()
        return MediaItem.Builder()
            .setMediaId(variant.sourceId)
            .setUri(gateway.streamUrl(variant.sourceId))
            .setMimeType(MimeTypes.AUDIO_MPEG)
            .setCustomCacheKey("audius:" + variant.sourceId)
            .setTag(variant.toMediaMetadata())
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(variant.title)
                    .setArtist(artist)
                    .setSubtitle(artist)
                    .setAlbumTitle(variant.albumName)
                    .setArtworkUri(variant.artworkUrl?.toUri())
                    .setMediaType(MEDIA_TYPE_MUSIC)
                    .build(),
            ).build()
    }

    override suspend fun resolveManagedDownload(variant: SourceVariant): ManagedDownloadResource? {
        if (variant.provider != ProviderId.AUDIUS) return null
        val track = gateway.getTrack(variant.sourceId).getOrNull() ?: return null
        if (!track.downloadable || track.downloadGated || !track.streamable || track.streamGated) {
            return null
        }
        return ManagedDownloadResource(url = gateway.downloadUrl(track.id))
    }

    private fun SourceVariant.toMediaMetadata(): MediaMetadata =
        MediaMetadata(
            id = sourceId,
            title = title,
            artists = artists.map { MediaMetadata.Artist(id = null, name = it) },
            duration = durationMs?.div(1_000L)?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt()
                ?: MediaMetadata.UNKNOWN_DURATION,
            thumbnailUrl = artworkUrl,
            explicit = explicit ?: false,
        )

    private companion object {
        const val CONTINUATION_PREFIX = "audius-offset:"
        const val MAX_PAGE_SIZE = 50
    }
}
