package com.arturo254.opentune.sources.resolution

import com.arturo254.opentune.core.model.AudioQualityInfo
import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.core.model.TrackIdentity
import com.arturo254.opentune.core.model.TrackVersionTrait
import com.arturo254.opentune.sources.SourceCapability
import com.arturo254.opentune.sources.SourceRegistry
import com.arturo254.opentune.sources.matching.TrackMatchResult
import com.arturo254.opentune.sources.matching.TrackMatchStatus
import com.arturo254.opentune.sources.matching.TrackMatcher
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Selects the best playable source variant for one conceptual track.
 *
 * This class is policy only: it does not open streams, build MediaItems or mutate
 * playback state. The existing player remains unchanged until later Phase 2 work.
 */
@Singleton
class SourceResolver
@Inject
constructor(
    private val registry: SourceRegistry,
    private val trackMatcher: TrackMatcher,
) {
    fun resolve(
        reference: TrackIdentity,
        candidates: Collection<SourcePlaybackCandidate>,
        preferences: SourceResolutionPreferences = SourceResolutionPreferences(),
    ): SourceResolutionResult {
        if (candidates.isEmpty()) return SourceResolutionResult()

        val evaluated =
            candidates.map { candidate ->
                evaluate(
                    reference = reference,
                    candidate = candidate,
                    preferences = preferences,
                )
            }

        val accepted =
            evaluated
                .filter { it.rejectionReason == null }
                .sortedWith(candidateComparator(preferences))

        val rejected =
            evaluated
                .filter { it.rejectionReason != null }
                .sortedWith(
                    compareBy<ResolvedSourceCandidate>(
                        { it.candidate.variant.provider.value },
                        { it.candidate.variant.sourceId },
                    ),
                )

        val selected = accepted.firstOrNull()
        return SourceResolutionResult(
            selected = selected?.copy(selectionReasons = selectionReasons(selected, preferences)),
            rankedCandidates = accepted,
            rejectedCandidates = rejected,
        )
    }

    private fun evaluate(
        reference: TrackIdentity,
        candidate: SourcePlaybackCandidate,
        preferences: SourceResolutionPreferences,
    ): ResolvedSourceCandidate {
        val variant = candidate.variant
        val match = trackMatcher.match(reference, variant)

        val forcedVariant = preferences.forcedVariant
        if (forcedVariant != null &&
            (forcedVariant.provider != variant.provider || forcedVariant.sourceId != variant.sourceId)
        ) {
            return rejected(candidate, match, SourceRejectionReason.FORCED_VARIANT_MISMATCH)
        }

        val forcedProvider = preferences.forcedProvider
        if (forcedProvider != null && forcedProvider != variant.provider) {
            return rejected(candidate, match, SourceRejectionReason.FORCED_PROVIDER_MISMATCH)
        }

        if (!match.isAccepted) {
            return rejected(candidate, match, SourceRejectionReason.MATCH_NOT_ACCEPTED)
        }

        playbackRejection(candidate)?.let { reason ->
            return rejected(candidate, match, reason)
        }

        return ResolvedSourceCandidate(
            candidate = candidate,
            match = match,
            quality = AudioQualityRank.from(variant.quality),
            providerPreferenceIndex = providerPreferenceIndex(variant.provider, preferences.preferredProviders),
            availabilityRank = availabilityRank(candidate.availability, preferences.preferOffline),
            localRank =
                if (preferences.preferLocal &&
                    (variant.provider == ProviderId.LOCAL ||
                        candidate.availability == PlaybackAvailability.LOCAL_FILE)
                ) {
                    1
                } else {
                    0
                },
            presentationRank = presentationRank(variant, preferences.presentationPreference),
            dataSaverRank = dataSaverRank(variant.quality),
        )
    }

    private fun playbackRejection(candidate: SourcePlaybackCandidate): SourceRejectionReason? {
        val variant = candidate.variant
        return when (candidate.availability) {
            PlaybackAvailability.UNAVAILABLE -> SourceRejectionReason.UNAVAILABLE

            PlaybackAvailability.OPEN_TUNE_OFFLINE,
            PlaybackAvailability.LOCAL_FILE,
            -> null

            PlaybackAvailability.PROVIDER_OFFLINE -> {
                val source = registry[variant.provider]
                    ?: return SourceRejectionReason.SOURCE_NOT_REGISTERED
                if (source.supports(SourceCapability.PROVIDER_MANAGED_OFFLINE)) {
                    null
                } else {
                    SourceRejectionReason.PROVIDER_OFFLINE_NOT_SUPPORTED
                }
            }

            PlaybackAvailability.ONLINE -> {
                val source = registry[variant.provider]
                    ?: return SourceRejectionReason.SOURCE_NOT_REGISTERED
                if (source.supports(SourceCapability.STREAM)) {
                    null
                } else {
                    SourceRejectionReason.STREAM_NOT_SUPPORTED
                }
            }
        }
    }

    private fun rejected(
        candidate: SourcePlaybackCandidate,
        match: TrackMatchResult,
        reason: SourceRejectionReason,
    ): ResolvedSourceCandidate =
        ResolvedSourceCandidate(
            candidate = candidate,
            match = match,
            quality = AudioQualityRank.from(candidate.variant.quality),
            rejectionReason = reason,
        )

    private fun candidateComparator(
        preferences: SourceResolutionPreferences,
    ): Comparator<ResolvedSourceCandidate> {
        var comparator =
            compareByDescending<ResolvedSourceCandidate> { matchPriority(it.match.status) }
                .thenByDescending { it.match.confidence }

        if (preferences.preferOffline) {
            comparator = comparator.thenByDescending { it.availabilityRank }
        }

        comparator =
            when (preferences.qualityMode) {
                AudioSelectionMode.BEST_QUALITY ->
                    comparator
                        .thenByDescending { it.quality }
                        .thenByDescending { it.localRank }
                        .thenBy { it.providerPreferenceIndex }
                        .thenByDescending { it.presentationRank }

                AudioSelectionMode.DATA_SAVER ->
                    comparator
                        .thenByDescending { it.dataSaverRank }
                        .thenByDescending { it.localRank }
                        .thenBy { it.providerPreferenceIndex }
                        .thenByDescending { it.presentationRank }

                AudioSelectionMode.AUTOMATIC ->
                    comparator
                        .thenByDescending { it.localRank }
                        .thenBy { it.providerPreferenceIndex }
                        .thenByDescending { it.presentationRank }
                        .thenByDescending { it.quality }
            }

        return comparator
            .thenBy { it.candidate.variant.provider.value }
            .thenBy { it.candidate.variant.sourceId }
    }

    private fun selectionReasons(
        selected: ResolvedSourceCandidate,
        preferences: SourceResolutionPreferences,
    ): Set<SourceSelectionReason> =
        buildSet {
            when (selected.match.status) {
                TrackMatchStatus.CONFIRMED -> add(SourceSelectionReason.CONFIRMED_MATCH)
                TrackMatchStatus.STRONG -> add(SourceSelectionReason.STRONG_MATCH)
                else -> Unit
            }

            when (selected.candidate.availability) {
                PlaybackAvailability.OPEN_TUNE_OFFLINE -> add(SourceSelectionReason.OPEN_TUNE_OFFLINE)
                PlaybackAvailability.LOCAL_FILE -> add(SourceSelectionReason.LOCAL_FILE)
                PlaybackAvailability.PROVIDER_OFFLINE -> add(SourceSelectionReason.PROVIDER_OFFLINE)
                PlaybackAvailability.ONLINE -> add(SourceSelectionReason.ONLINE_STREAM)
                PlaybackAvailability.UNAVAILABLE -> Unit
            }

            if (preferences.forcedVariant != null) add(SourceSelectionReason.USER_FORCED_VARIANT)
            if (preferences.forcedProvider != null) add(SourceSelectionReason.USER_FORCED_PROVIDER)
            if (selected.providerPreferenceIndex != Int.MAX_VALUE) {
                add(SourceSelectionReason.PREFERRED_PROVIDER)
            }
            if (selected.localRank > 0) add(SourceSelectionReason.LOCAL_PREFERRED)

            when (preferences.qualityMode) {
                AudioSelectionMode.BEST_QUALITY -> add(SourceSelectionReason.BEST_QUALITY)
                AudioSelectionMode.DATA_SAVER -> add(SourceSelectionReason.DATA_SAVER)
                AudioSelectionMode.AUTOMATIC -> Unit
            }

            when (preferences.presentationPreference) {
                PresentationPreference.AUDIO_FIRST ->
                    if (selected.presentationRank > 0) add(SourceSelectionReason.AUDIO_PRESENTATION)

                PresentationPreference.VIDEO_FIRST ->
                    if (selected.presentationRank > 0) add(SourceSelectionReason.VIDEO_PRESENTATION)

                PresentationPreference.ANY -> Unit
            }
        }

    private fun providerPreferenceIndex(
        provider: ProviderId,
        preferredProviders: List<ProviderId>,
    ): Int {
        val index = preferredProviders.indexOf(provider)
        return if (index >= 0) index else Int.MAX_VALUE
    }

    private fun availabilityRank(
        availability: PlaybackAvailability,
        preferOffline: Boolean,
    ): Int {
        if (!preferOffline) return if (availability == PlaybackAvailability.UNAVAILABLE) 0 else 1
        return when (availability) {
            PlaybackAvailability.OPEN_TUNE_OFFLINE -> 5
            PlaybackAvailability.LOCAL_FILE -> 4
            PlaybackAvailability.PROVIDER_OFFLINE -> 3
            PlaybackAvailability.ONLINE -> 2
            PlaybackAvailability.UNAVAILABLE -> 0
        }
    }

    private fun presentationRank(
        variant: SourceVariant,
        preference: PresentationPreference,
    ): Int {
        val traits = variant.version.traits
        val isVideo = TrackVersionTrait.MUSIC_VIDEO in traits
        val isOfficialAudio = TrackVersionTrait.OFFICIAL_AUDIO in traits

        return when (preference) {
            PresentationPreference.ANY -> 0
            PresentationPreference.AUDIO_FIRST ->
                when {
                    isOfficialAudio -> 3
                    !isVideo -> 2
                    else -> 0
                }

            PresentationPreference.VIDEO_FIRST ->
                when {
                    isVideo -> 3
                    !isOfficialAudio -> 2
                    else -> 0
                }
        }
    }

    private fun dataSaverRank(quality: AudioQualityInfo?): Long {
        val bitrate = quality?.bitrateBitsPerSecond?.takeIf { it > 0 } ?: return Long.MIN_VALUE
        return -bitrate.toLong()
    }

    private fun matchPriority(status: TrackMatchStatus): Int =
        when (status) {
            TrackMatchStatus.CONFIRMED -> 3
            TrackMatchStatus.STRONG -> 2
            TrackMatchStatus.POSSIBLE -> 1
            TrackMatchStatus.REJECTED -> 0
        }
}

