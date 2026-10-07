package com.arturo254.opentune.sources.playback

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
import com.arturo254.opentune.catalog.CatalogPlaybackResolver
import com.arturo254.opentune.catalog.CatalogTrackReference
import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.core.model.TrackVersionTrait
import com.arturo254.opentune.extensions.ExtraIsMusicVideo
import com.arturo254.opentune.innertube.models.WatchEndpoint
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.playback.PlayerConnection
import com.arturo254.opentune.playback.queues.ListQueue
import com.arturo254.opentune.playback.queues.YouTubeQueue
import com.arturo254.opentune.sources.apple.toAppleMusicCatalogReference
import com.arturo254.opentune.sources.deezer.toDeezerCatalogReference
import com.arturo254.opentune.sources.soundcloud.SoundCloudPlaybackResolver
import com.arturo254.opentune.sources.soundcloud.toSoundCloudCatalogReference
import com.arturo254.opentune.sources.spotify.toSpotifyCatalogReference
import com.arturo254.opentune.sources.tidal.toTidalCatalogReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/**
 * Playback migration boundary shared by Search and Home.
 *
 * The conceptual source stays distinct from the resolved audio source. A
 * Spotify/TIDAL/Apple/Deezer catalog ID is never passed to Media3 as a stream
 * URI; it first goes through the existing catalog playback resolver.
 *
 * YouTube's existing queue and the Media3 download cache remain unchanged.
 */
@Singleton
class SourcePlaybackCoordinator
@Inject
constructor(
    private val soundCloudResolver: SoundCloudPlaybackResolver,
) {
    suspend fun play(
        connection: PlayerConnection,
        variant: SourceVariant,
        queueTitle: String,
    ): Boolean =
        when (variant.playbackRoute()) {
            SourcePlaybackRoute.YOUTUBE_QUEUE -> {
                connection.playQueue(
                    YouTubeQueue(
                        endpoint = WatchEndpoint(videoId = variant.sourceId),
                        preloadItem = variant.toOpenTuneMetadata(),
                    ),
                )
                true
            }

            SourcePlaybackRoute.LOCAL_OR_OFFLINE -> {
                connection.playQueue(
                    ListQueue(
                        title = queueTitle,
                        items = listOf(variant.toPlayableMediaItem()),
                    ),
                )
                true
            }

            SourcePlaybackRoute.SOUNDCLOUD_DIRECT -> {
                val item =
                    try {
                        soundCloudResolver.resolveToMediaItem(variant)
                            ?: CatalogPlaybackResolver.resolveToMediaItem(
                                variant.toSoundCloudCatalogReference(),
                            )
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } catch (_: Exception) {
                        null
                    }
                playResolved(connection, item)
            }

            SourcePlaybackRoute.CATALOG_BRIDGE -> {
                val item =
                    try {
                        CatalogPlaybackResolver.resolveToMediaItem(
                            variant.toCatalogPlaybackReference(),
                        )
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } catch (_: Exception) {
                        null
                    }
                playResolved(connection, item)
            }

            SourcePlaybackRoute.UNSUPPORTED -> false
        }

    suspend fun playFirstAvailable(
        connection: PlayerConnection,
        variants: List<SourceVariant>,
        queueTitle: String,
    ): Boolean {
        for (variant in variants.distinctBy { it.provider to it.sourceId }) {
            if (play(connection, variant, queueTitle)) return true
        }
        return false
    }

    private fun playResolved(
        connection: PlayerConnection,
        item: MediaItem?,
    ): Boolean {
        if (item == null) return false
        connection.player.setMediaItem(item)
        connection.player.prepare()
        connection.player.playWhenReady = true
        return true
    }
}

enum class SourcePlaybackRoute {
    YOUTUBE_QUEUE,
    LOCAL_OR_OFFLINE,
    SOUNDCLOUD_DIRECT,
    CATALOG_BRIDGE,
    UNSUPPORTED,
}

fun SourceVariant.playbackRoute(): SourcePlaybackRoute =
    when (provider) {
        ProviderId.YOUTUBE_MUSIC -> SourcePlaybackRoute.YOUTUBE_QUEUE
        ProviderId.LOCAL, ProviderId.OPEN_TUNE_OFFLINE -> SourcePlaybackRoute.LOCAL_OR_OFFLINE
        ProviderId.SOUNDCLOUD -> SourcePlaybackRoute.SOUNDCLOUD_DIRECT
        ProviderId.DEEZER,
        ProviderId.SPOTIFY,
        ProviderId.TIDAL,
        ProviderId.APPLE_MUSIC -> SourcePlaybackRoute.CATALOG_BRIDGE
        else -> SourcePlaybackRoute.UNSUPPORTED
    }

private fun SourceVariant.toCatalogPlaybackReference(): CatalogTrackReference =
    when (provider) {
        ProviderId.DEEZER -> toDeezerCatalogReference()
        ProviderId.SPOTIFY -> toSpotifyCatalogReference()
        ProviderId.TIDAL -> toTidalCatalogReference()
        ProviderId.APPLE_MUSIC -> toAppleMusicCatalogReference()
        else -> error("Not a catalog playback source: " + provider.value)
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
            albumId?.let { id ->
                MediaMetadata.Album(
                    id = id,
                    title = albumName.orEmpty(),
                )
            },
        explicit = explicit ?: false,
    )
}

private fun SourceVariant.toPlayableMediaItem(): MediaItem {
    val metadata = toOpenTuneMetadata()

    return MediaItem
        .Builder()
        .setMediaId(sourceId)
        .setUri(sourceId)
        .setCustomCacheKey(sourceId)
        .setTag(metadata)
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata
                .Builder()
                .setTitle(title)
                .setSubtitle(artists.joinToString())
                .setArtist(artists.joinToString())
                .setArtworkUri(artworkUrl?.toUri())
                .setAlbumTitle(albumName)
                .setMediaType(MEDIA_TYPE_MUSIC)
                .setExtras(
                    Bundle().apply {
                        putBoolean(
                            ExtraIsMusicVideo,
                            TrackVersionTrait.MUSIC_VIDEO in version.traits,
                        )
                    },
                ).build(),
        ).build()
}
