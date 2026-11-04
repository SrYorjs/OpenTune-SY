package com.arturo254.opentune.ui.screens.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.R
import com.arturo254.opentune.core.designsystem.liquid.LocalGalaxLiquidBackdrop
import com.arturo254.opentune.core.designsystem.liquid.galaxLiquidEffect
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/** Search owns one continuous backdrop around its inset input and result surface. */
@Composable
internal fun PrismSearchBar(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    onSearch: (String) -> Unit,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    focusRequester: FocusRequester,
    pureBlack: Boolean,
    content: @Composable () -> Unit,
) {
    val backdrop = rememberLayerBackdrop()
    val inputLabel = stringResource(R.string.prism_input_label)
    BackHandler(active) { onActiveChange(false) }
    LaunchedEffect(active) { if (active) focusRequester.requestFocus() }
    Box(if (active) Modifier.fillMaxSize().imePadding() else Modifier.fillMaxWidth()) {
        if (active)
            PrismAtmosphere(true, pureBlack, Modifier.matchParentSize().layerBackdrop(backdrop))
        CompositionLocalProvider(
            LocalGalaxLiquidBackdrop provides
                (if (active) backdrop else LocalGalaxLiquidBackdrop.current),
            LocalPrismBackdrop provides (if (active) backdrop else null),
        ) {
            Column(if (active) Modifier.fillMaxSize() else Modifier.fillMaxWidth()) {
                Box(
                    Modifier.fillMaxWidth()
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        Modifier.widthIn(max = 1072.dp)
                            .fillMaxWidth()
                            .galaxLiquidEffect(
                                pureBlack = pureBlack,
                                shapeOverride = RoundedCornerShape(22.dp),
                            )
                            .heightIn(min = 56.dp)
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                painterResource(R.drawable.arrow_back),
                                stringResource(R.string.back_button_desc),
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            modifier =
                                Modifier.weight(1f)
                                    .padding(vertical = 8.dp)
                                    .focusRequester(focusRequester)
                                    .onFocusChanged {
                                        if (it.isFocused && !active) onActiveChange(true)
                                    }
                                    .semantics { contentDescription = inputLabel },
                            textStyle =
                                MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.tertiary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { onSearch(query.text) }),
                            decorationBox = { input ->
                                Box {
                                    if (query.text.isEmpty())
                                        Text(
                                            stringResource(R.string.prism_search_hint),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    input()
                                }
                            },
                        )
                        if (query.text.isNotEmpty())
                            IconButton(onClick = { onQueryChange(TextFieldValue()) }) {
                                Icon(
                                    painterResource(R.drawable.close),
                                    stringResource(R.string.prism_clear),
                                    Modifier.size(20.dp),
                                )
                            }
                        else Spacer(Modifier.width(16.dp))
                    }
                }
                if (active) Box(Modifier.weight(1f)) { content() }
            }
        }
    }
}
