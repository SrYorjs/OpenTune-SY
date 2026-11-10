package com.arturo254.opentune.ui.visual.library

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.R
import com.arturo254.opentune.db.entities.Album
import com.arturo254.opentune.db.entities.Artist
import com.arturo254.opentune.db.entities.Playlist
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.spotify.SpotifyMapper
import com.arturo254.opentune.spotify.models.SpotifyPlaylist
import androidx.compose.ui.res.stringResource
import com.arturo254.opentune.ui.visual.search.SearchHorizontalPadding

internal fun LazyListScope.originalLocalTab(
    songs: List<Song>,
    albums: List<Album>,
    artists: List<Artist>,
    playlists: List<Playlist>,
    importedFileCount: Int,
    onImport: () -> Unit,
    onOpen: (String) -> Unit,
    onPlay: (Song) -> Unit,
    onSongMenu: (Song) -> Unit,
    nowPlayingId: String?,
    isPlaying: Boolean,
) {
    item("local_import") {
        LibraryLocalImportCard(
            count = importedFileCount,
            title = stringResource(R.string.visual_library_import),
            subtitle = stringResource(R.string.visual_library_import_subtitle),
            onClick = onImport,
            modifier = Modifier.padding(horizontal = SearchHorizontalPadding),
        )
        Spacer(Modifier.height(16.dp))
    }
    if (songs.isEmpty()) {
        item("local_empty") {
            LibraryEmptyState(text = stringResource(R.string.visual_library_no_local))
        }
    } else {
        item("local_categories") {
            LibraryCardGrid(
                entries = listOf(
                    LibraryCardEntry(
                        stringResource(R.string.songs),
                        songs.size,
                        songs.take(4).map { it.thumbnailUrl },
                        { onOpen("library/songs?shelf=local") },
                    ),
                    LibraryCardEntry(
                        stringResource(R.string.playlists),
                        playlists.size,
                        playlists.take(4).flatMap { it.thumbnails.take(1) },
                        { onOpen("library/playlists?shelf=local") },
                    ),
                    LibraryCardEntry(
                        stringResource(R.string.albums),
                        albums.size,
                        albums.take(4).map { it.thumbnailUrl },
                        { onOpen("library/albums?shelf=local") },
                    ),
                    LibraryCardEntry(
                        stringResource(R.string.artists),
                        artists.size,
                        artists.take(4).map { it.thumbnailUrl },
                        { onOpen("library/artists?shelf=local") },
                        roundCovers = true,
                    ),
                )
            )
            Spacer(Modifier.height(18.dp))
        }
        item("local_songs_heading") {
            LibrarySectionTitle(stringResource(R.string.visual_library_local_files))
            Spacer(Modifier.height(8.dp))
            LibraryRule()
        }
        items(songs.take(24), key = { "local_" + it.id }) { song ->
            val active = nowPlayingId == song.id
            LibraryListRow(
                title = song.title,
                subtitle = song.artists.joinToString { it.name },
                thumbnailUrl = song.thumbnailUrl,
                isActive = active,
                isPlaying = isPlaying && active,
                songId = song.id,
                onClick = { onPlay(song) },
                onLongClick = { onSongMenu(song) },
            )
        }
    }
    item("local_manage") {
        Spacer(Modifier.height(12.dp))
        LibraryManagePill(
            text = stringResource(R.string.visual_library_manage_local),
            onClick = { onOpen("settings/storage") },
        )
    }
}

