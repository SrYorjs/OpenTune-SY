package com.arturo254.opentune.ui.original.adapters

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arturo254.opentune.playback.PlayerConnection
import com.arturo254.opentune.ui.original.component.GlassState
import com.arturo254.opentune.ui.original.fullconstants.PlayerBackgroundStyle
import com.arturo254.opentune.ui.original.fullconstants.PlayerCoverStyle
import com.arturo254.opentune.ui.original.player.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import me.bush.translator.Language

@Composable
fun OriginalMiniPiecesHost(
    connection: PlayerConnection,
    pageActive: Boolean,
    translationLanguage: StateFlow<Language?>,
    content: @Composable (OriginalMiniLyricsState) -> Unit,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    if (!pageActive || !resumed) return
    val scope = rememberCoroutineScope()
    val visible = remember { MutableStateFlow(pageActive && resumed) }
    val original = remember(connection, scope) { OriginalPlayerConnection(connection, scope, visible) }
    val lyricsFlow = remember(original, translationLanguage) { original.miniLyrics(translationLanguage) }
    val metadata by original.mediaMetadata.collectAsState()
    val lyrics by lyricsFlow.collectAsState(OriginalMiniLyricsState(metadata?.id, false, emptyList()))
    val carousel = rememberSongCarousel(original)
    CompositionLocalProvider(LocalOriginalPlayerConnection provides original, LocalSongCarousel provides carousel) {
        content(lyrics)
    }
}

@Composable
fun OriginalMorphingCover(
    progressProvider: () -> Float,
    horizontalOffsetProvider: () -> Float,
    collapsedBound: Dp,
    pureBlack: Boolean,
    glassState: GlassState,
    modifier: Modifier = Modifier,
    lyricsProgressProvider: () -> Float = { 0f },
    coverStyle: PlayerCoverStyle = PlayerCoverStyle.EDGE_TO_EDGE,
    backgroundStyle: PlayerBackgroundStyle = PlayerBackgroundStyle.COVER_GRADIENT,
) {
    val connection = LocalOriginalPlayerConnection.current ?: return
    val track by connection.uiState.trackInfo.collectAsState()
    val transport by connection.uiState.transportState.collectAsState()
    BoxWithConstraints(modifier) {
        MorphingCover(progressProvider, horizontalOffsetProvider, track, transport.isPlaying,
            transport.playbackState, maxWidth, maxHeight, collapsedBound, pureBlack, glassState,
            lyricsProgressProvider = lyricsProgressProvider, coverStyle = coverStyle, backgroundStyle = backgroundStyle)
    }
}

@Composable
fun OriginalMorphingSongInfo(
    lyricsProgressProvider: () -> Float,
    sourceRectProvider: () -> Rect?,
    targetY: Dp,
    modifier: Modifier = Modifier,
    sheetProgressProvider: () -> Float = { 1f },
) {
    val connection = LocalOriginalPlayerConnection.current ?: return
    val track by connection.uiState.trackInfo.collectAsState()
    BoxWithConstraints(modifier) {
        MorphingSongInfo(track, lyricsProgressProvider, sourceRectProvider, targetY, maxWidth,
            sheetProgressProvider = sheetProgressProvider)
    }
}

@Composable
fun OriginalMiniLyricsTicker(
    lyrics: OriginalMiniLyricsState,
    morph: CardMorphState,
    modifier: Modifier = Modifier,
    growthProvider: () -> Float = { 0f },
) {
    MiniLyricsTicker(lyrics.lines, lyrics.loaded, morph, modifier, growthProvider)
}
