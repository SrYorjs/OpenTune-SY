package com.arturo254.opentune.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.constants.GridThumbnailCornerRadius
import com.arturo254.opentune.constants.ThumbnailCornerRadius
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ShapeTokensTest {
    @Test
    fun musicAndGlassShapesMatchCurrentVisuals() {
        val t = createOpenTuneShapeTokens(Shapes(), artworkRadiusDp = 16f)

        assertEquals(RoundedCornerShape(14.dp), t.listArtwork)
        assertEquals(RoundedCornerShape(18.dp), t.gridArtwork)
        assertEquals(RoundedCornerShape(17.dp), t.searchArtwork)
        assertEquals(RoundedCornerShape(22.dp), t.mediaCard)
        assertEquals(RoundedCornerShape(12.dp), t.badge)
        assertEquals(RoundedCornerShape(50), t.pill)
        assertEquals(RoundedCornerShape(30.dp), t.sheet)
        assertEquals(RoundedCornerShape(30.dp), t.glassControlCluster)
        assertEquals(RoundedCornerShape(24.dp), t.glassCard)
        assertEquals(ThumbnailCornerRadius, OpenTuneShapeRadii.listArtwork)
        assertEquals(GridThumbnailCornerRadius, OpenTuneShapeRadii.gridArtwork)
    }

    @Test
    fun namedMaterialShapesFollowProvidedMaterialScheme() {
        val material = Shapes(
            small = RoundedCornerShape(9.dp),
            medium = RoundedCornerShape(15.dp),
            large = RoundedCornerShape(26.dp),
        )
        val result = createOpenTuneShapeTokens(material)

        assertEquals(material.extraSmall, result.extraSmall)
        assertEquals(material.small, result.small)
        assertEquals(material.medium, result.medium)
        assertEquals(material.large, result.large)
        assertEquals(material.extraLarge, result.extraLarge)
    }

    @Test
    fun artworkRadiusPreferenceAndLivePreviewRemainIndependentOfOtherShapes() {
        val material = Shapes()
        val small = createOpenTuneShapeTokens(material, 0f)
        val large = createOpenTuneShapeTokens(material, 40f)

        assertEquals(RoundedCornerShape(0.dp), small.artwork)
        assertEquals(RoundedCornerShape(40.dp), large.artwork)
        assertEquals(artworkShapeFor(24f), createOpenTuneShapeTokens(material, 24f).artwork)
        assertNotEquals(small.artwork, large.artwork)
        assertEquals(small.mediaCard, large.mediaCard)
        assertEquals(small.searchArtwork, large.searchArtwork)
    }

    @Test
    fun malformedRadiusCannotCrashShapeConstruction() {
        assertEquals(RoundedCornerShape(16.dp), artworkShapeFor(Float.NaN))
        assertEquals(RoundedCornerShape(16.dp), artworkShapeFor(Float.POSITIVE_INFINITY))
        assertEquals(RoundedCornerShape(0.dp), artworkShapeFor(-10f))
        assertEquals(RoundedCornerShape(45.dp), artworkShapeFor(200f))
    }
}
