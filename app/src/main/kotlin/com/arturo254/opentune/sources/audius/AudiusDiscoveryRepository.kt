package com.arturo254.opentune.sources.audius

import com.arturo254.opentune.core.model.SourceVariant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

data class AudiusArtist(
    val id: String,
    val name: String,
    val handle: String,
    val artworkUrl: String?,
    val bio: String?,
    val trackCount: Int?,
)

data class AudiusPlaylist(
    val id: String,
    val name: String,
    val creator: String,
    val artworkUrl: String?,
    val description: String?,
    val trackCount: Int?,
)

data class AudiusTrackCollectionPage(
    val tracks: List<SourceVariant>,
    val hasMore: Boolean,
)

enum class AudiusCollectionType { ARTIST, PLAYLIST }

data class AudiusSelectedCollection(
    val type: AudiusCollectionType,
    val id: String,
    val title: String,
    val subtitle: String,
    val artworkUrl: String?,
    val description: String?,
)

interface AudiusDiscoveryGateway {
    suspend fun searchArtists(query: String): Result<List<AudiusArtist>>
    suspend fun searchPlaylists(query: String): Result<List<AudiusPlaylist>>
    suspend fun artistTracks(id: String, offset: Int): Result<AudiusTrackCollectionPage>
    suspend fun playlistTracks(id: String): Result<AudiusTrackCollectionPage>
}

@Singleton
class AudiusDiscoveryRepository @Inject constructor(
    private val client: AudiusHttpGateway,
) : AudiusDiscoveryGateway {
    override suspend fun searchArtists(query: String): Result<List<AudiusArtist>> =
        runCatching {
            val root = client.fetchDiscovery(
                "/users/search",
                listOf("query" to query, "limit" to "30"),
            )
            root.arrayData().mapNotNull(::parseAudiusArtist).distinctBy { it.id }
        }

    override suspend fun searchPlaylists(query: String): Result<List<AudiusPlaylist>> =
        runCatching {
            val root = client.fetchDiscovery(
                "/playlists/search",
                listOf("query" to query),
            )
            root.arrayData().mapNotNull(::parseAudiusPlaylist).distinctBy { it.id }
        }

    override suspend fun artistTracks(
        id: String,
        offset: Int,
    ): Result<AudiusTrackCollectionPage> = runCatching {
        val root = client.fetchDiscovery(
            "/users/${validatedAudiusId(id)}/tracks",
            listOf("limit" to "50", "offset" to offset.toString()),
        )
        val items = root.arrayData()
        AudiusTrackCollectionPage(
            tracks = items.mapNotNull(::parseAudiusTrack)
                .filter { it.streamable && !it.streamGated }
                .map(AudiusTrack::toSourceVariant)
                .distinctBy { it.sourceId },
            hasMore = items.size >= 50,
        )
    }

    override suspend fun playlistTracks(id: String): Result<AudiusTrackCollectionPage> =
        runCatching {
            val root = client.fetchDiscovery("/playlists/${validatedAudiusId(id)}/tracks")
            AudiusTrackCollectionPage(
                tracks = root.arrayData()
                    .mapNotNull(::parseAudiusTrack)
                    .filter { it.streamable && !it.streamGated }
                    .map(AudiusTrack::toSourceVariant),
                hasMore = false,
            )
        }
}

internal fun validatedAudiusId(id: String): String {
    require(id.isNotBlank() && id.all {
        it.isLetterOrDigit() || it == '-' || it == '_'
    }) { "Invalid Audius identifier" }
    return id
}

internal fun parseAudiusArtist(element: JsonElement): AudiusArtist? {
    val data = element as? JsonObject ?: return null
    val id = data.value("id")?.takeIf(String::isNotBlank) ?: return null
    val name = data.value("name")?.takeIf(String::isNotBlank) ?: return null
    val photo = data["profile_picture"] as? JsonObject
        ?: data["profilePicture"] as? JsonObject
    return AudiusArtist(
        id = id,
        name = name,
        handle = data.value("handle").orEmpty(),
        artworkUrl = photo?.imageUrl(),
        bio = data.value("bio"),
        trackCount = data.value("track_count")?.toIntOrNull()
            ?: data.value("trackCount")?.toIntOrNull(),
    )
}

internal fun parseAudiusPlaylist(element: JsonElement): AudiusPlaylist? {
    val data = element as? JsonObject ?: return null
    val id = data.value("id") ?: data.value("playlist_id") ?: return null
    val name = data.value("playlist_name") ?: data.value("playlistName")
        ?: data.value("name") ?: return null
    val creator = (data["user"] as? JsonObject)?.value("name").orEmpty()
    val image = data["artwork"] as? JsonObject
        ?: data["playlist_image"] as? JsonObject
    return AudiusPlaylist(
        id = id,
        name = name,
        creator = creator,
        artworkUrl = image?.imageUrl(),
        description = data.value("description"),
        trackCount = data.value("track_count")?.toIntOrNull()
            ?: data.value("trackCount")?.toIntOrNull(),
    )
}

private fun JsonObject.arrayData(): List<JsonElement> =
    (get("data") as? JsonArray)?.toList().orEmpty()

private fun JsonObject.value(name: String): String? =
    (get(name) as? JsonPrimitive)?.contentOrNull

private fun JsonObject.imageUrl(): String? =
    listOf("480x480", "_480x480", "1000x1000", "_1000x1000", "150x150", "_150x150")
        .firstNotNullOfOrNull(::value)
