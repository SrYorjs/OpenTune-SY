package com.arturo254.opentune.ui.visual.ipod

import android.content.Context
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.ui.component.BottomSheetState
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.ui.player.state.PlayerState
import com.arturo254.opentune.ui.visual.player.TrackInfo
import com.arturo254.opentune.ui.visual.studio.LocalStyleInteractive
import com.arturo254.opentune.ui.visual.studio.whileStyleInteractive
import com.arturo254.opentune.ui.visual.theme.InterFontFamily
import androidx.compose.foundation.layout.systemBars
import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.delay

@Composable
fun OriginalIpodPlayer(
    state: PlayerState,
    sheet: BottomSheetState,
    track: MediaMetadata?,
    onStyles: () -> Unit,
    modifier: Modifier = Modifier,
    preview: Boolean = false,
) {
    val density = LocalDensity.current
    val bars = WindowInsets.systemBars
    val topInset = bars.getTop(density)
    val bottomInset = bars.getBottom(density)
    val look = rememberIpodLook()
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .then(if (preview) SwallowTouches else Modifier)
            .pointerInput(onStyles, preview) {
                if (!preview) detectTapGestures(onLongPress = { onStyles() })
            },
    ) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val wholeDevice = minOf(maxWidth, maxHeight) >= 600.dp || maxWidth > maxHeight * 0.62f
        val geometry = remember(w, h, topInset, bottomInset, wholeDevice, look.tall) {
            if (wholeDevice) deviceGeometry(w, h, topInset.toFloat(), bottomInset.toFloat(), look.tall)
            else phoneGeometry(w, h, topInset.toFloat(), bottomInset.toFloat(), look.tall)
        }
        Box(
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val paint = IpodBodyPaint(geometry, look)
                    onDrawBehind { with(paint) { draw(onBlack = wholeDevice) } }
                },
        )
        CompositionLocalProvider(
            LocalIpodLook provides look,
            LocalStyleInteractive provides { !preview && sheet.isExpanded },
            androidx.compose.material3.LocalTextStyle provides androidx.compose.ui.text.TextStyle(fontFamily = InterFontFamily),
        ) {
            IpodFace(geometry, preview, density, sheet, state, track)
        }
    }
}

@Composable
private fun IpodFace(
    geometry: IpodGeometry,
    preview: Boolean,
    density: Density,
    sheet: BottomSheetState,
    state: PlayerState,
    track: MediaMetadata?,
) {
    val connection = LocalPlayerConnection.current
    val look = LocalIpodLook.current
    val library: IpodLibraryViewModel? = if (preview) null else hiltViewModel()
    val scope = rememberCoroutineScope()
    val nav = remember(scope) { IpodNavigator(scope) }
    val nowPlaying = remember { IpodNowPlayingState() }
    val coverFlow = remember(scope) { IpodCoverFlowState(scope) }
    val haptics = rememberIpodHaptics()
    val context = LocalContext.current
    val controller = remember(nav, nowPlaying, coverFlow, scope, connection, library, haptics, context) {
        IpodController(nav, nowPlaying, coverFlow, scope, connection, library, context.getSystemService(Context.AUDIO_SERVICE) as AudioManager, haptics)
    }
    val showing: () -> Boolean = remember(sheet, preview) { { !preview && sheet.isExpanded } }
    val marquee = sheet.isExpanded && !preview

    BarTimeout(nowPlaying)
    Chevron(geometry, sheet, preview, density)

    val bezel = with(density) { geometry.bezel.toDp() }
    val outerCorner = with(density) { geometry.screenCorner.toDp() }
    val innerW = with(density) { (geometry.screen.width - geometry.bezel * 2f).toDp() }
    val innerH = with(density) { (geometry.screen.height - geometry.bezel * 2f).toDp() }
    val scale = remember(innerW, innerH, density.fontScale) { IpodScale(innerW, innerH, density.fontScale) }
    val covers by produceState(library?.cachedCovers.orEmpty(), library) {
        library?.let { value = it.covers() }
    }
    val fallback = state.artworkUrl
    val slideshowCovers = remember(covers, fallback) { covers.ifEmpty { listOfNotNull(fallback) } }
    Box(
        modifier = Modifier
            .place(geometry.screen, density)
            .clip(RoundedCornerShape(outerCorner))
            .background(look.body.bezel),
    ) {
        Box(
            modifier = Modifier
                .padding(bezel)
                .fillMaxSize()
                .clip(RoundedCornerShape((outerCorner - bezel).coerceAtLeast(0.dp)))
                .background(look.screen.background),
        ) {
            Column(Modifier.fillMaxSize()) {
                TitleBar(nav, coverFlow, scale)
                IpodPages(nav, Modifier.fillMaxWidth().weight(1f)) { entry, slideX, sidebarOnly, curtainOpen, showCoverCycle ->
                    IpodPageContent(
                        entry = entry,
                        slideX = slideX,
                        sidebarOnly = sidebarOnly,
                        curtainOpen = curtainOpen,
                        showCoverCycle = showCoverCycle,
                        covers = slideshowCovers,
                        controller = controller,
                        scale = scale,
                        marquee = marquee,
                        showing = showing,
                        state = state,
                        track = track,
                    )
                }
            }
        }
    }

    IpodClickWheel(
        actions = controller,
        haptics = haptics,
        enabled = !preview && connection != null,
        modifier = Modifier.place(geometry.wheel, density),
    )
}

