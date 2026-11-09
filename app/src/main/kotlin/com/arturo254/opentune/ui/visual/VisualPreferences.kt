package com.arturo254.opentune.ui.visual

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

enum class BlurQuality { LOW, HIGH }

val BlurStrengthPxKey = floatPreferencesKey("blurStrengthPx")
val EnableProgressiveBlurKey = booleanPreferencesKey("enableProgressiveBlur")

enum class ChromeGlassStyle { FROSTED, MODULE }

val ChromeGlassStyleKey = stringPreferencesKey("chromeGlassStyle")

val LocalDetailAccentColor = staticCompositionLocalOf<MutableState<Color?>> {
    mutableStateOf(null)
}
