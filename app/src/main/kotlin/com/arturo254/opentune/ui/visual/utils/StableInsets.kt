package com.arturo254.opentune.ui.visual.utils

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalLayoutApi::class)
val WindowInsets.Companion.stableSystemBars: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = WindowInsets.systemBarsIgnoringVisibility

@OptIn(ExperimentalLayoutApi::class)
val WindowInsets.Companion.stableStatusBars: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = WindowInsets.statusBarsIgnoringVisibility

@Composable
fun Modifier.stableStatusBarsPadding(): Modifier = windowInsetsPadding(WindowInsets.stableStatusBars)
