package com.arturo254.opentune.ui.visual.library

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.R
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.ui.visual.search.SearchColors

private enum class LocalManagePage {
    MAIN,
    SELECTION,
    CONFIRMATION,
}

@Composable
fun LibraryLocalManageMenu(
    importedSongs: List<Song>,
    running: Boolean,
    onImport: () -> Unit,
    onRemove: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var page by remember { mutableStateOf(LocalManagePage.MAIN) }
    val selected = remember { mutableStateListOf<String>() }
    val available = importedSongs.filter { it.song.isLocal }
    val toRemove = selected.filter { id -> available.any { it.id == id } }

    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when (page) {
            LocalManagePage.MAIN -> {
                Text(
                    text = stringResource(R.string.visual_library_manage_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = SearchColors.Primary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LocalManageTile(
                        title = stringResource(R.string.visual_library_manage_add),
                        icon = R.drawable.library_add,
                        onClick = {
                            onDismiss()
                            onImport()
                        },
                        modifier = Modifier.weight(1f),
                    )
                    LocalManageTile(
                        title = stringResource(R.string.visual_library_manage_select),
                        icon = R.drawable.delete,
                        onClick = {
                            selected.clear()
                            page = LocalManagePage.SELECTION
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                LocalManageTile(
                    title = stringResource(R.string.visual_library_manage_clear),
                    icon = R.drawable.delete,
                    onClick = {
                        selected.clear()
                        selected.addAll(available.map { it.id })
                        page = LocalManagePage.CONFIRMATION
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            LocalManagePage.SELECTION -> {
                Text(
                    text = stringResource(R.string.visual_library_manage_select_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = SearchColors.Primary,
                )
                if (available.isEmpty()) {
                    LibraryEmptyState(text = stringResource(R.string.visual_library_no_local))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(available, key = { it.id }) { song ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SearchColors.Tile)
                                    .clickable(role = Role.Checkbox) {
                                        if (song.id in selected) selected.remove(song.id) else selected.add(song.id)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = song.id in selected,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            if (song.id !in selected) selected.add(song.id)
                                        } else {
                                            selected.remove(song.id)
                                        }
                                    },
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = song.title,
                                        color = SearchColors.Primary,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = song.artists.joinToString { it.name },
                                        color = SearchColors.Secondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = { page = LocalManagePage.MAIN }) {
                        Text(stringResource(R.string.visual_library_manage_cancel))
                    }
                    Button(
                        enabled = toRemove.isNotEmpty() && !running,
                        onClick = { page = LocalManagePage.CONFIRMATION },
                    ) {
                        Text(stringResource(R.string.visual_library_manage_remove, toRemove.size))
                    }
                }
            }
            LocalManagePage.CONFIRMATION -> {
                Text(
                    text = stringResource(R.string.visual_library_manage_confirm_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = SearchColors.Primary,
                )
                Text(
                    text = stringResource(R.string.visual_library_manage_confirm_description, toRemove.size),
                    color = SearchColors.Secondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = { page = LocalManagePage.MAIN }) {
                        Text(stringResource(R.string.visual_library_manage_cancel))
                    }
                    Button(
                        enabled = toRemove.isNotEmpty() && !running,
                        onClick = {
                            onRemove(toRemove)
                            onDismiss()
                        },
                    ) {
                        Text(stringResource(R.string.visual_library_manage_remove, toRemove.size))
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalManageTile(
    title: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .heightIn(min = 92.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SearchColors.Tile)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = title,
            color = SearchColors.Primary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