internal fun LazyListScope.originalCachedTab(
    songs: List<Song>,
    onOpen: (String) -> Unit,
    onPlay: (Song) -> Unit,
    onSongMenu: (Song) -> Unit,
    nowPlayingId: String?,
    isPlaying: Boolean,
) {
    if (songs.isEmpty()) {
        item("cached_empty") {
            LibraryEmptyState(text = stringResource(R.string.visual_library_no_cached))
        }
    } else {
        item("cached_category") {
            LibraryCardGrid(
                entries = listOf(
                    LibraryCardEntry(
                        stringResource(R.string.visual_library_cached),
                        songs.size,
                        songs.take(4).map { it.thumbnailUrl },
                        { onOpen("library/songs?shelf=cached") },
                    )
                )
            )
        }
    }
    item("cached_manage") {
        Spacer(Modifier.height(12.dp))
        LibraryManagePill(
            text = stringResource(R.string.visual_library_manage_cache),
            onClick = { onOpen("cache_playlist/cached") },
        )
    }
    if (songs.isNotEmpty()) {
        item("cached_header") {
            Spacer(Modifier.height(24.dp))
            LibraryCapsLabel(stringResource(R.string.visual_library_cached))
            Spacer(Modifier.height(8.dp))
        }
        items(songs, key = { "cached_" + it.id }) { song ->
            val active = song.id == nowPlayingId
            LibraryListRow(
                title = song.title,
                subtitle = song.artists.joinToString { it.name },
                thumbnailUrl = song.thumbnailUrl,
                isActive = active,
                isPlaying = active && isPlaying,
                songId = song.id,
                onClick = { onPlay(song) },
                onLongClick = { onSongMenu(song) },
            )
        }
    }
}

internal fun LazyListScope.originalImportedTab(
    localSongs: List<Song>,
    localAlbums: List<Album>,
    syncPlaylists: List<Playlist>,
    spotifyPlaylists: List<SpotifyPlaylist>,
    onOpen: (String) -> Unit,
    onImport: () -> Unit,
    onSync: () -> Unit,
) {
    item("imported_banner") {
        LibraryLocalImportCard(
            count = localSongs.size,
            title = stringResource(R.string.visual_library_import),
            subtitle = stringResource(R.string.visual_library_imported_music),
            onClick = onImport,
            modifier = Modifier.padding(horizontal = SearchHorizontalPadding),
        )
        Spacer(Modifier.height(22.dp))
    }
    if (localSongs.isEmpty() && syncPlaylists.isEmpty() &&
        spotifyPlaylists.isEmpty() && localAlbums.isEmpty()
    ) {
        item("imported_empty") {
            LibraryEmptyState(text = stringResource(R.string.visual_library_no_imported))
        }
    }
    item("imported_music") {
        LibraryCapsLabel(stringResource(R.string.visual_library_imported_music))
        Spacer(Modifier.height(10.dp))
        LibraryCardGrid(
            entries = listOf(
                LibraryCardEntry(
                    stringResource(R.string.songs),
                    localSongs.size,
                    localSongs.take(4).map { it.thumbnailUrl },
                    { onOpen("library/songs?shelf=imported") },
                ),
                LibraryCardEntry(
                    stringResource(R.string.albums),
                    localAlbums.size,
                    localAlbums.take(4).map { it.thumbnailUrl },
                    { onOpen("library/albums?shelf=imported") },
                ),
            )
        )
        Spacer(Modifier.height(22.dp))
    }
    item("imported_playlists") {
        LibraryCapsLabel(stringResource(R.string.visual_library_imported_playlist_title))
        Spacer(Modifier.height(10.dp))
        LibraryCardGrid(
            entries = buildList {
                if (syncPlaylists.isNotEmpty()) {
                    add(
                        LibraryCardEntry(
                            stringResource(R.string.visual_library_youtube_playlists),
                            syncPlaylists.size,
                            syncPlaylists.take(4).flatMap { it.thumbnails.take(1) },
                            { onOpen("library/playlists?shelf=imported") },
                        )
                    )
                }
                if (spotifyPlaylists.isNotEmpty()) {
                    add(
                        LibraryCardEntry(
                            stringResource(R.string.visual_library_spotify_playlists),
                            spotifyPlaylists.size,
                            spotifyPlaylists.take(4).map { SpotifyMapper.getPlaylistThumbnail(it) },
                            { onOpen("library/spotify?shelf=imported") },
                        )
                    )
                }
            }
        )
    }
    item("imported_manage") {
        Spacer(Modifier.height(20.dp))
        LibraryManagePill(
            text = stringResource(R.string.visual_library_manage_imported),
            onClick = onSync,
        )
    }
}