@Composable
private fun BarTimeout(nowPlaying: IpodNowPlayingState) {
    LaunchedEffect(nowPlaying.bar, nowPlaying.touchedAt) {
        if (nowPlaying.bar == IpodBar.PROGRESS) return@LaunchedEffect
        delay(if (nowPlaying.bar == IpodBar.VOLUME) VolumeShownMs else ScrubShownMs)
        nowPlaying.bar = IpodBar.PROGRESS
    }
}

@Composable
private fun TitleBar(nav: IpodNavigator, coverFlow: IpodCoverFlowState, scale: IpodScale) {
    val page = nav.top.page
    val title = if (page == IpodPage.CoverFlow && coverFlow.flipped) coverFlow.current?.title ?: page.title else page.title
    IpodHeader(title, scale)
}

@Composable
private fun IpodPageContent(
    entry: IpodEntry,
    slideX: () -> Float,
    sidebarOnly: Boolean,
    curtainOpen: (() -> Float)?,
    showCoverCycle: Boolean,
    covers: List<String>,
    controller: IpodController,
    scale: IpodScale,
    marquee: Boolean,
    showing: () -> Boolean,
    state: PlayerState,
    track: MediaMetadata?,
) {
    val background = LocalIpodLook.current.screen.background
    when {
        entry.page == IpodPage.NowPlaying -> IpodNowPlaying(
            state = controller.nowPlaying,
            scale = scale,
            marquee = marquee,
            playerState = state,
            track = TrackInfo(
                mediaId = track?.id.orEmpty(),
                title = track?.title.orEmpty(),
                artist = track?.artists?.joinToString { it.name }.orEmpty(),
                album = track?.album?.title.orEmpty(),
                thumbnailUrl = state.artworkUrl,
            ),
        )
        entry.page == IpodPage.CoverFlow -> IpodCoverFlow(controller.coverFlow, scale, controller::tapCoverFlow, controller::tapTrack)
        entry.page.isSidebar -> Box(Modifier.fillMaxSize()) {
            if (showCoverCycle) {
                IpodCoverCycle(
                    covers = covers,
                    running = showing,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxWidth(0.5f)
                        .fillMaxHeight()
                        .graphicsLayer {
                            if (curtainOpen != null) translationX = size.width * curtainOpen()
                        },
                )
            }
            val halfWidthPx = with(LocalDensity.current) { (scale.width * 0.5f).toPx() }
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .fillMaxHeight()
                    .then(if (sidebarOnly) Modifier.clipToBounds() else Modifier),
            ) {
                IpodMenuList(
                    entry = entry,
                    scale = scale,
                    marquee = marquee,
                    onTapRow = { controller.tapRow(entry, it) },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = when {
                                curtainOpen != null -> -size.width * curtainOpen()
                                sidebarOnly -> size.width * slideX()
                                else -> 0f
                            }
                        }
                        .background(background),
                )
            }
            if (showCoverCycle) {
                IpodSidebarShadow(
                    scale = scale,
                    modifier = Modifier
                        .offset(x = scale.width * 0.5f)
                        .graphicsLayer {
                            if (curtainOpen != null) translationX = -halfWidthPx * curtainOpen()
                        },
                )
            }
        }
        else -> {
            IpodMenuList(entry, scale, marquee, { controller.tapRow(entry, it) }, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun Chevron(geometry: IpodGeometry, sheet: BottomSheetState, preview: Boolean, density: Density) {
    val interactive = LocalStyleInteractive.current
    val ink = LocalIpodLook.current.body.chevron
    val chevron = geometry.chevron ?: return
    val target = with(density) { Rect(Offset(chevron.x - 32.dp.toPx(), chevron.y - 24.dp.toPx()), Size(64.dp.toPx(), 48.dp.toPx())) }
    Box(
        modifier = Modifier
            .place(target, density)
            .then(
                if (preview) Modifier else Modifier.pointerInput(sheet, interactive) {
                    whileStyleInteractive(interactive) { detectTapGestures { sheet.collapseSoft() } }
                }
            )
            .drawWithCache {
                val half = geometry.unit * 0.0367f
                val depth = geometry.unit * 0.026f
                val path = Path().apply {
                    moveTo(size.width / 2f - half, size.height / 2f - depth / 2f)
                    lineTo(size.width / 2f, size.height / 2f + depth / 2f)
                    lineTo(size.width / 2f + half, size.height / 2f - depth / 2f)
                }
                val stroke = Stroke(width = geometry.unit * 0.0075f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                onDrawBehind { drawPath(path, ink, style = stroke) }
            },
    )
}

private val SwallowTouches = Modifier.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
    }
}

private const val VolumeShownMs = 1_600L
private const val ScrubShownMs = 4_000L
