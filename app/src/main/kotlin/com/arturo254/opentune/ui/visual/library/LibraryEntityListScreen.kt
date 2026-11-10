package com.arturo254.opentune.ui.visual.library

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.ui.visual.component.GlassState
import com.arturo254.opentune.ui.visual.search.rememberCollapseProgress
import kotlinx.coroutines.launch

private sealed interface IndexedRow<out T> {
    data class Header(val section: IndexSection) : IndexedRow<Nothing>
    data class Entry<T>(val item: T) : IndexedRow<T>
}

@Composable
fun <T> LibraryEntityListScreen(
    title: String,
    subtitle: String,
    searchPlaceholder: String,
    items: List<T>,
    idOf: (T) -> String,
    nameOf: (T) -> String,
    subtitleOf: (T) -> String?,
    thumbnailOf: (T) -> String?,
    onClick: (T) -> Unit,
    onLongClick: (T) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    playingIdOf: ((T) -> String)? = null,
    roundThumbnails: Boolean = false,
    sharedElementKeyOf: ((T) -> String?)? = null,
    rowWrapper: @Composable (item: T, row: @Composable () -> Unit) -> Unit = { _, row -> row() },
    trailing: (@Composable () -> Unit)? = null,
) {
    var query by remember { mutableStateOf(TextFieldValue()) }
    val listState = rememberLazyListState()
    val glassState = remember { GlassState() }
    val collapse = rememberCollapseProgress(listState)
    val scope = rememberCoroutineScope()
    val bottomInset = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()

    val playerConnection = LocalPlayerConnection.current
    val nowPlaying = playerConnection?.mediaMetadata?.collectAsState()?.value
    val isPlaying = playerConnection?.isPlaying?.collectAsState()?.value == true

    val sections = remember(items, query.text) {
        val q = query.text.trim()
        val visible = if (q.isBlank()) {
            items
        } else {
            items.filter {
                nameOf(it).contains(q, ignoreCase = true) || subtitleOf(it)?.contains(q, ignoreCase = true) == true
            }
        }
        indexSections(visible, nameOf)
    }

    val rows = remember(sections) {
        buildList<IndexedRow<T>> {
            sections.forEach { (section, entries) ->
                add(IndexedRow.Header(section))
                entries.forEach { add(IndexedRow.Entry(it)) }
            }
        }
    }

    val sectionIndex = remember(rows) {
        buildMap {
            rows.forEachIndexed { index, row ->
                if (row is IndexedRow.Header) put(row.section.key, index + 1)
            }
        }
    }

    androidx.compose.foundation.layout.Box(modifier = modifier.fillMaxSize()) {
        LibraryScaffold(
            title = title,
            subtitle = subtitle,
            searchPlaceholder = searchPlaceholder,
            query = query,
            onQueryChange = { query = it },
            listState = listState,
            glassState = glassState,
            collapseProvider = { collapse.value },
            pureBlack = pureBlack,
            bottomInset = bottomInset,
            trailing = trailing,
        ) {
            item(key = "top_rule") {
                LibraryRule()
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (rows.isEmpty()) {
                item(key = "empty") { LibraryEmptyState() }
                return@LibraryScaffold
            }

            itemsIndexed(
                items = rows,
                key = { _, row ->
                    when (row) {
                        is IndexedRow.Header -> "h_${row.section.key}"
                        is IndexedRow.Entry -> idOf(row.item)
                    }
                },
                contentType = { _, row -> if (row is IndexedRow.Header) "header" else "entry" },
            ) { _, row ->
                when (row) {
                    is IndexedRow.Header -> LibraryLetterHeader(row.section.label)
                    is IndexedRow.Entry -> rowWrapper(row.item) {
                        val active = playingIdOf != null && nowPlaying?.id == playingIdOf(row.item)
                        LibraryListRow(
                            title = nameOf(row.item),
                            subtitle = subtitleOf(row.item),
                            thumbnailUrl = thumbnailOf(row.item),
                            isActive = active,
                            isPlaying = active && isPlaying,
                            round = roundThumbnails,
                            sharedElementKey = sharedElementKeyOf?.invoke(row.item),
                            songId = playingIdOf?.invoke(row.item),
                            onClick = { onClick(row.item) },
                            onLongClick = { onLongClick(row.item) },
                        )
                    }
                }
            }
        }

        if (sectionIndex.isNotEmpty()) {
            LibraryIndexRail(
                sections = sections.map { it.first },
                onSectionSelected = { key ->
                    sectionIndex[key]?.let { index ->
                        scope.launch { listState.animateScrollToItem(index) }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
