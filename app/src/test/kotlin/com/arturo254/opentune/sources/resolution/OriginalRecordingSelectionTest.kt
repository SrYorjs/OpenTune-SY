package com.arturo254.opentune.sources.resolution

import com.arturo254.opentune.core.model.ArtistIdentity
import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.core.model.TrackIdentity
import com.arturo254.opentune.core.model.TrackVersion
import com.arturo254.opentune.core.model.TrackVersionTrait
import org.junit.Assert.assertEquals
import org.junit.Test

class OriginalRecordingSelectionTest {
    private val identity = TrackIdentity(
        title = "Brillas",
        artists = listOf(ArtistIdentity("León Larregui")),
        durationMs = 235_000,
        isrc = "MX1234567",
    )

    @Test
    fun authenticVersionOutranksCoverEvenWhenCoverIsListedFirst() {
        val cover = SourceVariant(
            provider = ProviderId.AUDIUS,
            sourceId = "cover",
            title = "Brillas",
            artists = listOf("Un cantante"),
            durationMs = 235_000,
            version = TrackVersion(setOf(TrackVersionTrait.COVER)),
        )
        val official = SourceVariant(
            provider = ProviderId.YOUTUBE_MUSIC,
            sourceId = "official",
            title = "Brillas",
            artists = listOf("Leon Larregui"),
            isrc = "MX1234567",
            durationMs = 235_200,
            version = TrackVersion(setOf(TrackVersionTrait.OFFICIAL_AUDIO)),
        )
        assertEquals("official", originalRecordingOptions(identity, listOf(cover, official)).first().sourceId)
    }

    @Test
    fun matchingSourceIsPreferredToDifferentSongAndDuplicatesAreRemoved() {
        val right = SourceVariant(ProviderId.YOUTUBE_MUSIC, "right", "Brillas", listOf("León Larregui"))
        val wrong = SourceVariant(ProviderId.YOUTUBE_MUSIC, "wrong", "Otra canción", listOf("Otro artista"))
        assertEquals(
            listOf("right", "wrong"),
            originalRecordingOptions(identity, listOf(wrong, right, right)).map { it.sourceId },
        )
    }

    @Test
    fun explicitlySelectedRemixIsNotReplacedByOriginalRecording() {
        val remixIdentity = identity.copy(version = TrackVersion(setOf(TrackVersionTrait.REMIX)))
        val remix = SourceVariant(
            ProviderId.YOUTUBE_MUSIC, "remix", "Brillas", listOf("León Larregui"),
            version = TrackVersion(setOf(TrackVersionTrait.REMIX)),
        )
        val original = SourceVariant(
            ProviderId.YOUTUBE_MUSIC, "original", "Brillas", listOf("León Larregui"),
            version = TrackVersion(setOf(TrackVersionTrait.ORIGINAL)),
        )
        assertEquals("remix", originalRecordingOptions(remixIdentity, listOf(original, remix)).first().sourceId)
    }
}
