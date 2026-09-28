package com.arturo254.opentune.catalog

import androidx.media3.common.MediaItem
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.SongItem
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.models.toMediaMetadata
import com.arturo254.opentune.spotify.SpotifyMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Resolves a metadata-only catalog track against the existing YouTube Music
 * playback engine. This lets OpenTune use a much larger discovery catalog
 * without pretending that third-party catalog APIs provide unrestricted audio.
 */
object CatalogPlaybackResolver {
    private const val MIN_MATCH_THRESHOLD = 0.38
    private const val CACHE_MAX_SIZE = 768

    private val mutex = Mutex()
    private val cache =
        object : LinkedHashMap<String, MediaMetadata>(CACHE_MAX_SIZE, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, MediaMetadata>?
            ): Boolean = size > CACHE_MAX_SIZE
        }

    suspend fun resolveToMediaItem(track: CatalogTrackReference): MediaItem? =
        resolveToMetadata(track)?.toMediaItem()

    suspend fun resolveToMetadata(track: CatalogTrackReference): MediaMetadata? =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                cache[track.cacheId]?.let { return@withContext it }
            }

            val query =
                buildString {
                    append(track.title)
                    track.artists.firstOrNull()?.takeIf { it.isNotBlank() }?.let {
                        append(' ')
                        append(it)
                    }
                    track.albumName?.takeIf { it.isNotBlank() }?.let {
                        append(' ')
                        append(it)
                    }
                }

            val search =
                YouTube
                    .search(
                        query = query,
                        filter = YouTube.SearchFilter.FILTER_SONG,
                    )
                    .getOrNull()
                    ?: return@withContext null

            val candidates =
                search.items
                    .filterIsInstance<SongItem>()
                    .distinctBy { it.id }

            if (candidates.isEmpty()) return@withContext null

            val precomputed =
                SpotifyMapper.precompute(
                    title = track.title,
                    artist = track.artists.joinToString(" "),
                    durationMs = track.durationMs,
                )

            val (best, score) =
                candidates
                    .map { candidate ->
                        candidate to
                            SpotifyMapper.matchScorePrecomputed(
                                precomputed = precomputed,
                                candidateTitle = candidate.title,
                                candidateArtist = candidate.artists.joinToString(" ") { it.name },
                                candidateDurationSec = candidate.duration,
                            )
                    }
                    .maxByOrNull { it.second }
                    ?: return@withContext null

            if (score < MIN_MATCH_THRESHOLD) return@withContext null

            val base = best.toMediaMetadata()
            val metadata =
                base.copy(
                    title = track.title.ifBlank { base.title },
                    artists =
                        track.artists
                            .filter(String::isNotBlank)
                            .map { artist ->
                                MediaMetadata.Artist(
                                    id = null,
                                    name = artist,
                                )
                            }
                            .takeIf { it.isNotEmpty() }
                            ?: base.artists,
                    duration =
                        if (track.durationMs > 0) {
                            (track.durationMs / 1000L).toInt()
                        } else {
                            base.duration
                        },
                    thumbnailUrl = track.thumbnailUrl ?: base.thumbnailUrl,
                    album =
                        track.albumName
                            ?.takeIf(String::isNotBlank)
                            ?.let { albumName ->
                                MediaMetadata.Album(
                                    id = track.albumId ?: base.album?.id.orEmpty(),
                                    title = albumName,
                                )
                            }
                            ?: base.album,
                    explicit = track.explicit || base.explicit,
                )

            mutex.withLock {
                cache[track.cacheId] = metadata
            }

            metadata
        }
}

data class CatalogTrackReference(
    val cacheId: String,
    val title: String,
    val artists: List<String>,
    val durationMs: Long,
    val thumbnailUrl: String?,
    val albumId: String? = null,
    val albumName: String? = null,
    val explicit: Boolean = false,
)
