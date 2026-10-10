package com.arturo254.opentune.core.designsystem

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignSystemArchitectureTest {
    private val mainRoot: File =
        listOf(
            File("src/main/kotlin"),
            File("app/src/main/kotlin"),
        ).firstOrNull(File::isDirectory)
            ?: error("Unable to locate Android main source root")

    private val designSystemRoot =
        File(
            mainRoot,
            "com/arturo254/opentune/core/designsystem",
        )

    @Test
    fun packageSkeletonExists() {
        assertTrue(designSystemRoot.isDirectory)
        assertTrue(File(designSystemRoot, "theme").isDirectory)
        assertTrue(File(designSystemRoot, "glass").isDirectory)
        assertTrue(File(designSystemRoot, "component").isDirectory)
        assertTrue(File(designSystemRoot, "preview").isDirectory)
    }

    @Test
    fun appThemeInstallsDesignSystemBoundary() {
        val theme =
            File(
                mainRoot,
                "com/arturo254/opentune/ui/theme/Theme.kt",
            ).readText()

        assertTrue(theme.contains("ProvideOpenTuneDesignSystem"))
        assertTrue(theme.contains("MaterialExpressiveTheme"))
    }

    @Test
    fun designSystemDoesNotDependOnFeatureState() {
        val forbiddenImports =
            listOf(
                "com.arturo254.opentune.viewmodels.",
                "com.arturo254.opentune.sources.",
                "com.arturo254.opentune.playback.",
                "com.arturo254.opentune.db.",
                "com.arturo254.opentune.innertube.",
            )

        designSystemRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { source ->
                val content = source.readText()
                forbiddenImports.forEach { forbidden ->
                    assertFalse(
                        source.relativeTo(mainRoot).path +
                            " must remain feature-agnostic: " +
                            forbidden,
                        content.contains("import $forbidden"),
                    )
                }
            }
    }

    @Test
    fun structureStepDoesNotMoveLegacyVisualImplementationYet() {
        assertTrue(
            File(
                mainRoot,
                "com/arturo254/opentune/ui/component/LiquidGlassBackdrop.kt",
            ).isFile,
        )
        assertTrue(
            File(
                mainRoot,
                "com/arturo254/opentune/ui/theme/Theme.kt",
            ).isFile,
        )
    }
}
