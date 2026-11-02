package com.arturo254.opentune.sources.search

import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.core.model.TrackIdentity
import com.arturo254.opentune.sources.matching.TrackMatchIssue
import com.arturo254.opentune.sources.matching.TrackMatcher
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

internal fun sameRecording(
    reference: TrackIdentity,
    variants: List<SourceVariant>,
    candidate: SourceVariant,
    matcher: TrackMatcher,
): Boolean {
    val result = matcher.match(reference, candidate)
    if (result.issues.any {
            it == TrackMatchIssue.ISRC_CONFLICT ||
                it == TrackMatchIssue.RECORDING_VERSION_CONFLICT
        }) return false

    if (!result.isAccepted && !strictRecordingIdentity(reference, candidate)) return false

    return variants.all { existing ->
        val pair = matcher.match(existing, candidate)
        pair.issues.none {
            it == TrackMatchIssue.ISRC_CONFLICT ||
                it == TrackMatchIssue.RECORDING_VERSION_CONFLICT
        } && (pair.isAccepted || strictRecordingIdentity(existing, candidate))
    }
}

internal fun sameRecording(
    first: UnifiedTrackSearchResult,
    second: UnifiedTrackSearchResult,
    matcher: TrackMatcher,
): Boolean {
    val candidates = second.variants
    if (candidates.isNotEmpty()) {
        return candidates.all { sameRecording(first.identity, first.variants, it, matcher) }
    }
    val firstArtist = first.identity.artists.firstOrNull()?.name.orEmpty()
    val secondArtist = second.identity.artists.firstOrNull()?.name.orEmpty()
    return sameRecordingTitle(first.identity.title, second.identity.title) &&
        normalizedRecordingText(firstArtist).isNotBlank() &&
        normalizedRecordingText(firstArtist) == normalizedRecordingText(secondArtist) &&
        first.identity.version.traits == second.identity.version.traits &&
        compatibleDuration(first.identity.durationMs, second.identity.durationMs) &&
        compatibleIsrc(first.identity.isrc, second.identity.isrc)
}

private fun strictRecordingIdentity(reference: TrackIdentity, candidate: SourceVariant): Boolean {
    val artist = reference.artists.firstOrNull()?.name.orEmpty()
    val candidateArtist = candidate.artists.firstOrNull().orEmpty()
    if (normalizedRecordingText(artist).isBlank()) return false
    if (normalizedRecordingText(artist) != normalizedRecordingText(candidateArtist)) return false
    if (!sameRecordingTitle(reference.title, candidate.title)) return false
    if (!compatibleIsrc(reference.isrc, candidate.isrc)) return false
    if (!compatibleDuration(reference.durationMs, candidate.durationMs)) return false
    if (reference.version.traits != candidate.version.traits) return false
    val firstLabel = normalizedRecordingText(reference.version.label.orEmpty())
    val secondLabel = normalizedRecordingText(candidate.version.label.orEmpty())
    return firstLabel == secondLabel || firstLabel.isEmpty() || secondLabel.isEmpty()
}

private fun strictRecordingIdentity(left: SourceVariant, right: SourceVariant): Boolean =
    strictRecordingIdentity(
        TrackIdentity(
            title = left.title,
            artists = left.artists.map { com.arturo254.opentune.core.model.ArtistIdentity(it) },
            durationMs = left.durationMs,
            isrc = left.isrc,
            version = left.version,
        ),
        right,
    )

private fun sameRecordingTitle(a: String, b: String): Boolean =
    normalizedRecordingText(a).isNotEmpty() &&
        normalizedRecordingText(a) == normalizedRecordingText(b)

private fun compatibleIsrc(a: String?, b: String?): Boolean {
    val left = a?.filter(Char::isLetterOrDigit)?.uppercase(Locale.ROOT).orEmpty()
    val right = b?.filter(Char::isLetterOrDigit)?.uppercase(Locale.ROOT).orEmpty()
    return left.isEmpty() || right.isEmpty() || left == right
}

private fun compatibleDuration(a: Long?, b: Long?): Boolean =
    a == null || b == null || abs(a - b) <= 5_000L

private fun normalizedRecordingText(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")
