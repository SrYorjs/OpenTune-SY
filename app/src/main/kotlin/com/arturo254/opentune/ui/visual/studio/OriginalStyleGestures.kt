package com.arturo254.opentune.ui.visual.studio

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.ui.player.actions.PlayerActions
import com.arturo254.opentune.ui.visual.player.SongCarousel

fun Modifier.originalStyleGestures(
    enabled: Boolean,
    carousel: SongCarousel,
    canPrevious: Boolean,
    canNext: Boolean,
    actions: PlayerActions,
    onLongPress: () -> Unit,
): Modifier {
    if (!enabled) return this
    return this
        .pointerInput(actions, onLongPress) {
            detectTapGestures(
                onDoubleTap = { actions.playPause() },
                onLongPress = { onLongPress() },
            )
        }
        .pointerInput(carousel, canPrevious, canNext, actions) {
            val tracker = VelocityTracker()
            var travelled = 0f
            var active = false
            detectHorizontalDragGestures(
                onDragStart = {
                    tracker.resetTracking()
                    travelled = 0f
                    carousel.startDrag()
                    active = true
                },
                onHorizontalDrag = { change, delta ->
                    tracker.addPosition(change.uptimeMillis, change.position)
                    travelled += delta
                    carousel.dragTo(
                        travelledPx = travelled,
                        spacingPx = size.width.toFloat(),
                        hasPrevious = canPrevious,
                        hasNext = canNext,
                    )
                    change.consume()
                },
                onDragEnd = {
                    if (active) {
                        carousel.release(
                            travelledPx = travelled,
                            velocityPx = tracker.calculateVelocity().x,
                            spacingPx = size.width.toFloat(),
                            flingPx = 500.dp.toPx(),
                            canSkipPrevious = canPrevious,
                            canSkipNext = canNext,
                            onPrevious = actions::previous,
                            onNext = actions::next,
                        )
                        active = false
                    }
                },
                onDragCancel = {
                    if (active) {
                        carousel.cancelDrag()
                        active = false
                    }
                },
            )
        }
}
