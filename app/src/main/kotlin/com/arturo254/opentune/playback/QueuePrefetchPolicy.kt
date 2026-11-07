package com.arturo254.opentune.playback

/**
 * La cola siempre empieza sin esperar 50 resoluciones de streaming.
 * Una vez iniciada, mantenemos 50 elementos cuando están disponibles
 * y reponemos otra página antes de que el usuario alcance el final.
 */
internal object QueuePrefetchPolicy {
    const val MIN_INITIAL_ITEMS = 50
    const val LOW_WATER_MARK = 10

    fun shouldLoad(
        currentIndex: Int,
        totalItems: Int,
        hasNextPage: Boolean,
        autoLoadEnabled: Boolean,
        repeatEnabled: Boolean,
        suppressed: Boolean,
    ): Boolean {
        if (!hasNextPage || !autoLoadEnabled || repeatEnabled || suppressed) return false
        if (totalItems <= 0 || currentIndex !in 0 until totalItems) return false
        val upcoming = totalItems - currentIndex - 1
        return totalItems < MIN_INITIAL_ITEMS || upcoming <= LOW_WATER_MARK
    }
}
