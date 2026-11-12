package `in`.tuwi.yogaphone.guard

import org.junit.Assert.assertEquals
import org.junit.Test

class LearningPolicyTest {
    @Test fun accountingSubtractsAndNeverGoesNegative() {
        assertEquals(50L, LearningAccounting.effectiveMinutes(60, 600))
        assertEquals(0L, LearningAccounting.effectiveMinutes(5, 600))
    }
    @Test fun onlyAListedReadableWatchPageIsLearning() {
        assertEquals(true, LearningPolicy.isLearning(WatchInfo("NPTEL", "Lecture", true), setOf("nptel")))
        assertEquals(false, LearningPolicy.isLearning(WatchInfo(null, "Lecture", true), setOf("nptel")))
        assertEquals(false, LearningPolicy.isLearning(WatchInfo("NPTEL", "Lecture", false), setOf("nptel")))
    }
    @Test fun totalsAreAddressedByLocalDay() {
        val totals = mapOf("2026-11-08|com.google.android.youtube" to 90L, "2026-11-07|com.google.android.youtube" to 20L)
        assertEquals(90L, LearningAccounting.totalFor(totals, "2026-11-08", "com.google.android.youtube"))
        assertEquals(20L, LearningAccounting.totalFor(totals, "2026-11-07", "com.google.android.youtube"))
        assertEquals(0L, LearningAccounting.totalFor(totals, "2026-11-09", "com.google.android.youtube"))
    }
    @Test fun focusAllowsOnlyListedWatchPagesByDefault() {
        val listed = setOf("nptel")
        assertEquals(YouTubeFocusAction.ALLOW, LearningFocusPolicy.decide(true, false, WatchInfo("NPTEL", null, true), "nptel", listed, false))
        assertEquals(YouTubeFocusAction.BLOCK_WATCH, LearningFocusPolicy.decide(true, false, WatchInfo("Other", null, true), "other", listed, false))
        assertEquals(YouTubeFocusAction.BLOCK_HOME, LearningFocusPolicy.decide(true, false, WatchInfo(), null, listed, false))
        assertEquals(YouTubeFocusAction.ALLOW, LearningFocusPolicy.decide(true, false, WatchInfo(), null, listed, true))
    }
    @Test fun focusGateDebouncesAndEscalates() {
        val gate = LearningFocusGate()
        assertEquals(GateAction.BACK, gate.evaluate(true, 0))
        assertEquals(GateAction.NONE, gate.evaluate(true, 500))
        assertEquals(GateAction.BACK, gate.evaluate(true, 1000))
        assertEquals(GateAction.HOME, gate.evaluate(true, 2000))
        assertEquals(GateAction.NONE, gate.evaluate(true, 3000))
        assertEquals(GateAction.NONE, gate.evaluate(false, 4000))
    }
}
