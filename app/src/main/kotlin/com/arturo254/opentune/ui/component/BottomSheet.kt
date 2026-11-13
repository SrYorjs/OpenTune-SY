package com.arturo254.opentune.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.core.designsystem.component.OpenTuneOverlayDimensions
import com.arturo254.opentune.ui.original.component.BottomSheet as OriginalBottomSheet
import com.arturo254.opentune.ui.original.component.rememberBottomSheetState as rememberOriginalBottomSheetState

typealias BottomSheetState = com.arturo254.opentune.ui.original.component.BottomSheetState

const val EXPANDED_ANCHOR = 2
const val COLLAPSED_ANCHOR = 1
const val DISMISSED_ANCHOR = 0

@Composable
fun BottomSheet(
    state: BottomSheetState,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    onDismiss: (() -> Unit)? = null,
    collapsedContent: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit,
) = OriginalBottomSheet(
    state = state,
    modifier = modifier,
    background = {
        Box(
            Modifier
                .fillMaxSize()
                .clip(
                    RoundedCornerShape(
                        topStart = if (!state.isExpanded) OpenTuneOverlayDimensions.collapsedPlayerTopRadius else 0.dp,
                        topEnd = if (!state.isExpanded) OpenTuneOverlayDimensions.collapsedPlayerTopRadius else 0.dp,
                    )
                )
                .background(backgroundColor.copy(alpha = backgroundColor.alpha * state.progress.coerceIn(0f, 1f)))
        )
    },
    onDismiss = onDismiss,
    collapsedContent = collapsedContent,
    content = content,
)

@Composable
fun rememberBottomSheetState(
    dismissedBound: Dp,
    expandedBound: Dp,
    collapsedBound: Dp = dismissedBound,
    initialAnchor: Int = DISMISSED_ANCHOR,
): BottomSheetState = rememberOriginalBottomSheetState(
    dismissedBound = dismissedBound,
    expandedBound = expandedBound,
    collapsedBound = collapsedBound,
    initialAnchor = initialAnchor,
)
