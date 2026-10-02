package com.arturo254.opentune.sources.local

import com.arturo254.opentune.core.model.AudioQualityInfo
import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.sources.SourceCapabilities
import com.arturo254.opentune.sources.SourceCapability
import com.arturo254.opentune.sources.TrackSearchPage
import com.arturo254.opentune.sources.TrackSearchRequest
import com.arturo254.opentune.sources.TrackSearchSource
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adapter for tracks explicitly marked as local in OpenTune's Room library.
 *
 * This source is intentionally narrower than LocalSearchViewModel, which searches
 * the user's whole OpenTune library. ProviderId.LOCAL represents media that the
 * database itself marks with isLocal=true; downloaded YouTube tracks keep their
 * YouTube identity and will later be represented by OfflineEdition instead.
 */
@Singleton
class LocalMusicSource
@Inject
constructor(
    private val database: MusicDatabase,
) : TrackSearchSource {
    override val id: ProviderId = ProviderId.LOCAL
    override val displayName: String = "Local"

    override val capabilities: SourceCapabilities =
        SourceCapabilities.of(
            SourceCapability.TRACK_SEARCH,
            SourceCapability.TRACK_METADATA,
            SourceCapability.STREAM,
            SourceCapability.AUDIO_QUALITY,
            SourceCapability.STATIC_ARTWORK,
        )

    override suspend fun searchTracks(request: TrackSearchRequest): Result<TrackSearchPage> =
        runCatching {
            if (request.continuationToken != null || request.query.isBlank()) {
                return@runCatching TrackSearchPage(items = emptyList())
            }

            val songs =
                database
                    .searchLocalSongs(
                        query = request.query.trim(),
                        limit = request.limit,
                    )
                    .first()

            TrackSearchPage(
                items = songs.map(Song::toLocalSourceVariant),
            )
        }
}

internal fun Song.toLocalSourceVariant(): SourceVariant {
    val storedFormat = format
    val quality =
        storedFormat?.let {
            AudioQualityInfo(
                mimeType = it.mimeType.takeIf(String::isNotBlank),
                codec = it.codecs.takeIf(String::isNotBlank),
                bitrateBitsPerSecond = it.bitrate.takeIf { bitrate -> bitrate > 0 },
                sampleRateHz = it.sampleRate?.takeIf { sampleRate -> sampleRate > 0 },
            )
        }

    return SourceVariant(
        provider = ProviderId.LOCAL,
        sourceId = song.id,
        title = song.title,
        artists = artists.map { it.name },
        albumId = album?.id ?: song.albumId,
        albumName = album?.title ?: song.albumName,
        durationMs = song.duration.takeIf { it >= 0 }?.times(1_000L),
        explicit = song.explicit,
        artworkUrl = song.thumbnailUrl,
        quality = quality,
    )
}
