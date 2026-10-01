/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RoomWarnings
import androidx.room.Transaction
import com.arturo254.opentune.constants.ArtistSongSortType
import com.arturo254.opentune.constants.ArtistSortType
import com.arturo254.opentune.db.entities.Album
import com.arturo254.opentune.db.entities.Artist
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.extensions.reversed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.Collator
import java.util.Locale

/**
 * Artist-focused reads and sorting.
 *
 * Extracted from [DatabaseDao] without changing schema or query behavior.
 */
@Dao
interface ArtistDao {
    @Transaction
    @Query(
        "SELECT song.* FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = :artistId AND inLibrary IS NOT NULL ORDER BY inLibrary",
    )
    fun artistSongsByCreateDateAsc(artistId: String): Flow<List<Song>>

    @Transaction
    @Query(
        "SELECT song.* FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = :artistId AND inLibrary IS NOT NULL ORDER BY title",
    )
    fun artistSongsByNameAsc(artistId: String): Flow<List<Song>>

    @Transaction
    @Query(
        "SELECT song.* FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = :artistId AND inLibrary IS NOT NULL ORDER BY totalPlayTime",
    )
    fun artistSongsByPlayTimeAsc(artistId: String): Flow<List<Song>>

    fun artistSongs(
        artistId: String,
        sortType: ArtistSongSortType,
        descending: Boolean,
    ) = when (sortType) {
        ArtistSongSortType.CREATE_DATE -> artistSongsByCreateDateAsc(artistId)
        ArtistSongSortType.NAME ->
            artistSongsByNameAsc(artistId).map { artistSongs ->
                val collator = Collator.getInstance(Locale.getDefault())
                collator.strength = Collator.PRIMARY
                artistSongs.sortedWith(compareBy(collator) { it.song.title })
            }

        ArtistSongSortType.PLAY_TIME -> artistSongsByPlayTimeAsc(artistId)
    }.map { it.reversed(descending) }

    @Transaction
    @Query(
        "SELECT song.* FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = :artistId AND inLibrary IS NOT NULL LIMIT :previewSize",
    )
    fun artistSongsPreview(
        artistId: String,
        previewSize: Int = 3,
    ): Flow<List<Song>>

    @Transaction
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("""
        SELECT album.*, count(song.dateDownload) downloadCount
        FROM album_artist_map 
            JOIN album ON album_artist_map.albumId = album.id
            JOIN song ON album_artist_map.albumId = song.albumId
        WHERE artistId = :artistId
        GROUP BY album.id
        LIMIT :previewSize
    """)
    fun artistAlbumsPreview(artistId: String, previewSize: Int = 6): Flow<List<Album>>

    @Transaction
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query(
        """
        SELECT DISTINCT artist.*,
               (SELECT COUNT(1)
                FROM song_artist_map
                         JOIN event ON song_artist_map.songId = event.songId
                WHERE artistId = artist.id) AS songCount
        FROM artist
                 LEFT JOIN(SELECT artistId, SUM(songTotalPlayTime) AS totalPlayTime
                      FROM song_artist_map
                               JOIN (SELECT songId, SUM(playTime) AS songTotalPlayTime
                                     FROM event
                                     GROUP BY songId) AS e
                                    ON song_artist_map.songId = e.songId
                      GROUP BY artistId
                      ORDER BY totalPlayTime DESC) AS artistTotalPlayTime
                     ON artist.id = artistId
                     OR artist.bookmarkedAt IS NOT NULL
                     ORDER BY 
                      CASE 
                        WHEN artistTotalPlayTime.artistId IS NULL THEN 1 
                        ELSE 0 
                      END, 
                      artistTotalPlayTime.totalPlayTime DESC
    """,
    )
    fun allArtistsByPlayTime(): Flow<List<Artist>>

    @Transaction
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount FROM artist WHERE songCount > 0 ORDER BY rowId")
    fun artistsByCreateDateAsc(): Flow<List<Artist>>

