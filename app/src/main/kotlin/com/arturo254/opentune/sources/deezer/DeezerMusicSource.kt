package com.arturo254.opentune.sources.deezer

import com.arturo254.opentune.catalog.CatalogTrackReference
import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.deezer.Deezer
import com.arturo254.opentune.deezer.DeezerTrack
import com.arturo254.opentune.deezer.DeezerTrackPage
import com.arturo254.opentune.sources.SourceCapabilities
import com.arturo254.opentune.sources.SourceCapability
import com.arturo254.opentune.sources.TrackSearchPage
import com.arturo254.opentune.sources.TrackSearchRequest
import com.arturo254.opentune.sources.TrackSearchSource
import javax.inject.Inject
import javax.inject.Singleton

interface DeezerCatalogGateway {
    suspend fun searchTracksPage(
        query: String,
        limit: Int,
        index: Int,
    ): Result<DeezerTrackPage>

    suspend fun trendingTracks(limit: Int): Result<List<DeezerTrack>>
}

@Singleton
class DefaultDeezerCatalogGateway
@Inject
constructor() : DeezerCatalogGateway {
    override suspend fun searchTracksPage(
        query: String,
        limit: Int,
        index: Int,
    ): Result<DeezerTrackPage> =
        Deezer.searchTracksPage(
            query = query,
            limit = limit,
            index = index,
        )

    override suspend fun trendingTracks(limit: Int): Result<List<DeezerTrack>> =
        Deezer.trendingTracks(limit)
}

/**
 * Provider-neutral Deezer catalog adapter.
 *
 * Deezer is currently a discovery/metadata source. OpenTune still resolves
 * playback through CatalogPlaybackResolver, so this source deliberately does not
 * claim STREAM until provider playback is represented by the source layer itself.
 */
@Singleton
class DeezerMusicSource
@Inject
constructor(
    private val gateway: DeezerCatalogGateway,
) : TrackSearchSource {
    override val id: ProviderId = ProviderId.DEEZER
    override val displayName: String = "Deezer"

    override val capabilities: SourceCapabilities =
        SourceCapabilities.of(
            SourceCapability.TRACK_SEARCH,
            SourceCapability.TRACK_METADATA,
            SourceCapability.STATIC_ARTWORK,
        )

    override suspend fun searchTracks(request: TrackSearchRequest): Result<TrackSearchPage> {
        val query = request.query.trim()
        if (query.isEmpty()) {
            return Result.success(TrackSearchPage(items = emptyList()))
        }

        val index =
            runCatching { decodeContinuation(request.continuationToken) }
                .getOrElse { return Result.failure(it) }

        return gateway
            .searchTracksPage(
                query = query,
                limit = request.limit.coerceAtMost(MAX_PAGE_SIZE),
                index = index,
            ).map { page ->
                TrackSearchPage(
                    items = page.items.map(DeezerTrack::toDeezerSourceVariant),
                    continuationToken = page.nextIndex?.let(::encodeContinuation),
                )
            }
    }

    suspend fun trendingTracks(limit: Int = DEFAULT_TRENDING_LIMIT): Result<List<SourceVariant>> {
        require(limit > 0) { "Trending limit must be positive" }

        return gateway
            .trendingTracks(limit.coerceAtMost(MAX_PAGE_SIZE))
            .map { tracks -> tracks.map(DeezerTrack::toDeezerSourceVariant) }
    }

    private fun decodeContinuation(token: String?): Int {
        if (token == null) return 0
        require(token.startsWith(CONTINUATION_PREFIX)) {
            "Invalid Deezer continuation token"
        }

        return token
            .removePrefix(CONTINUATION_PREFIX)
            .toIntOrNull()
            ?.takeIf { it >= 0 }
            ?: throw IllegalArgumentException("Invalid Deezer continuation index")
    }

    private fun encodeContinuation(index: Int): String = CONTINUATION_PREFIX + index

    companion object {
        private const val CONTINUATION_PREFIX = "index:"
        private const val MAX_PAGE_SIZE = 50
        private const val DEFAULT_TRENDING_LIMIT = 30
    }
}

internal fun DeezerTrack.toDeezerSourceVariant(): SourceVariant =
    SourceVariant(
        provider = ProviderId.DEEZER,
        sourceId = id,
        title = title,
        artists = listOf(artistName),
        albumId = albumId,
        albumName = albumName,
        durationMs = durationSeconds.takeIf { it >= 0 }?.times(1_000L),
        isrc = isrc,
        explicit = explicit,
        artworkUrl = artworkUrl,
    )

internal fun SourceVariant.toDeezerCatalogReference(): CatalogTrackReference {
    require(provider == ProviderId.DEEZER) {
        "Only Deezer variants can be converted to Deezer catalog references"
    }

    return CatalogTrackReference(
        cacheId = "deezer:" + sourceId,
        title = title,
        artists = artists,
        durationMs = durationMs ?: 0L,
        thumbnailUrl = artworkUrl,
        albumId = albumId?.let { "deezer-album:" + it },
        albumName = albumName,
        explicit = explicit ?: false,
    )
}
