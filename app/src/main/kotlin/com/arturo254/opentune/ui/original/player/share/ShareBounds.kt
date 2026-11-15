package com.arturo254.opentune.ui.original.player.share
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.geometry.Rect
internal fun LayoutCoordinates.fullBoundsInWindow(): Rect {
 val root = findRootCoordinates()
 return root.localBoundingBoxOf(this, clipBounds = false).translate(root.positionInWindow())
}
