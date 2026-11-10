package com.arturo254.opentune.ui.visual.player

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import com.arturo254.opentune.ui.player.actions.PlayerActions

fun Modifier.originalCoverGestures(
    enabled: Boolean,
    carousel: SongCarousel,
    spacingPx: Float,
    canPrevious: Boolean,
    canNext: Boolean,
    actions: PlayerActions,
    onLongPress: () -> Unit,
): Modifier {
    if (!enabled) return this
    return this
        .pointerInput(actions, onLongPress) {
            detectTapGestures(
                onDoubleTap = { actions.toggleFavorite() },
                onLongPress = { onLongPress() },
            )
        }
        .pointerInput(carousel, spacingPx, canPrevious, canNext, actions) {
            var dragTotal = 0f
            var tracker = VelocityTracker()
            detectHorizontalDragGestures(
                onDragStart = {
                    dragTotal = 0f
                    tracker = VelocityTracker()
                    carousel.startDrag()
                },
                onHorizontalDrag = { change, delta ->
                    dragTotal += delta
                    tracker.addPosition(change.uptimeMillis, change.position)
                    carousel.dragTo(
                        travelledPx = dragTotal,
                        spacingPx = spacingPx,
                        hasPrevious = canPrevious,
                        hasNext = canNext,
                    )
                    change.consume()
                },
                onDragEnd = {
                    carousel.release(
                        travelledPx = dragTotal,
                        velocityPx = tracker.calculateVelocity().x,
                        spacingPx = spacingPx,
                        flingPx = spacingPx * 1.5f,
                        canSkipPrevious = canPrevious,
                        canSkipNext = canNext,
                        onPrevious = actions::previous,
                        onNext = actions::next,
                    )
                },
                onDragCancel = carousel::cancelDrag,
            )
        }
}
