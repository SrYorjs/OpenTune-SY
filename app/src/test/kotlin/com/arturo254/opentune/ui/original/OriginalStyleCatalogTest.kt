package com.arturo254.opentune.ui.original
import androidx.datastore.preferences.core.*
import com.arturo254.opentune.constants.*
import com.arturo254.opentune.ui.original.fullconstants.PlayerStyle
import com.arturo254.opentune.ui.original.fullconstants.PlayerStyleKey
import com.arturo254.opentune.ui.original.adapters.*
import org.junit.Assert.*
import org.junit.Test
class OriginalStyleCatalogTest {
 @Test fun selectorContainsOnlyOriginalStylesAndEveryColumnIsReachableOnce() {
  assertTrue(OriginalStyleCatalog.choices.isNotEmpty())
  assertTrue(OriginalStyleCatalog.choices.all { it.id.startsWith("mochy:") })
  assertFalse(OriginalStyleCatalog.choices.any { it.selectorName.matches(Regex("V[0-9]+")) })
  assertFalse(OriginalStyleCatalog.choices.any { it.mochy == PlayerStyle.MODULE })
  assertEquals(OriginalStyleCatalog.choices.indices.toList().sorted(), OriginalStyleCatalog.columns.flatten().sorted())
 }
 @Test fun everyRemovedThemeMigratesToFlagshipAndKeepsRollbackAndLiquidValues() {
  PlayerDesignStyle.entries.forEach { legacy -> listOf(null, "galax").forEach { family ->
   val p = mutablePreferencesOf(PlayerDesignStyleKey to legacy.name, PlayerStyleKey to "TIMELINE",
    GalaxLiquidVariantKey to "CRYSTAL", GalaxLiquidRefractionAmountKey to 0.72f)
   family?.let { p[OriginalStyleCatalog.familyKey] = it }
   assertEquals(OriginalStyleCatalog.defaultChoice, OriginalStyleCatalog.read(p))
   OriginalStyleCatalog.migrate(p)
   assertEquals(PlayerStyle.FLAGSHIP, OriginalStyleCatalog.read(p).mochy)
   assertEquals("FLAGSHIP", p[PlayerStyleKey])
   assertEquals("mochy", p[OriginalStyleCatalog.familyKey])
   assertEquals(legacy.name, p[PlayerDesignStyleKey])
   assertEquals("CRYSTAL", p[GalaxLiquidVariantKey])
   assertEquals(0.72f, p[GalaxLiquidRefractionAmountKey])
   val once = p.asMap().toMap(); OriginalStyleCatalog.migrate(p); assertEquals(once,p.asMap())
  } }
 }
 @Test fun originalSelectionsRoundTripAndMigrationNeverResetsThem() {
  OriginalStyleCatalog.choices.forEach { choice ->
   val p = mutablePreferencesOf(PlayerDesignStyleKey to "V17")
   OriginalStyleCatalog.select(p,choice); OriginalStyleCatalog.migrate(p)
   assertEquals(choice,OriginalStyleCatalog.read(p)); assertEquals("V17",p[PlayerDesignStyleKey])
  }
 }
 @Test fun unknownOrUnsupportedValuesSafelyUseDefault() {
  listOf("MODULE","future-style").forEach { value ->
   val p=mutablePreferencesOf(PlayerStyleKey to value,OriginalStyleCatalog.familyKey to "mochy")
   assertEquals(OriginalStyleCatalog.defaultChoice,OriginalStyleCatalog.read(p))
   OriginalStyleCatalog.migrate(p); assertEquals("FLAGSHIP",p[PlayerStyleKey])
  }
 }
 @Test fun preferenceNamespacesAndUnavailableActionsRemainProtected() {
  assertEquals("playerDesignStyle",PlayerDesignStyleKey.name); assertEquals("playerStyle",PlayerStyleKey.name)
  assertNotEquals(PlayerDesignStyleKey,PlayerStyleKey)
  assertFalse(C2Capabilities.actionAvailable(com.arturo254.opentune.ui.original.player.studio.PlayerMenuAction.OUTPUT))
  assertFalse(C2Capabilities.actionAvailable(com.arturo254.opentune.ui.original.player.studio.PlayerMenuAction.TRANSLATE_LYRICS))
  assertTrue(C2Capabilities.actionAvailable(com.arturo254.opentune.ui.original.player.studio.PlayerMenuAction.LIKE))
 }
}
