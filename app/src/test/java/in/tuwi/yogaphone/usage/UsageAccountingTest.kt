package `in`.tuwi.yogaphone.usage

import `in`.tuwi.yogaphone.data.db.UsageAggregateEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class UsageAccountingTest {
    @Test fun manyShortAppsDoNotEachRoundUpBeforeTheHeadlineIsSummed() {
        val result = allocateUsageMinutes(mapOf("a" to 40_000L, "b" to 40_000L, "c" to 40_000L), setOf("a", "b", "c"))
        assertEquals(2L, result.values.sum())
    }

    @Test fun eligibleAndAllAppTotalsAreRoundedFromTheirOwnMillisecondTotals() {
        val eligible = setOf("a", "b")
        val result = allocateUsageMinutes(mapOf("a" to 31_000L, "b" to 31_000L, "other" to 31_000L), eligible)
        assertEquals(1L, result.filterKeys { it in eligible }.values.sum())
        assertEquals(2L, result.values.sum())
    }

    @Test fun allocationIsDeterministicWhenRemaindersTie() {
        assertEquals(mapOf("a" to 1L, "b" to 0L), allocateUsageMinutes(mapOf("b" to 30_000L, "a" to 30_000L), setOf("a", "b")))
    }

    @Test fun learningChangesCountedRowsButNotRawAllAppRows() {
        val raw = listOf(UsageAggregateEntity("2026-11-17", "com.google.android.youtube", 60, true))
        val counted = applyLearningExemption(raw, mapOf("2026-11-17|com.google.android.youtube" to 600L), true)
        assertEquals(60L, raw.single().foregroundMinutes)
        assertEquals(50L, counted.single().foregroundMinutes)
    }

    @Test fun fallBackDayHasTwoWindowsForRepeatedHour() {
        val windows = localHourWindows(LocalDate.of(2026, 11, 1), ZoneId.of("America/New_York"))
        assertEquals(25, windows.size)
        assertEquals(2, windows.count { it.hour == 1 })
    }

    @Test fun springForwardDaySkipsTheMissingHour() {
        val windows = localHourWindows(LocalDate.of(2026, 3, 8), ZoneId.of("America/New_York"))
        assertEquals(23, windows.size)
        assertEquals(0, windows.count { it.hour == 2 })
    }

    @Test fun eventLookbackUsesLocalCalendarDaysAcrossDst() {
        val date = LocalDate.of(2026, 11, 2)
        val zone = ZoneId.of("America/New_York")
        val expected = date.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals(expected, usageEventQueryStart(date, zone))
    }

    @Test fun staleVisibleWeekDatesAreSelectedForOneTimeBackfill() {
        val today = LocalDate.of(2026, 11, 17)
        assertEquals((1L..6L).map(today::minusDays), usageBackfillDates(today))
    }
}
