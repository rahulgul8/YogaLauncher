package `in`.tuwi.yogaphone.ui

import `in`.tuwi.yogaphone.data.db.UsageAggregateEntity
import `in`.tuwi.yogaphone.data.db.UsageRuleEntity
import `in`.tuwi.yogaphone.usage.HomeDisplayStyle
import `in`.tuwi.yogaphone.usage.RuleEvaluator
import `in`.tuwi.yogaphone.usage.UsageSnapshot
import `in`.tuwi.yogaphone.usage.homeDisplayStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HomeUsageNumbersTest {
    private val today = LocalDate.of(2026, 11, 8)

    private fun total(minutes: Int) = UsageRuleEntity("total", "TOTAL", dailyAllowanceMinutes = minutes)

    private fun evaluation(rule: UsageRuleEntity, used: Long) =
        RuleEvaluator().evaluate(listOf(rule.toRule()), mapOf("eligible" to used))

    @Test
    fun `allowance uses evaluated total`() {
        val rule = total(90)
        val snapshot = UsageSnapshot(
            today = listOf(UsageAggregateEntity(today.toString(), "other", 12, true)),
            evaluation = evaluation(rule, 42),
        )

        val numbers = homeUsageNumbers(snapshot, listOf(rule), today)

        assertEquals(42, numbers.todayMinutes)
        assertEquals(90, numbers.allowanceMinutes)
        assertFalse(numbers.over)
        assertEquals(0, numbers.excessMinutes)
    }

    @Test
    fun `without allowance sums eligible today`() {
        val snapshot = UsageSnapshot(today = listOf(
            UsageAggregateEntity(today.toString(), "eligible", 42, true),
            UsageAggregateEntity(today.toString(), "essential", 20, false),
        ))

        val numbers = homeUsageNumbers(snapshot, emptyList(), today)

        assertEquals(42, numbers.todayMinutes)
        assertNull(numbers.allowanceMinutes)
        assertFalse(numbers.over)
    }

    @Test
    fun `over limit reports excess`() {
        val rule = total(90)
        val numbers = homeUsageNumbers(UsageSnapshot(evaluation = evaluation(rule, 112)), listOf(rule), today)

        assertTrue(numbers.over)
        assertEquals(22, numbers.excessMinutes)
    }

    @Test
    fun `week runs oldest through today and keeps missing days null`() {
        val first = today.minusDays(6)
        val middle = today.minusDays(3)
        val snapshot = UsageSnapshot(
            week = listOf(
                UsageAggregateEntity(today.toString(), "a", 72, true),
                UsageAggregateEntity(first.toString(), "a", 45, true),
                UsageAggregateEntity(middle.toString(), "a", 9, false),
            ),
            knownDays = setOf(first.toString(), middle.toString(), today.toString()),
        )

        assertEquals(listOf(45, null, null, 0, null, null, 72), homeUsageNumbers(snapshot, emptyList(), today).week)
    }

    @Test
    fun `Yoga and home packages are excluded`() {
        val snapshot = UsageSnapshot(
            today = listOf(
                UsageAggregateEntity(today.toString(), "eligible", 30, true),
                UsageAggregateEntity(today.toString(), "yoga", 11, true),
                UsageAggregateEntity(today.toString(), "home", 13, true),
            ),
            week = listOf(
                UsageAggregateEntity(today.toString(), "eligible", 30, true),
                UsageAggregateEntity(today.toString(), "home", 13, true),
            ),
            knownDays = setOf(today.toString()),
            excludedPackages = setOf("yoga", "home"),
        )

        val numbers = homeUsageNumbers(snapshot, emptyList(), today)

        assertEquals(30, numbers.todayMinutes)
        assertEquals(30, numbers.week.last())
    }

    @Test
    fun `legacy disabled home migrates to off only when style is missing`() {
        assertEquals(HomeDisplayStyle.OFF, homeDisplayStyle(null, false))
        assertEquals(HomeDisplayStyle.LINE, homeDisplayStyle(null, true))
        assertEquals(HomeDisplayStyle.WEEK, homeDisplayStyle("WEEK", false))
    }
}
