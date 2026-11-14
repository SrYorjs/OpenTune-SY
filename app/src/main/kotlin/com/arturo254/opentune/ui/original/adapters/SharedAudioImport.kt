package com.arturo254.opentune.ui.original.adapters

import android.content.Context
import android.net.Uri
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.db.entities.ArtistEntity
import com.arturo254.opentune.db.entities.FormatEntity
import com.arturo254.opentune.db.entities.SongArtistMap
import com.arturo254.opentune.db.entities.SongEntity
import com.arturo254.opentune.ui.original.screens.extractAudioMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.util.UUID

/** Stores a durable URI as the local source identity used by the existing playback coordinator. */
suspend fun importSharedAudio(context: Context, database: MusicDatabase, shared: Uri) = withContext(Dispatchers.IO) {
    val directory = File(context.filesDir, "shared_audio/${UUID.randomUUID()}").apply { check(mkdirs()) }
    val displayName = context.contentResolver.query(shared, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { if (it.moveToFirst()) it.getString(0) else null }
        ?.replace(Regex("[\\\\/:*?\"<>|]"), "_")?.takeIf { it.isNotBlank() } ?: "audio"
    val audio = File(directory, displayName)
    try {
        requireNotNull(context.contentResolver.openInputStream(shared)).use { input -> audio.outputStream().use { input.copyTo(it) } }
        val uri = Uri.fromFile(audio)
        val tags = extractAudioMetadata(context, uri)
        val artwork = tags.embeddedArtwork?.let { bytes ->
            File(directory, "artwork.jpg").also { it.writeBytes(bytes) }.let { Uri.fromFile(it).toString() }
        }
        // LOCAL's SourceVariant.toPlayableMediaItem uses sourceId as its URI.
        val id = uri.toString()
        database.withTransaction {
            insert(SongEntity(id, tags.title, duration = tags.durationSeconds, thumbnailUrl = artwork,
                albumName = tags.album, isLocal = true, inLibrary = LocalDateTime.now()))
            val artistId = artistByName(tags.artist)?.id ?: ArtistEntity.generateArtistId()
            insert(ArtistEntity(id = artistId, name = tags.artist))
            insert(SongArtistMap(songId = id, artistId = artistId, position = 0))
            upsert(FormatEntity(id, -1, tags.mimeType ?: "audio/mpeg", "", tags.bitrate ?: 0,
                tags.sampleRate, audio.length(), null, playbackUrl = id))
        }
    } catch (error: Throwable) {
        directory.deleteRecursively()
        throw error
    }
}
