package com.arturo254.opentune.sources.matching

import com.arturo254.opentune.core.model.AlbumIdentity
import com.arturo254.opentune.core.model.ArtistIdentity
import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.core.model.TrackIdentity
import com.arturo254.opentune.core.model.TrackVersion
import com.arturo254.opentune.core.model.TrackVersionTrait
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackMatcherTest {
    private val matcher = TrackMatcher()

    @Test
    fun sameIsrcConfirmsRecordingAcrossProviders() {
        val reference =
            identity(
                title = "Blinding Lights",
                artist = "The Weeknd",
                durationMs = 200_000,
                isrc = "US-UG1-19-04206",
            )
        val candidate =
            variant(
                provider = ProviderId.DEEZER,
                title = "Blinding Lights",
                artist = "The Weeknd",
                durationMs = 201_000,
                isrc = "USUG11904206",
            )

        val result = matcher.match(reference, candidate)

        assertEquals(TrackMatchStatus.CONFIRMED, result.status)
        assertEquals(1.0, result.confidence, 0.0)
        assertTrue(TrackMatchEvidence.ISRC in result.evidence)
        assertTrue(result.isAccepted)
    }

    @Test
    fun conflictingIsrcIsNeverAcceptedOnlyFromMatchingMetadata() {
        val reference =
            identity(
                title = "Song",
                artist = "Artist",
                durationMs = 180_000,
                isrc = "USAAA1111111",
            )
        val candidate =
            variant(
                provider = ProviderId.DEEZER,
                title = "Song",
                artist = "Artist",
                durationMs = 180_000,
                isrc = "USBBB2222222",
            )

        val result = matcher.match(reference, candidate)

        assertTrue(TrackMatchIssue.ISRC_CONFLICT in result.issues)
        assertFalse(result.isAccepted)
        assertTrue(result.confidence <= 0.55)
    }

    @Test
    fun titleNormalizationHandlesFeaturingPunctuationAndDiacritics() {
        val reference =
            identity(
                title = "Canción (feat. Invitado)",
                artist = "Beyoncé",
                durationMs = 210_000,
            )
        val candidate =
            variant(
                title = "Cancion",
                artist = "Beyonce",
                durationMs = 211_000,
            )

        val result = matcher.match(reference, candidate)

        assertEquals(TrackMatchStatus.STRONG, result.status)
        assertTrue(TrackMatchEvidence.TITLE in result.evidence)
        assertTrue(TrackMatchEvidence.ARTIST in result.evidence)
    }

    @Test
    fun liveRecordingIsRejectedAsOriginalEvenWhenMetadataOtherwiseMatches() {
        val reference =
            identity(
                title = "The Hills",
                artist = "The Weeknd",
                durationMs = 242_000,
            )
        val candidate =
            variant(
                title = "The Hills - Live",
                artist = "The Weeknd",
                durationMs = 243_000,
            )

        val result = matcher.match(reference, candidate)

        assertEquals(TrackMatchStatus.REJECTED, result.status)
        assertTrue(TrackMatchIssue.RECORDING_VERSION_CONFLICT in result.issues)
        assertFalse(result.isAccepted)
    }

    @Test
    fun remixRecordingIsRejectedAsOriginal() {
        val reference =
            identity(
                title = "Midnight",
                artist = "Artist",
                durationMs = 180_000,
            )
        val candidate =
            variant(
                title = "Midnight (Club Remix)",
                artist = "Artist",
                durationMs = 180_000,
            )

        val result = matcher.match(reference, candidate)

        assertEquals(TrackMatchStatus.REJECTED, result.status)
        assertTrue(TrackMatchIssue.RECORDING_VERSION_CONFLICT in result.issues)
    }

    @Test
    fun musicVideoCanRemainSameRecordingButPresentationDifferenceIsPreserved() {
        val reference =
            identity(
                title = "Song",
                artist = "Artist",
                durationMs = 180_000,
            )
        val candidate =
            variant(
                title = "Song",
                artist = "Artist",
                durationMs = 180_000,
                version = TrackVersion(setOf(TrackVersionTrait.MUSIC_VIDEO)),
            )

        val result = matcher.match(reference, candidate)

        assertEquals(TrackMatchStatus.STRONG, result.status)
        assertTrue(TrackMatchIssue.PRESENTATION_DIFFERENCE in result.issues)
        assertTrue(result.isAccepted)
    }

    @Test
    fun largeDurationDifferencePreventsStrongMatch() {
        val reference =
            identity(
                title = "Song",
                artist = "Artist",
                durationMs = 180_000,
            )
        val candidate =
            variant(
                title = "Song",
                artist = "Artist",
                durationMs = 260_000,
            )

        val result = matcher.match(reference, candidate)

        assertEquals(TrackMatchStatus.POSSIBLE, result.status)
        assertTrue(TrackMatchIssue.DURATION_CONFLICT in result.issues)
        assertFalse(result.isAccepted)
    }

    @Test
    fun sameExplicitRecordingTraitsCanMatchStrongly() {
        val reference =
            identity(
                title = "Song (Remastered 2011)",
                artist = "Artist",
                durationMs = 180_000,
                version = TrackVersion(setOf(TrackVersionTrait.REMASTER)),
            )
        val candidate =
            variant(
                title = "Song - Remastered",
                artist = "Artist",
                durationMs = 181_000,
                version = TrackVersion(setOf(TrackVersionTrait.REMASTER)),
            )

        val result = matcher.match(reference, candidate)

        assertTrue(result.isAccepted)
        assertTrue(TrackMatchEvidence.VERSION in result.evidence)
    }

    @Test
    fun literalVersionWordInSongNameIsNotTreatedAsQualifier() {
        val reference =
            identity(
                title = "Live Forever",
                artist = "Oasis",
                durationMs = 276_000,
            )
        val candidate =
            variant(
                title = "Live Forever",
                artist = "Oasis",
                durationMs = 276_500,
            )

        val result = matcher.match(reference, candidate)

        assertEquals(TrackMatchStatus.STRONG, result.status)
        assertFalse(TrackMatchIssue.RECORDING_VERSION_CONFLICT in result.issues)
    }

    @Test
    fun explicitOriginalAndImplicitOriginalAreCompatible() {
        val reference =
            identity(
                title = "Song",
                artist = "Artist",
                durationMs = 180_000,
                version = TrackVersion(setOf(TrackVersionTrait.ORIGINAL)),
            )
        val candidate =
            variant(
                title = "Song",
                artist = "Artist",
                durationMs = 180_000,
            )

        val result = matcher.match(reference, candidate)

        assertEquals(TrackMatchStatus.STRONG, result.status)
        assertTrue(result.isAccepted)
    }

    @Test
    fun bestAcceptedMatchSkipsWrongVersion() {
        val reference =
            identity(
                title = "Song",
                artist = "Artist",
                durationMs = 200_000,
            )
        val live =
            variant(
                provider = ProviderId.DEEZER,
                sourceId = "live",
                title = "Song - Live",
                artist = "Artist",
                durationMs = 200_000,
            )
        val original =
            variant(
                provider = ProviderId.YOUTUBE_MUSIC,
                sourceId = "original",
                title = "Song",
                artist = "Artist",
                durationMs = 201_000,
            )

        val best = matcher.bestAcceptedMatch(reference, listOf(live, original))

        assertNotNull(best)
        assertEquals("original", best?.variant?.sourceId)
        assertEquals(TrackMatchStatus.STRONG, best?.match?.status)
    }

    @Test
    fun variantToVariantMatchingSupportsProviderAggregation() {
        val youtube =
            variant(
                provider = ProviderId.YOUTUBE_MUSIC,
                sourceId = "yt",
                title = "Song",
                artist = "Artist",
                durationMs = 180_000,
            )
        val deezer =
            variant(
                provider = ProviderId.DEEZER,
                sourceId = "dz",
                title = "Song",
                artist = "Artist",
                durationMs = 181_000,
            )

        val result = matcher.match(youtube, deezer)

        assertEquals(TrackMatchStatus.STRONG, result.status)
        assertTrue(result.isAccepted)
    }

    private fun identity(
        title: String,
        artist: String,
        durationMs: Long,
        isrc: String? = null,
        version: TrackVersion = TrackVersion(),
    ): TrackIdentity =
        TrackIdentity(
            title = title,
            artists = listOf(ArtistIdentity(name = artist)),
            album = AlbumIdentity(title = "Album"),
            durationMs = durationMs,
            isrc = isrc,
            version = version,
        )

    private fun variant(
        provider: ProviderId = ProviderId.YOUTUBE_MUSIC,
        sourceId: String = "source-id",
        title: String,
        artist: String,
        durationMs: Long,
        isrc: String? = null,
        version: TrackVersion = TrackVersion(),
    ): SourceVariant =
        SourceVariant(
            provider = provider,
            sourceId = sourceId,
            title = title,
            artists = listOf(artist),
            albumName = "Album",
            durationMs = durationMs,
            isrc = isrc,
            version = version,
        )
}
