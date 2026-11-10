package com.arturo254.opentune.ui.visual.ipod

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.arturo254.opentune.constants.AlbumSortType
import com.arturo254.opentune.constants.ArtistSongSortType
import com.arturo254.opentune.constants.ArtistSortType
import com.arturo254.opentune.constants.PlaylistSortType
import com.arturo254.opentune.constants.SongSortType
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.db.entities.Album
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.ui.utils.resize
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@HiltViewModel
class IpodLibraryViewModel @Inject constructor(
    private val database: MusicDatabase,
) : ViewModel() {
    private val cache = ConcurrentHashMap<IpodPage, IpodList>().apply {
        put(IpodPage.Main, IpodMainList)
        put(IpodPage.Music, IpodMusicList)
    }

    @Volatile
    var cachedCoverFlow: List<IpodAlbum>? = null
        private set

    @Volatile
    var cachedCovers: List<String>? = null
        private set

    init {
        viewModelScope.launch(Dispatchers.Default) {
            runCatching { covers() }
            runCatching { coverFlow() }
            val playlists = runCatching { load(IpodPage.Playlists) }.getOrNull()
            runCatching { load(IpodPage.Artists) }
            runCatching { load(IpodPage.Albums) }
            runCatching { load(IpodPage.LikedSongs) }
            runCatching { load(IpodPage.Songs) }
            playlists?.rows?.forEach { row ->
                val open = row.pick as? IpodPick.Open ?: return@forEach
                runCatching { load(open.page) }
            }
        }
    }

    fun cached(page: IpodPage): IpodList? = cache[page]

    suspend fun load(page: IpodPage): IpodList = withContext(Dispatchers.Default) {
        cache[page]?.let { return@withContext it }
        buildList(page).also { cache[page] = it }
    }

    fun refreshAsync(page: IpodPage) {
        if (page == IpodPage.Main || page == IpodPage.Music || page == IpodPage.NowPlaying || page == IpodPage.CoverFlow) return
        viewModelScope.launch(Dispatchers.Default) {
            runCatching { cache[page] = buildList(page) }
        }
    }

    private suspend fun buildList(page: IpodPage): IpodList = when (page) {
        IpodPage.Main -> IpodMainList
        IpodPage.Music -> IpodMusicList
        IpodPage.NowPlaying, IpodPage.CoverFlow -> IpodList.Loading
        IpodPage.Playlists -> withArt(
            database.playlists(PlaylistSortType.NAME, false).first().map {
                IpodRow(
                    key = it.id,
                    title = it.playlist.name,
                    pick = IpodPick.Open(IpodPage.Playlist(it.id, it.playlist.name)),
                    subtitle = songCount(it.songCount),
                    art = it.thumbnails.firstOrNull()?.resize(RowPx, RowPx),
                )
            },
            "No playlists yet",
        )
        IpodPage.Artists -> withArt(
            database.artists(ArtistSortType.NAME, false).first().map {
                IpodRow(
                    key = it.id,
                    title = it.artist.name,
                    pick = IpodPick.Open(IpodPage.Artist(it.id, it.artist.name)),
                    subtitle = songCount(it.songCount),
                    art = it.artist.thumbnailUrl?.resize(RowPx, RowPx),
                    roundArt = true,
                )
            },
            "No artists yet",
        )
        IpodPage.Albums -> withArt(
            albums().map {
                IpodRow(
                    key = it.id,
                    title = it.album.title,
                    pick = IpodPick.Open(IpodPage.Album(it.id, it.album.title)),
                    subtitle = it.artists.joinToString { a -> a.name }.ifBlank { null },
                    art = it.album.thumbnailUrl?.resize(RowPx, RowPx),
                )
            },
            "No albums yet",
        )
        IpodPage.Songs -> songs(database.songs(SongSortType.NAME, false).first(), art = true)
        IpodPage.LikedSongs -> songs(database.likedSongs(SongSortType.NAME, false).first(), art = true)
        is IpodPage.Playlist -> songs(database.playlistSongs(page.id).first().map { it.song }, art = true)
        is IpodPage.Artist -> songs(database.artistSongs(page.id, ArtistSongSortType.NAME, false).first(), art = true)
        is IpodPage.Album -> albumTracks(page.id)
    }

    suspend fun covers(): List<String> = withContext(Dispatchers.Default) {
        cachedCovers?.let { return@withContext it }
        database.albums(AlbumSortType.CREATE_DATE, true).first()
            .plus(database.albumsLiked(AlbumSortType.CREATE_DATE, true).first())
            .mapNotNull { it.album.thumbnailUrl?.resize(CoverPx, CoverPx) }
            .distinct()
            .take(MaxCovers)
            .also { cachedCovers = it }
    }

    suspend fun coverFlow(): List<IpodAlbum> = withContext(Dispatchers.Default) {
        cachedCoverFlow?.let { return@withContext it }
        albums()
            .map { IpodAlbum(it.id, it.album.title, it.artists.joinToString { a -> a.name }, it.album.thumbnailUrl?.resize(CoverPx, CoverPx)) }
            .also { cachedCoverFlow = it }
    }

    suspend fun shuffledLibrary(): List<MediaItem> = withContext(Dispatchers.Default) {
        database.songs(SongSortType.NAME, false).first().shuffled().map { it.toMediaItem() }
    }

    private suspend fun albums(): List<Album> {
        val all = (database.albumsLiked(AlbumSortType.NAME, false).first() + database.albums(AlbumSortType.NAME, false).first())
            .distinctBy { it.id }
        val collator = Collator.getInstance(Locale.getDefault()).apply { strength = Collator.PRIMARY }
        return all.sortedWith(compareBy(collator) { it.album.title })
    }

    private suspend fun albumTracks(id: String): IpodList {
        val stored = database.albumWithSongs(id).first()?.songs.orEmpty()
        return songs(stored, art = false)
    }

    private fun withArt(rows: List<IpodRow>, emptyText: String) =
        if (rows.isEmpty()) IpodList(listOf(IpodRow("empty", emptyText, null))) else IpodList(rows, art = true)

    private fun songs(songs: List<Song>, art: Boolean): IpodList {
        if (songs.isEmpty()) return IpodList(listOf(IpodRow("empty", "No songs yet", null)))
        val rows = songs.mapIndexed { index, song ->
            IpodRow(
                key = "${song.id}#$index",
                title = song.song.title,
                pick = IpodPick.Play(index),
                subtitle = if (art) song.artists.joinToString { it.name }.ifBlank { null } else null,
                art = if (art) song.song.thumbnailUrl?.resize(RowPx, RowPx) else null,
            )
        }
        return IpodList(rows, songs, art = art)
    }

    private fun songCount(count: Int) = if (count == 1) "1 song" else "$count songs"

    private companion object {
        const val CoverPx = 400
        const val RowPx = 120
        const val MaxCovers = 24
        const val FetchTimeoutMs = 10_000L
    }
}
