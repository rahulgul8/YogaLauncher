package `in`.tuwi.yogaphone.insights

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InsightEngineTest {
    private val today = LocalDate.of(2026, 11, 6)
    private fun days(range: LongRange, minutes: Long) = range.associate { today.minusDays(it) to minutes }
    private fun input(eligible: Map<LocalDate, Long>, hourly: Map<LocalDate, LongArray> = emptyMap(), allowance: Int? = 90) = InsightInput(
        today, eligible, hourly, allowance, 0, 0, 0, 0, emptyMap(), null, nowHour = 22,
    )

    @Test fun noBaselineNoPercentage() {
        // Only 4 days last week: no comparison card.
        val cards = InsightEngine.cards(input(days(0L..6L, 60) + days(7L..10L, 120)), CoachingTone.DATA_DRIVEN)
        assertFalse(cards.any { it.id == "week_change" })
    }

    @Test fun weekOverWeekChange() {
        val cards = InsightEngine.cards(input(days(0L..6L, 60) + days(7L..13L, 80)), CoachingTone.DATA_DRIVEN)
        assertEquals("Distracting-app time: 25% lower per day than last week.", cards.first { it.id == "week_change" }.body)
    }

    @Test fun withinAllowanceCountsOnlySampledDays() {
        val cards = InsightEngine.cards(input(mapOf(today to 50L, today.minusDays(1) to 120L)), CoachingTone.GENTLE)
        assertEquals("1 of 2 recorded days this week stayed within 90 minutes.", cards.first { it.id == "within_allowance" }.body)
    }

    @Test fun lateNightBucketsAndBreaks() {
        val hours = LongArray(24).also { it[23] = 10; it[0] = 5; it[2] = 1; it[12] = 3 }
        assertEquals(16L, InsightEngine.lateNightMinutes(hours))
        // 07..11 free (5 h), 12 used, 13..21 free (9 h) when it is 22:00.
        assertEquals(9, InsightEngine.longestBreakHours(hours, 22))
        assertEquals(5, InsightEngine.longestBreakHours(hours, 12))
    }

    @Test fun unlockCardNeedsThreeDays() {
        val two = input(emptyMap()).copy(unlocksByDay = mapOf(today to 40, today.minusDays(1) to 50))
        assertFalse(InsightEngine.cards(two, CoachingTone.GENTLE).any { it.id == "unlocks" })
        val three = two.copy(unlocksByDay = two.unlocksByDay + (today.minusDays(2) to 60))
        assertEquals("About 50 unlocks a day this week.", InsightEngine.cards(three, CoachingTone.GENTLE).first { it.id == "unlocks" }.body)
    }

    @Test fun percentChangeNeedsPositiveBaseline() {
        assertNull(InsightEngine.percentChange(0.0, 10.0))
        assertNull(InsightEngine.percentChange(null, 10.0))
        assertEquals(-50, InsightEngine.percentChange(20.0, 10.0))
    }

    @Test fun adaptiveAllowanceSuggestion() {
        assertEquals(75, InsightEngine.suggestedAllowance(90, days(1L..7L, 60), today))
        assertNull(InsightEngine.suggestedAllowance(90, days(1L..7L, 80), today))
        assertNull(InsightEngine.suggestedAllowance(90, days(1L..3L, 10), today))
        assertNull(InsightEngine.suggestedAllowance(15, days(1L..7L, 0), today))
        assertTrue(InsightEngine.cards(input(emptyMap(), allowance = null), CoachingTone.GENTLE).isEmpty())
    }
}
