package com.arturo254.opentune.ui.original.utils

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize

// a phone on its side is always wider than this, a phone held upright never is, and every tablet is
private const val WideMinWidthDp = 600

// the regular layout stays for a phone held upright; on its side or on a tablet the app gets the wide one
@Composable
@ReadOnlyComposable
fun isWideLayout(): Boolean = availableWindowSize().width.value >= WideMinWidthDp

// the window is wider than it is tall, so the player can put its cover beside the controls
@Composable
@ReadOnlyComposable
fun isLandscapeWindow(): Boolean {
    val size = availableWindowSize()
    return size.width > size.height
}

// a tv, watched from across the room: a square cover stretched over the whole screen there is one huge blurry crop
@Composable
@ReadOnlyComposable
fun isTvLayout(): Boolean =
    (LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION

private val LocalMeasuredWindowSize = staticCompositionLocalOf<DpSize?> { null }

@Composable
@ReadOnlyComposable
private fun availableWindowSize(): DpSize {
    LocalMeasuredWindowSize.current?.let { return it }
    val pixels = LocalWindowInfo.current.containerSize
    return with(LocalDensity.current) { DpSize(pixels.width.toDp(), pixels.height.toDp()) }
}

/** The root supplies Compose's available constraints, including embedded and split windows. */
@Composable
fun MeasuredWindowLayout(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalMeasuredWindowSize provides DpSize(maxWidth, maxHeight)) {
            content()
        }
    }
}
