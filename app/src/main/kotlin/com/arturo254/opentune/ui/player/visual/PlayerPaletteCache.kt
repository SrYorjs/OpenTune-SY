package com.arturo254.opentune.ui.player.visual

/**
 * Tiny, bounded LRU for in-memory artwork palettes.
 *
 * The previous implementation cleared every palette after 64 entries,
 * repeatedly decoding recently played artwork during long radio sessions.
 * This keeps the most recently read entries and evicts only the eldest.
 *
 * This is a main-thread-only Compose-owned map; never share it between
 * workers without synchronization. PlayerVisualState writes on the UI thread.
 */
internal class PlayerPaletteCache<K, V>(
    private val maxEntries: Int = PLAYER_PALETTE_CACHE_CAPACITY,
) : LinkedHashMap<K, V>(maxEntries.coerceAtLeast(1), 0.75f, true) {
    init {
        require(maxEntries > 0) { "Palette cache capacity must be positive" }
    }

    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean =
        size > maxEntries
}
