package com.arturo254.opentune.ui.visual.library

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.R
import com.arturo254.opentune.ui.visual.search.SearchColors
import com.arturo254.opentune.ui.visual.search.SearchHorizontalPadding

enum class LibraryTab(val labelRes: Int) {
    All(R.string.visual_library_tab_all),
    Local(R.string.visual_library_tab_local),
    Cached(R.string.visual_library_tab_cached),
    Imported(R.string.visual_library_tab_imported),
}

private val ChipTrack = Color(0xFF161618)
private val Chip = Color(0xFF232326)
private val ChipSelected = Color(0xFF3D3D41)

private val ChipTrackPadding = 3.dp
private val ChipGap = 4.dp
private val ChipRowGap = 6.dp

@Composable
fun LibraryTabChips(
    selected: LibraryTab,
    onSelect: (LibraryTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(
        modifier = modifier,
        content = {
            LibraryTab.entries.forEach { _ -> Box(Modifier.background(ChipTrack, RoundedCornerShape(percent = 50))) }
            LibraryTab.entries.forEach { tab -> LibraryTabChip(tab, isSelected = tab == selected, onSelect = onSelect) }
        },
    ) { measurables, constraints ->
        val tracks = measurables.take(LibraryTab.entries.size)
        val pad = ChipTrackPadding.roundToPx()
        val gap = ChipGap.roundToPx()
        val rowGap = ChipRowGap.roundToPx()
        val room = if (constraints.hasBoundedWidth) (constraints.maxWidth - pad * 2).coerceAtLeast(0) else Constraints.Infinity
        val chips = measurables.drop(LibraryTab.entries.size).map { it.measure(Constraints(maxWidth = room)) }

        val rows = mutableListOf(mutableListOf<Placeable>())
        var rowWidth = 0
        chips.forEach { chip ->
            val row = rows.last()
            val widthWithChip = if (row.isEmpty()) chip.width else rowWidth + gap + chip.width
            if (row.isNotEmpty() && widthWithChip > room) {
                rows += mutableListOf(chip)
                rowWidth = chip.width
            } else {
                row += chip
                rowWidth = widthWithChip
            }
        }

        val rowHeights = rows.map { row -> row.maxOf { it.height } }
        val trackPlaceables = rows.mapIndexed { index, row ->
            val width = row.sumOf { it.width } + gap * (row.size - 1) + pad * 2
            tracks[index].measure(Constraints.fixed(width, rowHeights[index] + pad * 2))
        }
        val width = trackPlaceables.maxOf { it.width }.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = trackPlaceables.sumOf { it.height } + rowGap * (rows.size - 1)

        layout(width, height) {
            var y = 0
            rows.forEachIndexed { index, row ->
                trackPlaceables[index].placeRelative(0, y)
                var x = pad
                row.forEach { chip ->
                    chip.placeRelative(x, y + pad + (rowHeights[index] - chip.height) / 2)
                    x += chip.width + gap
                }
                y += trackPlaceables[index].height + rowGap
            }
        }
    }
}

@Composable
private fun LibraryTabChip(tab: LibraryTab, isSelected: Boolean, onSelect: (LibraryTab) -> Unit) {
    val background by animateColorAsState(
        targetValue = if (isSelected) ChipSelected else Chip,
        animationSpec = tween(220),
        label = "libraryTabChip",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(background)
            .clickable(
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onSelect(tab) },
            )
            .heightIn(min = 28.dp)
            .padding(horizontal = 14.dp, vertical = 5.dp),
    ) {
        Text(
            text = stringResource(tab.labelRes),
            color = if (isSelected) SearchColors.Primary else SearchColors.Primary.copy(alpha = 0.72f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun LibraryCapsLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = SearchColors.Primary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.4.sp,
        modifier = modifier.padding(horizontal = SearchHorizontalPadding),
    )
}
