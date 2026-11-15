package com.arturo254.opentune.ui.original.adapters
import androidx.datastore.preferences.core.stringPreferencesKey
/** Original editor background must not overwrite the Galax background enum under playerBackgroundStyle. */
val OriginalClassicBackgroundStyleKey = stringPreferencesKey("originalPlayerBackgroundStyle")

/** New return IDs retain the family; old plain style names/aliases remain readable. */
internal fun resolveOriginalVideoReturn(stored: String): OriginalStyleChoice? {
 val direct = OriginalStyleCatalog.choices.firstOrNull { it.id == stored }
 val old = com.arturo254.opentune.ui.original.fullconstants.PlayerStyle.entries.firstOrNull { it.name == stored }
  ?: com.arturo254.opentune.ui.original.fullconstants.PlayerStyle.fromStored(stored)
 val choice = direct ?: old?.let { OriginalStyleChoice(mochy = it) }
 return choice?.takeIf { it in OriginalStyleCatalog.choices && it.mochy != com.arturo254.opentune.ui.original.fullconstants.PlayerStyle.VIDEO }
}
