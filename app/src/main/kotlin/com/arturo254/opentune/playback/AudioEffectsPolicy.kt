package com.arturo254.opentune.playback

import kotlin.math.ceil
import kotlin.math.floor

internal object AudioEffectsPolicy {
    fun resampleLevelsByIndex(
        levelsMb: List<Int>,
        targetCount: Int,
    ): List<Int> {
        if (targetCount <= 0) return emptyList()
        if (levelsMb.isEmpty()) return List(targetCount) { 0 }
        if (levelsMb.size == targetCount) return levelsMb
        if (targetCount == 1) return listOf(levelsMb.sum() / levelsMb.size)

        val lastIndex = levelsMb.lastIndex.toFloat().coerceAtLeast(1f)
        return List(targetCount) { index ->
            val position = index.toFloat() * lastIndex / (targetCount - 1).toFloat()
            val low = floor(position).toInt().coerceIn(0, levelsMb.lastIndex)
            val high = ceil(position).toInt().coerceIn(0, levelsMb.lastIndex)
            val fraction = (position - low.toFloat()).coerceIn(0f, 1f)
            val a = levelsMb[low]
            val b = levelsMb[high]
            (a + ((b - a) * fraction)).toInt()
        }
    }
}
