package com.arturo254.opentune.ui.visual.ipod

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

enum class IpodTheme { SILVER, BLACK, U2, TRUE_DARK }

enum class IpodWheelColor { THEME, WHITE, BLACK, RED }

val IpodThemeKey = stringPreferencesKey("ipodTheme")
val IpodWheelColorKey = stringPreferencesKey("ipodWheelColor")
val IpodTallScreenKey = booleanPreferencesKey("ipodTallScreen")
val IpodShadowKey = booleanPreferencesKey("ipodShadow")
