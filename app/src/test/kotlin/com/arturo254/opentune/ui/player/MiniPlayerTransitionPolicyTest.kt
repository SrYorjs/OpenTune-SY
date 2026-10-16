package com.arturo254.opentune.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class MiniPlayerTransitionPolicyTest {
    @Test fun externalPlayerAndNotificationSkipsFollowActualMedia3Order() {
        assertEquals(1, miniPlayerTrackTransitionDirection(2, 3, 10))
        assertEquals(-1, miniPlayerTrackTransitionDirection(3, 2, 10))
        assertEquals(1, miniPlayerTrackTransitionDirection(5, 5, 10))
    }

    @Test fun repeatAllWrapDoesNotAnimateBackwardWhenMovingForward() {
        assertEquals(1, miniPlayerTrackTransitionDirection(9, 0, 10))
        assertEquals(-1, miniPlayerTrackTransitionDirection(0, 9, 10))
    }

    @Test fun explicitGestureDirectionWinsForShuffleAndTogether() {
        assertEquals(-1, miniPlayerTrackTransitionDirection(0, 2, 5, -1))
        assertEquals(1, miniPlayerTrackTransitionDirection(4, 1, 5, 1))
        assertEquals(1, miniPlayerTrackTransitionDirection(4, 1, 5, 0))
    }

    @Test fun missingOrEmptyQueueDefaultsToForwardWithoutCrashing() {
        assertEquals(1, miniPlayerTrackTransitionDirection(-1, 0, 0))
        assertEquals(1, miniPlayerTrackTransitionDirection(2, -1, 0))
        assertEquals(1, miniPlayerTrackTransitionDirection(0, 0, 1))
    }
}
