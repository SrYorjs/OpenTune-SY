package com.arturo254.opentune.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class GalaxSettingsArea {
    APPEARANCE,
    PLAYER,
    CONTENT,
    STORAGE,
    PRIVACY,
    INTEGRATIONS,
}

@Composable
fun GalaxSettingsSectionHero(
    title: String,
    description: String,
    area: GalaxSettingsArea,
    modifier: Modifier = Modifier,
) {
    AsterSettingsHero(
        title = title,
        description = description,
        section = when (area) {
            GalaxSettingsArea.APPEARANCE -> AsterSettingsSection.APPEARANCE
            GalaxSettingsArea.PLAYER -> AsterSettingsSection.PLAYER
            GalaxSettingsArea.CONTENT -> AsterSettingsSection.CONTENT
            GalaxSettingsArea.STORAGE -> AsterSettingsSection.STORAGE
            GalaxSettingsArea.PRIVACY -> AsterSettingsSection.PRIVACY
            GalaxSettingsArea.INTEGRATIONS -> AsterSettingsSection.INTEGRATIONS
        },
        modifier = modifier,
    )
}
