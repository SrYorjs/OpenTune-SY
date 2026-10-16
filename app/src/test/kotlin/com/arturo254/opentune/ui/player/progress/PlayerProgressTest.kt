package com.arturo254.opentune.ui.player.progress

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerProgressTest {
    @Test fun normalProgressUsesSameFractionForAllPlayerSkins() {
        val value = calculatePlayerProgress(30_000L, 120_000L)
        assertEquals(30_000L, value.displayPositionMs)
        assertEquals(120_000L, value.durationMs)
        assertTrue(value.canSeek)
        assertEquals(0.25f, value.fraction, 0.000001f)
        assertEquals(30_000f, value.sliderValue, 0f)
        assertEquals(0f, value.sliderRange.start, 0f)
        assertEquals(120_000f, value.sliderRange.endInclusive, 0f)
        assertEquals(90_000L, value.remainingMs)
    }

    @Test fun draggingChangesPreviewWithoutChangingPlaybackPosition() {
        val snapshot = calculatePlayerProgress(30_000L, 120_000L, 87_000L)
        assertEquals(30_000L, snapshot.positionMs)
        assertEquals(87_000L, snapshot.displayPositionMs)
        assertEquals(87_000f / 120_000f, snapshot.fraction, 0.000001f)
        assertEquals(33_000L, snapshot.remainingMs)
    }

    @Test fun unknownAndUnseekableDurationsStayFinite() {
        for (invalidDuration in listOf(0L, -1L, C.TIME_UNSET)) {
            val p = calculatePlayerProgress(5_000L, invalidDuration)
            assertFalse(p.canSeek)
            assertEquals(0L, p.durationMs)
            assertEquals(0f, p.fraction, 0f)
            assertEquals(0f, p.sliderValue, 0f)
            assertEquals(1f, p.sliderRange.endInclusive, 0f)
        }
    }

    @Test fun negativeAndOverflowPositionsClampToTrack() {
        val before = calculatePlayerProgress(-500L, 10_000L, -1L)
        assertEquals(0L, before.positionMs)
        assertEquals(0L, before.displayPositionMs)
        val after = calculatePlayerProgress(12_000L, 10_000L, 20_000L)
        assertEquals(10_000L, after.positionMs)
        assertEquals(10_000L, after.displayPositionMs)
        assertEquals(1f, after.fraction, 0f)
        assertEquals(0L, after.remainingMs)
    }

    @Test fun toleranceClampsKnownDurationAndHandlesUnsetWithoutOverflow() {
        assertTrue(shouldSettlePlayerSeek(15_000L, 14_000L, 20_000L))
        assertFalse(shouldSettlePlayerSeek(15_000L, 12_000L, 20_000L))
        assertTrue(shouldSettlePlayerSeek(25_000L, 20_000L, 20_000L))
        assertTrue(shouldSettlePlayerSeek(10_000L, 10_000L, C.TIME_UNSET))
        assertFalse(shouldSettlePlayerSeek(null, 10_000L, 20_000L))
        assertFalse(shouldSettlePlayerSeek(Long.MAX_VALUE, Long.MIN_VALUE, 20_000L))
    }

    @Test fun previewCommitKeepsPendingUntilEngineReachesTarget() {
        val session = PlayerSeekSession()
        session.onSeekPreview(50_000L)
        assertTrue(session.isDragging)
        assertEquals(50_000L, session.pendingSeekMs)
        session.settle(50_000L, 100_000L)
        assertEquals(50_000L, session.pendingSeekMs) // still dragging
        val commands = mutableListOf<Long>()
        session.finish { commands.add(it) }
        assertFalse(session.isDragging)
        assertEquals(listOf(50_000L), commands)
        session.settle(45_000L, 100_000L)
        assertEquals(50_000L, session.pendingSeekMs)
        session.settle(49_000L, 100_000L)
        assertNull(session.pendingSeekMs)
    }

    @Test fun commitWithoutDragNeverIssuesSeek() {
        val session = PlayerSeekSession()
        var commands = 0
        session.finish { commands++ }
        assertEquals(0, commands)
        assertFalse(session.isDragging)
        session.onSeekPreview(-50L)
        session.finish { assertEquals(0L, it) }
    }
}
