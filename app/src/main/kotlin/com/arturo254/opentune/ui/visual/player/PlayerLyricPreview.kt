package com.arturo254.opentune.ui.visual.player

import com.arturo254.opentune.db.entities.LyricsEntity
import com.arturo254.opentune.lyrics.LyricsUtils

data class PlayerLyricLine(
    val timeMs: Long,
    val text: String,
)

fun parsePlayerLyricPreview(lyrics: String?): List<PlayerLyricLine> {
    val raw = lyrics?.trim()?.takeIf {
        it.isNotEmpty() && it != LyricsEntity.LYRICS_NOT_FOUND
    } ?: return emptyList()
    val parsed = runCatching {
        if (LyricsUtils.isTtml(raw)) LyricsUtils.parseTtml(raw)
        else LyricsUtils.parseLyrics(raw)
    }.getOrDefault(emptyList())
    if (parsed.isNotEmpty()) {
        return parsed.mapNotNull { entry ->
            entry.text.trim().takeIf(String::isNotBlank)?.let {
                PlayerLyricLine(entry.time, it)
            }
        }
    }
    if (raw.startsWith("<")) return emptyList()
    return raw.lineSequence()
        .map(String::trim)
        .filter { it.isNotBlank() && !it.startsWith("[") }
        .take(1)
        .map { PlayerLyricLine(-1L, it) }
        .toList()
}

fun playerLyricPreviewAt(
    lines: List<PlayerLyricLine>,
    positionMs: Long,
): String? {
    if (lines.isEmpty()) return null
    if (lines.first().timeMs < 0) return lines.first().text
    return lines.lastOrNull { it.timeMs <= positionMs }?.text ?: lines.first().text
}
