package com.arturo254.opentune.ui.screens.search

import com.arturo254.opentune.sources.search.UnifiedTrackSearchResult
import java.util.Locale

internal enum class SearchResultCategory(val title: String) {
    ALL("Todo"),
    SONGS("Canciones"),
    PLAYLISTS("Listas"),
    ALBUMS("Álbumes"),
}

internal data class SearchAlbumGroup(
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val tracks: List<UnifiedTrackSearchResult>,
) {
    val key: String get() = searchCategoryKey(title) + "|" + searchCategoryKey(artist)
}

internal fun searchCategoryKey(text: String): String =
    text.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")

internal fun groupSearchAlbums(tracks: List<UnifiedTrackSearchResult>): List<SearchAlbumGroup> {
    val grouped = linkedMapOf<String, MutableList<UnifiedTrackSearchResult>>()
    val names = linkedMapOf<String, Pair<String, String>>()
    for (track in tracks) {
        val album = track.identity.album?.title?.trim().orEmpty()
        if (album.isEmpty()) continue
        val artist = track.identity.artists.firstOrNull()?.name.orEmpty().trim()
        val key = searchCategoryKey(album) + "|" + searchCategoryKey(artist)
        names.putIfAbsent(key, album to artist)
        grouped.getOrPut(key) { mutableListOf() }.add(track)
    }
    return grouped.map { (key, songs) ->
        SearchAlbumGroup(
            title = names.getValue(key).first,
            artist = names.getValue(key).second,
            artworkUrl = songs.firstNotNullOfOrNull { song ->
                song.recommendedVariant?.artworkUrl
                    ?: song.variants.firstNotNullOfOrNull { it.artworkUrl }
            },
            tracks = songs.distinct(),
        )
    }
}
