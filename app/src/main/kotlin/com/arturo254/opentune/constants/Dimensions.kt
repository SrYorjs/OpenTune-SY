/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.arturo254.opentune.constants

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.core.designsystem.theme.DefaultOpenTuneSizeTokens
import com.arturo254.opentune.core.designsystem.theme.DefaultOpenTuneMotionTokens
import com.arturo254.opentune.core.designsystem.theme.OpenTuneShapeRadii

const val CONTENT_TYPE_HEADER = 0
const val CONTENT_TYPE_LIST = 1
const val CONTENT_TYPE_SONG = 2
const val CONTENT_TYPE_ARTIST = 3
const val CONTENT_TYPE_ALBUM = 4
const val CONTENT_TYPE_PLAYLIST = 5

val FloatingToolbarHeight = DefaultOpenTuneSizeTokens.floatingToolbarHeight
val SlimFloatingToolbarHeight = DefaultOpenTuneSizeTokens.slimFloatingToolbarHeight
val FloatingToolbarHorizontalPadding = DefaultOpenTuneSizeTokens.floatingToolbarHorizontalPadding
val FloatingToolbarBottomPadding = DefaultOpenTuneSizeTokens.floatingToolbarBottomPadding
val NavigationBarHeight = FloatingToolbarHeight
val SlimNavBarHeight = SlimFloatingToolbarHeight
val MiniPlayerHeight = DefaultOpenTuneSizeTokens.miniPlayerHeight
val MiniPlayerBottomSpacing = DefaultOpenTuneSizeTokens.miniPlayerBottomSpacing // Space between MiniPlayer and NavigationBar
val QueuePeekHeight = DefaultOpenTuneSizeTokens.queuePeekHeight
val AppBarHeight = DefaultOpenTuneSizeTokens.appBarHeight

val ListItemHeight = DefaultOpenTuneSizeTokens.listItemHeight
val SuggestionItemHeight = DefaultOpenTuneSizeTokens.suggestionItemHeight
val SearchFilterHeight = DefaultOpenTuneSizeTokens.searchFilterHeight
val ListThumbnailSize = DefaultOpenTuneSizeTokens.listThumbnailSize
val SmallGridThumbnailHeight = DefaultOpenTuneSizeTokens.smallGridThumbnailHeight
val GridThumbnailHeight = DefaultOpenTuneSizeTokens.gridThumbnailHeight
val AlbumThumbnailSize = DefaultOpenTuneSizeTokens.albumThumbnailSize

val ThumbnailCornerRadius = OpenTuneShapeRadii.listArtwork
val GridThumbnailCornerRadius = OpenTuneShapeRadii.gridArtwork

val PlayerHorizontalPadding = DefaultOpenTuneSizeTokens.playerHorizontalPadding

val NavigationBarAnimationSpec = DefaultOpenTuneMotionTokens.navigationBar
val BottomSheetAnimationSpec = DefaultOpenTuneMotionTokens.bottomSheet
val BottomSheetSoftAnimationSpec = DefaultOpenTuneMotionTokens.bottomSheetSoft
