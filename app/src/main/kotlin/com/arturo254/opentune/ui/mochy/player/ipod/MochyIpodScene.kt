package com.arturo254.opentune.ui.mochy.player.ipod

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.ui.player.state.PlayerState

private data class IpodQueueTrack(
    val queueIndex: Int,
    val title: String,
    val artist: String,
    val album: String,
    val groupId: String,
    val cover: String?,
)

private fun songRows(tracks: List<IpodQueueTrack>): IpodList =
    IpodList(
        rows = if (tracks.isEmpty()) {
            listOf(IpodRow("empty", "No hay canciones", null))
        } else tracks.map {
            IpodRow(
                key = it.queueIndex.toString(),
                title = it.title,
                subtitle = it.artist,
                art = it.cover,
                pick = IpodPick.Play(it.queueIndex),
            )
        },
        art = tracks.isNotEmpty(),
    )

private fun ipodQueuePage(
    page: IpodPage,
    tracks: List<IpodQueueTrack>,
    liked: Boolean,
    activeIndex: Int,
): IpodList = when (page) {
    IpodPage.Main -> IpodMainList
    IpodPage.Music -> IpodMusicList
    IpodPage.Songs -> songRows(tracks)
    IpodPage.Albums -> IpodList(
        tracks.distinctBy { it.groupId }.map {
            IpodRow(it.groupId, it.album, IpodPick.Open(IpodPage.Album(it.groupId, it.album)), it.artist, it.cover)
        }.ifEmpty { listOf(IpodRow("empty", "No hay álbumes en cola", null)) },
        art = tracks.isNotEmpty(),
    )
    IpodPage.Artists -> IpodList(
        tracks.distinctBy { it.artist }.map {
            IpodRow(it.artist, it.artist, IpodPick.Open(IpodPage.Artist(it.artist, it.artist)), null, it.cover, true)
        }.ifEmpty { listOf(IpodRow("empty", "No hay artistas en cola", null)) },
        art = tracks.isNotEmpty(),
    )
    IpodPage.LikedSongs -> songRows(tracks.filter { liked && it.queueIndex == activeIndex })
    is IpodPage.Album -> songRows(tracks.filter { it.groupId == page.id })
    is IpodPage.Artist -> songRows(tracks.filter { it.artist == page.id })
    IpodPage.Playlists, is IpodPage.Playlist -> IpodList(
        listOf(IpodRow("empty", "Biblioteca de playlists pendiente", null))
    )
    IpodPage.CoverFlow, IpodPage.NowPlaying -> IpodList.Loading
}

