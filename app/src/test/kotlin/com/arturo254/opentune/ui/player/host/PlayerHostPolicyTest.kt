package com.arturo254.opentune.ui.player.host

import com.arturo254.opentune.ui.component.COLLAPSED_ANCHOR
import com.arturo254.opentune.ui.component.DISMISSED_ANCHOR
import com.arturo254.opentune.ui.component.EXPANDED_ANCHOR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerHostPolicyTest {
    @Test fun anchorPriorityRemainsExpandedDismissedCollapsed() {
        assertEquals(EXPANDED_ANCHOR, hostPlayerAnchor(isExpanded = true, isDismissed = false))
        assertEquals(DISMISSED_ANCHOR, hostPlayerAnchor(isExpanded = false, isDismissed = true))
        assertEquals(COLLAPSED_ANCHOR, hostPlayerAnchor(isExpanded = false, isDismissed = false))
        assertEquals(EXPANDED_ANCHOR, hostPlayerAnchor(isExpanded = true, isDismissed = true))
    }

    @Test fun onlySettledAnchorsCanBePersistedDuringFullscreenTransitions() {
        assertEquals(
            EXPANDED_ANCHOR,
            settledHostPlayerAnchor(isExpanded = true, isCollapsed = false, isDismissed = false),
        )
        assertEquals(
            COLLAPSED_ANCHOR,
            settledHostPlayerAnchor(isExpanded = false, isCollapsed = true, isDismissed = false),
        )
        assertEquals(
            DISMISSED_ANCHOR,
            settledHostPlayerAnchor(isExpanded = false, isCollapsed = false, isDismissed = true),
        )
        assertEquals(
            null,
            settledHostPlayerAnchor(isExpanded = false, isCollapsed = false, isDismissed = false),
        )
    }

    @Test fun specialScreensNeverOverwritePersistedMiniPlayerAnchor() {
        assertTrue(shouldPersistHostAnchor(false, false, true))
        assertFalse(shouldPersistHostAnchor(true, false, true))
        assertFalse(shouldPersistHostAnchor(false, true, true))
        assertFalse(shouldPersistHostAnchor(true, true, true))
        assertFalse(shouldPersistHostAnchor(false, false, false))
    }

    @Test fun backPressPrioritizesLyricsThenQueueThenFullPlayer() {
        assertEquals(
            PlayerHostBackDestination.LYRICS,
            playerHostBackDestination(lyricsOpen = true, queueOpen = true, playerOpen = true),
        )
        assertEquals(
            PlayerHostBackDestination.QUEUE,
            playerHostBackDestination(lyricsOpen = false, queueOpen = true, playerOpen = true),
        )
        assertEquals(
            PlayerHostBackDestination.PLAYER,
            playerHostBackDestination(lyricsOpen = false, queueOpen = false, playerOpen = true),
        )
        assertEquals(
            PlayerHostBackDestination.NONE,
            playerHostBackDestination(lyricsOpen = false, queueOpen = false, playerOpen = false),
        )
    }
}
