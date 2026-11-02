package com.arturo254.opentune.sources.playback

import android.os.Bundle
import androidx.media3.common.Player
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
import com.arturo254.opentune.catalog.CatalogPlaybackResolver
import com.arturo254.opentune.catalog.CatalogTrackReference
import com.arturo254.opentune.core.model.ArtistIdentity
import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.TrackIdentity
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.core.model.TrackVersionTrait
import com.arturo254.opentune.extensions.ExtraIsMusicVideo
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.innertube.models.WatchEndpoint
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.playback.PlayerConnection
import com.arturo254.opentune.playback.queues.ListQueue
import com.arturo254.opentune.playback.queues.YouTubeQueue
import com.arturo254.opentune.sources.SourceRegistry
import com.arturo254.opentune.sources.StreamMusicSource
import com.arturo254.opentune.sources.TrackSearchRequest
import com.arturo254.opentune.sources.TrackSearchSource
import com.arturo254.opentune.sources.matching.TrackMatcher
import com.arturo254.opentune.sources.search.UnifiedSearchRepository
import com.arturo254.opentune.sources.search.UnifiedSearchResult
import com.arturo254.opentune.sources.search.UnifiedTrackSearchResult
import com.arturo254.opentune.sources.apple.toAppleMusicCatalogReference
import com.arturo254.opentune.sources.deezer.toDeezerCatalogReference
import com.arturo254.opentune.sources.soundcloud.toSoundCloudCatalogReference
import com.arturo254.opentune.sources.spotify.toSpotifyCatalogReference
import com.arturo254.opentune.sources.tidal.toTidalCatalogReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

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
    private val sourceRegistry: SourceRegistry,
    private val trackMatcher: TrackMatcher,
    private val searchRepository: UnifiedSearchRepository,
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
                        (sourceRegistry[variant.provider] as? StreamMusicSource)
                            ?.resolvePlayback(variant)
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

            SourcePlaybackRoute.UNSUPPORTED -> {
                val item = (sourceRegistry[variant.provider] as? StreamMusicSource)
                    ?.resolvePlayback(variant)
                playResolved(connection, item)
            }
        }

    suspend fun playSearchResults(
        connection: PlayerConnection,
        result: UnifiedSearchResult,
        selected: UnifiedTrackSearchResult,
        preferredVariants: List<SourceVariant>,
        queueTitle: String,
    ): Boolean {
        val choices = preferredVariants
            .ifEmpty { listOfNotNull(selected.recommendedVariant) + selected.variants }
            .distinctBy { it.provider to it.sourceId }
        for (choice in choices) {
            val queue = SearchResultsQueue(
                title = queueTitle,
                initialResult = result,
                selected = selected,
                selectedVariants = listOf(choice),
                repository = searchRepository,
                coordinator = this,
            )
            val firstItem = queue.getInitialStatus().items.firstOrNull() ?: continue
            connection.playQueue(queue)
            if (awaitPlaybackStart(connection, firstItem.mediaId)) return true
        }
        return false
    }

    internal suspend fun resolveMediaItem(variant: SourceVariant): MediaItem? =
        try {
            val direct = (sourceRegistry[variant.provider] as? StreamMusicSource)
                ?.resolvePlayback(variant)
            if (direct != null) return direct
            when (variant.playbackRoute()) {
                SourcePlaybackRoute.YOUTUBE_QUEUE -> variant.toOpenTuneMetadata().toMediaItem()
                SourcePlaybackRoute.LOCAL_OR_OFFLINE -> variant.toPlayableMediaItem()
                SourcePlaybackRoute.SOUNDCLOUD_DIRECT ->
                    CatalogPlaybackResolver.resolveToMediaItem(variant.toSoundCloudCatalogReference())
                SourcePlaybackRoute.CATALOG_BRIDGE ->
                    CatalogPlaybackResolver.resolveToMediaItem(variant.toCatalogPlaybackReference())
                SourcePlaybackRoute.UNSUPPORTED -> null
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }

    internal suspend fun resolveTrack(
        variants: List<SourceVariant>,
        allowSoundCloudSearch: Boolean,
    ): MediaItem? {
        val candidates = variants.distinctBy { it.provider to it.sourceId }
        for (variant in candidates) {
            val resolved = withTimeoutOrNull(RESOLVE_ITEM_TIMEOUT_MS) {
                resolveMediaItem(variant)
            }
            if (resolved != null) return resolved
        }
        if (!allowSoundCloudSearch) return null
        val reference = candidates.firstOrNull() ?: return null
        if (candidates.any { it.provider == ProviderId.SOUNDCLOUD }) return null
        val soundCloud = sourceRegistry[ProviderId.SOUNDCLOUD] as? TrackSearchSource ?: return null
        val query = (listOf(reference.title) + reference.artists.take(1))
            .filter(String::isNotBlank).joinToString(" ")
        if (query.isBlank()) return null
        val found = withTimeoutOrNull(SOUNDCLOUD_SEARCH_TIMEOUT_MS) {
            soundCloud.searchTracks(TrackSearchRequest(query = query, limit = 12))
                .getOrNull()?.items
        }.orEmpty()
        for (variant in matchingSoundCloudFallbacks(reference, found, trackMatcher)) {
            val resolved = withTimeoutOrNull(RESOLVE_ITEM_TIMEOUT_MS) {
                resolveMediaItem(variant)
            }
            if (resolved != null) return resolved
        }
        return null
    }

    suspend fun playFirstAvailable(
        connection: PlayerConnection,
        variants: List<SourceVariant>,
        queueTitle: String,
    ): Boolean {
        val candidates = variants.distinctBy { it.provider to it.sourceId }
        for (variant in candidates) {
            if (tryPlayback(connection, variant, queueTitle)) return true
        }

        val reference = candidates.firstOrNull() ?: return false
        if (candidates.any { it.provider == ProviderId.SOUNDCLOUD }) return false
        val soundCloud = sourceRegistry[ProviderId.SOUNDCLOUD] as? TrackSearchSource ?: return false
        val query = (listOf(reference.title) + reference.artists.take(1))
            .filter(String::isNotBlank)
            .joinToString(" ")
        if (query.isBlank()) return false

        val discovered = withTimeoutOrNull(SOUNDCLOUD_SEARCH_TIMEOUT_MS) {
            soundCloud.searchTracks(TrackSearchRequest(query = query, limit = 12))
                .getOrNull()?.items
        }.orEmpty()
        val fallbackCandidates = matchingSoundCloudFallbacks(reference, discovered, trackMatcher)
        for (variant in fallbackCandidates) {
            if (tryPlayback(connection, variant, queueTitle)) return true
        }
        return false
    }

    private suspend fun tryPlayback(
        connection: PlayerConnection,
        variant: SourceVariant,
        queueTitle: String,
    ): Boolean {
        val dispatched = try {
            play(connection, variant, queueTitle)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
        if (!dispatched) return false
        val expectedMediaId = if (variant.playbackRoute() == SourcePlaybackRoute.CATALOG_BRIDGE) {
            connection.player.currentMediaItem?.mediaId ?: return false
        } else {
            variant.sourceId
        }
        return awaitPlaybackStart(connection, expectedMediaId)
    }

    private suspend fun awaitPlaybackStart(
        connection: PlayerConnection,
        mediaId: String,
    ): Boolean = withTimeoutOrNull(PLAYBACK_START_TIMEOUT_MS) {
        suspendCancellableCoroutine { continuation ->
            val player = connection.player
            val listener = object : Player.Listener {
                fun inspect(player: Player) {
                    if (player.currentMediaItem?.mediaId != mediaId) return
                    val success = when {
                        player.isPlaying -> true
                        player.playerError != null -> false
                        player.playbackState == Player.STATE_ENDED -> false
                        else -> return
                    }
                    player.removeListener(this)
                    if (continuation.isActive) continuation.resume(success)
                }

                override fun onEvents(player: Player, events: Player.Events) {
                    inspect(player)
                }
            }
            player.addListener(listener)
            continuation.invokeOnCancellation { player.removeListener(listener) }
            listener.inspect(player)
        }
    } ?: false

    private companion object {
        const val PLAYBACK_START_TIMEOUT_MS = 16_000L
        const val SOUNDCLOUD_SEARCH_TIMEOUT_MS = 6_000L
        const val RESOLVE_ITEM_TIMEOUT_MS = 8_000L
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

internal fun matchingSoundCloudFallbacks(
    reference: SourceVariant,
    candidates: List<SourceVariant>,
    trackMatcher: TrackMatcher,
): List<SourceVariant> {
    val identity = TrackIdentity(
        title = reference.title,
        artists = reference.artists.map { ArtistIdentity(name = it) },
        durationMs = reference.durationMs,
        isrc = reference.isrc,
        explicit = reference.explicit,
        version = reference.version,
    )
    return trackMatcher.rankCandidates(identity, candidates)
        .asSequence()
        .filter { it.variant.provider == ProviderId.SOUNDCLOUD && it.match.isAccepted }
        .map { it.variant }
        .take(3)
        .toList()
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
