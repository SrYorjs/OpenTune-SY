package com.arturo254.opentune.sources.resolution

import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.core.model.TrackIdentity
import com.arturo254.opentune.core.model.TrackVersionTrait
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

/**
 * Invisible, deterministic source ordering for the recording the listener tapped.
 *
 * Prefer the same recording (ISRC, title, artist and edition) rather than simply
 * choosing the first catalog hit. It never claims a source is "original" unless
 * its metadata provides evidence. A playable fallback remains available.
 */
internal fun originalRecordingOptions(
    identity: TrackIdentity,
    variants: List<SourceVariant>,
): List<SourceVariant> = variants
    .distinctBy { it.provider to it.sourceId }
    .sortedWith(
        compareByDescending<SourceVariant> { originalRecordingScore(identity, it) }
            .thenBy { when (it.provider) {
                ProviderId.OPEN_TUNE_OFFLINE, ProviderId.LOCAL -> 0
                ProviderId.YOUTUBE_MUSIC -> 1
                ProviderId.AUDIUS -> 2
                else -> 3
            } }
    )

internal fun originalRecordingScore(identity: TrackIdentity, variant: SourceVariant): Int {
    val wantedTitle = recordingText(identity.title)
    val candidateTitle = recordingText(variant.title)
    val wantedArtists = identity.artists.map { recordingText(it.name) }.filter { it.isNotEmpty() }
    val candidateArtists = variant.artists.map(::recordingText).filter { it.isNotEmpty() }
    var score = 0

    if (!identity.isrc.isNullOrBlank() && identity.isrc.equals(variant.isrc, ignoreCase = true)) {
        score += 200
    }
    score += when {
        wantedTitle == candidateTitle -> 95
        candidateTitle.contains(wantedTitle) && wantedTitle.isNotBlank() -> 32
        wantedTitle.contains(candidateTitle) && candidateTitle.isNotBlank() -> 16
        else -> -80
    }
    if (wantedArtists.isNotEmpty()) {
        score += if (candidateArtists.any { it in wantedArtists }) 75 else -90
    }

    val wantedTraits = identity.version.traits
    val otherTraits = variant.version.traits
    val divergentEdits = setOf(
        TrackVersionTrait.COVER, TrackVersionTrait.LIVE, TrackVersionTrait.REMIX,
        TrackVersionTrait.ACOUSTIC, TrackVersionTrait.INSTRUMENTAL,
        TrackVersionTrait.SPED_UP, TrackVersionTrait.SLOWED,
    )
    score -= ((otherTraits intersect divergentEdits) - wantedTraits).size * 110
    if (wantedTraits.isNotEmpty()) {
        score -= ((wantedTraits intersect divergentEdits) - otherTraits).size * 75
    }
    if (TrackVersionTrait.ORIGINAL in otherTraits ||
        TrackVersionTrait.OFFICIAL_AUDIO in otherTraits) score += 18

    val duration = identity.durationMs
    val variantDuration = variant.durationMs
    if (duration != null && duration > 0 && variantDuration != null && variantDuration > 0) {
        val difference = abs(duration - variantDuration)
        score += when {
            difference <= 3_000 -> 24
            difference <= 10_000 -> 8
            difference > 35_000 -> -36
            else -> 0
        }
    }
    if (variant.provider == ProviderId.LOCAL || variant.provider == ProviderId.OPEN_TUNE_OFFLINE) {
        score += 6
    }
    return score
}

private fun recordingText(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