data class SourcePlaybackCandidate(
    val variant: SourceVariant,
    val availability: PlaybackAvailability = PlaybackAvailability.ONLINE,
)

enum class PlaybackAvailability {
    /**
     * OpenTune already owns a complete local playable copy.
     * It must stay playable even when the original provider is unavailable.
     */
    OPEN_TUNE_OFFLINE,

    /** A device-local media file represented by ProviderId.LOCAL. */
    LOCAL_FILE,

    /** Offline playback remains managed by the provider integration itself. */
    PROVIDER_OFFLINE,

    /** Requires the provider's online STREAM capability. */
    ONLINE,

    /** Known to be unavailable for playback in the current context. */
    UNAVAILABLE,
}

enum class AudioSelectionMode {
    /** Prefer offline/local/provider order, then use quality as a tiebreaker. */
    AUTOMATIC,

    /** Among equally valid matches, prefer the strongest known audio quality. */
    BEST_QUALITY,

    /** Among equally valid matches, prefer the lowest known bitrate. */
    DATA_SAVER,
}

enum class PresentationPreference {
    ANY,
    AUDIO_FIRST,
    VIDEO_FIRST,
}

data class SourceVariantKey(
    val provider: ProviderId,
    val sourceId: String,
)

data class SourceResolutionPreferences(
    val preferOffline: Boolean = true,
    val preferLocal: Boolean = true,
    val preferredProviders: List<ProviderId> = emptyList(),
    val forcedProvider: ProviderId? = null,
    val forcedVariant: SourceVariantKey? = null,
    val qualityMode: AudioSelectionMode = AudioSelectionMode.AUTOMATIC,
    val presentationPreference: PresentationPreference = PresentationPreference.ANY,
)

