package com.arturo254.opentune.sources.offline

import com.arturo254.opentune.core.model.ArtistIdentity
import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.core.model.TrackIdentity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineDownloadPlannerTest {
    @Test
    fun youtubeVariantBecomesBackingAudioWhileAllDiscoverySourcesArePreserved() =
        runBlocking {
            val planner = OfflineDownloadPlanner()
            val identity =
                TrackIdentity(
                    title = "Song",
                    artists = listOf(ArtistIdentity("Artist")),
                    durationMs = 180_000L,
                    isrc = "USAAA2600200",
                )
            val variants =
                listOf(
                    SourceVariant(
                        provider = ProviderId.TIDAL,
                        sourceId = "tidal-1",
                        title = "Song",
                        artists = listOf("Artist"),
                        durationMs = 180_000L,
                        isrc = "USAAA2600200",
                        artworkUrl = "https://example.test/cover.jpg",
                        animatedArtworkUrl = "https://example.test/canvas.m3u8",
                    ),
                    SourceVariant(
                        provider = ProviderId.YOUTUBE_MUSIC,
                        sourceId = "yt-1",
                        title = "Song",
                        artists = listOf("Artist"),
                        durationMs = 180_000L,
                        isrc = "USAAA2600200",
                    ),
                )

            val plan = planner.plan(identity, variants).getOrThrow()

            assertEquals("yt-1", plan.asset.mediaId)
            assertEquals(ProviderId.YOUTUBE_MUSIC, plan.asset.backingProvider)
            assertEquals("yt-1", plan.asset.backingSourceId)
            assertEquals("USAAA2600200", plan.asset.isrc)
            assertEquals("https://example.test/cover.jpg", plan.asset.artworkUrl)
            assertEquals("https://example.test/canvas.m3u8", plan.asset.animatedArtworkUrl)
            assertEquals(
                setOf(
                    ProviderId.TIDAL,
                    ProviderId.YOUTUBE_MUSIC,
                ),
                plan.asset.discoverySources.map { it.provider }.toSet(),
            )
            assertEquals("yt-1", plan.metadata.id)
            assertEquals("Song", plan.metadata.title)
        }

    @Test
    fun localOnlyTrackDoesNotPretendToNeedAnOfflineDownload() =
        runBlocking {
            val planner = OfflineDownloadPlanner()
            val identity =
                TrackIdentity(
                    title = "Local Song",
                    artists = listOf(ArtistIdentity("Artist")),
                )
            val variants =
                listOf(
                    SourceVariant(
                        provider = ProviderId.LOCAL,
                        sourceId = "content://media/1",
                        title = "Local Song",
                        artists = listOf("Artist"),
                    ),
                )

            val result = planner.plan(identity, variants)

            assertTrue(result.isFailure)
        }
}
