package `in`.tuwi.yogaphone.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLimitPolicyTest {
    private fun result(id: String, scope: RuleScope, target: String?, allowance: Int, state: RuleState = RuleState.OVER) =
        RuleResult(UsageRule(id, scope, target, allowance), allowance + 1L, allowance, 0, state, 1)

    @Test fun appRuleWinsOverCategoryAndTotalNeverClosesAnApp() {
        val category = result("category:social", RuleScope.CATEGORY, "social", 20)
        val app = result("app:chat", RuleScope.APP, "chat", 30)
        val total = result("total", RuleScope.TOTAL, null, 90)
        assertEquals(app, AppLimitPolicy.blockingRule("chat", "social", RuleEvaluation(listOf(total, category, app), total)))
        assertNull(AppLimitPolicy.blockingRule("other", "other", RuleEvaluation(listOf(total), total)))
    }

    @Test fun onlyAnOverMatchingRuleBlocks() {
        val under = result("app:chat", RuleScope.APP, "chat", 15, RuleState.NEAR)
        val other = result("category:games", RuleScope.CATEGORY, "games", 15)
        assertNull(AppLimitPolicy.blockingRule("chat", "social", RuleEvaluation(listOf(under, other), null)))
        assertEquals(other, AppLimitPolicy.blockingRule("game", "games", RuleEvaluation(listOf(other), null)))
    }
}

class UrgentAccessPolicyTest {
    @Test fun quotaIsSharedAcrossAppsButRecordedPerAppAndDay() {
        var records = emptyList<UrgentOpenRecord>()
        records = UrgentAccessPolicy.record(records, "2026-11-17", "a")
        records = UrgentAccessPolicy.record(records, "2026-11-17", "b")
        records = UrgentAccessPolicy.record(records, "2026-11-17", "a")
        assertFalse(UrgentAccessPolicy.canGrant(records, "2026-11-17"))
        assertEquals(2, records.first { it.packageName == "a" }.count)
        assertEquals(1, records.first { it.packageName == "b" }.count)
        assertTrue(UrgentAccessPolicy.canGrant(records, "2026-11-18"))
        assertEquals(records, UrgentAccessPolicy.record(records, "2026-11-17", "c"))
    }

    @Test fun preferenceEncodingRoundTripsCountersAndWindows() {
        val records = listOf(UrgentOpenRecord("2026-11-17", "com.example.app", 2))
        val windows = listOf(UrgentWindow("com.example.app", 1234))
        assertEquals(records, UrgentAccessCodec.decodeRecords(UrgentAccessCodec.encodeRecords(records)))
        assertEquals(windows.associateBy { it.packageName }, UrgentAccessCodec.decodeWindows(UrgentAccessCodec.encodeWindows(windows)))
    }
}

class UrgentWindowPolicyTest {
    @Test fun liveWindowNeedsNoIntervention() {
        val window = UrgentWindow("app", 1_000)
        assertTrue(UrgentWindowPolicy.isLive(window, 999))
        assertFalse(UrgentWindowPolicy.isLive(window, 1_000))
        assertEquals(UrgentInterventionDecision(false, false, emptySet()),
            UrgentWindowPolicy.decide(listOf(window), "app", 999, canGrey = true, lastReminderAt = null))
    }

    @Test fun expiredWindowGreysCurrentAppAndRestoresAfterLeaving() {
        val window = UrgentWindow("app", 1_000)
        assertEquals(UrgentInterventionDecision(true, false, emptySet()),
            UrgentWindowPolicy.decide(listOf(window), "app", 1_000, canGrey = true, lastReminderAt = null))
        assertEquals(UrgentInterventionDecision(false, false, setOf("app")),
            UrgentWindowPolicy.decide(listOf(window), "home", 1_001, canGrey = true, lastReminderAt = null))
    }

    @Test fun reminderRepeatsEveryFiveMinutesWhenGreyIsUnavailable() {
        val window = UrgentWindow("app", 1_000)
        assertTrue(UrgentWindowPolicy.decide(listOf(window), "app", 1_000, false, null).remind)
        assertFalse(UrgentWindowPolicy.decide(listOf(window), "app", 2_000, false, 1_000).remind)
        assertTrue(UrgentWindowPolicy.decide(listOf(window), "app",
            1_000 + UrgentAccessPolicy.REMINDER_MILLIS, false, 1_000).remind)
    }
}
