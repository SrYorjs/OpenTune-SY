package com.arturo254.opentune.ui.visual.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

import com.arturo254.opentune.ui.visual.component.GlassState
import com.arturo254.opentune.ui.visual.component.glassRoot
import com.arturo254.opentune.ui.visual.search.SearchColors
import com.arturo254.opentune.ui.visual.search.SearchField
import com.arturo254.opentune.ui.visual.search.SearchGlassTopBar
import com.arturo254.opentune.ui.visual.search.searchTopBarHeight

@Composable
fun LibraryScaffold(
    title: String,
    subtitle: String?,
    searchPlaceholder: String,
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    listState: LazyListState,
    glassState: GlassState,
    collapseProvider: () -> Float,
    pureBlack: Boolean,
    bottomInset: Dp,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SearchColors.page(pureBlack)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .glassRoot(glassState, isActive = { collapseProvider() > 0.01f }),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = searchTopBarHeight(
                        withTitle = true,
                        titleBlockHeight = if (subtitle != null) LibraryTitleBlockHeight else 62.dp,
                    ),
                    bottom = bottomInset + 24.dp,
                ),
                content = content,
            )
        }

        SearchGlassTopBar(
            glassState = glassState,
            progressProvider = collapseProvider,
            pureBlack = pureBlack,
            title = title,
            subtitle = subtitle,
            titleBlockHeight = if (subtitle != null) LibraryTitleBlockHeight else 62.dp,
            trailing = trailing,
        ) {
            SearchField(
                value = query,
                onValueChange = onQueryChange,
                onSearch = {},
                placeholder = searchPlaceholder,
            )
        }
    }
}
