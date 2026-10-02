package com.arturo254.opentune.sources.youtube

import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.core.model.TrackVersion
import com.arturo254.opentune.core.model.TrackVersionTrait
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.SongItem
import com.arturo254.opentune.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_OMV
import com.arturo254.opentune.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_UGC
import com.arturo254.opentune.sources.SourceCapabilities
import com.arturo254.opentune.sources.SourceCapability
import com.arturo254.opentune.sources.TrackSearchPage
import com.arturo254.opentune.sources.TrackSearchRequest
import com.arturo254.opentune.sources.TrackSearchSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 2 adapter over the existing YouTube Music/Innertube implementation.
 *
 * This adapter does not replace OnlineSearchViewModel or playback yet. It gives the
 * new source layer a provider-neutral view of YouTube Music while the old runtime
 * keeps working unchanged.
 */
@Singleton
class YouTubeMusicSource
@Inject
constructor() : TrackSearchSource {
    override val id: ProviderId = ProviderId.YOUTUBE_MUSIC
    override val displayName: String = "YouTube Music"

    override val capabilities: SourceCapabilities =
        SourceCapabilities.of(
            SourceCapability.TRACK_SEARCH,
            SourceCapability.TRACK_METADATA,
            SourceCapability.ALBUMS,
            SourceCapability.ARTISTS,
            SourceCapability.PLAYLISTS,
            SourceCapability.RADIO,
            SourceCapability.ACCOUNT_LIBRARY,
            SourceCapability.STREAM,
            SourceCapability.OPEN_TUNE_MANAGED_DOWNLOAD,
            SourceCapability.AUDIO_QUALITY,
            SourceCapability.STATIC_ARTWORK,
        )

    override suspend fun searchTracks(request: TrackSearchRequest): Result<TrackSearchPage> {
        val continuation = request.continuationToken
        if (continuation == null && request.query.isBlank()) {
            return Result.success(TrackSearchPage(items = emptyList()))
        }

        val result =
            if (continuation != null) {
                YouTube.searchContinuation(continuation)
            } else {
                YouTube.search(
                    query = request.query.trim(),
                    filter = YouTube.SearchFilter.FILTER_SONG,
                )
            }

        return result.map { page ->
            TrackSearchPage(
                items =
                    page.items
                        .filterIsInstance<SongItem>()
                        .take(request.limit)
                        .map(SongItem::toYouTubeSourceVariant),
                continuationToken = page.continuation,
            )
        }
    }
}

internal fun SongItem.toYouTubeSourceVariant(): SourceVariant {
    val musicVideoType =
        endpoint
            ?.watchEndpointMusicSupportedConfigs
            ?.watchEndpointMusicConfig
            ?.musicVideoType
    val traits =
        if (musicVideoType == MUSIC_VIDEO_TYPE_OMV || musicVideoType == MUSIC_VIDEO_TYPE_UGC) {
            setOf(TrackVersionTrait.MUSIC_VIDEO)
        } else {
            emptySet()
        }

    return SourceVariant(
        provider = ProviderId.YOUTUBE_MUSIC,
        sourceId = id,
        title = title,
        artists = artists.map { it.name },
        albumId = album?.id,
        albumName = album?.name,
        durationMs = duration?.takeIf { it >= 0 }?.times(1_000L),
        explicit = explicit,
        version = TrackVersion(traits = traits),
        artworkUrl = thumbnail,
    )
}
