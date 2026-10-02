package com.arturo254.opentune.sources.matching

import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.core.model.TrackIdentity
import com.arturo254.opentune.core.model.TrackVersion
import com.arturo254.opentune.core.model.TrackVersionTrait
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max

/**
 * Provider-neutral matcher for deciding whether two catalog results represent the
 * same recording.
 *
 * It is deliberately conservative about recording variants. A live, remix, cover,
 * acoustic, instrumental, clean, sped-up/slowed or remastered recording is not
 * silently accepted as the original merely because title and artist are similar.
 *
 * Presentation differences such as MUSIC_VIDEO vs OFFICIAL_AUDIO are preserved as
 * diagnostics, but do not by themselves mean that the underlying recording is
 * different. SourceResolver can later use that distinction as a user preference.
 */
@Singleton
class TrackMatcher
@Inject
constructor() {
    fun match(
        reference: TrackIdentity,
        candidate: SourceVariant,
    ): TrackMatchResult =
        matchDescriptors(
            left =
                MatchDescriptor(
                    title = reference.title,
                    artists = reference.artists.map { it.name },
                    albumName = reference.album?.title,
                    durationMs = reference.durationMs,
                    isrc = reference.isrc,
                    explicit = reference.explicit,
                    version = reference.version,
                ),
            right = candidate.toMatchDescriptor(),
        )

    fun match(
        left: SourceVariant,
        right: SourceVariant,
    ): TrackMatchResult =
        matchDescriptors(
            left = left.toMatchDescriptor(),
            right = right.toMatchDescriptor(),
        )

    fun rankCandidates(
        reference: TrackIdentity,
        candidates: Collection<SourceVariant>,
    ): List<RankedSourceVariant> =
        candidates
            .map { candidate ->
                RankedSourceVariant(
                    variant = candidate,
                    match = match(reference, candidate),
                )
            }
            .sortedWith(
                compareByDescending<RankedSourceVariant> { it.match.status.priority }
                    .thenByDescending { it.match.confidence }
                    .thenBy { it.variant.provider.value }
                    .thenBy { it.variant.sourceId },
            )

    fun bestAcceptedMatch(
        reference: TrackIdentity,
        candidates: Collection<SourceVariant>,
    ): RankedSourceVariant? =
        rankCandidates(reference, candidates)
            .firstOrNull { it.match.isAccepted }

    private fun matchDescriptors(
        left: MatchDescriptor,
        right: MatchDescriptor,
    ): TrackMatchResult {
        val leftTraits = resolveTraits(left)
        val rightTraits = resolveTraits(right)
        val issues = linkedSetOf<TrackMatchIssue>()
        val evidence = linkedSetOf<TrackMatchEvidence>()

        val leftRecordingTraits = recordingTraits(leftTraits)
        val rightRecordingTraits = recordingTraits(rightTraits)

        if (leftRecordingTraits != rightRecordingTraits) {
            issues += TrackMatchIssue.RECORDING_VERSION_CONFLICT
            return TrackMatchResult(
                status = TrackMatchStatus.REJECTED,
                confidence = 0.0,
                evidence = evidence,
                issues = issues,
                score = MatchScoreBreakdown(),
            )
        }

        val leftPresentation = presentationTraits(leftTraits)
        val rightPresentation = presentationTraits(rightTraits)
        if (leftPresentation != rightPresentation &&
            (leftPresentation.isNotEmpty() || rightPresentation.isNotEmpty())
        ) {
            issues += TrackMatchIssue.PRESENTATION_DIFFERENCE
        } else if (leftPresentation.isNotEmpty() && leftPresentation == rightPresentation) {
            evidence += TrackMatchEvidence.VERSION
        }

        val normalizedLeftIsrc = normalizeIsrc(left.isrc)
        val normalizedRightIsrc = normalizeIsrc(right.isrc)
        val bothHaveIsrc = normalizedLeftIsrc != null && normalizedRightIsrc != null

        if (bothHaveIsrc && normalizedLeftIsrc == normalizedRightIsrc) {
            evidence += TrackMatchEvidence.ISRC
            if (left.explicit != null && right.explicit != null && left.explicit != right.explicit) {
                issues += TrackMatchIssue.EXPLICIT_METADATA_DIFFERENCE
            }

            return TrackMatchResult(
                status = TrackMatchStatus.CONFIRMED,
                confidence = 1.0,
                evidence = evidence,
                issues = issues,
                score =
                    MatchScoreBreakdown(
                        isrc = 1.0,
                    ),
            )
        }

        if (bothHaveIsrc) {
            issues += TrackMatchIssue.ISRC_CONFLICT
        }

        val titleScore =
            textSimilarity(
                normalizeTitle(left.title),
                normalizeTitle(right.title),
            )
        val artistScore = artistSimilarity(left.artists, right.artists)
        val durationScore = durationSimilarity(left.durationMs, right.durationMs)
        val albumScore =
            if (!left.albumName.isNullOrBlank() && !right.albumName.isNullOrBlank()) {
                textSimilarity(
                    normalizeText(left.albumName),
                    normalizeText(right.albumName),
                )
            } else {
                null
            }

        if (titleScore >= TITLE_EVIDENCE_THRESHOLD) evidence += TrackMatchEvidence.TITLE
        if (artistScore >= ARTIST_EVIDENCE_THRESHOLD) evidence += TrackMatchEvidence.ARTIST
        if (durationScore != null && durationScore >= DURATION_EVIDENCE_THRESHOLD) {
            evidence += TrackMatchEvidence.DURATION
        }
        if (albumScore != null && albumScore >= ALBUM_EVIDENCE_THRESHOLD) {
            evidence += TrackMatchEvidence.ALBUM
        }
        if (leftRecordingTraits == rightRecordingTraits) {
            evidence += TrackMatchEvidence.VERSION
        }

        if (left.artists.isNotEmpty() && right.artists.isEmpty() ||
            left.artists.isEmpty() && right.artists.isNotEmpty()
        ) {
            issues += TrackMatchIssue.MISSING_ARTIST_METADATA
        }

        if (durationScore != null && durationScore <= DURATION_CONFLICT_THRESHOLD) {
            issues += TrackMatchIssue.DURATION_CONFLICT
        }

        if (left.explicit != null && right.explicit != null && left.explicit != right.explicit) {
            issues += TrackMatchIssue.EXPLICIT_METADATA_DIFFERENCE
        }

        var confidence =
            weightedAverage(
                WeightedScore(titleScore, TITLE_WEIGHT),
                WeightedScore(artistScore, ARTIST_WEIGHT),
                durationScore?.let { WeightedScore(it, DURATION_WEIGHT) },
                albumScore?.let { WeightedScore(it, ALBUM_WEIGHT) },
            )

        if (TrackMatchIssue.PRESENTATION_DIFFERENCE in issues) {
            confidence *= PRESENTATION_DIFFERENCE_MULTIPLIER
        }
        if (TrackMatchIssue.EXPLICIT_METADATA_DIFFERENCE in issues) {
            confidence *= EXPLICIT_DIFFERENCE_MULTIPLIER
        }
        if (TrackMatchIssue.ISRC_CONFLICT in issues) {
            confidence = confidence.coerceAtMost(ISRC_CONFLICT_CONFIDENCE_CAP)
        }

        confidence = confidence.coerceIn(0.0, 1.0)

        val status =
            when {
                confidence >= STRONG_MATCH_THRESHOLD &&
                    titleScore >= MIN_STRONG_TITLE_SCORE &&
                    artistScore >= MIN_STRONG_ARTIST_SCORE &&
                    TrackMatchIssue.ISRC_CONFLICT !in issues -> TrackMatchStatus.STRONG

                confidence >= POSSIBLE_MATCH_THRESHOLD -> TrackMatchStatus.POSSIBLE
                else -> TrackMatchStatus.REJECTED
            }

        return TrackMatchResult(
            status = status,
            confidence = confidence,
            evidence = evidence,
            issues = issues,
            score =
                MatchScoreBreakdown(
                    title = titleScore,
                    artist = artistScore,
                    duration = durationScore,
                    album = albumScore,
                    isrc = if (bothHaveIsrc) 0.0 else null,
                ),
        )
    }

    private fun SourceVariant.toMatchDescriptor(): MatchDescriptor =
        MatchDescriptor(
            title = title,
            artists = artists,
            albumName = albumName,
            durationMs = durationMs,
            isrc = isrc,
            explicit = explicit,
            version = version,
        )

    private fun resolveTraits(descriptor: MatchDescriptor): Set<TrackVersionTrait> =
        descriptor.version.traits +
            inferTitleTraits(descriptor.title) +
            inferTraits(descriptor.version.label.orEmpty())

    /**
     * Titles are only treated as version metadata when the qualifier is clearly
     * separated (parentheses/brackets or a dash suffix). This avoids false
     * positives for legitimate song names such as "Live Forever" or "Cover Me".
     */
    private fun inferTitleTraits(title: String): Set<TrackVersionTrait> =
        buildSet {
            TITLE_QUALIFIER_REGEX
                .findAll(title)
                .mapNotNull { it.groupValues.getOrNull(1) }
                .forEach { qualifier -> addAll(inferTraits(qualifier)) }

            TITLE_SUFFIX_REGEX
                .find(title)
                ?.groupValues
                ?.getOrNull(1)
                ?.let { suffix -> addAll(inferTraits(suffix)) }
        }

    private fun inferTraits(raw: String): Set<TrackVersionTrait> {
        val normalized = normalizeText(raw)
        if (normalized.isBlank()) return emptySet()

        return buildSet {
            if (REMASTER_REGEX.containsMatchIn(normalized)) add(TrackVersionTrait.REMASTER)
            if (LIVE_REGEX.containsMatchIn(normalized)) add(TrackVersionTrait.LIVE)
            if (ACOUSTIC_REGEX.containsMatchIn(normalized)) add(TrackVersionTrait.ACOUSTIC)
            if (REMIX_REGEX.containsMatchIn(normalized)) add(TrackVersionTrait.REMIX)
            if (COVER_REGEX.containsMatchIn(normalized)) add(TrackVersionTrait.COVER)
            if (INSTRUMENTAL_REGEX.containsMatchIn(normalized)) add(TrackVersionTrait.INSTRUMENTAL)
            if (CLEAN_REGEX.containsMatchIn(normalized)) add(TrackVersionTrait.CLEAN)
            if (SPED_UP_REGEX.containsMatchIn(normalized)) add(TrackVersionTrait.SPED_UP)
            if (SLOWED_REGEX.containsMatchIn(normalized)) add(TrackVersionTrait.SLOWED)
            if (MUSIC_VIDEO_REGEX.containsMatchIn(normalized)) add(TrackVersionTrait.MUSIC_VIDEO)
            if (OFFICIAL_AUDIO_REGEX.containsMatchIn(normalized)) add(TrackVersionTrait.OFFICIAL_AUDIO)
            if (ORIGINAL_REGEX.containsMatchIn(normalized)) add(TrackVersionTrait.ORIGINAL)
        }
    }

    private fun recordingTraits(traits: Set<TrackVersionTrait>): Set<TrackVersionTrait> {
        val explicitRecordingTraits = traits.intersect(RECORDING_TRAITS)
        return when {
            explicitRecordingTraits.isEmpty() -> setOf(TrackVersionTrait.ORIGINAL)
            explicitRecordingTraits == setOf(TrackVersionTrait.ORIGINAL) ->
                setOf(TrackVersionTrait.ORIGINAL)

            else -> explicitRecordingTraits - TrackVersionTrait.ORIGINAL
        }
    }

    private fun presentationTraits(traits: Set<TrackVersionTrait>): Set<TrackVersionTrait> =
        traits.intersect(PRESENTATION_TRAITS)

    private fun normalizeIsrc(value: String?): String? =
        value
            ?.trim()
            ?.uppercase(Locale.ROOT)
            ?.replace(Regex("[^A-Z0-9]"), "")
            ?.takeIf { it.isNotBlank() }

    private fun normalizeTitle(value: String): String {
        var stripped = FEAT_BRACKET_REGEX.replace(value, " ")
        stripped = VERSION_BRACKET_REGEX.replace(stripped, " ")
        stripped = VERSION_SUFFIX_REGEX.replace(stripped, " ")
        return normalizeText(stripped)
    }

    private fun normalizeText(value: String): String {
        val decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
        return decomposed
            .replace(COMBINING_MARKS_REGEX, "")
            .lowercase(Locale.ROOT)
            .replace(AMPERSAND_REGEX, " and ")
            .replace(NON_ALNUM_REGEX, " ")
            .replace(MULTI_SPACE_REGEX, " ")
            .trim()
    }

    private fun artistSimilarity(
        left: List<String>,
        right: List<String>,
    ): Double {
        val normalizedLeft = left.map(::normalizeText).filter(String::isNotBlank)
        val normalizedRight = right.map(::normalizeText).filter(String::isNotBlank)

        if (normalizedLeft.isEmpty() && normalizedRight.isEmpty()) return 1.0
        if (normalizedLeft.isEmpty() || normalizedRight.isEmpty()) return 0.0

        val leftToRight =
            normalizedLeft.map { artist ->
                normalizedRight.maxOf { candidate -> textSimilarity(artist, candidate) }
            }.average()
        val rightToLeft =
            normalizedRight.map { artist ->
                normalizedLeft.maxOf { candidate -> textSimilarity(artist, candidate) }
            }.average()

        // Extra featured artists should reduce confidence slightly, not erase a
        // strong primary-artist match.
        return (leftToRight * 0.65 + rightToLeft * 0.35).coerceIn(0.0, 1.0)
    }

    private fun durationSimilarity(
        leftMs: Long?,
        rightMs: Long?,
    ): Double? {
        if (leftMs == null || rightMs == null || leftMs <= 0L || rightMs <= 0L) return null

        val diffMs = abs(leftMs - rightMs)
        val longest = max(leftMs, rightMs).toDouble()
        val ratio = diffMs / longest

        return when {
            diffMs <= 2_000L -> 1.0
            diffMs <= 5_000L -> 0.90
            diffMs <= 10_000L || ratio <= 0.03 -> 0.70
            diffMs <= 20_000L || ratio <= 0.07 -> 0.35
            else -> 0.0
        }
    }

    private fun textSimilarity(
        left: String,
        right: String,
    ): Double {
        if (left == right && left.isNotBlank()) return 1.0
        if (left.isBlank() || right.isBlank()) return 0.0

        val leftTokens = left.split(' ').filter(String::isNotBlank).toSet()
        val rightTokens = right.split(' ').filter(String::isNotBlank).toSet()
        val union = leftTokens union rightTokens
        val tokenScore =
            if (union.isEmpty()) {
                0.0
            } else {
                (leftTokens intersect rightTokens).size.toDouble() / union.size
            }

        val bigramScore = diceBigramSimilarity(left, right)
        return (tokenScore * 0.55 + bigramScore * 0.45).coerceIn(0.0, 1.0)
    }

    private fun diceBigramSimilarity(
        left: String,
        right: String,
    ): Double {
        if (left == right) return 1.0
        if (left.length < 2 || right.length < 2) return 0.0

        val leftBigrams = left.windowed(2).toSet()
        val rightBigrams = right.windowed(2).toSet()
        if (leftBigrams.isEmpty() || rightBigrams.isEmpty()) return 0.0

        val intersection = leftBigrams.count { it in rightBigrams }
        return (2.0 * intersection) / (leftBigrams.size + rightBigrams.size)
    }

    private fun weightedAverage(vararg scores: WeightedScore?): Double {
        val known = scores.filterNotNull()
        val totalWeight = known.sumOf { it.weight }
        if (totalWeight <= 0.0) return 0.0
        return known.sumOf { it.value * it.weight } / totalWeight
    }

    private data class MatchDescriptor(
        val title: String,
        val artists: List<String>,
        val albumName: String?,
        val durationMs: Long?,
        val isrc: String?,
        val explicit: Boolean?,
        val version: TrackVersion,
    )

    private data class WeightedScore(
        val value: Double,
        val weight: Double,
    )

    companion object {
        private const val TITLE_WEIGHT = 0.45
        private const val ARTIST_WEIGHT = 0.30
        private const val DURATION_WEIGHT = 0.20
        private const val ALBUM_WEIGHT = 0.05

        private const val STRONG_MATCH_THRESHOLD = 0.82
        private const val POSSIBLE_MATCH_THRESHOLD = 0.55
        private const val MIN_STRONG_TITLE_SCORE = 0.75
        private const val MIN_STRONG_ARTIST_SCORE = 0.65

        private const val TITLE_EVIDENCE_THRESHOLD = 0.80
        private const val ARTIST_EVIDENCE_THRESHOLD = 0.75
        private const val DURATION_EVIDENCE_THRESHOLD = 0.70
        private const val ALBUM_EVIDENCE_THRESHOLD = 0.80
        private const val DURATION_CONFLICT_THRESHOLD = 0.35

        private const val PRESENTATION_DIFFERENCE_MULTIPLIER = 0.97
        private const val EXPLICIT_DIFFERENCE_MULTIPLIER = 0.97
        private const val ISRC_CONFLICT_CONFIDENCE_CAP = 0.55

        private val RECORDING_TRAITS =
            setOf(
                TrackVersionTrait.ORIGINAL,
                TrackVersionTrait.REMASTER,
                TrackVersionTrait.LIVE,
                TrackVersionTrait.ACOUSTIC,
                TrackVersionTrait.REMIX,
                TrackVersionTrait.COVER,
                TrackVersionTrait.INSTRUMENTAL,
                TrackVersionTrait.CLEAN,
                TrackVersionTrait.SPED_UP,
                TrackVersionTrait.SLOWED,
            )

        private val PRESENTATION_TRAITS =
            setOf(
                TrackVersionTrait.MUSIC_VIDEO,
                TrackVersionTrait.OFFICIAL_AUDIO,
            )

        private val COMBINING_MARKS_REGEX = Regex("\\p{M}+")
        private val NON_ALNUM_REGEX = Regex("[^\\p{L}\\p{N}]+")
        private val MULTI_SPACE_REGEX = Regex("\\s+")
        private val AMPERSAND_REGEX = Regex("[&＋+]")

        private val FEAT_BRACKET_REGEX =
            Regex(
                """[\[(]\s*(?:feat\.?|ft\.?|featuring|with)\b[^\])]*[\])]""",
                RegexOption.IGNORE_CASE,
            )
        private val TITLE_QUALIFIER_REGEX =
            Regex(
                """[\[(]([^\])]+)[\])]""",
                RegexOption.IGNORE_CASE,
            )
        private val TITLE_SUFFIX_REGEX =
            Regex(
                """\s*[-–—]\s*(.+)$""",
                RegexOption.IGNORE_CASE,
            )
        private val VERSION_BRACKET_REGEX =
            Regex(
                """[\[(][^\])]*(?:remaster(?:ed)?|live|acoustic|remix|cover|instrumental|clean|sped\s*up|slowed|official\s+(?:music\s+)?video|official\s+audio|original(?:\s+(?:version|mix))?)[^\])]*[\])]""",
                RegexOption.IGNORE_CASE,
            )
        private val VERSION_SUFFIX_REGEX =
            Regex(
                """\s*[-–—]\s*.*\b(?:remaster(?:ed)?|live|acoustic|remix|cover|instrumental|clean|sped\s*up|slowed|official\s+(?:music\s+)?video|official\s+audio|original(?:\s+(?:version|mix))?)\b.*$""",
                RegexOption.IGNORE_CASE,
            )

        private val REMASTER_REGEX = Regex("\\bremaster(?:ed)?\\b")
        private val LIVE_REGEX = Regex("\\blive\\b")
        private val ACOUSTIC_REGEX = Regex("\\bacoustic\\b|\\bacustic[oa]\\b")
        private val REMIX_REGEX = Regex("\\bremix\\b")
        private val COVER_REGEX = Regex("\\bcover\\b")
        private val INSTRUMENTAL_REGEX = Regex("\\binstrumental\\b")
        private val CLEAN_REGEX = Regex("\\bclean(?: version| edit)?\\b")
        private val SPED_UP_REGEX = Regex("\\bsped\\s*up\\b")
        private val SLOWED_REGEX = Regex("\\bslowed\\b")
        private val MUSIC_VIDEO_REGEX =
            Regex("\\bofficial\\s+(?:music\\s+)?video\\b|\\bmusic\\s+video\\b")
        private val OFFICIAL_AUDIO_REGEX = Regex("\\bofficial\\s+audio\\b")
        private val ORIGINAL_REGEX = Regex("\\boriginal(?: version| mix)?\\b")
    }
}

