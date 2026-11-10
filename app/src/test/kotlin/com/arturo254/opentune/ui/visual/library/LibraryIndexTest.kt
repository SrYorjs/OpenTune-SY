package com.arturo254.opentune.ui.visual.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryIndexTest {
    @Test
    fun latinDiacriticsShareOneSection() {
        val entry = indexSectionOf("Álbum")
        assertEquals(IndexScript.LATIN, entry.script)
        assertEquals("A", entry.label)
    }

    @Test
    fun kanaHaveTheOriginalAlphabetGroup() {
        val entry = indexSectionOf("かえる")
        assertEquals(IndexScript.JAPANESE, entry.script)
        assertEquals("か", entry.label)
    }

    @Test
    fun sortingUsesTheSameIndexAsTheVisibleRail() {
        val input = listOf("Zeta", "Álbum", "Acorde", "Beta")
        val groups = indexSections(input) { it }
        assertEquals(listOf("A", "B", "Z"), groups.map { it.first.label })
        assertEquals(input.toSet(), sortForLibraryIndex(input) { it }.toSet())
    }

    @Test
    fun emptyTitleGoesToSymbolSection() {
        assertEquals(IndexScript.SYMBOL, indexSectionOf("  ").script)
        assertTrue(indexSectionOf("#mix").label == "#")
    }
}
