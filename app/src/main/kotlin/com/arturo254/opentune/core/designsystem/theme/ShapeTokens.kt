package com.arturo254.opentune.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem

/**
 * Canonical fixed radii used by existing UI. Keep these stable until the
 * explicit redesign and migration stages (3.9 and 3.15).
 */
object OpenTuneShapeRadii {
    val listArtwork: Dp = 14.dp
    val gridArtwork: Dp = 18.dp
    val searchArtwork: Dp = 17.dp
    val mediaCard: Dp = 22.dp
    val badge: Dp = 12.dp
    val sheet: Dp = 30.dp
    val glassControlCluster: Dp = 30.dp
    val glassCard: Dp = 24.dp
}

/**
 * The artwork preference remains in dp, with the same 0..45 range as the
 * existing settings UI. The pure helper can also render a slider preview.
 */
fun artworkShapeFor(radiusDp: Float): RoundedCornerShape {
    val safeRadius = radiusDp.takeIf { it.isFinite() }?.coerceIn(0f, 45f) ?: 16f
    return RoundedCornerShape(safeRadius.dp)
}

/**
 * Material 3 is the source of truth for generic components. OpenTune provides
 * named shapes for its musical and glass UI, and one user-controlled artwork
 * shape; it does not alter MaterialTheme.shapes globally.
 */
@Immutable
data class OpenTuneShapeTokens(
    val extraSmall: Shape,
    val small: Shape,
    val medium: Shape,
    val large: Shape,
    val extraLarge: Shape,
    val listArtwork: RoundedCornerShape,
    val gridArtwork: RoundedCornerShape,
    val searchArtwork: RoundedCornerShape,
    val artwork: RoundedCornerShape,
    val mediaCard: RoundedCornerShape,
    val badge: RoundedCornerShape,
    val pill: RoundedCornerShape,
    val sheet: RoundedCornerShape,
    val glassControlCluster: RoundedCornerShape,
    val glassCard: RoundedCornerShape,
)

internal fun createOpenTuneShapeTokens(
    materialShapes: Shapes,
    artworkRadiusDp: Float = 16f,
): OpenTuneShapeTokens =
    OpenTuneShapeTokens(
        extraSmall = materialShapes.extraSmall,
        small = materialShapes.small,
        medium = materialShapes.medium,
        large = materialShapes.large,
        extraLarge = materialShapes.extraLarge,
        listArtwork = RoundedCornerShape(OpenTuneShapeRadii.listArtwork),
        gridArtwork = RoundedCornerShape(OpenTuneShapeRadii.gridArtwork),
        searchArtwork = RoundedCornerShape(OpenTuneShapeRadii.searchArtwork),
        artwork = artworkShapeFor(artworkRadiusDp),
        mediaCard = RoundedCornerShape(OpenTuneShapeRadii.mediaCard),
        badge = RoundedCornerShape(OpenTuneShapeRadii.badge),
        pill = RoundedCornerShape(50),
        sheet = RoundedCornerShape(OpenTuneShapeRadii.sheet),
        glassControlCluster = RoundedCornerShape(OpenTuneShapeRadii.glassControlCluster),
        glassCard = RoundedCornerShape(OpenTuneShapeRadii.glassCard),
    )

internal val LocalOpenTuneShapeTokens =
    staticCompositionLocalOf<OpenTuneShapeTokens> {
        error("OpenTune shapes are not installed in this composition")
    }

val OpenTuneDesignSystem.shapes: OpenTuneShapeTokens
    @Composable
    @ReadOnlyComposable
    get() = LocalOpenTuneShapeTokens.current
