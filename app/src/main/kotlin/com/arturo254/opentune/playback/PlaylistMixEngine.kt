package com.arturo254.opentune.playback

import androidx.media3.common.MediaItem
import com.arturo254.opentune.deezer.Deezer
import com.arturo254.opentune.db.entities.PlaylistSong
import com.arturo254.opentune.extensions.toMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

data class MixTrackAnalysis(
    val playlistSong: PlaylistSong,
    val bpm: Float?,
    val gainDb: Float?,
    val sourceTrackId: String?,
    val confidence: Float,
)

data class MixTransition(
    val outgoingSongId: String,
    val incomingSongId: String,
    val durationMs: Int,
    val curve: MixTransitionCurve,
    val bpmDelta: Float?,
    val gainDelta: Float?,
)

data class PlaylistMixPlan(
    val orderedTracks: List<MixTrackAnalysis>,
    val transitions: List<MixTransition>,
) {
    fun toMediaItems(): List<MediaItem> =
        orderedTracks.mapIndexed { index, analysis ->
            val base = analysis.playlistSong.song.toMediaItem()
            val transition = transitions.getOrNull(index)
            if (transition == null) {
                base
            } else {
                base.withMixTransition(
                    durationMs = transition.durationMs,
                    curve = transition.curve,
                    bpm = analysis.bpm,
                    gainDb = analysis.gainDb,
                )
            }
        }
}

/**
 * Smart playlist mixer.
 *
 * This is intentionally deterministic and explainable: it does not claim a
 * neural model. It uses catalog tempo/loudness metadata when available, then
 * orders tracks to reduce abrupt BPM and energy jumps. Manual mode preserves
 * the user's current playlist order and only applies the selected transition.
 */
object PlaylistMixEngine {
    private const val FEATURE_CONCURRENCY = 4
    private val cache = ConcurrentHashMap<String, MixTrackAnalysis>()

