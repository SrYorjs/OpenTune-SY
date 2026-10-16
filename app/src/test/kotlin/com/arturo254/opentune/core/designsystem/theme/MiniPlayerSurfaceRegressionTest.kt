package com.arturo254.opentune.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.unit.dp
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniPlayerSurfaceRegressionTest {
    @Test
    fun miniPlayerCornerAndOpaqueColorAreCanonical() {
        val shapes = createOpenTuneShapeTokens(Shapes())
        val surfaces = createOpenTuneSurfaceTokens(darkColorScheme())

        assertEquals(RoundedCornerShape(32.dp), shapes.miniPlayer)
        assertEquals(0.82f, surfaces.opacity.miniPlayerPureBlack, 0f)
        assertEquals(0.88f, surfaces.opacity.miniPlayerOpaque, 0f)
        assertEquals(1.dp, surfaces.borders.miniPlayerOpaqueWidth)
    }

    @Test
    fun nonGlassBackgroundIsClippedBeforeItIsDrawn() {
        val sourceRoot =
            listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
                .firstOrNull(File::isDirectory)
                ?: error("Android source tree is missing")
        val file = File(sourceRoot, "com/arturo254/opentune/ui/player/MiniPlayer.kt")
        val content = file.readText()

        val clip = content.indexOf(".clip(miniPlayerShape)")
        val background = content.indexOf(".background(", clip)
        val glass = content.indexOf(".playerVisualGlassEffect(", background)
        val outline = content.indexOf("if (!glassEnabled)", glass)

        assertTrue("Mini player must clip before its background", clip >= 0 && background > clip)
        assertTrue("Canonical Glass effect must not control the base clip", glass > background)
        assertTrue("Opaque fallback must have a border", outline > glass)
    }
}
