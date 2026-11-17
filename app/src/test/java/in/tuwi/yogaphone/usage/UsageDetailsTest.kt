package `in`.tuwi.yogaphone.usage

import `in`.tuwi.yogaphone.data.db.UsageAggregateEntity
import `in`.tuwi.yogaphone.data.db.UsageHourEntity
import `in`.tuwi.yogaphone.insights.GoalDone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class UsageDetailsTest {
    private val today = LocalDate.of(2026, 11, 17)

    @Test fun weekRunsFromSixDaysAgoToToday() {
        val days = UsageDetails.weekDays(today)
        assertEquals(7, days.size)
        assertEquals(LocalDate.of(2026, 11, 11), days.first())
        assertEquals(today, days.last())
    }

    @Test fun steppingStaysInsideTheWeek() {
        assertFalse(UsageDetails.canStepForward(today, today))
        assertTrue(UsageDetails.canStepBack(today, today))
        val first = today.minusDays(6)
        assertFalse(UsageDetails.canStepBack(first, today))
        assertTrue(UsageDetails.canStepForward(first, today))
        assertEquals(first, UsageDetails.step(first, -1, today))
        assertEquals(today, UsageDetails.step(today, 1, today))
        assertEquals(today.minusDays(1), UsageDetails.step(today, -1, today))
        assertEquals(first, UsageDetails.step(today, -30, today))
    }

    @Test fun barsScaleToTheTallestBar() {
        assertEquals(listOf(0.5f, 1f, 0f), UsageDetails.barFractions(listOf(30L, 60L, 0L)))
    }

    @Test fun barsScaleToTheAllowanceWhenItIsHigher() {
        assertEquals(listOf(0.25f, 0.5f), UsageDetails.barFractions(listOf(30L, 60L), reference = 120L))
        assertEquals(1f, UsageDetails.referenceFraction(listOf(30L, 60L), 120L))
        assertEquals(0.5f, UsageDetails.referenceFraction(listOf(240L, 60L), 120L))
    }

    @Test fun missingDaysStayMissingAndEmptyChartsAreFlat() {
        assertEquals(listOf(null, 1f), UsageDetails.barFractions(listOf(null, 10L)))
        assertEquals(listOf(0f, null), UsageDetails.barFractions(listOf(0L, null)))
        assertNull(UsageDetails.referenceFraction(listOf(10L), null))
        assertNull(UsageDetails.referenceFraction(listOf(10L), 0L))
    }

    @Test fun hourValuesFillAllTwentyFourHours() {
        val values = UsageDetails.hourValues(listOf(UsageHourEntity("d", 9, 12), UsageHourEntity("d", 23, 3)))
        assertEquals(24, values.size)
        assertEquals(12L, values[9])
        assertEquals(3L, values[23])
        assertEquals(0L, values[0])
    }

    @Test fun appMinutesFollowTheSelectedDays() {
        val rows = listOf(
            UsageAggregateEntity("2026-11-16", "a", 10, true),
            UsageAggregateEntity("2026-11-17", "a", 5, true),
            UsageAggregateEntity("2026-11-17", "b", 20, false),
            UsageAggregateEntity("2026-11-17", "c", 0, false),
        )
        assertEquals(listOf("b" to 20L, "a" to 15L), UsageDetails.appMinutes(rows))
        assertEquals(listOf("b" to 20L, "a" to 5L), UsageDetails.appMinutes(rows, setOf("2026-11-17")))
        assertEquals(listOf("a" to 10L), UsageDetails.appMinutes(rows, setOf("2026-11-16")))
    }

    @Test fun dailyEligibleMarksUnrecordedDays() {
        val rows = listOf(
            UsageAggregateEntity("2026-11-16", "a", 10, true),
            UsageAggregateEntity("2026-11-16", "b", 7, false),
        )
        val days = listOf(LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 16), today)
        assertEquals(listOf(null, 10L, 0L), UsageDetails.dailyEligible(rows, days, setOf("2026-11-17")))
    }

    @Test fun dayTitlesAreFriendly() {
        assertEquals("Today", UsageDetails.dayTitle(today, today))
        assertEquals("Yesterday", UsageDetails.dayTitle(today.minusDays(1), today))
    }

    @Test fun goalLinesSayHowLongTheyTook() {
        assertEquals("25 of 30 min planned", UsageDetails.goalLine(GoalDone("Read", 30, 25, GoalDone.Kind.TIMEBOX)))
        assertEquals("30 min, as planned", UsageDetails.goalLine(GoalDone("Read", 30, 30, GoalDone.Kind.TIMEBOX)))
        assertEquals("12 min", UsageDetails.goalLine(GoalDone("Focus", 0, 12, GoalDone.Kind.FOCUS)))
    }

    @Test fun hourRangeWrapsAtMidnight() {
        assertEquals("09:00–10:00", UsageDetails.hourRange(9))
        assertEquals("23:00–00:00", UsageDetails.hourRange(23))
    }
}
