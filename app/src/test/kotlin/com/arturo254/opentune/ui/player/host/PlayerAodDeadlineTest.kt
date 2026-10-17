package com.arturo254.opentune.ui.player.host

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerAodDeadlineTest {
    @Test fun schedulesOnlyTheRemainingTime() {
        assertEquals(30_000L, playerAodRemainingDelayMs(30, 0L))
        assertEquals(11_500L, playerAodRemainingDelayMs(30, 18_500L))
        assertEquals(0L, playerAodRemainingDelayMs(30, 30_000L))
        assertEquals(0L, playerAodRemainingDelayMs(30, 99_000L))
    }

    @Test fun handlesDisabledAndClockSkewWithoutNegativeDelays() {
        assertEquals(0L, playerAodRemainingDelayMs(0, 8_000L))
        assertEquals(0L, playerAodRemainingDelayMs(-10, 8_000L))
        assertEquals(30_000L, playerAodRemainingDelayMs(30, -6_000L))
    }
}
