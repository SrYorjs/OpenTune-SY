package com.arturo254.opentune.ui.player.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Composable
fun GalaxPlayerLandscapeScene(
    accent: Color,
    isPlaying: Boolean,
    bottomInset: Dp,
    artwork: @Composable BoxScope.() -> Unit,
    controls: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .padding(bottom = bottomInset),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.weight(1f),
            content = artwork,
        )
        Column(
            modifier = Modifier.weight(1f)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            controls()
            Spacer(Modifier.weight(1f))
        }
    }
}
