package com.arturo254.opentune.sources.local

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.db.entities.AlbumArtistMap
import com.arturo254.opentune.db.entities.AlbumEntity
import com.arturo254.opentune.db.entities.SongAlbumMap
import com.arturo254.opentune.models.MediaMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

data class LocalAudioImportResult(
    val imported: Int,
    val existing: Int,
    val failed: Int,
)

@Singleton
class LocalAudioImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
) {
    suspend fun import(uris: List<Uri>): LocalAudioImportResult = withContext(Dispatchers.IO) {
        var imported = 0
        var existing = 0
        var failed = 0
        uris.distinct().forEach { uri ->
            runCatching {
                val id = uri.toString()
                if (database.song(id).firstOrNull() != null) {
                    existing++
                    return@runCatching
                }
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
                val titleFromFile = context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }?.substringBeforeLast('.')
                val reader = MediaMetadataRetriever()
                val details = try {
                    reader.setDataSource(context, uri)
                    val title = reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                        ?.takeIf { it.isNotBlank() }
                        ?: titleFromFile.orEmpty().ifBlank { "Archivo de audio" }
                    val artist = reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        ?.takeIf { it.isNotBlank() }
                        ?: "Artista desconocido"
                    val albumTitle = reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                        ?.takeIf { it.isNotBlank() }
                    val duration = reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull()?.div(1000L)?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt() ?: -1
                    val cover = reader.embeddedPicture?.takeIf { it.isNotEmpty() }
                        ?.let { bytes ->
                            val directory = File(context.filesDir, "localArtwork").apply { mkdirs() }
                            val file = File(directory, digest(id) + ".jpg")
                            file.writeBytes(bytes)
                            Uri.fromFile(file).toString()
                        }
                    LocalAudioDetails(title, artist, albumTitle, duration, cover)
                } finally {
                    reader.release()
                }
                val albumId = details.album?.let { "local-album:" + digest(it + "|" + details.artist) }
                val existingAlbum = albumId?.let { database.album(it).firstOrNull()?.album }
                val now = LocalDateTime.now()
                val media = MediaMetadata(
                    id = id,
                    title = details.title,
                    artists = listOf(MediaMetadata.Artist(id = null, name = details.artist)),
                    duration = details.duration,
                    thumbnailUrl = details.artwork,
                    album = albumId?.let { MediaMetadata.Album(it, details.album.orEmpty()) },
                    inLibrary = now,
                )
                database.withTransaction {
                    insert(media) { it.copy(isLocal = true) }
                    artistByName(details.artist)?.let { artist ->
                        update(
                            artist.copy(
                                isLocal = artist.isLocal || artist.id.startsWith("LA"),
                                bookmarkedAt = artist.bookmarkedAt ?: now,
                                thumbnailUrl = artist.thumbnailUrl ?: details.artwork,
                            )
                        )
                    }
                    if (albumId != null) {
                        if (existingAlbum == null) {
                            insert(
                                AlbumEntity(
                                    id = albumId,
                                    title = details.album.orEmpty(),
                                    thumbnailUrl = details.artwork,
                                    songCount = 1,
                                    duration = details.duration.coerceAtLeast(0),
                                    bookmarkedAt = now,
                                    inLibrary = now,
                                    isLocal = true,
                                )
                            )
                        } else {
                            update(
                                existingAlbum.copy(
                                    songCount = existingAlbum.songCount + 1,
                                    duration = existingAlbum.duration + details.duration.coerceAtLeast(0),
                                )
                            )
                        }
                        insert(SongAlbumMap(songId = id, albumId = albumId, index = existingAlbum?.songCount ?: 0))
                        artistByName(details.artist)?.let { artist ->
                            insert(
                                AlbumArtistMap(
                                    albumId = albumId,
                                    artistId = artist.id,
                                    order = 0,
                                )
                            )
                        }
                    }
                }
                imported++
            }.onFailure {
                failed++
            }
        }
        LocalAudioImportResult(imported = imported, existing = existing, failed = failed)
    }

    private fun digest(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .take(14)
            .joinToString("") { "%02x".format(it) }
}

private data class LocalAudioDetails(
    val title: String,
    val artist: String,
    val album: String?,
    val duration: Int,
    val artwork: String?,
)
