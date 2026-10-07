package com.arturo254.opentune.sources.offline

import com.arturo254.opentune.catalog.CatalogPlaybackResolver
import com.arturo254.opentune.catalog.CatalogTrackReference
import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.core.model.TrackIdentity
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.playback.DownloadUtil
import javax.inject.Inject
import javax.inject.Singleton

data class OfflineDownloadPlan(
    val asset: OfflineTrackAsset,
    val metadata: MediaMetadata,
)

@Singleton
class OfflineDownloadPlanner
@Inject
constructor() {
    suspend fun plan(
        identity: TrackIdentity,
        variants: List<SourceVariant>,
    ): Result<OfflineDownloadPlan> =
        runCatching {
            val candidates =
                variants.filterNot { variant ->
                    variant.provider == ProviderId.OPEN_TUNE_OFFLINE ||
                        variant.provider == ProviderId.LOCAL
                }
            require(candidates.isNotEmpty()) {
                "No downloadable online variant is available"
            }

            val youtubeVariant =
                candidates.firstOrNull { it.provider == ProviderId.YOUTUBE_MUSIC }
            val discoveryVariant =
                youtubeVariant
                    ?: candidates.firstOrNull { it.provider == ProviderId.SOUNDCLOUD }
                    ?: candidates.first()

            val metadata =
                if (youtubeVariant != null) {
                    youtubeVariant.toOfflineMetadata(identity)
                } else {
                    CatalogPlaybackResolver.resolveToMetadata(
                        CatalogTrackReference(
                            cacheId =
                                discoveryVariant.provider.value +
                                    ":" +
                                    discoveryVariant.sourceId,
                            title = identity.title,
                            artists =
                                identity.artists
                                    .map { it.name }
                                    .ifEmpty { discoveryVariant.artists },
                            durationMs =
                                identity.durationMs
                                    ?: discoveryVariant.durationMs
                                    ?: 0L,
                            thumbnailUrl =
                                discoveryVariant.artworkUrl
                                    ?: variants.firstNotNullOfOrNull { it.artworkUrl },
                            albumId =
                                identity.album?.canonicalId
                                    ?: discoveryVariant.albumId,
                            albumName =
                                identity.album?.title
                                    ?: discoveryVariant.albumName,
                            explicit =
                                identity.explicit
                                    ?: discoveryVariant.explicit
                                    ?: false,
                        ),
                    ) ?: error("Could not resolve this track to an offline-capable source")
                }

            val backingVariant =
                youtubeVariant
                    ?: SourceVariant(
                        provider = ProviderId.YOUTUBE_MUSIC,
                        sourceId = metadata.id,
                        title = metadata.title,
                        artists = metadata.artists.map { it.name },
                        albumId = metadata.album?.id,
                        albumName = metadata.album?.title,
                        durationMs =
                            metadata.duration
                                .takeIf { it >= 0 }
                                ?.toLong()
                                ?.times(1_000L),
                        explicit = metadata.explicit,
                        artworkUrl = metadata.thumbnailUrl,
                    )

            val discoverySources =
                candidates
                    .map { variant ->
                        OfflineSourceRef(
                            provider = variant.provider,
                            sourceId = variant.sourceId,
                        )
                    }.distinctBy { it.provider to it.sourceId }

            OfflineDownloadPlan(
                asset =
                    OfflineTrackAsset(
                        mediaId = metadata.id,
                        discoverySources = discoverySources,
                        backingProvider = ProviderId.YOUTUBE_MUSIC,
                        backingSourceId = metadata.id,
                        title = identity.title.ifBlank { metadata.title },
                        artists =
                            identity.artists
                                .map { it.name }
                                .ifEmpty { metadata.artists.map { it.name } },
                        albumId =
                            identity.album?.canonicalId
                                ?: metadata.album?.id,
                        albumName =
                            identity.album?.title
                                ?: metadata.album?.title,
                        durationMs =
                            identity.durationMs
                                ?: metadata.duration
                                    .takeIf { it >= 0 }
                                    ?.toLong()
                                    ?.times(1_000L),
                        isrc =
                            identity.isrc
                                ?: candidates.firstNotNullOfOrNull { it.isrc },
                        explicit = identity.explicit ?: metadata.explicit,
                        version = identity.version,
                        artworkUrl =
                            variants.firstNotNullOfOrNull { it.artworkUrl }
                                ?: metadata.thumbnailUrl,
                        quality = backingVariant.quality,
                    ),
                metadata =
                    metadata.copy(
                        title = identity.title.ifBlank { metadata.title },
                        artists =
                            identity.artists
                                .map {
                                    MediaMetadata.Artist(
                                        id = null,
                                        name = it.name,
                                    )
                                }.takeIf { it.isNotEmpty() }
                                ?: metadata.artists,
                        duration =
                            identity.durationMs
                                ?.div(1_000L)
                                ?.coerceAtMost(Int.MAX_VALUE.toLong())
                                ?.toInt()
                                ?: metadata.duration,
                        thumbnailUrl =
                            variants.firstNotNullOfOrNull { it.artworkUrl }
                                ?: metadata.thumbnailUrl,
                        album =
                            identity.album?.let { album ->
                                MediaMetadata.Album(
                                    id = album.canonicalId ?: metadata.album?.id.orEmpty(),
                                    title = album.title,
                                )
                            } ?: metadata.album,
                        explicit = identity.explicit ?: metadata.explicit,
                    ),
            )
        }

    private fun SourceVariant.toOfflineMetadata(identity: TrackIdentity): MediaMetadata {
        val resolvedArtists =
            identity.artists
                .map { it.name }
                .ifEmpty { artists }

        return MediaMetadata(
            id = sourceId,
            title = identity.title.ifBlank { title },
            artists =
                resolvedArtists.map { artist ->
                    MediaMetadata.Artist(
                        id = null,
                        name = artist,
                    )
                },
            duration =
                (identity.durationMs ?: durationMs)
                    ?.div(1_000L)
                    ?.coerceAtMost(Int.MAX_VALUE.toLong())
                    ?.toInt()
                    ?: MediaMetadata.UNKNOWN_DURATION,
            thumbnailUrl = artworkUrl,
            album =
                (identity.album?.title ?: albumName)
                    ?.takeIf(String::isNotBlank)
                    ?.let { albumTitle ->
                        MediaMetadata.Album(
                            id =
                                identity.album?.canonicalId
                                    ?: albumId
                                    ?: "",
                            title = albumTitle,
                        )
                    },
            explicit = identity.explicit ?: explicit ?: false,
        )
    }
}

@Singleton
class OfflineDownloadCoordinator
@Inject
constructor(
    private val planner: OfflineDownloadPlanner,
    private val downloadUtil: DownloadUtil,
) {
    suspend fun enqueue(
        identity: TrackIdentity,
        variants: List<SourceVariant>,
    ): Result<OfflineTrackAsset> =
        planner
            .plan(
                identity = identity,
                variants = variants,
            ).mapCatching { plan ->
                downloadUtil.enqueueOfflineDownload(
                    asset = plan.asset,
                    metadata = plan.metadata,
                )
                plan.asset
            }
}
