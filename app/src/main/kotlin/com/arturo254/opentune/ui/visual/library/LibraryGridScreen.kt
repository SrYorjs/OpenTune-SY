package com.arturo254.opentune.ui.visual.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import androidx.compose.ui.text.TextStyle
import com.arturo254.opentune.ui.visual.theme.InterFontFamily
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import com.arturo254.opentune.R
import com.arturo254.opentune.ui.visual.component.BoneColor
import com.arturo254.opentune.ui.visual.component.HomeCardCornerRadius
import com.arturo254.opentune.ui.visual.component.HomeCardHighlight
import com.arturo254.opentune.ui.visual.component.HomeCardSpacing
import com.arturo254.opentune.ui.visual.component.containerTransformSource

import com.arturo254.opentune.ui.utils.resize
import com.arturo254.opentune.ui.visual.component.GlassState
import com.arturo254.opentune.ui.visual.search.SearchColors
import com.arturo254.opentune.ui.visual.search.SearchHorizontalPadding
import com.arturo254.opentune.ui.visual.search.rememberCollapseProgress

private val TrackTitleStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 17.sp,
    letterSpacing = (-0.03).em,
)
private val TrackSubtitleStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 11.5.sp,
    lineHeight = 14.sp,
    letterSpacing = (-0.02).em,
)

data class LibraryGridEntry(
    val id: String,
    val title: String,
    val subtitle: String?,
    val thumbnailUrl: String?,
    
    val sharedElementKey: String? = null,
)

@Composable
fun LibraryGridScreen(
    title: String,
    subtitle: String,
    searchPlaceholder: String,
    entries: List<LibraryGridEntry>,
    onOpen: (LibraryGridEntry) -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
) {
    var query by remember { mutableStateOf(TextFieldValue()) }
    val listState = rememberLazyListState()
    val glassState = remember { GlassState() }
    val collapse = rememberCollapseProgress(listState)
    val bottomInset = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()

    val visible = remember(entries, query.text) {
        val q = query.text.trim()
        if (q.isBlank()) entries else entries.filter { it.title.contains(q, ignoreCase = true) }
    }
    val rows = remember(visible) { visible.chunked(2) }

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
        modifier = modifier,
    ) {
        item(key = "rule") {
            LibraryRule()
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (rows.isEmpty()) {
            item(key = "empty") { LibraryEmptyState() }
            return@LibraryScaffold
        }

        item(key = "playbar") {
            LibraryPlayBar(onPlay = onPlay, onMore = onShuffle)
            Spacer(modifier = Modifier.height(20.dp))
        }

        items(rows.size, key = { rows[it].first().id }) { index ->
            val row = rows[index]
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SearchHorizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(HomeCardSpacing),
            ) {
                row.forEach { entry ->
                    LibraryGridTile(entry = entry, onClick = { onOpen(entry) }, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun LibraryGridTile(entry: LibraryGridEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(HomeCardCornerRadius)
    Column(
        modifier = modifier.clickable(
            interactionSource = null,
            indication = HomeCardHighlight,
            onClick = onClick,
        ),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .containerTransformSource(
                    key = entry.sharedElementKey,
                    cornerRadius = HomeCardCornerRadius,
                    coverUrl = entry.thumbnailUrl,
                )
                .clip(shape)
                .background(BoneColor),
        ) {
            if (entry.thumbnailUrl != null) {
                AsyncImage(
                    model = entry.thumbnailUrl.resize(544, 544),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.music_note),
                    contentDescription = null,
                    tint = SearchColors.Secondary,
                    modifier = Modifier.fillMaxSize(0.3f),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = entry.title,
            style = TrackTitleStyle,
            color = Color.White.copy(alpha = 0.92f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!entry.subtitle.isNullOrBlank()) {
            Text(
                text = entry.subtitle,
                style = TrackSubtitleStyle,
                color = Color.White.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
