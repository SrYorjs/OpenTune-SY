package com.arturo254.opentune.ui.original

import com.arturo254.opentune.db.entities.LyricsEntity
import com.arturo254.opentune.lyrics.LyricsUtils as SourceLyrics
import com.arturo254.opentune.ui.original.lyrics.LyricsUtils
import org.junit.Assert.*
import org.junit.Test

class OriginalTtmlLyricsRegressionTest {
    // The reported fragment is unchanged. Only the missing document envelope is supplied;
    // namespace declarations are fixture scaffolding, not assumed provider root attributes.
    private val fragment = """<div begin="2:52.981" end="3:31.448"><p begin="2:55.579" end="3:00.271" lrc:key="L64" ttm:agent="v1"><span begin="2:55.579" end="2:55.856">Yo</span> <span begin="2:55.856" end="2:56.276">quiero</span> <span begin="2:56.276" end="2:56.984">hacérte</span><span begin="2:56.984" end="2:57.365">lo</span> <span begin="2:57.365" end="2:57.376">bi</span><span begin="2:57.376" end="2:57.615">en</span> <span begin="2:57.615" end="3:00.271">high</span></p></div>"""
    private fun document(body: String) = """<tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata" xmlns:lrc="urn:lyrics:test"><body>$body</body></tt>"""

    @Test fun storedTtmlUsesTheRetainedPlayersParserAndKeepsEveryTimedPiece() {
        val entity = LyricsEntity("reported-song", document(fragment))
        // This is exactly the LRC-only call that discarded TTML in C4.
        assertTrue(SourceLyrics.parseLyrics(entity.lyrics).isEmpty())
        val oldPlayer = SourceLyrics.parseTtml(entity.lyrics).single()
        val actual = LyricsUtils.parseLyrics(entity.lyrics).single()
        assertEquals(175579L, actual.time)
        assertEquals("Yo quiero hacértelo bien high", actual.text)
        assertEquals("v1", actual.agent)
        assertEquals(oldPlayer.text, actual.text)
        val pieces = actual.words!!
        assertEquals(listOf("Yo ", "quiero ", "hacérte", "lo ", "bi", "en ", "high"), pieces.map { it.text })
        val starts = listOf(175.579, 175.856, 176.276, 176.984, 177.365, 177.376, 177.615)
        val ends = listOf(175.856, 176.276, 176.984, 177.365, 177.376, 177.615, 180.271)
        pieces.forEachIndexed { index, word ->
            assertEquals(starts[index], word.startTime, 0.000001)
            assertEquals(ends[index], word.endTime, 0.000001)
            assertEquals(oldPlayer.words!![index].text, word.text)
        }
        assertNull(actual.rubyFlow.value)
        assertNull(actual.translatedTextFlow.value)
    }

    @Test fun multilineSongKeepsOrderSpacesLineSelectionAndFinalWordEnd() {
        // Synthetic neighbouring lines exercise a song body; they are not claimed provider data.
        val before = """<div><p begin="2:50.000" end="2:51.500"><span begin="2:50.000" end="2:51.500">Antes</span></p></div>"""
        val after = """<div><p begin="3:01.000" end="3:03.250"><span begin="3:01.000" end="3:02.000">Otra</span> <span begin="3:02.000" end="3:03.250">línea</span></p></div>"""
        val lines = LyricsUtils.parseLyrics(document(after + fragment + before))
        assertEquals(listOf(170000L, 175579L, 181000L), lines.map { it.time })
        assertEquals(listOf("Antes", "Yo quiero hacértelo bien high", "Otra línea"), lines.map { it.text })
        assertFalse(LyricsUtils.isUnsynced(lines))
        assertEquals(1, LyricsUtils.findCurrentLineIndex(lines, 176500L))
        assertEquals(2, LyricsUtils.findCurrentLineIndex(lines, 182000L))
        assertEquals(180271L, LyricsUtils.lineEndMs(lines, 1))
        assertEquals(183250L, LyricsUtils.lyricsEndMs(lines))
        assertEquals(lines.size, LyricsUtils.findCurrentLineIndex(lines, 184000L))
        assertEquals(2, lines.last().words!!.size)
    }

    @Test fun lrcStillUsesItsExistingWordTimingAndPlainTextRemainsUntimed() {
        val raw = "[00:02.000]Yo<00:02.500> quiero<00:03.000>"
        val source = SourceLyrics.parseLyrics(raw).single()
        val mapped = LyricsUtils.parseLyrics(raw).single()
        assertEquals(source.time, mapped.time)
        assertEquals(source.text, mapped.text)
        assertEquals(source.words!!.map { it.startTime to it.endTime }, mapped.words!!.map { it.startTime to it.endTime })
        assertTrue(LyricsUtils.parseLyrics("Sin tiempos").isEmpty())
    }
}