    suspend fun analyze(
        songs: List<PlaylistSong>,
    ): List<MixTrackAnalysis> =
        coroutineScope {
            val semaphore = Semaphore(FEATURE_CONCURRENCY)
            songs.map { playlistSong ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        analyzeTrack(playlistSong)
                    }
                }
            }.awaitAll()
        }

    suspend fun buildSmartPlan(
        songs: List<PlaylistSong>,
    ): PlaylistMixPlan =
        withContext(Dispatchers.Default) {
            val analyzed = analyze(songs)
            if (analyzed.size <= 1) {
                return@withContext PlaylistMixPlan(analyzed, emptyList())
            }

            val remaining = analyzed.toMutableList()
            val first =
                chooseStartingTrack(remaining)
                    .also { remaining.remove(it) }
            val ordered = mutableListOf(first)

            while (remaining.isNotEmpty()) {
                val previous = ordered.last()
                val next =
                    remaining.maxByOrNull { candidate ->
                        compatibility(previous, candidate)
                    } ?: remaining.first()

                ordered += next
                remaining.remove(next)
            }

            PlaylistMixPlan(
                orderedTracks = ordered,
                transitions = buildSuggestedTransitions(ordered),
            )
        }

    suspend fun buildManualPlan(
        songs: List<PlaylistSong>,
        durationMs: Int,
        curve: MixTransitionCurve,
    ): PlaylistMixPlan =
        withContext(Dispatchers.Default) {
            val ordered =
                songs.map { playlistSong ->
                    MixTrackAnalysis(
                        playlistSong = playlistSong,
                        bpm = null,
                        gainDb = null,
                        sourceTrackId = null,
                        confidence = 0f,
                    )
                }

            PlaylistMixPlan(
                orderedTracks = ordered,
                transitions =
                    ordered.zipWithNext { current, next ->
                        MixTransition(
                            outgoingSongId = current.playlistSong.song.id,
                            incomingSongId = next.playlistSong.song.id,
                            durationMs = durationMs.coerceIn(1_500, 20_000),
                            curve = curve,
                            bpmDelta = null,
                            gainDelta = null,
                        )
                    },
            )
        }

    fun rebuildWithManualTransitions(
        analyzed: List<MixTrackAnalysis>,
        transitions: List<MixTransition>,
    ): PlaylistMixPlan =
        PlaylistMixPlan(
            orderedTracks = analyzed,
            transitions = transitions,
        )

    private suspend fun analyzeTrack(
        playlistSong: PlaylistSong,
    ): MixTrackAnalysis {
        val song = playlistSong.song
        val artist = song.artists.joinToString(" ") { it.name }.trim()
        val key =
            buildString {
                append(song.title.lowercase())
                append('|')
                append(artist.lowercase())
                append('|')
                append(song.song.duration)
            }

        cache[key]?.let { cached ->
            return cached.copy(playlistSong = playlistSong)
        }

        val details =
            Deezer
                .findAudioFeatures(
                    title = song.title,
                    artist = artist,
                    durationSeconds = song.song.duration,
                )
                .getOrNull()

        val titleMatch =
            details?.title?.equals(song.title, ignoreCase = true) == true
        val artistMatch =
            details?.artistName
                ?.let { resolved ->
                    artist.contains(resolved, ignoreCase = true) ||
                        resolved.contains(artist, ignoreCase = true)
                } == true
        val durationMatch =
            details?.durationSeconds
                ?.takeIf { it > 0 && song.song.duration > 0 }
                ?.let { abs(it - song.song.duration) <= 5 }
                ?: false

        val confidence =
            (
                (if (titleMatch) 0.50f else 0.28f) +
                    (if (artistMatch) 0.30f else 0.10f) +
                    (if (durationMatch) 0.20f else 0.06f)
            ).coerceIn(0f, 1f)

        val analysis =
            MixTrackAnalysis(
                playlistSong = playlistSong,
                bpm = details?.bpm,
                gainDb = details?.gainDb,
                sourceTrackId = details?.id,
                confidence = if (details == null) 0f else confidence,
            )

        cache[key] = analysis
        return analysis
    }

    private fun chooseStartingTrack(
        tracks: List<MixTrackAnalysis>,
    ): MixTrackAnalysis {
        val knownBpms = tracks.mapNotNull { it.bpm }.sorted()
        val median =
            knownBpms
                .takeIf { it.isNotEmpty() }
                ?.let { bpms -> bpms[bpms.size / 2] }

        return if (median == null) {
            tracks.first()
        } else {
            tracks.minByOrNull { track ->
                track.bpm?.let { abs(it - median) } ?: 999f
            } ?: tracks.first()
        }
    }

    private fun compatibility(
        current: MixTrackAnalysis,
        candidate: MixTrackAnalysis,
    ): Float {
        val currentArtist =
            current.playlistSong.song.artists
                .firstOrNull()
                ?.name
                .orEmpty()
        val candidateArtist =
            candidate.playlistSong.song.artists
                .firstOrNull()
                ?.name
                .orEmpty()

        val bpmScore =
            when {
                current.bpm == null || candidate.bpm == null -> 0.50f
                else -> (1f - (abs(current.bpm - candidate.bpm) / 28f)).coerceIn(0f, 1f)
            }

        val gainScore =
            when {
                current.gainDb == null || candidate.gainDb == null -> 0.55f
                else -> (1f - (abs(current.gainDb - candidate.gainDb) / 10f)).coerceIn(0f, 1f)
            }

        val artistSpread =
            if (
                currentArtist.isNotBlank() &&
                    candidateArtist.isNotBlank() &&
                    currentArtist.equals(candidateArtist, ignoreCase = true)
            ) {
                0.45f
            } else {
                1f
            }

        val confidence =
            ((current.confidence + candidate.confidence) * 0.5f).coerceIn(0f, 1f)

        return (
            bpmScore * 0.55f +
                gainScore * 0.22f +
                artistSpread * 0.13f +
                confidence * 0.10f
        )
    }

    private fun buildSuggestedTransitions(
        ordered: List<MixTrackAnalysis>,
    ): List<MixTransition> =
        ordered.zipWithNext { current, next ->
            val bpmDelta = bpmDelta(current, next)
            val gainDelta = gainDelta(current, next)

            val curve =
                when {
                    bpmDelta == null -> MixTransitionCurve.BLEND
                    bpmDelta <= 2.5f -> MixTransitionCurve.BLEND
                    bpmDelta <= 6f -> MixTransitionCurve.MELT
                    bpmDelta <= 12f -> MixTransitionCurve.RISE
                    else -> MixTransitionCurve.FADE
                }

            val durationMs =
                when {
                    bpmDelta == null -> 6_000
                    bpmDelta <= 2.5f -> 10_000
                    bpmDelta <= 6f -> 8_000
                    bpmDelta <= 12f -> 6_000
                    else -> 4_000
                }

            MixTransition(
                outgoingSongId = current.playlistSong.song.id,
                incomingSongId = next.playlistSong.song.id,
                durationMs = durationMs,
                curve = curve,
                bpmDelta = bpmDelta,
                gainDelta = gainDelta,
            )
        }

    private fun bpmDelta(
        first: MixTrackAnalysis,
        second: MixTrackAnalysis,
    ): Float? =
        if (first.bpm != null && second.bpm != null) {
            abs(first.bpm - second.bpm)
        } else {
            null
        }

    private fun gainDelta(
        first: MixTrackAnalysis,
        second: MixTrackAnalysis,
    ): Float? =
        if (first.gainDb != null && second.gainDb != null) {
            abs(first.gainDb - second.gainDb)
        } else {
            null
        }
}
