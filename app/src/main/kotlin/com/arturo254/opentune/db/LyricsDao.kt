/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.arturo254.opentune.db.entities.LyricsEntity
import kotlinx.coroutines.flow.Flow

/**
 * Lyrics persistence boundary.
 *
 * Extracted from [DatabaseDao] without changing schema or query behavior.
 */
@Dao
interface LyricsDao {
    @Transaction
    @Query("SELECT * FROM lyrics WHERE id = :id")
    fun lyrics(id: String?): Flow<LyricsEntity?>

    @Transaction
    @Query("SELECT * FROM lyrics WHERE id = :id LIMIT 1")
    suspend fun getLyricsById(id: String): LyricsEntity?

    @Upsert
    fun upsert(lyrics: LyricsEntity)

    @Delete
    fun delete(lyrics: LyricsEntity)
}
