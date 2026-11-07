package com.arturo254.opentune.sources.playback

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.media3.common.Player
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
import com.arturo254.opentune.catalog.CatalogPlaybackResolver
import com.arturo254.opentune.catalog.CatalogTrackReference
import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.core.model.TrackVersionTrait
import com.arturo254.opentune.extensions.ExtraIsMusicVideo
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.innertube.models.WatchEndpoint
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.playback.PlayerConnection
import com.arturo254.opentune.playback.queues.ListQueue
import com.arturo254.opentune.playback.PlaybackGateClassifier
import com.arturo254.opentune.playback.queues.YouTubeQueue
import com.arturo254.opentune.sources.SourceRegistry
import com.arturo254.opentune.sources.resolution.originalRecordingOptions
import com.arturo254.opentune.sources.StreamMusicSource
import com.arturo254.opentune.sources.search.UnifiedSearchRepository
import com.arturo254.opentune.sources.search.UnifiedSearchResult
import com.arturo254.opentune.sources.search.UnifiedTrackSearchResult
import com.arturo254.opentune.sources.apple.toAppleMusicCatalogReference
import com.arturo254.opentune.sources.deezer.toDeezerCatalogReference
import com.arturo254.opentune.sources.spotify.toSpotifyCatalogReference
import com.arturo254.opentune.sources.tidal.toTidalCatalogReference
import com.arturo254.opentune.utils.YTPlayerUtils
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import java.util.concurrent.atomic.AtomicBoolean

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

            SourcePlaybackRoute.AUDIUS_DIRECT,
            SourcePlaybackRoute.UNSUPPORTED -> {
                val item = (sourceRegistry[variant.provider] as? StreamMusicSource)
                    ?.resolvePlayback(variant)
                playResolved(connection, item)
            }
        }

    suspend fun playCollection(
        connection: PlayerConnection,
        tracks: List<SourceVariant>,
        startIndex: Int,
        queueTitle: String,
    ): Boolean {
        if (tracks.isEmpty() || startIndex !in tracks.indices) return false
        val requested = tracks[startIndex]
        if (requested.provider != ProviderId.AUDIUS) return false
        val collectionTracks = tracks.filter { it.provider == ProviderId.AUDIUS }
        val selectedIndex = collectionTracks.indexOfFirst { it.sourceId == requested.sourceId }
        if (selectedIndex < 0) return false
        connection.playQueue(
            AudiusCollectionQueue(
                title = queueTitle,
                tracks = collectionTracks,
                startIndex = selectedIndex,
                resolver = ::resolveMediaItem,
            ),
        )
        return awaitPlaybackStart(connection, requested.sourceId)
    }

    suspend fun playSearchResults(
        connection: PlayerConnection,
        result: UnifiedSearchResult,
        selected: UnifiedTrackSearchResult,
        preferredVariants: List<SourceVariant>,
        queueTitle: String,
    ): Boolean {
        val choices = originalRecordingOptions(
            selected.identity,
            preferredVariants.ifEmpty { listOfNotNull(selected.recommendedVariant) + selected.variants },
        )
        var ageRestricted = false
        for (choice in choices) {
            if (ageRestricted && choice.playbackRoute() == SourcePlaybackRoute.CATALOG_BRIDGE) continue
            val queue = withContext(Dispatchers.Default) {
                SearchResultsQueue(
                    title = queueTitle,
                    initialResult = result,
                    selected = selected,
                    selectedVariants = listOf(choice),
                    repository = searchRepository,
                    coordinator = this@SourcePlaybackCoordinator,
                )
            }
            val firstItem = withContext(Dispatchers.IO) {
                queue.getInitialStatus().items.firstOrNull()
            } ?: continue
            connection.playQueue(queue)
            if (awaitPlaybackStart(connection, firstItem.mediaId)) return true
            if (choice.provider == ProviderId.YOUTUBE_MUSIC) {
                ageRestricted = hasAgeVerificationError(connection.player.playerError)
            }
        }
        return false
    }

    internal suspend fun resolveMediaItem(variant: SourceVariant): MediaItem? =
        try {
            // The canonical Media3 YouTube source resolves its stream on demand.
            // Do not resolve that same URL here before dispatching playback.
            if (variant.playbackRoute() == SourcePlaybackRoute.YOUTUBE_QUEUE) {
                variant.toOpenTuneMetadata().toMediaItem()
            } else {
                val direct = (sourceRegistry[variant.provider] as? StreamMusicSource)
                    ?.resolvePlayback(variant)
                if (direct != null) direct else when (variant.playbackRoute()) {
                    SourcePlaybackRoute.YOUTUBE_QUEUE -> variant.toOpenTuneMetadata().toMediaItem()
                    SourcePlaybackRoute.LOCAL_OR_OFFLINE -> variant.toPlayableMediaItem()
                    SourcePlaybackRoute.CATALOG_BRIDGE ->
                        CatalogPlaybackResolver.resolveToMediaItem(variant.toCatalogPlaybackReference())
                    SourcePlaybackRoute.AUDIUS_DIRECT,
                    SourcePlaybackRoute.UNSUPPORTED -> null
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }

    internal suspend fun resolveTrack(
        variants: List<SourceVariant>,
    ): MediaItem? {
        for (variant in variants.distinctBy { it.provider to it.sourceId }) {
            val item = withTimeoutOrNull(RESOLVE_ITEM_TIMEOUT_MS) {
                resolveMediaItem(variant)
            }
            if (item != null) return item
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
    ): Boolean = withContext(Dispatchers.Main.immediate) {
        withTimeoutOrNull(PLAYBACK_START_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val player = connection.player
                val applicationLooper = player.applicationLooper
                val handler = Handler(applicationLooper)
                val removed = AtomicBoolean(false)
                val listener = object : Player.Listener {
                    fun inspect(player: Player) {
                        if (player.currentMediaItem?.mediaId != mediaId) return
                        val success = when {
                            player.isPlaying -> true
                            player.playerError != null -> false
                            player.playbackState == Player.STATE_ENDED -> false
                            else -> return
                        }
                        if (removed.compareAndSet(false, true)) player.removeListener(this)
                        if (continuation.isActive) continuation.resume(success)
                    }

                    override fun onEvents(player: Player, events: Player.Events) {
                        inspect(player)
                    }
                }
                player.addListener(listener)
                continuation.invokeOnCancellation {
                    val cleanup = Runnable {
                        if (removed.compareAndSet(false, true)) player.removeListener(listener)
                    }
                    if (Looper.myLooper() == applicationLooper) {
                        cleanup.run()
                    } else {
                        handler.post(cleanup)
                    }
                }
                listener.inspect(player)
            }
        } ?: false
    }

    private companion object {
        const val PLAYBACK_START_TIMEOUT_MS = 16_000L
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

internal fun searchPlaybackPriority(provider: ProviderId): Int = when (provider) {
    ProviderId.LOCAL, ProviderId.OPEN_TUNE_OFFLINE -> 0
    ProviderId.AUDIUS -> 1
    ProviderId.YOUTUBE_MUSIC -> 2
    else -> 3
}

internal fun hasAgeVerificationError(error: Throwable?): Boolean {
    var current = error
    while (current != null) {
        if (current is YTPlayerUtils.LoginRequiredForPlaybackException &&
            PlaybackGateClassifier.requiresAgeVerification(current.message)
        ) return true
        current = current.cause
    }
    return false
}

enum class SourcePlaybackRoute {
    YOUTUBE_QUEUE,
    LOCAL_OR_OFFLINE,
    AUDIUS_DIRECT,
    CATALOG_BRIDGE,
    UNSUPPORTED,
}

fun SourceVariant.playbackRoute(): SourcePlaybackRoute =
    when (provider) {
        ProviderId.YOUTUBE_MUSIC -> SourcePlaybackRoute.YOUTUBE_QUEUE
        ProviderId.LOCAL, ProviderId.OPEN_TUNE_OFFLINE -> SourcePlaybackRoute.LOCAL_OR_OFFLINE
        ProviderId.AUDIUS -> SourcePlaybackRoute.AUDIUS_DIRECT
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
