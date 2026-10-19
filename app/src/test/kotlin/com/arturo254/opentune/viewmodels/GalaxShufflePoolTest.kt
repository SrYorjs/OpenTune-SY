package com.arturo254.opentune.viewmodels

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GalaxShufflePoolTest {
    @Test
    fun removesDuplicatesAndEmptyIdsBeforeBuildingQueue() {
        val candidates = listOf("songA", "songB", "songA", "", "songC", "songB")
        val shuffled = galaxShufflePool(candidates, key = { it }, random = Random(24))
        assertEquals(setOf("songA", "songB", "songC"), shuffled.toSet())
        assertEquals(3, shuffled.size)
    }

    @Test
    fun usesSeededRandomAndRespectsMaximumSize() {
        val songs = (1..80).map { "track$it" }
        val first = galaxShufflePool(songs, key = { it }, maxSize = 20, random = Random(42))
        val second = galaxShufflePool(songs, key = { it }, maxSize = 20, random = Random(42))
        assertEquals(first, second)
        assertEquals(20, first.size)
        assertEquals(20, first.distinct().size)
        assertTrue(first != songs.take(20))
    }

    @Test
    fun emptyOrInvalidSourcesReturnAnEmptyQueue() {
        assertTrue(galaxShufflePool(emptyList<String>(), key = { it }).isEmpty())
        assertTrue(galaxShufflePool(listOf("", ""), key = { it }).isEmpty())
        assertTrue(galaxShufflePool(listOf("music"), key = { it }, maxSize = 0).isEmpty())
    }
}
