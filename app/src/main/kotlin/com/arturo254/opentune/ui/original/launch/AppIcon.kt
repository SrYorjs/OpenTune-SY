package com.arturo254.opentune.ui.original.launch

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

/** Artwork contract for the original intro; launcher selection stays with the host app. */
data class AppIcon(
    @DrawableRes val foreground: Int,
    @ColorRes val background: Int?,
)
