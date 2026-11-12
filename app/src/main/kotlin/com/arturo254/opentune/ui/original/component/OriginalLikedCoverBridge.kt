package com.arturo254.opentune.ui.original.component

import com.arturo254.opentune.db.MusicDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun MusicDatabase.likedCovers(): Flow<List<String>> =
    likedSongsByCreateDateAsc().map { songs ->
        songs.asReversed().mapNotNull { it.song.thumbnailUrl }.take(4)
    }
