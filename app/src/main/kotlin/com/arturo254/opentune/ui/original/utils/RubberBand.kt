package com.arturo254.opentune.ui.original.utils

import kotlin.math.abs
import kotlin.math.sign


internal fun rubberBand(over: Float, reach: Float): Float {
    if (over == 0f) return 0f
    val d = abs(over)
    return sign(over) * (1f - 1f / (d * 0.55f / reach + 1f)) * reach
}
