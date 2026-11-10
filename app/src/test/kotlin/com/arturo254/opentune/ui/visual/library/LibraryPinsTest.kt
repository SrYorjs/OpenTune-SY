package com.arturo254.opentune.ui.visual.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryPinsTest {
    @Test
    fun roundTripPreservesTypeIdAndOrder() {
        val selected = listOf(
            LibraryPinnedRef(LibraryPinKind.SONG, "content://com.android.documents/audio%3A15"),
            LibraryPinnedRef(LibraryPinKind.PLAYLIST, "PL:92"),
            LibraryPinnedRef(LibraryPinKind.ALBUM, "Álbum Uno"),
            LibraryPinnedRef(LibraryPinKind.ARTIST, "artist 1"),
        )
        assertEquals(selected, decodeLibraryPins(encodeLibraryPins(selected)))
    }

    @Test
    fun malformedEntriesCannotCreateFakePins() {
        val raw = "UNKNOWN:123\nSONG:\nSONG:good\nSONG:good"
        val pins = decodeLibraryPins(raw)
        assertEquals(listOf(LibraryPinnedRef(LibraryPinKind.SONG, "good")), pins)
    }

    @Test
    fun pinnedItemsLimitAndEmptyStateAreStable() {
        val pins = (0 until 100).map { LibraryPinnedRef(LibraryPinKind.SONG, "song-$it") }
        val serialized = encodeLibraryPins(pins)
        assertEquals(64, decodeLibraryPins(serialized).size)
        assertTrue(decodeLibraryPins("").isEmpty())
        assertFalse(decodeLibraryPins(serialized).contains(LibraryPinnedRef(LibraryPinKind.SONG, "song-99")))
    }
}
