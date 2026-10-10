package com.arturo254.opentune.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Stable entry point for OpenTune's visual system.
 *
 * The first generation intentionally contains no visual tokens. Colors,
 * typography, spacing, shapes, motion and glass are introduced in later 3.x
 * steps without forcing feature screens to depend on their implementation
 * packages directly.
 */
@Immutable
data class DesignSystemEnvironment(
    val schemaVersion: Int,
)

internal val LocalDesignSystemEnvironment =
    staticCompositionLocalOf<DesignSystemEnvironment> {
        error("OpenTune Design System is not installed in this composition")
    }

object OpenTuneDesignSystem {
    const val SchemaVersion: Int = 1

    val environment: DesignSystemEnvironment
        @Composable
        @ReadOnlyComposable
        get() = LocalDesignSystemEnvironment.current
}
