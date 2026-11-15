package com.arturo254.opentune.ui.original
import androidx.datastore.preferences.core.*
import com.arturo254.opentune.constants.PlayerDesignStyle
import com.arturo254.opentune.constants.PlayerDesignStyleKey
import com.arturo254.opentune.ui.original.fullconstants.PlayerStyle
import com.arturo254.opentune.ui.original.fullconstants.PlayerStyleKey
import com.arturo254.opentune.ui.original.adapters.*
import org.junit.Assert.*
import org.junit.Test
class OriginalStyleCatalogTest {
 private val expected = (1..17).map { "V" + it }
 @Test fun allSeventeenRetainTheirSerializedNamesAndShareTheSelector() {
  assertEquals(expected, PlayerDesignStyle.entries.map { it.name })
  assertEquals(expected, OriginalStyleCatalog.galax.map { it.galax!!.name })
  assertEquals(17, OriginalStyleCatalog.galax.size)
  assertEquals(OriginalStyleCatalog.choices.indices.toSet(), OriginalStyleCatalog.columns.flatten().toSet())
  assertEquals(OriginalStyleCatalog.choices.size, OriginalStyleCatalog.columns.flatten().size)
  assertEquals(OriginalStyleCatalog.choices.size, OriginalStyleCatalog.choices.map { it.id }.toSet().size)
  assertTrue(OriginalStyleCatalog.mochy.isNotEmpty())
 }
 @Test fun preferenceKeysAreUnchangedAndSeparate() {
  assertEquals("playerDesignStyle", PlayerDesignStyleKey.name)
  assertEquals("playerStyle", PlayerStyleKey.name)
  assertNotEquals(PlayerStyleKey, PlayerDesignStyleKey)
 }
 @Test fun everyLegacySavedThemeWinsWithoutMigrationOrWrites() {
  expected.forEach { name ->
   val prefs = mutablePreferencesOf(PlayerDesignStyleKey to name, PlayerStyleKey to "TIMELINE")
   val before = prefs.asMap().toMap()
   assertEquals(name, OriginalStyleCatalog.read(prefs).galax!!.name)
   assertEquals(before, prefs.asMap())
  }
 }
 @Test fun everyThemeRoundTripsWithoutTouchingMochyOrLiquidPreferences() {
  val liquidVariant = com.arturo254.opentune.constants.GalaxLiquidVariantKey
  val liquidTuning = com.arturo254.opentune.constants.GalaxLiquidRefractionAmountKey
  OriginalStyleCatalog.galax.forEach { choice ->
   val prefs = mutablePreferencesOf(PlayerStyleKey to "CLASSIC", liquidVariant to "custom-existing", liquidTuning to 0.72f)
   val untouched = prefs.asMap().toMap()
   OriginalStyleCatalog.select(prefs, choice)
   assertEquals(choice, OriginalStyleCatalog.read(prefs))
   untouched.forEach { (key, value) -> assertEquals(value, prefs.asMap()[key]) }
   assertEquals(choice.galax!!.name, prefs[PlayerDesignStyleKey])
  }
 }
 @Test fun mochySelectionDoesNotOverwriteAnySavedGalaxTheme() {
  expected.forEach { name -> OriginalStyleCatalog.mochy.forEach { choice ->
   val prefs = mutablePreferencesOf(PlayerDesignStyleKey to name)
   OriginalStyleCatalog.select(prefs, choice)
   assertEquals(name, prefs[PlayerDesignStyleKey])
   assertEquals(choice, OriginalStyleCatalog.read(prefs))
   prefs[OriginalStyleCatalog.familyKey] = "galax"
   assertEquals(name, OriginalStyleCatalog.read(prefs).galax!!.name)
  }}
 }
 @Test fun unsupportedModuleNeverReplacesSavedGalaxTheme() {
  val prefs = mutablePreferencesOf(PlayerDesignStyleKey to "V17", PlayerStyleKey to "MODULE", OriginalStyleCatalog.familyKey to "mochy")
  assertEquals(PlayerDesignStyle.V17, OriginalStyleCatalog.read(prefs).galax)
  assertFalse(OriginalStyleCatalog.choices.any { it.mochy == PlayerStyle.MODULE })
 }
 @Test fun unavailableActionsCannotBeAssignedInStudio() {
  assertFalse(C2Capabilities.actionAvailable(com.arturo254.opentune.ui.original.player.studio.PlayerMenuAction.OUTPUT))
  assertFalse(C2Capabilities.actionAvailable(com.arturo254.opentune.ui.original.player.studio.PlayerMenuAction.TRANSLATE_LYRICS))
  assertTrue(C2Capabilities.actionAvailable(com.arturo254.opentune.ui.original.player.studio.PlayerMenuAction.LIKE))
 }
 @Test fun unknownValueUsesExistingDefaultAndIsNotRewritten() {
  val prefs = mutablePreferencesOf(PlayerDesignStyleKey to "future-style")
  assertEquals(PlayerDesignStyle.V8, OriginalStyleCatalog.read(prefs).galax)
  assertEquals("future-style", prefs[PlayerDesignStyleKey])
 }
}
