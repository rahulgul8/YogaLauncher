package `in`.tuwi.yogaphone.guard

import `in`.tuwi.yogaphone.usage.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

class BurnMeterTest {
    @Test fun countsFromTheFirstMinuteByDefault() {
        assertEquals(StartCounting.FROM_FIRST_MINUTE, GuardPreferences().startCounting)
    }
    private fun total(used: Long) = RuleEvaluator().evaluate(
        listOf(UsageRule("total", RuleScope.TOTAL, null, 60)), mapOf("app" to used)).overall
    @Test fun afterLimitUsesExcessAndLiveSeconds() {
        assertEquals(0, BigDecimal("10.5").compareTo(BurnMeter.amount(StartCounting.AFTER_LIMIT, total(70), 70, 30, BigDecimal(60))))
    }
    @Test fun firstMinuteNeedsNoLimitAndUsesEligibleBaseline() {
        assertEquals(0, BigDecimal("70.5").compareTo(BurnMeter.amount(StartCounting.FROM_FIRST_MINUTE, null, 70, 30, BigDecimal(60))))
    }
    @Test fun afterLimitRequiresAnOverTotalRule() {
        assertNull(BurnMeter.amount(StartCounting.AFTER_LIMIT, null, 90, 30, BigDecimal(60)))
        assertNull(BurnMeter.amount(StartCounting.AFTER_LIMIT, total(60), 60, 30, BigDecimal(60)))
        assertNull(BurnMeter.amount(StartCounting.AFTER_LIMIT, total(20), 20, 30, BigDecimal(60)))
    }
    @Test fun precisionSwitchesAtOneUnitPerMinute() {
        assertEquals(2, BurnMeter.fractionDigits(BigDecimal("59.99"), "INR"))
        assertEquals(0, BurnMeter.fractionDigits(BigDecimal("60"), "INR"))
        assertEquals(0, BurnMeter.fractionDigits(BigDecimal("10"), "JPY"))
        assertEquals(3, BurnMeter.fractionDigits(BigDecimal("10"), "KWD"))
        assertEquals("$1.25", BurnMeter.format(BigDecimal("1.25"), BigDecimal(30), "USD", Locale.US))
        assertEquals("$1", BurnMeter.format(BigDecimal("1.25"), BigDecimal(60), "USD", Locale.US))
    }
    @Test fun zeroHourlyValueAndNegativeElapsedAreSafe() {
        assertEquals(0, BigDecimal.ZERO.compareTo(BurnMeter.amount(StartCounting.FROM_FIRST_MINUTE, null, 10, 30, BigDecimal.ZERO)))
        assertEquals(0, BigDecimal.ZERO.compareTo(BurnMeter.amount(StartCounting.FROM_FIRST_MINUTE, null, -1, -1, BigDecimal(60))))
    }
}
