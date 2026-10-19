package com.arturo254.opentune.viewmodels

import kotlin.random.Random

internal fun <T> galaxShufflePool(
    tracks: List<T>,
    key: (T) -> String,
    maxSize: Int = 300,
    random: Random = Random.Default,
): List<T> = tracks
    .filter { key(it).isNotBlank() }
    .distinctBy(key)
    .shuffled(random)
    .take(maxSize.coerceAtLeast(0))
