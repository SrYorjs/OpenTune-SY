package com.arturo254.opentune.ui.player

/**
 * Visual-only direction for track changes that did not originate from the
 * mini-player swipe (hardware/notification/Player controls/Together/radio).
 * Media3 keeps owning the real order, skip and crossfade.
 */
internal fun miniPlayerTrackTransitionDirection(
    previousIndex: Int,
    currentIndex: Int,
    itemCount: Int,
    requestedSwipeDirection: Int = 0,
): Int {
    if (requestedSwipeDirection == -1 || requestedSwipeDirection == 1) {
        return requestedSwipeDirection
    }
    if (previousIndex < 0 || currentIndex < 0 || itemCount <= 1) return 1
    if (previousIndex == itemCount - 1 && currentIndex == 0) return 1
    if (previousIndex == 0 && currentIndex == itemCount - 1) return -1
    return if (currentIndex < previousIndex) -1 else 1
}
