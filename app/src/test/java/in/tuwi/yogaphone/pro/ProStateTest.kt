package `in`.tuwi.yogaphone.pro

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ProStateTest {
    private val utc = ZoneId.of("UTC")
    private fun at(value: String) = Instant.parse(value).toEpochMilli()
    private val start = at("2026-11-01T23:59:00Z")

    @Test fun localMidnightConsumesOneDay() {
        assertEquals(14, trialClock(start, start, utc).daysLeft)
        assertEquals(13, trialClock(start, at("2026-11-02T00:00:00Z"), utc).daysLeft)
        assertEquals(0, trialClock(start, at("2026-11-15T00:00:00Z"), utc).daysLeft)
    }
    @Test fun timezoneTravelCannotAddMultipleDays() {
        val now = at("2026-11-10T00:30:00Z")
        val zones = listOf("Pacific/Kiritimati", "Pacific/Pago_Pago", "Asia/Kolkata", "UTC")
        val left = zones.map { trialClock(start, now, ZoneId.of(it)).daysLeft }
        assertTrue(left.max() - left.min() <= 1)
    }
    @Test fun clockRollbackNeverRestoresConsumedDays() {
        val observed = trialClock(start, at("2026-11-12T12:00:00Z"), utc)
        assertEquals(observed.daysLeft, trialClock(start, start - 86_400_000L, utc, observed.elapsedDays).daysLeft)
        assertEquals(0, trialClock(start, start, utc, 14).daysLeft)
    }
    @Test fun futureStartAndLongExpiredStartAreBounded() {
        assertEquals(14, trialClock(start, start - 1_000_000, utc).daysLeft)
        assertEquals(0, trialClock(start, at("2028-01-01T00:00:00Z"), utc).daysLeft)
    }
    @Test fun everyFeatureInEveryEntitlement() {
        val free = setOf(ProFeature.LAUNCHER, ProFeature.THEMES, ProFeature.TODAY_SCREEN_TIME,
            ProFeature.SHORTS_BLOCKING, ProFeature.BASIC_MINDFUL_PAUSE, ProFeature.FIRST_ALLOWANCE)
        Entitlement.entries.forEach { entitlement -> ProFeature.entries.forEach { feature ->
            assertEquals("$entitlement / $feature", entitlement != Entitlement.FREE || feature in free,
                ProGate.allows(feature, ProState(entitlement)))
        } }
    }
    @Test fun onlyPurchasedKnownProductsGrantPro() {
        assertNull(restoredPlan(listOf(OwnedProduct(listOf("yoga_pro_yearly"), false))))
        assertNull(restoredPlan(listOf(OwnedProduct(listOf("unknown"), true))))
        assertEquals(ProPlan.YEARLY, restoredPlan(listOf(OwnedProduct(listOf("yoga_pro_yearly"), true))))
        assertEquals(ProPlan.LIFETIME, restoredPlan(listOf(OwnedProduct(listOf("yoga_pro_yearly", "yoga_pro_lifetime"), true))))
    }
    @Test fun failedRestoreKeepsCacheSuccessfulEmptyRestoreClearsIt() {
        assertEquals(ProState(Entitlement.PRO, 0, ProPlan.YEARLY), resolveProState(0, ProPlan.YEARLY))
        assertEquals(ProState(Entitlement.FREE, 0), resolveProState(0, ProPlan.YEARLY, emptyList()))
        assertEquals(ProState(Entitlement.TRIAL, 7), resolveProState(7, ProPlan.YEARLY, emptyList()))
    }
    @Test fun freeQuotasAreDeterministicAndDeduplicated() {
        assertEquals(setOf("a", "b", "c"), freeQuota(listOf("d", "b", "a", "b", "c"), 3))
        assertEquals(setOf("a"), freeQuota(listOf("z", "a"), 1))
    }
}
