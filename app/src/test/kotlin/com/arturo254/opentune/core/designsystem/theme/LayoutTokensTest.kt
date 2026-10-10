package com.arturo254.opentune.core.designsystem.theme

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.constants.AlbumThumbnailSize
import com.arturo254.opentune.constants.AppBarHeight
import com.arturo254.opentune.constants.FloatingToolbarHeight
import com.arturo254.opentune.constants.GridThumbnailHeight
import com.arturo254.opentune.constants.ListItemHeight
import com.arturo254.opentune.constants.ListThumbnailSize
import com.arturo254.opentune.constants.MiniPlayerHeight
import com.arturo254.opentune.constants.PlayerHorizontalPadding
import com.arturo254.opentune.constants.SlimFloatingToolbarHeight
import org.junit.Assert.assertEquals
import org.junit.Test

class LayoutTokensTest {
    @Test
    fun sharedDimensionsPreserveLegacySizes() {
        val sizes = DefaultOpenTuneSizeTokens

        assertEquals(72.dp, sizes.floatingToolbarHeight)
        assertEquals(FloatingToolbarHeight, sizes.floatingToolbarHeight)
        assertEquals(SlimFloatingToolbarHeight, sizes.slimFloatingToolbarHeight)
        assertEquals(MiniPlayerHeight, sizes.miniPlayerHeight)
        assertEquals(AppBarHeight, sizes.appBarHeight)
        assertEquals(ListItemHeight, sizes.listItemHeight)
        assertEquals(ListThumbnailSize, sizes.listThumbnailSize)
        assertEquals(GridThumbnailHeight, sizes.gridThumbnailHeight)
        assertEquals(AlbumThumbnailSize, sizes.albumThumbnailSize)
        assertEquals(PlayerHorizontalPadding, sizes.playerHorizontalPadding)
    }

    @Test
    fun universalSearchKeepsItsExistingSpacingAndDimensions() {
        val spacing = DefaultOpenTuneSpacingTokens
        val sizes = DefaultOpenTuneSizeTokens

        assertEquals(12.dp, spacing.md)
        assertEquals(8.dp, spacing.sm)
        assertEquals(128.dp, spacing.searchBottomInset)
        assertEquals(10.dp, spacing.mediaRowPadding)
        assertEquals(14.dp, spacing.mediaRowArtworkGap)
        assertEquals(3.dp, spacing.mediaRowTextGap)
        assertEquals(8.dp, spacing.badgeHorizontalPadding)
        assertEquals(5.dp, spacing.badgeVerticalPadding)
        assertEquals(60.dp, sizes.searchRowArtwork)
        assertEquals(36.dp, sizes.searchVersionsButton)
        assertEquals(21.dp, sizes.searchVersionsIcon)
    }

    @Test
    fun dpUsesDeviceDensityOnlyOnceAndIgnoresFontScale() {
        val artwork = DefaultOpenTuneSizeTokens.searchRowArtwork
        val normal = with(Density(2f, 1f)) { artwork.toPx() }
        val largeFonts = with(Density(2f, 1.5f)) { artwork.toPx() }
        val higherDensity = with(Density(3f, 1.5f)) { artwork.toPx() }

        assertEquals(120f, normal, 0.001f)
        assertEquals(normal, largeFonts, 0.001f)
        assertEquals(180f, higherDensity, 0.001f)
        assertEquals(60.dp, artwork)
    }
}
