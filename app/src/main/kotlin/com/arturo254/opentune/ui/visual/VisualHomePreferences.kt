package com.arturo254.opentune.ui.visual

import androidx.datastore.preferences.core.stringPreferencesKey

enum class HomeStyle {
    MOCHYY,
    LISTEN_NOW,
    HOP_IN,
}

val HomeStyleKey = stringPreferencesKey("homeStyle")
