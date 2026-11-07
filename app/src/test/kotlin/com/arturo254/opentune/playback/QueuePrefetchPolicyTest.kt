package com.arturo254.opentune.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QueuePrefetchPolicyTest {
    @Test fun cargaLaPrimeraPaginaConSoloUnaCancion() {
        assertTrue(shouldLoad(index = 0, total = 1))
        assertTrue(shouldLoad(index = 0, total = 49))
    }

    @Test fun esperaAlUmbralConCincuentaCanciones() {
        assertFalse(shouldLoad(index = 0, total = 50))
        assertFalse(shouldLoad(index = 38, total = 50))
        assertTrue(shouldLoad(index = 39, total = 50))
        assertTrue(shouldLoad(index = 49, total = 50))
    }

    @Test fun respetaBloqueosYColasAgotadas() {
        assertFalse(shouldLoad(index = 0, total = 1, hasMore = false))
        assertFalse(shouldLoad(index = 0, total = 1, auto = false))
        assertFalse(shouldLoad(index = 0, total = 1, repeat = true))
        assertFalse(shouldLoad(index = 0, total = 1, suppressed = true))
        assertFalse(shouldLoad(index = -1, total = 50))
        assertFalse(shouldLoad(index = 0, total = 0))
    }

    private fun shouldLoad(
        index: Int,
        total: Int,
        hasMore: Boolean = true,
        auto: Boolean = true,
        repeat: Boolean = false,
        suppressed: Boolean = false,
    ): Boolean = QueuePrefetchPolicy.shouldLoad(index, total, hasMore, auto, repeat, suppressed)
}
