package com.arturo254.opentune.playback

import com.arturo254.opentune.db.entities.ArtistEntity
import com.arturo254.opentune.db.entities.PlaylistSong
import com.arturo254.opentune.db.entities.PlaylistSongMap
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.db.entities.SongEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistMixEngineManualPlanTest {
    private fun playlistSong(
        id: String,
        position: Int,
    ): PlaylistSong =
        PlaylistSong(
            map = PlaylistSongMap(
                playlistId = "playlist",
                songId = id,
                position = position,
            ),
            song = Song(
                song = SongEntity(
                    id = id,
                    title = "Track $id",
                    duration = 180,
                ),
                artists = listOf(
                    ArtistEntity(
                        id = "artist-$id",
                        name = "Artist $id",
                    )
                ),
            ),
        )

    @Test
    fun manualPlan_preservesPlaylistOrderAndRequestedCurve() = runBlocking {
        val songs = listOf(
            playlistSong("a", 0),
            playlistSong("b", 1),
            playlistSong("c", 2),
        )

        val plan = PlaylistMixEngine.buildManualPlan(
            songs = songs,
            durationMs = 7_500,
            curve = MixTransitionCurve.MELT,
        )

        assertEquals(listOf("a", "b", "c"), plan.orderedTracks.map { it.playlistSong.song.id })
        assertEquals(2, plan.transitions.size)
        assertEquals(listOf("a" to "b", "b" to "c"), plan.transitions.map { it.outgoingSongId to it.incomingSongId })
        plan.transitions.forEach { transition ->
            assertEquals(7_500, transition.durationMs)
            assertEquals(MixTransitionCurve.MELT, transition.curve)
        }
    }

    @Test
    fun manualPlan_clampsTransitionDurationToSupportedRange() = runBlocking {
        val songs = listOf(
            playlistSong("a", 0),
            playlistSong("b", 1),
        )

        val tooShort = PlaylistMixEngine.buildManualPlan(
            songs = songs,
            durationMs = 200,
            curve = MixTransitionCurve.FADE,
        )
        val tooLong = PlaylistMixEngine.buildManualPlan(
            songs = songs,
            durationMs = 60_000,
            curve = MixTransitionCurve.RISE,
        )

        assertEquals(1_500, tooShort.transitions.single().durationMs)
        assertEquals(20_000, tooLong.transitions.single().durationMs)
    }

    @Test
    fun manualPlan_singleTrackHasNoTransitions() = runBlocking {
        val plan = PlaylistMixEngine.buildManualPlan(
            songs = listOf(playlistSong("only", 0)),
            durationMs = 6_000,
            curve = MixTransitionCurve.BLEND,
        )

        assertEquals(1, plan.orderedTracks.size)
        assertEquals(emptyList<MixTransition>(), plan.transitions)
    }
}