enum class TrackMatchStatus(
    internal val priority: Int,
) {
    REJECTED(0),
    POSSIBLE(1),
    STRONG(2),
    CONFIRMED(3),
}

enum class TrackMatchEvidence {
    ISRC,
    TITLE,
    ARTIST,
    ALBUM,
    DURATION,
    VERSION,
}

enum class TrackMatchIssue {
    ISRC_CONFLICT,
    RECORDING_VERSION_CONFLICT,
    PRESENTATION_DIFFERENCE,
    EXPLICIT_METADATA_DIFFERENCE,
    MISSING_ARTIST_METADATA,
    DURATION_CONFLICT,
}

data class MatchScoreBreakdown(
    val title: Double? = null,
    val artist: Double? = null,
    val duration: Double? = null,
    val album: Double? = null,
    val isrc: Double? = null,
)

data class TrackMatchResult(
    val status: TrackMatchStatus,
    val confidence: Double,
    val evidence: Set<TrackMatchEvidence>,
    val issues: Set<TrackMatchIssue>,
    val score: MatchScoreBreakdown,
) {
    val isAccepted: Boolean
        get() = status == TrackMatchStatus.CONFIRMED || status == TrackMatchStatus.STRONG
}

data class RankedSourceVariant(
    val variant: SourceVariant,
    val match: TrackMatchResult,
)
