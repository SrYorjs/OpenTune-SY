package com.arturo254.opentune.sources.spotify

import com.arturo254.opentune.catalog.CatalogTrackReference
import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.sources.SourceCapabilities
import com.arturo254.opentune.sources.SourceCapability
import com.arturo254.opentune.sources.TrackSearchPage
import com.arturo254.opentune.sources.TrackSearchRequest
import com.arturo254.opentune.sources.TrackSearchSource
import com.arturo254.opentune.spotify.Spotify
import com.arturo254.opentune.spotify.SpotifyNativeTrackMetadata
import com.arturo254.opentune.spotify.SpotifyWebMetadataClient
import com.arturo254.opentune.spotify.models.SpotifyPaging
import com.arturo254.opentune.spotify.models.SpotifyTrack
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

interface SpotifyCatalogGateway {
    suspend fun searchTracksPage(
        query: String,
        limit: Int,
        offset: Int,
    ): Result<SpotifyPaging<SpotifyTrack>>

    suspend fun trackMetadata(trackId: String): Result<SpotifyNativeTrackMetadata>
}

@Singleton
class DefaultSpotifyCatalogGateway
@Inject
constructor() : SpotifyCatalogGateway {
    override suspend fun searchTracksPage(
        query: String,
        limit: Int,
        offset: Int,
    ): Result<SpotifyPaging<SpotifyTrack>> =
        Spotify
            .search(
                query = query,
                types = listOf("track"),
                limit = limit,
                offset = offset,
            ).map { result ->
                result.tracks
                    ?: SpotifyPaging(
                        items = emptyList(),
                        total = 0,
                        limit = limit,
                        offset = offset,
                    )
            }

    override suspend fun trackMetadata(trackId: String): Result<SpotifyNativeTrackMetadata> =
        SpotifyWebMetadataClient.trackMetadata(trackId)
}

/**
 * Provider-neutral adapter over the existing Spotify catalog integration.
 *
 * Spotify currently contributes catalog identity/search metadata to Sources.
 * Playback is still resolved through OpenTune's catalog playback bridge, so the
 * adapter deliberately does not claim STREAM or download capabilities.
 */
@Singleton
class SpotifyMusicSource
@Inject
constructor(
    private val gateway: SpotifyCatalogGateway,
) : TrackSearchSource {
    override val id: ProviderId = ProviderId.SPOTIFY
    override val displayName: String = "Spotify"

    override val capabilities: SourceCapabilities =
        SourceCapabilities.of(
            SourceCapability.TRACK_SEARCH,
            SourceCapability.TRACK_METADATA,
            SourceCapability.ALBUMS,
            SourceCapability.ARTISTS,
            SourceCapability.PLAYLISTS,
            SourceCapability.ACCOUNT_LIBRARY,
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

        val page =
            gateway
                .searchTracksPage(
                    query = query,
                    limit = request.limit.coerceAtMost(MAX_PAGE_SIZE),
                    offset = offset,
                ).getOrElse { return Result.failure(it) }

        val validTracks =
            page.items
                .asSequence()
                .filterNot(SpotifyTrack::isLocal)
                .filter { it.id.isNotBlank() && it.name.isNotBlank() }
                .toList()

        val variants =
            coroutineScope {
                validTracks.mapIndexed { index, track ->
                    async {
                        val nativeMetadata =
                            if (index < NATIVE_METADATA_ENRICH_LIMIT) {
                                gateway.trackMetadata(track.id).getOrNull()
                            } else {
                                null
                            }
                        track.toSpotifySourceVariant(nativeMetadata)
                    }
                }.awaitAll()
            }

        val nextOffset =
            (page.offset + page.items.size)
                .takeIf { page.items.isNotEmpty() && it < page.total }

        return Result.success(
            TrackSearchPage(
                items = variants,
                continuationToken = nextOffset?.let(::encodeContinuation),
            ),
        )
    }

    private fun decodeContinuation(token: String?): Int {
        if (token == null) return 0
        require(token.startsWith(CONTINUATION_PREFIX)) {
            "Invalid Spotify continuation token"
        }

        return token
            .removePrefix(CONTINUATION_PREFIX)
            .toIntOrNull()
            ?.takeIf { it >= 0 }
            ?: throw IllegalArgumentException("Invalid Spotify continuation offset")
    }

    private fun encodeContinuation(offset: Int): String = CONTINUATION_PREFIX + offset

    companion object {
        private const val CONTINUATION_PREFIX = "offset:"
        private const val MAX_PAGE_SIZE = 50
        private const val NATIVE_METADATA_ENRICH_LIMIT = 12
    }
}

internal fun SpotifyTrack.toSpotifySourceVariant(
    nativeMetadata: SpotifyNativeTrackMetadata? = null,
): SourceVariant =
    SourceVariant(
        provider = ProviderId.SPOTIFY,
        sourceId = id,
        title = name,
        artists = artists.map { it.name }.filter(String::isNotBlank),
        albumId = album?.id?.takeIf(String::isNotBlank),
        albumName = album?.name?.takeIf(String::isNotBlank),
        durationMs =
            nativeMetadata?.durationMs
                ?: durationMs.takeIf { it > 0 }?.toLong(),
        isrc = nativeMetadata?.isrc,
        explicit = nativeMetadata?.explicit ?: explicit,
        artworkUrl = bestArtworkUrl(),
    )

private fun SpotifyTrack.bestArtworkUrl(): String? {
    val images = album?.images.orEmpty()
    return images.firstOrNull { it.width in 200..400 }?.url
        ?: images.firstOrNull()?.url
}

internal fun SourceVariant.toSpotifyCatalogReference(): CatalogTrackReference {
    require(provider == ProviderId.SPOTIFY) {
        "Only Spotify variants can be converted to Spotify catalog references"
    }

    return CatalogTrackReference(
        cacheId = "spotify:" + sourceId,
        title = title,
        artists = artists,
        durationMs = durationMs ?: 0L,
        thumbnailUrl = artworkUrl,
        albumId = albumId?.let { "spotify-album:" + it },
        albumName = albumName,
        explicit = explicit ?: false,
    )
}
