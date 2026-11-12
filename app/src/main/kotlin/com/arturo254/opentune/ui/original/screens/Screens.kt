// Screens.kt

package com.arturo254.opentune.ui.original.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.arturo254.opentune.R

@Immutable
sealed class Screens(
    @StringRes val titleId: Int,
    val iconIdInactive: Int,
    val iconIdActive: Int,
    val route: String,
) {
    object Home : Screens(
        titleId = R.string.home,
        iconIdInactive = com.arturo254.opentune.foundation.R.raw.original_home,
        iconIdActive = com.arturo254.opentune.foundation.R.raw.original_home_filled,
        route = "home"
    )

    object Search : Screens(
        titleId = R.string.search,
        iconIdInactive = com.arturo254.opentune.foundation.R.raw.original_search,
        iconIdActive = com.arturo254.opentune.foundation.R.raw.original_search_filled,
        route = "search_input"
    )

    object Library : Screens(
        titleId = com.arturo254.opentune.foundation.R.string.original_your_library,
        iconIdInactive = com.arturo254.opentune.foundation.R.raw.original_lib,
        iconIdActive = com.arturo254.opentune.foundation.R.raw.original_lib_fill,
        route = "library"
    )

    object Settings : Screens(
        titleId = R.string.settings,
        iconIdInactive = R.drawable.settings,
        iconIdActive = R.drawable.settings,
        route = "settings"
    )

    companion object {
        val MainScreens = listOf(Home, Search, Library, Settings)
    }
}

enum class NavigationTab {
    HOME, SEARCH
}

enum class DarkMode {
    AUTO, ON, OFF
}

enum class LyricsPosition {
    LEFT, CENTER, RIGHT
}

enum class OptionStats {
    WEEKS, MONTHS, YEARS, CONTINUOUS
}
