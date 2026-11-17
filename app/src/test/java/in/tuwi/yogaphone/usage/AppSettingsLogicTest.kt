package `in`.tuwi.yogaphone.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsLogicTest {
    @Test fun typeRoundTripsThroughClassifyFlags() {
        AppType.entries.forEach { type ->
            val (essential, distracting) = AppSettingsLogic.flags(type)
            assertEquals(type, AppSettingsLogic.typeOf(essential, distracting))
        }
        assertEquals(AppType.ESSENTIAL, AppSettingsLogic.typeOf(essential = true, distracting = true))
    }

    @Test fun movingTowardsEssentialIsLoosening() {
        assertTrue(AppSettingsLogic.isLoosening(AppType.DISTRACTING, AppType.ESSENTIAL))
        assertTrue(AppSettingsLogic.isLoosening(AppType.DISTRACTING, AppType.NORMAL))
        assertTrue(AppSettingsLogic.isLoosening(AppType.NORMAL, AppType.ESSENTIAL))
        assertFalse(AppSettingsLogic.isLoosening(AppType.ESSENTIAL, AppType.DISTRACTING))
        assertFalse(AppSettingsLogic.isLoosening(AppType.NORMAL, AppType.DISTRACTING))
        assertFalse(AppSettingsLogic.isLoosening(AppType.NORMAL, AppType.NORMAL))
    }

    @Test fun removingOrRaisingALimitIsLoosening() {
        assertTrue(AppSettingsLogic.limitIsLoosening(30, null))
        assertTrue(AppSettingsLogic.limitIsLoosening(30, 45))
        assertFalse(AppSettingsLogic.limitIsLoosening(30, 15))
        assertFalse(AppSettingsLogic.limitIsLoosening(30, 30))
        assertFalse(AppSettingsLogic.limitIsLoosening(null, 30))
        assertFalse(AppSettingsLogic.limitIsLoosening(null, null))
    }

    @Test fun summariesSayWhatYogaDoes() {
        assertEquals("Distracting · pause · 30 min a day", AppSettingsLogic.summary(AppType.DISTRACTING, true, 30))
        assertEquals("Distracting · no pause", AppSettingsLogic.summary(AppType.DISTRACTING, false, null))
        assertEquals("Normal · 45 min a day", AppSettingsLogic.summary(AppType.NORMAL, true, 45))
        assertEquals("Normal", AppSettingsLogic.summary(AppType.NORMAL, false, null))
        assertEquals("Essential", AppSettingsLogic.summary(AppType.ESSENTIAL, true, 30))
    }

    private data class App(val label: String, val type: AppType)
    private val apps = listOf(
        App("Phone", AppType.ESSENTIAL), App("chrome", AppType.ESSENTIAL), App("YouTube", AppType.DISTRACTING),
        App("Notes", AppType.NORMAL), App("Instagram", AppType.DISTRACTING), App("Calendar", AppType.NORMAL),
    )

    @Test fun listShowsDistractingThenNormalThenEssential() {
        val ordered = AppSettingsLogic.order(apps, "", { it.label }, { it.type }).map { it.label }
        assertEquals(listOf("Instagram", "YouTube", "Calendar", "Notes", "chrome", "Phone"), ordered)
    }

    @Test fun searchUsesTheDrawerRanking() {
        assertEquals(listOf("chrome"), AppSettingsLogic.order(apps, "chr", { it.label }, { it.type }).map { it.label })
        assertEquals(listOf("Phone", "YouTube"), AppSettingsLogic.order(apps, "  o", { it.label }, { it.type }).map { it.label }.filter { it in setOf("Phone", "YouTube") }.sorted())
        assertTrue(AppSettingsLogic.order(apps, "zzz", { it.label }, { it.type }).isEmpty())
    }
}
