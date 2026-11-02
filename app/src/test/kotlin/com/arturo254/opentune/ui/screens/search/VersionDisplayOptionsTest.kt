package com.arturo254.opentune.ui.screens.search

import com.arturo254.opentune.core.model.ArtistIdentity
import com.arturo254.opentune.core.model.AudioQualityInfo
import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.core.model.TrackIdentity
import com.arturo254.opentune.core.model.TrackVersion
import com.arturo254.opentune.core.model.TrackVersionTrait
import com.arturo254.opentune.sources.search.UnifiedTrackSearchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionDisplayOptionsTest {
    @Test
    fun sameQualityAcrossProvidersIsShownOnlyOnce() {
        val songs = listOf(
            variant(ProviderId.YOUTUBE_MUSIC, "first", quality = mp3(320_000)),
            variant(ProviderId.AUDIUS, "second", quality = mp3(320_000)),
            variant(ProviderId.AUDIUS, "third", quality = mp3(128_000)),
            variant(ProviderId.LOCAL, "flac", quality = AudioQualityInfo(
                codec = "FLAC",
                bitrateBitsPerSecond = 920_000,
                lossless = true,
            )),
        )
        val options = distinctQualityOptions(songs)
        assertEquals(3, options.size)
        assertEquals("flac", options[0].sourceId)
        assertTrue(options.any { it.sourceId == "second" })
        assertFalse(options.any { it.sourceId == "first" })
        assertTrue(options.any { it.sourceId == "third" })
    }

    @Test
    fun sameProviderUnknownQualityDoesNotProduceDuplicateRows() {
        val first = variant(ProviderId.YOUTUBE_MUSIC, "one")
        val second = variant(ProviderId.YOUTUBE_MUSIC, "two")
        val third = variant(ProviderId.AUDIUS, "three")
        val options = distinctQualityOptions(listOf(first, second, third, first))
        assertEquals(2, options.size)
        assertEquals(setOf("one", "three"), options.map { it.sourceId }.toSet())
    }

    @Test
    fun playableSourceHidesRedundantMetadataCatalogFallback() {
        val metadata = variant(ProviderId.TIDAL, "metadata")
        val playable = variant(ProviderId.AUDIUS, "audio", quality = mp3(256_000))
        assertEquals(listOf(playable), distinctQualityOptions(listOf(metadata, playable)))
        assertEquals(1, distinctQualityOptions(listOf(
            metadata,
            variant(ProviderId.SPOTIFY, "spotify"),
            variant(ProviderId.DEEZER, "deezer"),
        )).size)
    }

    @Test
    fun relatedCopiesMergeButLiveAndRemixStaySeparate() {
        val original = recording(variant(ProviderId.AUDIUS, "audius", quality = mp3(256_000)))
        val same = recording(variant(ProviderId.YOUTUBE_MUSIC, "youtube", quality = mp3(256_000)))
        val live = recording(variant(ProviderId.YOUTUBE_MUSIC, "live", title = "Song (Live)",
            version = TrackVersion(setOf(TrackVersionTrait.LIVE))))
        val remix = recording(variant(ProviderId.YOUTUBE_MUSIC, "remix", title = "Song Remix",
            version = TrackVersion(setOf(TrackVersionTrait.REMIX))))
        val groups = versionDisplayRecordings(original, listOf(same, live, remix))
        assertEquals(3, groups.size)
        assertEquals(listOf("audius"), groups.first().variants.map { it.sourceId })
        assertEquals(listOf("live"), groups[1].variants.map { it.sourceId })
        assertEquals(listOf("remix"), groups[2].variants.map { it.sourceId })
    }

    @Test
    fun conflictingRecordingIdentifiersRemainSeparate() {
        val first = recording(variant(ProviderId.AUDIUS, "first", isrc = "USAAA2600001"))
        val other = recording(variant(ProviderId.YOUTUBE_MUSIC, "other", isrc = "USBBB2600002"))
        assertEquals(2, versionDisplayRecordings(first, listOf(other)).size)
    }

    @Test
    fun verifiedQualityLabelsNeverInventBitrate() {
        assertEquals("MP3", variantQualityDescription(variant(ProviderId.AUDIUS, "a",
            quality = AudioQualityInfo(mimeType = "audio/mpeg"))))
        assertEquals("Lossless • FLAC", variantQualityDescription(variant(ProviderId.LOCAL, "b",
            quality = AudioQualityInfo(codec = "FLAC", lossless = true))))
        assertEquals(null, variantQualityDescription(variant(ProviderId.TIDAL, "catalog",
            quality = AudioQualityInfo(codec = "FLAC", lossless = true))))
    }

    private fun mp3(bitrate: Int) = AudioQualityInfo(
        mimeType = "audio/mpeg",
        bitrateBitsPerSecond = bitrate,
        lossless = false,
    )

    private fun variant(
        provider: ProviderId,
        id: String,
        title: String = "Song",
        quality: AudioQualityInfo? = null,
        version: TrackVersion = TrackVersion(),
        isrc: String? = null,
    ) = SourceVariant(
        provider = provider,
        sourceId = id,
        title = title,
        artists = listOf("Artist"),
        durationMs = 180_000,
        version = version,
        isrc = isrc,
        quality = quality,
    )

    private fun recording(variant: SourceVariant) = UnifiedTrackSearchResult(
        identity = TrackIdentity(
            title = variant.title,
            artists = listOf(ArtistIdentity("Artist")),
            durationMs = variant.durationMs,
            version = variant.version,
            isrc = variant.isrc,
        ),
        variants = listOf(variant),
        recommendedVariant = variant,
    )
}
