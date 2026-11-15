package com.arturo254.opentune.ui.original.player.styles
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
internal suspend fun PointerInputScope.whileStyleInteractive(
    interactive: () -> Boolean,
    detect: suspend PointerInputScope.() -> Unit,
): Unit = kotlinx.coroutines.coroutineScope {
    var listening: kotlinx.coroutines.Job? = null
    // started in place, not through collectLatest: its hop to the next dispatch let the touch that wakes this handler (every button's first tap) go by unheard
    snapshotFlow { interactive() }.collect { on ->
        listening?.cancelAndJoin()
        listening = if (on) launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { this@whileStyleInteractive.detect() } else null
    }
}

fun Modifier.styleSongSwipe(
    interactive: () -> Boolean,
    carousel: com.arturo254.opentune.ui.original.player.SongCarousel?,
    player: () -> androidx.media3.common.Player?,
): Modifier =
    this.then(
            if (carousel == null) Modifier else Modifier.pointerInput(interactive, carousel) {
                val tracker = androidx.compose.ui.input.pointer.util.VelocityTracker()
                val flingPx = SwipeFling.toPx()
                var travelled = 0f
                var hasPrevious = false
                var hasNext = false
                // whether the row is this swipe's to put back if the swipe is cut off
                var mine = false
                whileStyleInteractive(interactive) {
                    try {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                travelled = 0f
                                tracker.resetTracking()
                                val p = player()
                                hasPrevious = p?.hasPreviousMediaItem() == true
                                hasNext = p?.hasNextMediaItem() == true
                                carousel.startDrag()
                                mine = true
                            },
                            onDragEnd = {
                                mine = false
                                carousel.release(travelled, tracker.calculateVelocity().x, size.width.toFloat(), flingPx, player())
                            },
                            onDragCancel = {
                                mine = false
                                carousel.cancelDrag()
                            },
                            onHorizontalDrag = { change, amount ->
                                tracker.addPointerInputChange(change)
                                travelled += amount
                                carousel.dragTo(travelled, size.width.toFloat(), hasPrevious, hasNext)
                            },
                        )
                    } finally {
                        if (mine) {
                            mine = false
                            carousel.cancelDrag()
                        }
                    }
                }
            }
        )

/** A flick faster than this skips however short it was. */
private val SwipeFling = 500.dp
