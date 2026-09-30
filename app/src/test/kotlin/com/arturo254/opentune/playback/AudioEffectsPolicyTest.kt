package com.arturo254.opentune.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioEffectsPolicyTest {
    @Test
    fun emptyInput_createsFlatTargetBands() {
        assertEquals(
            listOf(0, 0, 0, 0, 0),
            AudioEffectsPolicy.resampleLevelsByIndex(emptyList(), 5),
        )
    }

    @Test
    fun matchingBandCount_isPreserved() {
        val input = listOf(-500, 0, 400)
        assertEquals(
            input,
            AudioEffectsPolicy.resampleLevelsByIndex(input, 3),
        )
    }

    @Test
    fun oneTargetBand_usesAverage() {
        assertEquals(
            listOf(0),
            AudioEffectsPolicy.resampleLevelsByIndex(
                listOf(-600, 0, 600),
                1,
            ),
        )
    }

    @Test
    fun resampling_preservesEndpoints() {
        val output =
            AudioEffectsPolicy.resampleLevelsByIndex(
                listOf(-1000, 1000),
                5,
            )
        assertEquals(-1000, output.first())
        assertEquals(1000, output.last())
        assertEquals(5, output.size)
    }

    @Test
    fun nonPositiveTarget_hasNoBands() {
        assertEquals(
            emptyList<Int>(),
            AudioEffectsPolicy.resampleLevelsByIndex(
                listOf(1, 2, 3),
                0,
            ),
        )
    }
}
