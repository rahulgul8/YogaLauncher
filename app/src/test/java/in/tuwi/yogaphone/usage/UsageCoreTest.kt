package `in`.tuwi.yogaphone.usage

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

class UsageCalculatorTest {
    private val calculator = UsageCalculator("launcher")
    private fun event(pkg: String, time: Long, type: ForegroundEvent.Type) = ForegroundEvent(pkg, time, type)
    private val resume = ForegroundEvent.Type.RESUMED
    private val pause = ForegroundEvent.Type.PAUSED
    @Test fun openSessionEndsAtNow() {
        assertEquals(mapOf("app" to 40L), calculator.calculate(listOf(event("app", 10, resume)), 0, 100, 50))
    }
    @Test fun clampsBothWindowEdges() {
        assertEquals(mapOf("app" to 50L), calculator.calculate(listOf(event("app", 0, resume), event("app", 90, pause)), 20, 70))
    }
    @Test fun screenOffClosesEverySession() {
        assertEquals(mapOf("a" to 40L, "b" to 30L), calculator.calculate(listOf(event("a", 10, resume), event("b", 20, resume),
            event("", 50, ForegroundEvent.Type.SCREEN_OFF)), 0, 100))
    }
    @Test fun shutdownClosesAndStoppedIsNotDoubleCounted() {
        assertEquals(mapOf("a" to 20L), calculator.calculate(listOf(event("a", 10, resume), event("", 30, ForegroundEvent.Type.SHUTDOWN),
            event("a", 40, ForegroundEvent.Type.STOPPED)), 0, 100))
    }
    @Test fun sessionSplitsAtMidnightWithoutDoubleCounting() {
        val midnight = 86_400_000L
        val events = listOf(event("a", midnight - 30_000, resume), event("a", midnight + 90_000, pause))
        assertEquals(30_000L, calculator.calculate(events, 0, midnight)["a"])
        assertEquals(90_000L, calculator.calculate(events, midnight, midnight * 2)["a"])
    }
    @Test fun duplicatesDoNotResetSessionAndLauncherIsExcluded() {
        assertEquals(mapOf("a" to 30L), calculator.calculate(listOf(event("a", 10, resume), event("a", 20, resume),
            event("launcher", 0, resume), event("a", 40, pause), event("a", 50, pause)), 0, 100))
    }
    @Test fun lateStopOfPreviousActivityDoesNotEndAppSession() {
        val events = listOf(
            ForegroundEvent("a", 10, resume, "A1"),
            ForegroundEvent("a", 20, pause, "A1"),
            ForegroundEvent("a", 21, resume, "A2"),
            ForegroundEvent("a", 25, ForegroundEvent.Type.STOPPED, "A1"),
            ForegroundEvent("a", 60, pause, "A2"),
        )
        assertEquals(mapOf("a" to 49L), calculator.calculate(events, 0, 100))
    }
    @Test fun stopWithoutPauseClosesSession() {
        val events = listOf(ForegroundEvent("a", 10, resume, "A1"), ForegroundEvent("a", 30, ForegroundEvent.Type.STOPPED, "A1"))
        assertEquals(mapOf("a" to 20L), calculator.calculate(events, 0, 100))
    }
    @Test fun ignoresUnmatchedPauseAndInvalidWindow() {
        assertTrue(calculator.calculate(listOf(event("a", 40, pause)), 0, 100).isEmpty())
        assertTrue(calculator.calculate(listOf(event("a", 0, resume)), 100, 50).isEmpty())
    }
}

class RuleEvaluatorTest {
    private val total = UsageRule("total", RuleScope.TOTAL, null, 90)
    @Test fun boundariesAndRemaining() {
        val evaluator = RuleEvaluator()
        assertEquals(RuleState.UNDER, evaluator.evaluate(listOf(total), mapOf("a" to 71)).overall!!.state)
        assertEquals(RuleState.NEAR, evaluator.evaluate(listOf(total), mapOf("a" to 72)).overall!!.state)
        assertEquals(0L, evaluator.evaluate(listOf(total), mapOf("a" to 90)).overall!!.excessMinutes)
        val over = evaluator.evaluate(listOf(total), mapOf("a" to 102)).overall!!
        assertEquals(RuleState.OVER, over.state); assertEquals(12L, over.excessMinutes); assertEquals(0L, over.remaining)
    }
    @Test fun appAndTotalAreNotDoubleCounted() {
        val result = RuleEvaluator().evaluate(listOf(total, UsageRule("app", RuleScope.APP, "a", 15)), mapOf("b" to 20), mapOf("a" to 30, "b" to 20))
        assertEquals(20L, result.overall!!.used); assertEquals(30L, result.rules[1].used)
    }
    @Test fun offThresholdAndDisabledRule() {
        assertEquals(RuleState.UNDER, RuleEvaluator().evaluate(listOf(total.copy(warnAtPercent = 0)), mapOf("a" to 80)).overall!!.state)
        assertTrue(RuleEvaluator().evaluate(listOf(total.copy(enabled = false)), emptyMap()).rules.isEmpty())
        assertNull(RuleEvaluator().evaluate(emptyList(), emptyMap()).overall)
    }
}

class TimeValueCalculatorTest {
    private val calculator = TimeValueCalculator()
    @Test fun roundsToWholeUnits() {
        assertEquals(BigDecimal("167"), calculator.equivalent(20, BigDecimal("500")))
        assertEquals(BigDecimal("100"), calculator.equivalent(12, BigDecimal("500")))
        assertEquals(BigDecimal("1"), calculator.equivalent(1, BigDecimal("30")))
        assertEquals("$167", calculator.format(20, BigDecimal("500"), "USD", Locale.US))
    }
    @Test fun zeroAndNegativeInputsNeverProduceNegativeValues() {
        assertEquals(BigDecimal.ZERO, calculator.equivalent(-20, BigDecimal("500")))
        assertEquals(BigDecimal.ZERO, calculator.equivalent(0, BigDecimal("500")))
        assertEquals(BigDecimal.ZERO, calculator.equivalent(20, BigDecimal("-500")))
    }
}

class ReminderLedgerTest {
    @Test fun oncePerStateAndDay() {
        var sent = emptySet<String>()
        assertTrue(ReminderLedger.shouldSend(sent, "2026-11-06", "total", RuleState.NEAR))
        sent = ReminderLedger.record(sent, "2026-11-06", "total", RuleState.NEAR)
        assertFalse(ReminderLedger.shouldSend(sent, "2026-11-06", "total", RuleState.NEAR))
        assertTrue(ReminderLedger.shouldSend(sent, "2026-11-06", "total", RuleState.OVER))
        assertTrue(ReminderLedger.shouldSend(sent, "2026-11-07", "total", RuleState.NEAR))
        assertTrue(ReminderLedger.shouldSend(sent, "2026-11-06", "app:a", RuleState.NEAR))
    }
    @Test fun jumpingOverDoesNotSendStaleNearAndPrunesYesterday() {
        val sent = ReminderLedger.record(setOf("2026-11-05|total|NEAR"), "2026-11-06", "total", RuleState.OVER)
        assertEquals(1, sent.size)
        assertFalse(ReminderLedger.shouldSend(sent, "2026-11-06", "total", RuleState.NEAR))
        assertFalse(ReminderLedger.shouldSend(sent, "2026-11-06", "total", RuleState.OVER))
        assertFalse(ReminderLedger.shouldSend(sent, "2026-11-06", "total", RuleState.UNDER))
    }
}
