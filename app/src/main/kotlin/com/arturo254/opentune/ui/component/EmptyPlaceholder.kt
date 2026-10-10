/*
 * OpenTune legacy API. The rendering implementation lives in the
 * canonical Design System common states; do not add a second visual tree.
 */
package com.arturo254.opentune.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arturo254.opentune.core.designsystem.component.OpenTuneEmptyState

/**
 * Compatibility adapter. New screens must use OpenTuneEmptyState directly.
 * Existing import paths remain valid until all feature migrations are complete.
 */
@Composable
fun EmptyPlaceholder(
    @DrawableRes icon: Int,
    text: String,
    modifier: Modifier = Modifier,
) {
    OpenTuneEmptyState(
        icon = icon,
        text = text,
        modifier = modifier,
    )
}