@Composable
fun MochyIpodScene(
    state: PlayerState,
    queuePeekHeight: Dp,
    onClose: () -> Unit,
    onStyles: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val look = rememberIpodLook()
    val haptics = rememberIpodHaptics()
    val density = LocalDensity.current
    val inset = WindowInsets.systemBars.asPaddingValues()
    val connection = LocalPlayerConnection.current
    val windows = connection?.queueWindows?.collectAsState()?.value.orEmpty()
    val activeIndex = connection?.currentMediaItemIndex?.collectAsState()?.value ?: -1
    val tracks = remember(windows, activeIndex, state.trackId, state.artworkUrl) {
        windows.mapIndexed { index, window ->
            val item = window.mediaItem
            val metadata = item.mediaMetadata
            val title = metadata.title?.toString().orEmpty().ifBlank { item.mediaId }
            val artist = metadata.artist?.toString().orEmpty()
            val album = metadata.albumTitle?.toString().orEmpty().ifBlank { title }
            val group = album + "|" + artist
            IpodQueueTrack(
                index,
                title,
                artist,
                album,
                group,
                metadata.artworkUri?.toString() ?: if (index == activeIndex) state.artworkUrl else null,
            )
        }.ifEmpty {
            state.mediaMetadata?.let {
                listOf(
                    IpodQueueTrack(
                        -1,
                        it.title,
                        it.artists.joinToString { artist -> artist.name },
                        it.album?.title ?: it.title,
                        it.album?.id ?: it.id,
                        state.artworkUrl,
                    )
                )
            }.orEmpty()
        }
    }
    val albums = remember(tracks) {
        tracks.distinctBy { it.groupId }.map {
            IpodAlbum(it.groupId, it.album, it.artist, it.cover)
        }
    }
    val scope = rememberCoroutineScope()
    val nav = remember(scope) { IpodNavigator(scope) }
    val coverFlow = remember(scope) { IpodCoverFlowState(scope) }
    LaunchedEffect(albums) { coverFlow.show(albums) }
    LaunchedEffect(tracks, state.isLiked, activeIndex, nav.top.page) {
        val current = nav.top
        if (current.page != IpodPage.NowPlaying && current.page != IpodPage.CoverFlow) {
            current.list = ipodQueuePage(current.page, tracks, state.isLiked, activeIndex)
            current.clamp()
        }
    }
    fun pick(selection: IpodPick?) {
        when (selection) {
            is IpodPick.Open -> nav.push(
                selection.page,
                ipodQueuePage(selection.page, tracks, state.isLiked, activeIndex),
            )
            is IpodPick.Play -> {
                if (selection.index >= 0) {
                    connection?.player?.seekTo(selection.index, 0L)
                    connection?.player?.play()
                }
                nav.showNowPlaying()
            }
            IpodPick.Shuffle -> {
                connection?.player?.shuffleModeEnabled = true
                connection?.player?.play()
                nav.showNowPlaying()
            }
            null -> Unit
        }
    }
    fun openCover() {
        val album = coverFlow.current ?: return
        val members = tracks.filter { it.groupId == album.id }
        coverFlow.turnOver(songRows(members))
    }
    val wheelActions = remember(
        nav, coverFlow, state.positionMs, state.durationMs, state.canSeek,
        tracks, state.isLiked, activeIndex, connection, onPlayPause, onPrevious, onNext, onSeek,
    ) {
        object : IpodWheelActions {
            override val accelerates: Boolean get() = nav.top.page != IpodPage.NowPlaying
            override fun rotate(steps: Int): Boolean = when {
                nav.top.page == IpodPage.CoverFlow -> coverFlow.move(steps)
                nav.top.page != IpodPage.NowPlaying -> nav.top.move(steps)
                !state.canSeek || state.durationMs <= 0L -> false
                else -> {
                    onSeek((state.positionMs + steps * 3000L).coerceIn(0L, state.durationMs))
                    true
                }
            }
            override fun menu() {
                if (nav.top.page == IpodPage.CoverFlow && coverFlow.turnBack()) return
                nav.back()
            }
            override fun menuHeld() {
                coverFlow.turnBack()
                nav.backToMain()
            }
            override fun center() {
                when (nav.top.page) {
                    IpodPage.NowPlaying -> onPlayPause()
                    IpodPage.CoverFlow -> if (coverFlow.flipped) {
                        pick(coverFlow.back.list.rows.getOrNull(coverFlow.back.selected)?.pick)
                    } else openCover()
                    else -> pick(nav.top.list.rows.getOrNull(nav.top.selected)?.pick)
                }
            }
            override fun next() = onNext()
            override fun previous() = onPrevious()
            override fun playPause() = onPlayPause()
            override fun scan(forward: Boolean) {
                if (state.canSeek && state.durationMs > 0L) {
                    val delta = if (forward) 4000L else -4000L
                    onSeek((state.positionMs + delta).coerceIn(0L, state.durationMs))
                }
            }
        }
    }
    CompositionLocalProvider(LocalIpodLook provides look) {
        Column(
            modifier = modifier.fillMaxSize().background(Color.Black)
                .padding(
                    top = inset.calculateTopPadding(),
                    bottom = queuePeekHeight.coerceAtLeast(0.dp),
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onClose) { Text("Volver", color = Color.White) }
                Text("iPod · Mochy", color = Color.White.copy(alpha = .7f),
                    style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = onStyles) { Text("Estilos", color = Color.White) }
            }
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val w = with(density) { maxWidth.toPx() }
                val h = with(density) { maxHeight.toPx() }
                val geometry = remember(w, h, look.tall) {
                    deviceGeometry(w, h, 3f, 4f, look.tall)
                }
                val scale = remember(geometry.screen, density.fontScale) {
                    IpodScale(
                        with(density) { geometry.screen.width.toDp() },
                        with(density) { geometry.screen.height.toDp() },
                        density.fontScale,
                    )
                }
                val bodyPaint = remember(geometry, look) { IpodBodyPaint(geometry, look) }
                Canvas(Modifier.fillMaxSize()) {
                    with(bodyPaint) { draw(onBlack = true) }
                }
                Box(
                    Modifier.place(geometry.screen, density)
                        .clip(RoundedCornerShape(with(density) { geometry.screenCorner.toDp() }))
                        .background(look.screen.background),
                ) {
                    IpodPages(nav, Modifier.fillMaxSize()) { entry, _, _, _, _ ->
                        Column(Modifier.fillMaxSize()) {
                            IpodHeader(entry.page.title, scale, state.isPlaying)
                            Box(Modifier.weight(1f).fillMaxWidth()) {
                                when (entry.page) {
                                    IpodPage.NowPlaying -> IpodGalaxNowPlaying(state)
                                    IpodPage.CoverFlow -> IpodCoverFlow(
                                        state = coverFlow,
                                        scale = scale,
                                        onTapSide = { direction ->
                                            if (direction == 0) openCover() else coverFlow.move(direction)
                                        },
                                        onTapTrack = { index ->
                                            pick(coverFlow.back.list.rows.getOrNull(index)?.pick)
                                        },
                                    )
                                    else -> IpodMenuList(
                                        entry = entry,
                                        scale = scale,
                                        marquee = true,
                                        onTapRow = { index ->
                                            entry.selected = index
                                            pick(entry.list.rows.getOrNull(index)?.pick)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                Box(Modifier.place(geometry.wheel, density)) {
                    IpodClickWheel(
                        actions = wheelActions,
                        haptics = haptics,
                        enabled = true,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun IpodGalaxNowPlaying(state: PlayerState) {
    val look = LocalIpodLook.current.screen
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val art = rememberCoverPainter(state.artworkUrl, 380)
        Canvas(Modifier.weight(1f).fillMaxWidth(.80f)) {
            val side = size.minDimension * .85f
            val x = (size.width - side) / 2f
            val y = (size.height - side) / 2f
            withTransform({ translate(x, y) }) {
                drawCoverSquare(art, side, look.artBlank)
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                state.title.orEmpty(),
                color = look.ink,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                state.mediaMetadata?.artists?.joinToString { it.name }.orEmpty(),
                color = look.softInk,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            LinearProgressIndicator(
                progress = { state.progressFraction.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = look.ink,
                trackColor = look.quietInk.copy(alpha = .27f),
            )
        }
        Spacer(Modifier.height(7.dp))
    }
}
