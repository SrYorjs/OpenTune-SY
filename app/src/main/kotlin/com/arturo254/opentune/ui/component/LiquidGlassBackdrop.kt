package com.arturo254.opentune.ui.component

import androidx.compose.runtime.staticCompositionLocalOf
import com.skydoves.cloudy.Sky

val LocalSky = staticCompositionLocalOf<Sky?> { null }

val LocalEnableLiquidGlass = staticCompositionLocalOf { false }