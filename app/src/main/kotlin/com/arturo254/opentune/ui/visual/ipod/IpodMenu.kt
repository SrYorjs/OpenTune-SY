package com.arturo254.opentune.ui.visual.ipod

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.arturo254.opentune.db.entities.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Immutable
sealed class IpodPage(val title: String) {
    data object Main : IpodPage("Music")
    data object Music : IpodPage("Music")
    data object NowPlaying : IpodPage("Now Playing")
    data object CoverFlow : IpodPage("Cover Flow")
    data object Playlists : IpodPage("Playlists")
    data object Artists : IpodPage("Artists")
    data object Albums : IpodPage("Albums")
    data object Songs : IpodPage("Songs")
    data object LikedSongs : IpodPage("Liked Songs")
    data class Playlist(val id: String, val name: String) : IpodPage(name)
    data class Artist(val id: String, val name: String) : IpodPage(name)
    data class Album(val id: String, val name: String) : IpodPage(name)

    val isSidebar: Boolean get() = this == Main || this == Music
}

@Immutable
sealed interface IpodPick {
    class Open(val page: IpodPage) : IpodPick
    class Play(val index: Int) : IpodPick
    data object Shuffle : IpodPick
}

@Immutable
class IpodRow(
    val key: String,
    val title: String,
    val pick: IpodPick?,
    val subtitle: String? = null,
    val art: String? = null,
    val roundArt: Boolean = false,
) {
    val opensPage: Boolean get() = pick is IpodPick.Open
}

@Immutable
class IpodList(val rows: List<IpodRow>, val songs: List<Song> = emptyList(), val art: Boolean = false) {
    companion object {
        val Loading = IpodList(emptyList())
    }
}

val IpodLoadingList = IpodList(listOf(IpodRow("loading", "Loading…", null)))

val IpodMainList = IpodList(
    listOf(
        IpodRow("coverflow", "Cover Flow", IpodPick.Open(IpodPage.CoverFlow)),
        IpodRow("music", "Music", IpodPick.Open(IpodPage.Music)),
        IpodRow("now", "Now Playing", IpodPick.Open(IpodPage.NowPlaying)),
    )
)

val IpodMusicList = IpodList(
    listOf(
        IpodRow("playlists", "Playlists", IpodPick.Open(IpodPage.Playlists)),
        IpodRow("artists", "Artists", IpodPick.Open(IpodPage.Artists)),
        IpodRow("albums", "Albums", IpodPick.Open(IpodPage.Albums)),
        IpodRow("songs", "Songs", IpodPick.Open(IpodPage.Songs)),
        IpodRow("liked", "Liked Songs", IpodPick.Open(IpodPage.LikedSongs)),
        IpodRow("shuffle", "Shuffle Songs", IpodPick.Shuffle),
        IpodRow("coverflow", "Cover Flow", IpodPick.Open(IpodPage.CoverFlow)),
    )
)

@Stable
class IpodEntry(val page: IpodPage, val id: Long) {
    var list by mutableStateOf(
        when (page) {
            IpodPage.Main -> IpodMainList
            IpodPage.Music -> IpodMusicList
            else -> IpodList.Loading
        }
    )
    var selected by mutableIntStateOf(0)
    var first by mutableIntStateOf(0)

    var visibleRows = 7

    fun move(steps: Int): Boolean {
        val count = list.rows.size
        if (count == 0) return false
        val target = (selected + steps).coerceIn(0, count - 1)
        if (target == selected) return false
        selected = target
        clamp()
        return true
    }

    fun clamp() {
        val last = (list.rows.size - 1).coerceAtLeast(0)
        if (selected > last) selected = last
        val maxFirst = (list.rows.size - visibleRows).coerceAtLeast(0)
        first = first.coerceIn((selected - visibleRows + 1).coerceAtLeast(0), minOf(selected, maxFirst))
    }
}

@Stable
class IpodNavigator(private val scope: CoroutineScope) {
    private var nextId = 0L
    val stack = mutableStateListOf(entry(IpodPage.Main), entry(IpodPage.NowPlaying))
    var leaving by mutableStateOf<IpodEntry?>(null)
        private set
    var forward by mutableStateOf(true)
        private set
    val slide = Animatable(1f)

    val top: IpodEntry get() = stack.last()

    fun push(page: IpodPage, list: IpodList? = null): IpodEntry {
        val entry = entry(page)
        if (list != null) entry.list = list
        leaving = top
        stack.add(entry)
        forward = true
        play()
        return entry
    }

    fun back(): Boolean {
        if (stack.size <= 1) return false
        leaving = stack.removeAt(stack.lastIndex)
        forward = false
        play()
        return true
    }

    fun backToMain() {
        if (stack.size <= 1) return
        leaving = top
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
        forward = false
        play()
    }

    fun showNowPlaying() {
        if (top.page != IpodPage.NowPlaying) push(IpodPage.NowPlaying)
    }

    private fun entry(page: IpodPage) = IpodEntry(page, nextId++)

    private fun play() {
        scope.launch {
            slide.snapTo(0f)
            repeat(PrepareFrames) { withFrameNanos { } }
            slide.animateTo(1f, tween(SlideMs, easing = SlideEasing))
            leaving = null
        }
    }
}

private const val SlideMs = 300
private const val PrepareFrames = 2
private val SlideEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1f)
