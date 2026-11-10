package com.arturo254.opentune.canvas

import java.text.Normalizer
import kotlin.math.abs

object CanvasMatch {
    
    private const val DURATION_TOLERANCE_SEC = 4.0

    private val OtherTake = Regex(
        "\\b(live|remix|instrumental|acoustic|karaoke|sped up|slowed|nightcore|8d|demo|inst)\\b",
        RegexOption.IGNORE_CASE,
    )
    private val Bracketed = Regex("\\s*[(\\[][^)\\]]*[)\\]]")
    private val DashTail = Regex("\\s+[-–—]\\s+.*$")
    private val Marks = Regex("\\p{M}+")
    private val NonWord = Regex("[^\\p{L}\\p{N}]+")

    
    fun sameSong(candidate: String, wanted: String): Boolean {
        val key = titleKey(candidate)
        if (key.isEmpty() || key != titleKey(wanted)) return false
        return !OtherTake.containsMatchIn(candidate) || OtherTake.containsMatchIn(wanted)
    }

    fun sameArtist(candidate: String, wanted: String): Boolean {
        val a = fold(candidate)
        val b = fold(wanted)
        return a.isNotEmpty() && b.isNotEmpty() && (a.contains(b) || b.contains(a))
    }

    
    fun closeDuration(candidateSec: Double?, wantedSec: Int?): Boolean {
        if (candidateSec == null || candidateSec <= 0 || wantedSec == null || wantedSec <= 0) return true
        return abs(candidateSec - wantedSec) <= DURATION_TOLERANCE_SEC
    }

    private fun titleKey(title: String): String =
        fold(title.replace(Bracketed, "").replace(DashTail, ""))

    private fun fold(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFKD).replace(Marks, "").replace(NonWord, "")
}
