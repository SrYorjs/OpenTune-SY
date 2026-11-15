package com.arturo254.opentune.ui.original.lyrics
import com.arturo254.opentune.ui.original.adapters.toOriginalMiniLine
object LyricsUtils {
    private val parsed = android.util.LruCache<String, List<LyricsEntry>>(8)
    @Synchronized fun parseLyricsCached(raw: String): List<LyricsEntry> = parsed.get(raw) ?:
        parseLyrics(raw).also { parsed.put(raw, it) }

    /** Keep the same format dispatch as the retained player, including cached TTML bodies. */
    fun parseLyrics(raw: String): List<LyricsEntry> {
        val source = raw.trim()
        val entries = if (com.arturo254.opentune.lyrics.LyricsUtils.isTtml(source)) {
            com.arturo254.opentune.lyrics.LyricsUtils.parseTtml(source)
        } else {
            com.arturo254.opentune.lyrics.LyricsUtils.parseLyrics(source)
        }
        return entries.map { it.toOriginalMiniLine() }
    }
    fun parsePlainLyrics(raw: String): List<LyricsEntry> = raw.lines().filter { it.isNotBlank() }.map { LyricsEntry(0L, it) }
    fun isUnsynced(lines: List<LyricsEntry>): Boolean = lines.isNotEmpty() && lines.all { it.time == lines.first().time && it.words.isNullOrEmpty() }

    const val LineLeadInMs = 300L

    /**
     * Index of the line that should read as ACTIVE at [position], or -1 before the first line.
     *
     * Returns [lines].size — one past the end — once playback has run past the final line's own
     * end, so callers can tell "the last line is being sung" apart from "the lyrics are over".
     * Without that distinction the last line stayed lit through the entire outro.
     */
    fun findCurrentLineIndex(
        lines: List<LyricsEntry>,
        position: Long,
    ): Int {
        if (lines.isEmpty()) return -1
        if (position >= lyricsEndMs(lines)) return lines.size
        for (index in lines.indices) {
            if (lines[index].time >= position + LineLeadInMs) {
                return index - 1
            }
        }
        return lines.lastIndex
    }

    /**
     * When a line stops being sung: the end of its last word if the line is word-synced, otherwise
     * the start of the next line, otherwise a plain-LRC guess.
     */
    fun lineEndMs(lines: List<LyricsEntry>, index: Int): Long {
        val entry = lines.getOrNull(index) ?: return 0L
        entry.words?.lastOrNull()?.let { return (it.endTime * 1000).toLong() }
        lines.getOrNull(index + 1)?.let { return it.time }
        return entry.time + UntimedLineDurationMs
    }

    /** Where the lyrics as a whole finish. */
    fun lyricsEndMs(lines: List<LyricsEntry>): Long {
        if (lines.isEmpty()) return 0L
        return lineEndMs(lines, lines.lastIndex)
    }

    /** Fallback duration for a line with no word timings and no successor. */
    private const val UntimedLineDurationMs = 5_000L


}
