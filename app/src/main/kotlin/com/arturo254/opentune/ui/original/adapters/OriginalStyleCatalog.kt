package com.arturo254.opentune.ui.original.adapters
import androidx.datastore.preferences.core.*
import com.arturo254.opentune.constants.PlayerDesignStyle
import com.arturo254.opentune.constants.PlayerDesignStyleKey
import com.arturo254.opentune.ui.player.preferences.PlayerPresentationDefaults
import com.arturo254.opentune.ui.original.fullconstants.PlayerStyle
import com.arturo254.opentune.ui.original.fullconstants.PlayerStyleKey
/** Two distinct persisted namespaces. Never serialize a GalaxOT theme into playerStyle. */
data class OriginalStyleChoice(val galax: PlayerDesignStyle? = null, val mochy: PlayerStyle? = null) {
 init { require((galax == null) != (mochy == null)) }
 val id: String get() = galax?.let { "galax:" + it.name } ?: ("mochy:" + mochy!!.name)
 val selectorName: String get() = galax?.name ?: mochy!!.selectorName
 val editable: Boolean get() = mochy?.editable == true
}
object OriginalStyleCatalog {
 val familyKey = stringPreferencesKey("originalPlayerFamily")
 val galax = PlayerDesignStyle.entries.map { OriginalStyleChoice(galax = it) }
 val mochy = PlayerStyle.entries.filter { it != PlayerStyle.MODULE }.map { OriginalStyleChoice(mochy = it) }
 val choices = galax + mochy
 val columns: List<List<Int>> = galax.map { listOf(choices.indexOf(it)) } + PlayerStyle.selectorColumns.map { column -> column.mapNotNull { style -> choices.indexOfFirst { it.mochy == style }.takeIf { it >= 0 } } }
 fun read(preferences: Preferences): OriginalStyleChoice {
  if (preferences[familyKey] == "mochy") {
   val stored = preferences[PlayerStyleKey]
   val style = PlayerStyle.entries.firstOrNull { it.name == stored } ?: PlayerStyle.fromStored(stored)
   if (style != null && style != PlayerStyle.MODULE) return OriginalStyleChoice(mochy = style)
  }
  val style = PlayerDesignStyle.entries.firstOrNull { it.name == preferences[PlayerDesignStyleKey] } ?: PlayerPresentationDefaults.designStyle
  return OriginalStyleChoice(galax = style)
 }
 fun select(preferences: MutablePreferences, choice: OriginalStyleChoice) {
  require(choice in choices)
  choice.galax?.let { preferences[PlayerDesignStyleKey] = it.name; preferences[familyKey] = "galax" }
  choice.mochy?.let { preferences[PlayerStyleKey] = it.name; preferences[familyKey] = "mochy" }
 }
}
