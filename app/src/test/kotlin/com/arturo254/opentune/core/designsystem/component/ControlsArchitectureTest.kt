package com.arturo254.opentune.core.designsystem.component

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlsArchitectureTest {
    private fun source(path: String): String {
        val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
            .firstOrNull(File::isDirectory) ?: error("Android source tree missing")
        return File(root, "com/arturo254/opentune/$path").readText()
    }

    @Test
    fun controlsAreStateHoistedAndRetainAccessibility() {
        val c = source("core/designsystem/component/ControlComponents.kt")
        listOf("fun OpenTuneButton(", "fun OpenTunePillChip(", "fun OpenTuneSlider(",
            "fun OpenTuneSwitch(", "fun OpenTuneIcon(", "fun OpenTuneIconButton(")
            .forEach { assertTrue(c.contains(it)) }
        assertTrue(c.contains("onValueChange: (Float) -> Unit"))
        assertTrue(c.contains("onCheckedChange: ((Boolean) -> Unit)?"))
        assertTrue(c.contains("contentDescription: String"))
        assertTrue(!c.contains("rememberPreference("))
        assertTrue(!c.contains("MusicService"))
    }

    @Test
    fun existingChipsDelegateToNewPillWithoutChangingBehavior() {
        val c = source("ui/component/ChipsRow.kt")
        assertTrue(c.contains("OpenTunePillChip("))
        assertTrue(c.contains("onValueUpdate(value)"))
        assertTrue(c.contains("targetState = selected"))
    }
}
