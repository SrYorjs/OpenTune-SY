package com.arturo254.opentune.ui.original.adapters
import androidx.datastore.preferences.core.*
import com.arturo254.opentune.ui.original.fullconstants.PlayerStyle
import com.arturo254.opentune.ui.original.fullconstants.PlayerStyleKey
/** Only original styles; the old playerDesignStyle preference is retained solely for rollback. */
data class OriginalStyleChoice(val mochy: PlayerStyle? = PlayerStyle.FLAGSHIP) {
 init { require(mochy != null) }
 val id: String get() = "mochy:" + mochy!!.name
 val selectorName: String get() = mochy!!.selectorName
 val editable: Boolean get() = mochy!!.editable
}
object OriginalStyleCatalog {
 val familyKey = stringPreferencesKey("originalPlayerFamily")
 val defaultChoice = OriginalStyleChoice(mochy = PlayerStyle.FLAGSHIP)
 val choices = PlayerStyle.entries.filter { it != PlayerStyle.MODULE }.map { OriginalStyleChoice(mochy = it) }
 val columns = PlayerStyle.selectorColumns.map { column -> column.mapNotNull { style -> choices.indexOfFirst { it.mochy == style }.takeIf { it >= 0 } } }.filter { it.isNotEmpty() }
 fun read(preferences: Preferences): OriginalStyleChoice {
  if (preferences[familyKey] != "mochy") return defaultChoice
  val stored = preferences[PlayerStyleKey]
  val style = PlayerStyle.entries.firstOrNull { it.name == stored } ?: PlayerStyle.fromStored(stored)
  return choices.firstOrNull { it.mochy == style } ?: defaultChoice
 }
 /** Idempotent migration inside the existing DataStore; never rewrites rollback or liquid keys. */
 fun migrate(preferences: MutablePreferences) { select(preferences, read(preferences)) }
 fun select(preferences: MutablePreferences, choice: OriginalStyleChoice) {
  require(choice in choices)
  preferences[PlayerStyleKey] = choice.mochy!!.name
  preferences[familyKey] = "mochy"
 }
}
