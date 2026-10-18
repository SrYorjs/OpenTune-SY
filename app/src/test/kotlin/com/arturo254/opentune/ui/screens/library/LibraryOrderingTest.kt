package com.arturo254.opentune.ui.screens.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryOrderingTest {
    @Test
    fun sectionHeadersNeverChangePlaylistOrder() {
        val entries = mutableListOf("a", "b", "c")
        assertNull(moveLibraryEntry(entries, "header", "a") { it })
        assertNull(moveLibraryEntry(entries, "a", "filters") { it })
        assertEquals(listOf("a", "b", "c"), entries)
    }

    @Test
    fun movingUsesStableKeysIndependentOfSurroundingSections() {
        val entries = mutableListOf("a", "b", "c")
        assertEquals(2 to 0, moveLibraryEntry(entries, "c", "a") { it })
        assertEquals(listOf("c", "a", "b"), entries)
        assertEquals(0 to 2, moveLibraryEntry(entries, "c", "b") { it })
        assertEquals(listOf("a", "b", "c"), entries)
    }

    @Test
    fun sameDestinationDoesNotCreateAnOrderChange() {
        val entries = mutableListOf("a", "b")
        assertNull(moveLibraryEntry(entries, "a", "a") { it })
        assertEquals(listOf("a", "b"), entries)
    }
}
