package com.arturo254.opentune.ui.visual.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import com.arturo254.opentune.constants.LibraryPinnedEntitiesKey
import com.arturo254.opentune.utils.rememberPreference
import java.net.URLDecoder
import java.net.URLEncoder

enum class LibraryPinKind {
    SONG,
    PLAYLIST,
    ALBUM,
    ARTIST,
}

data class LibraryPinnedRef(
    val kind: LibraryPinKind,
    val id: String,
)

fun decodeLibraryPins(raw: String): List<LibraryPinnedRef> =
    raw.lineSequence().mapNotNull { token ->
        val separator = token.indexOf(':')
        if (separator < 1 || separator == token.lastIndex) return@mapNotNull null
        val kind = LibraryPinKind.entries.firstOrNull {
            it.name == token.substring(0, separator)
        } ?: return@mapNotNull null
        val id = runCatching {
            URLDecoder.decode(token.substring(separator + 1), "UTF-8")
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        LibraryPinnedRef(kind, id)
    }.distinct().take(64).toList()

fun encodeLibraryPins(pins: List<LibraryPinnedRef>): String =
    pins.distinct().take(64).joinToString("\n") { pin ->
        pin.kind.name + ":" + URLEncoder.encode(pin.id, "UTF-8")
    }

class LibraryPinsState(private val storage: MutableState<String>) {
    val pins: List<LibraryPinnedRef>
        get() = decodeLibraryPins(storage.value)

    fun contains(kind: LibraryPinKind, id: String): Boolean =
        pins.contains(LibraryPinnedRef(kind, id))

    fun toggle(kind: LibraryPinKind, id: String) {
        val selected = LibraryPinnedRef(kind, id)
        val current = pins
        storage.value = encodeLibraryPins(
            if (selected in current) current.filterNot { it == selected }
            else listOf(selected) + current,
        )
    }

    fun remove(kind: LibraryPinKind, id: String) {
        storage.value = encodeLibraryPins(pins.filterNot { it.kind == kind && it.id == id })
    }
}

@Composable
fun rememberLibraryPins(): LibraryPinsState {
    val storage = rememberPreference(LibraryPinnedEntitiesKey, "")
    return remember(storage) { LibraryPinsState(storage) }
}
