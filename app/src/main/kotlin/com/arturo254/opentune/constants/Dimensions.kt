/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.arturo254.opentune.constants

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

const val CONTENT_TYPE_HEADER = 0
const val CONTENT_TYPE_LIST = 1
const val CONTENT_TYPE_SONG = 2
const val CONTENT_TYPE_ARTIST = 3
const val CONTENT_TYPE_ALBUM = 4
const val CONTENT_TYPE_PLAYLIST = 5

val FloatingToolbarHeight = 72.dp
val SlimFloatingToolbarHeight = 64.dp
val FloatingToolbarHorizontalPadding = 16.dp
val FloatingToolbarBottomPadding = 12.dp
val NavigationBarHeight = FloatingToolbarHeight
val SlimNavBarHeight = SlimFloatingToolbarHeight
val MiniPlayerHeight = 64.dp
val MiniPlayerBottomSpacing = 8.dp // Space between MiniPlayer and NavigationBar
val QueuePeekHeight = 64.dp
val AppBarHeight = 64.dp

val ListItemHeight = 72.dp
val SuggestionItemHeight = 60.dp
val SearchFilterHeight = 50.dp
val ListThumbnailSize = 54.dp
val SmallGridThumbnailHeight = 116.dp
val GridThumbnailHeight = 148.dp
val AlbumThumbnailSize = 156.dp

val ThumbnailCornerRadius = 14.dp
val GridThumbnailCornerRadius = 18.dp

val PlayerHorizontalPadding = 32.dp

val NavigationBarAnimationSpec = spring<Dp>(
    dampingRatio = 0.86f,
    stiffness = 520f,
)

val BottomSheetAnimationSpec = spring<Dp>(
    dampingRatio = 0.88f,
    stiffness = 430f,
)

val BottomSheetSoftAnimationSpec = spring<Dp>(
	dampingRatio = Spring.DampingRatioNoBouncy,
	stiffness = Spring.StiffnessLow
)
