package com.arturo254.opentune.sources.navidrome

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
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
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

data class NavidromeSong(
    val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val albumId: String?,
    val durationMs: Long?,
    val coverArt: String?,
    val contentType: String?,
    val suffix: String?,
    val bitrate: Int?,
    val size: Long?,
)

data class NavidromeSearchPage(
    val songs: List<NavidromeSong>,
    val hasMore: Boolean,
)

interface NavidromeGateway {
    suspend fun ping(credentials: NavidromeCredentials): Result<Unit>
    suspend fun search(query: String, offset: Int, limit: Int): Result<NavidromeSearchPage>
    fun streamUrl(songId: String): String?
    fun downloadUrl(songId: String): String?
    fun coverArtUrl(coverId: String): String?
}

@Singleton
class NavidromeHttpGateway @Inject constructor(
    private val accounts: NavidromeAccountStore,
) : NavidromeGateway {
    private val json = Json { ignoreUnknownKeys = true }
    private val random = SecureRandom()
    private val client = OkHttpClient.Builder()
        .connectTimeout(7, TimeUnit.SECONDS)
        .readTimeout(16, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    override suspend fun ping(credentials: NavidromeCredentials): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                request("ping", credentials)
                Unit
            }
        }

    override suspend fun search(
        query: String,
        offset: Int,
        limit: Int,
    ): Result<NavidromeSearchPage> = withContext(Dispatchers.IO) {
        val credentials = accounts.credentials()
            ?: return@withContext Result.success(NavidromeSearchPage(emptyList(), false))
        try {
            val root = request(
                "search3",
                credentials,
                listOf(
                    "query" to query,
                    "songCount" to limit.toString(),
                    "songOffset" to offset.toString(),
                    "artistCount" to "0",
                    "albumCount" to "0",
                ),
            )
            val songs = root.subsonicObject("searchResult3").songs()
                .mapNotNull(::parseNavidromeSong)
            Result.success(NavidromeSearchPage(songs, songs.size >= limit))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    override fun streamUrl(songId: String): String? =
        accounts.credentials()?.let {
            authenticatedUrl(it, "stream", listOf("id" to songId)).toString()
        }

    override fun downloadUrl(songId: String): String? =
        accounts.credentials()?.let {
            authenticatedUrl(it, "download", listOf("id" to songId)).toString()
        }

    override fun coverArtUrl(coverId: String): String? =
        accounts.credentials()?.let {
            authenticatedUrl(it, "getCoverArt", listOf("id" to coverId)).toString()
        }

    private fun request(
        endpoint: String,
        credentials: NavidromeCredentials,
        parameters: List<Pair<String, String>> = emptyList(),
    ): JsonObject {
        val url = authenticatedUrl(credentials, endpoint, parameters)
        val response = client.newCall(Request.Builder().url(url).get().build())
            .execute()
        return response.use {
            if (!it.isSuccessful) error("El servidor Navidrome respondió HTTP ${it.code}")
            val data = json.parseToJsonElement(it.body.string()).jsonObject
            val root = data["subsonic-response"] as? JsonObject
                ?: error("Respuesta OpenSubsonic no reconocida")
            if (root.stringValue("status") != "ok") {
                val detail = root["error"] as? JsonObject
                val code = detail?.stringValue("code").orEmpty()
                error("Navidrome rechazó la solicitud (código $code)")
            }
            root
        }
    }

    internal fun authenticatedUrl(
        credentials: NavidromeCredentials,
        endpoint: String,
        parameters: List<Pair<String, String>> = emptyList(),
    ): HttpUrl {
        val base = validateNavidromeUrl(credentials.baseUrl).toHttpUrl()
        val saltBytes = ByteArray(16).also(random::nextBytes)
        val salt = saltBytes.joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val token = MessageDigest.getInstance("MD5")
            .digest((credentials.password + salt).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return base.newBuilder()
            .addPathSegment("rest")
            .addPathSegment("$endpoint.view")
            .addQueryParameter("u", credentials.username)
            .addQueryParameter("t", token)
            .addQueryParameter("s", salt)
            .addQueryParameter("v", "1.16.1")
            .addQueryParameter("c", "GalaxOT")
            .addQueryParameter("f", "json")
            .apply { parameters.forEach { (key, value) -> addQueryParameter(key, value) } }
            .build()
    }
}

internal fun parseNavidromeSong(element: JsonElement): NavidromeSong? {
    val data = element as? JsonObject ?: return null
    val id = data.stringValue("id")?.takeIf(String::isNotBlank) ?: return null
    val title = data.stringValue("title")?.takeIf(String::isNotBlank) ?: return null
    val artist = data.stringValue("artist").orEmpty().ifBlank { "Artista desconocido" }
    val duration = data.stringValue("duration")?.toLongOrNull()?.takeIf { it > 0L }
    return NavidromeSong(
        id = id,
        title = title,
        artist = artist,
        album = data.stringValue("album"),
        albumId = data.stringValue("albumId"),
        durationMs = duration?.times(1_000L),
        coverArt = data.stringValue("coverArt"),
        contentType = data.stringValue("contentType"),
        suffix = data.stringValue("suffix"),
        bitrate = data.stringValue("bitRate")?.toIntOrNull(),
        size = data.stringValue("size")?.toLongOrNull(),
    )
}

internal fun NavidromeSong.toVariant(coverUrl: String?): SourceVariant {
    val extension = suffix?.lowercase().orEmpty()
    val mime = contentType?.takeIf(String::isNotBlank) ?: when (extension) {
        "flac" -> "audio/flac"
        "mp3" -> "audio/mpeg"
        "m4a", "mp4" -> "audio/mp4"
        "ogg" -> "audio/ogg"
        "opus" -> "audio/opus"
        "wav" -> "audio/wav"
        else -> null
    }
    return SourceVariant(
        provider = ProviderId.NAVIDROME,
        sourceId = id,
        title = title,
        artists = listOf(artist),
        albumId = albumId,
        albumName = album,
        durationMs = durationMs,
        artworkUrl = coverUrl,
        quality = AudioQualityInfo(
            mimeType = mime,
            codec = extension.takeIf(String::isNotBlank)?.uppercase(),
            bitrateBitsPerSecond = bitrate?.takeIf { it > 0 }?.times(1_000),
            lossless = extension in setOf("flac", "wav", "alac", "aiff"),
        ),
        downloadable = true,
    )
}

private fun JsonObject.stringValue(key: String): String? =
    (get(key) as? JsonPrimitive)?.contentOrNull

private fun JsonObject.subsonicObject(key: String): JsonObject =
    get(key) as? JsonObject ?: JsonObject(emptyMap())

private fun JsonObject.songs(): List<JsonElement> =
    (get("song") as? JsonArray)?.toList().orEmpty()

@Singleton
class NavidromeMusicSource @Inject constructor(
    private val accounts: NavidromeAccountStore,
    private val gateway: NavidromeGateway,
) : TrackSearchSource, StreamMusicSource, ManagedDownloadMusicSource {
    override val id: ProviderId = ProviderId.NAVIDROME
    override val displayName: String = "Navidrome"
    override val capabilities = SourceCapabilities.of(
        SourceCapability.TRACK_SEARCH,
        SourceCapability.TRACK_METADATA,
        SourceCapability.ACCOUNT_LIBRARY,
        SourceCapability.AUDIO_QUALITY,
        SourceCapability.STREAM,
        SourceCapability.OPEN_TUNE_MANAGED_DOWNLOAD,
        SourceCapability.STATIC_ARTWORK,
    )

    override suspend fun searchTracks(request: TrackSearchRequest): Result<TrackSearchPage> {
        if (accounts.credentials() == null) return Result.success(TrackSearchPage(emptyList()))
        val offset = runCatching {
            request.continuationToken?.let {
                require(it.startsWith(CURSOR_PREFIX))
                it.removePrefix(CURSOR_PREFIX).toInt().also { n -> require(n >= 0) }
            } ?: 0
        }.getOrElse { return Result.failure(it) }
        val limit = request.limit.coerceIn(1, 50)
        return gateway.search(request.query.trim(), offset, limit).map { page ->
            TrackSearchPage(
                items = page.songs.map { song ->
                    song.toVariant(song.coverArt?.let(gateway::coverArtUrl))
                },
                continuationToken = if (page.hasMore) "$CURSOR_PREFIX${offset + page.songs.size}"
                    else null,
            )
        }
    }

    override suspend fun resolvePlayback(variant: SourceVariant): MediaItem? {
        if (variant.provider != ProviderId.NAVIDROME) return null
        val uri = gateway.streamUrl(variant.sourceId) ?: return null
        val artist = variant.artists.joinToString()
        val metadata = MediaMetadata(
            id = variant.sourceId,
            title = variant.title,
            artists = variant.artists.map { MediaMetadata.Artist(id = null, name = it) },
            duration = variant.durationMs?.div(1_000L)?.toInt()
                ?: MediaMetadata.UNKNOWN_DURATION,
            thumbnailUrl = variant.artworkUrl,
            album = variant.albumId?.let { MediaMetadata.Album(it, variant.albumName.orEmpty()) },
        )
        return MediaItem.Builder()
            .setMediaId(variant.sourceId)
            .setUri(uri)
            .setCustomCacheKey("navidrome:" + variant.sourceId)
            .setMimeType(variant.quality?.mimeType)
            .setTag(metadata)
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
        if (variant.provider != ProviderId.NAVIDROME) return null
        return gateway.downloadUrl(variant.sourceId)?.let(::ManagedDownloadResource)
    }

    private companion object {
        const val CURSOR_PREFIX = "navidrome-offset:"
    }
}
