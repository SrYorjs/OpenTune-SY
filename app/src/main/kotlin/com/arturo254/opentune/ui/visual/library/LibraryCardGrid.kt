package com.arturo254.opentune.ui.visual.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.ui.visual.search.SearchHorizontalPadding

@Immutable
class LibraryCardEntry(
    val title: String,
    val count: Int,
    val covers: List<String?>,
    val onClick: () -> Unit,
    val roundCovers: Boolean = false,
    val icon: (@Composable () -> Unit)? = null,
)


@Composable
fun LibraryCardGrid(entries: List<LibraryCardEntry>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = SearchHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { entry ->
                    LibraryCategoryCard(
                        title = entry.title,
                        count = entry.count,
                        covers = entry.covers,
                        onClick = entry.onClick,
                        icon = entry.icon,
                        roundCovers = entry.roundCovers,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
