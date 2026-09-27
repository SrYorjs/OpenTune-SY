package com.arturo254.opentune.constants

import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Which typeface OpenTune should render across the entire app.
 *
 * Replaces the old boolean `UseSystemFontKey` (fuente del sistema: sí/no)
 * with a 3-way choice.
 */
enum class AppFontStyle {
    /** OpenTune's own default custom typeface. */
    OPEN_TUNE,

    /** The device's system font (Compose falls back to the platform default). */
    SYSTEM,

    /** Monocraft — monospaced pixel-style font, res/font/monocraft.ttf. */
    MONOCRAFT,
}

/**
 * DataStore key for the selected [AppFontStyle].
 *
 * NOTE: adjust this if your project's other enum preferences (e.g.
 * PlayerBackgroundStyleKey) declare their stringPreferencesKey with a
 * different helper/import — match that same pattern here so it's
 * consistent with the rest of constants/.
 */
val AppFontKey = stringPreferencesKey("appFont")