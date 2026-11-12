package com.arturo254.opentune.ui.original.component

import android.content.Context
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import com.arturo254.opentune.R
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.db.entities.SongEntity
import com.arturo254.opentune.innertube.models.SongItem
import com.arturo254.opentune.models.toMediaMetadata
import com.arturo254.opentune.ui.original.importer.ImportSource
import java.time.LocalDateTime

internal fun MusicDatabase.setOriginalStoredSongMembership(song: SongEntity) {
    query {
        getSongByIdBlocking(song.id)?.song?.let { current ->
            update(current.toggleLibrary())
        }
    }
}

internal fun MusicDatabase.setOriginalRemoteSongLocalMembership(song: SongItem, adding: Boolean) {
    transaction {
        if (adding) {
            insert(song.toMediaMetadata())
        }
        inLibrary(song.id, if (adding) LocalDateTime.now() else null)
    }
}

internal fun unavailableOriginalImportRemoval(context: Context, source: ImportSource) =
    SwipeAction(
        icon = R.drawable.info,
        color = Color.Gray,
        label = "Importación sin vínculo: no se puede eliminar",
    ) {
        Toast.makeText(
            context,
            "No se puede eliminar solo la importación de ${source.label}: la biblioteca actual no registra canciones por origen o sesión.",
            Toast.LENGTH_LONG,
        ).show()
    }