    @Transaction
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount FROM artist WHERE songCount > 0 ORDER BY name")
    fun artistsByNameAsc(): Flow<List<Artist>>

    @Transaction
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount FROM artist WHERE songCount > 0 ORDER BY songCount")
    fun artistsBySongCountAsc(): Flow<List<Artist>>

    @Transaction
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query(
        """
        SELECT artist.*,
               (SELECT COUNT(1)
                FROM song_artist_map
                         JOIN song ON song_artist_map.songId = song.id
                WHERE artistId = artist.id
                  AND song.inLibrary IS NOT NULL) AS songCount
        FROM artist
                 JOIN(SELECT artistId, SUM(totalPlayTime) AS totalPlayTime
                      FROM song_artist_map
                               JOIN song
                                    ON song_artist_map.songId = song.id
                      GROUP BY artistId
                      ORDER BY totalPlayTime)
                     ON artist.id = artistId
        WHERE songCount > 0
    """
    )
    fun artistsByPlayTimeAsc(): Flow<List<Artist>>

    @Transaction
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount FROM artist WHERE bookmarkedAt IS NOT NULL ORDER BY bookmarkedAt")
    fun artistsBookmarkedByCreateDateAsc(): Flow<List<Artist>>

    @Transaction
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount FROM artist WHERE bookmarkedAt IS NOT NULL ORDER BY name")
    fun artistsBookmarkedByNameAsc(): Flow<List<Artist>>

    @Transaction
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount FROM artist WHERE bookmarkedAt IS NOT NULL ORDER BY songCount")
    fun artistsBookmarkedBySongCountAsc(): Flow<List<Artist>>

    @Transaction
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query(
        """
        SELECT artist.*,
               (SELECT COUNT(1)
                FROM song_artist_map
                         JOIN song ON song_artist_map.songId = song.id
                WHERE artistId = artist.id
                  AND song.inLibrary IS NOT NULL) AS songCount
        FROM artist
                 JOIN(SELECT artistId, SUM(totalPlayTime) AS totalPlayTime
                      FROM song_artist_map
                               JOIN song
                                    ON song_artist_map.songId = song.id
                      GROUP BY artistId
                      ORDER BY totalPlayTime)
                     ON artist.id = artistId
        WHERE bookmarkedAt IS NOT NULL
    """
    )
    fun artistsBookmarkedByPlayTimeAsc(): Flow<List<Artist>>

    fun artists(sortType: ArtistSortType, descending: Boolean) =
        when (sortType) {
            ArtistSortType.CREATE_DATE -> artistsByCreateDateAsc()
            ArtistSortType.NAME -> artistsByNameAsc()
            ArtistSortType.SONG_COUNT -> artistsBySongCountAsc()
            ArtistSortType.PLAY_TIME -> artistsByPlayTimeAsc()
        }.map { artists ->
            artists
                .filter { it.artist.isYouTubeArtist || it.artist.isLocal } // TODO: add ui to filter by local or remote or something idk
                .reversed(descending)
        }

    fun artistsBookmarked(sortType: ArtistSortType, descending: Boolean) =
        when (sortType) {
            ArtistSortType.CREATE_DATE -> artistsBookmarkedByCreateDateAsc()
            ArtistSortType.NAME -> artistsBookmarkedByNameAsc()
            ArtistSortType.SONG_COUNT -> artistsBookmarkedBySongCountAsc()
            ArtistSortType.PLAY_TIME -> artistsBookmarkedByPlayTimeAsc()
        }.map { artists ->
            artists
                .filter { it.artist.isYouTubeArtist || it.artist.isLocal } // TODO: add ui to filter by local or remote or something idk
                .reversed(descending)
        }

    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount FROM artist WHERE id = :id")
    fun artist(id: String): Flow<Artist?>

    @Transaction
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query(
        "SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount FROM artist WHERE name LIKE '%' || :query || '%' AND songCount > 0 LIMIT :previewSize",
    )
    fun searchArtists(
        query: String,
        previewSize: Int = Int.MAX_VALUE,
    ): Flow<List<Artist>>
}