enum class SourceRejectionReason {
    MATCH_NOT_ACCEPTED,
    UNAVAILABLE,
    SOURCE_NOT_REGISTERED,
    STREAM_NOT_SUPPORTED,
    PROVIDER_OFFLINE_NOT_SUPPORTED,
    FORCED_PROVIDER_MISMATCH,
    FORCED_VARIANT_MISMATCH,
}

enum class SourceSelectionReason {
    CONFIRMED_MATCH,
    STRONG_MATCH,
    OPEN_TUNE_OFFLINE,
    LOCAL_FILE,
    PROVIDER_OFFLINE,
    ONLINE_STREAM,
    USER_FORCED_PROVIDER,
    USER_FORCED_VARIANT,
    PREFERRED_PROVIDER,
    LOCAL_PREFERRED,
    BEST_QUALITY,
    DATA_SAVER,
    AUDIO_PRESENTATION,
    VIDEO_PRESENTATION,
}

data class SourceResolutionResult(
    val selected: ResolvedSourceCandidate? = null,
    val rankedCandidates: List<ResolvedSourceCandidate> = emptyList(),
    val rejectedCandidates: List<ResolvedSourceCandidate> = emptyList(),
)

data class ResolvedSourceCandidate(
    val candidate: SourcePlaybackCandidate,
    val match: TrackMatchResult,
    val quality: AudioQualityRank,
    val providerPreferenceIndex: Int = Int.MAX_VALUE,
    val availabilityRank: Int = 0,
    val localRank: Int = 0,
    val presentationRank: Int = 0,
    val dataSaverRank: Long = Long.MIN_VALUE,
    val rejectionReason: SourceRejectionReason? = null,
    val selectionReasons: Set<SourceSelectionReason> = emptySet(),
)

data class AudioQualityRank(
    val losslessRank: Int,
    val bitDepth: Int,
    val sampleRateHz: Int,
    val bitrateBitsPerSecond: Int,
) : Comparable<AudioQualityRank> {
    override fun compareTo(other: AudioQualityRank): Int =
        compareValuesBy(
            this,
            other,
            AudioQualityRank::losslessRank,
            AudioQualityRank::bitDepth,
            AudioQualityRank::sampleRateHz,
            AudioQualityRank::bitrateBitsPerSecond,
        )

    companion object {
        val UNKNOWN = AudioQualityRank(0, 0, 0, 0)

        fun from(info: AudioQualityInfo?): AudioQualityRank {
            if (info == null) return UNKNOWN
            return AudioQualityRank(
                losslessRank =
                    when (info.lossless) {
                        true -> 2
                        false -> 1
                        null -> 0
                    },
                bitDepth = info.bitDepth?.coerceAtLeast(0) ?: 0,
                sampleRateHz = info.sampleRateHz?.coerceAtLeast(0) ?: 0,
                bitrateBitsPerSecond = info.bitrateBitsPerSecond?.coerceAtLeast(0) ?: 0,
            )
        }
    }
}
