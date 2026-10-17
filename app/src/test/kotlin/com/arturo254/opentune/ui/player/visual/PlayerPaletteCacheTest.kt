package com.arturo254.opentune.ui.player.visual

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerPaletteCacheTest {
    @Test fun retainsRecentPalettesWithoutClearingAllOnCapacity() {
        val cache = PlayerPaletteCache<Int, String>(3)
        cache[1] = "one"
        cache[2] = "two"
        cache[3] = "three"
        assertEquals("one", cache[1]) // Hot palette must survive next eviction.
        cache[4] = "four"
        assertEquals(3, cache.size)
        assertTrue(cache.containsKey(1))
        assertFalse(cache.containsKey(2))
        assertTrue(cache.containsKey(3))
        assertTrue(cache.containsKey(4))
    }

    @Test fun defaultCapacityIsBoundedAndValueRefreshAlsoReorders() {
        val cache = PlayerPaletteCache<Int, Int>()
        for (i in 0 until PLAYER_PALETTE_CACHE_CAPACITY) cache[i] = i
        cache[0] = 100
        cache[PLAYER_PALETTE_CACHE_CAPACITY] = 64
        assertEquals(PLAYER_PALETTE_CACHE_CAPACITY, cache.size)
        assertEquals(100, cache[0])
        assertFalse(cache.containsKey(1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroCapacityIsRejected() {
        PlayerPaletteCache<Int, Int>(0)
    }
}
