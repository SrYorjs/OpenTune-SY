package com.arturo254.opentune.ui.original.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

val DisableBlurKey = com.arturo254.opentune.constants.DisableBlurKey
val BlurQualityKey = stringPreferencesKey("blurQuality")
val BlurStrengthPxKey = floatPreferencesKey("blurStrengthPx")
val EnableProgressiveBlurKey = booleanPreferencesKey("enableProgressiveBlur")
val EnableElementBlurAnimatorKey = booleanPreferencesKey("enableElementBlurAnimator")
val ElementBlurKey = booleanPreferencesKey("elementBlur")
val AnimationSpeedKey = floatPreferencesKey("animationSpeed")
enum class BlurQuality { LOW, HIGH }
