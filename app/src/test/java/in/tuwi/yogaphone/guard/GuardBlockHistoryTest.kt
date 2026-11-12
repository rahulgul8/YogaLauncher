package `in`.tuwi.yogaphone.guard

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class GuardBlockHistoryTest {
    private val today = LocalDate.of(2026, 11, 8)
    private fun daily(daysAgo: Long, count: Int) = GuardDaily(today.minusDays(daysAgo).toString(), mapOf(GuardSurface.YOUTUBE to count))
    @Test fun migratesPreviousDayAndStoresToday() {
        val rows = guardBlockHistory(emptySet(), daily(1, 9), daily(0, 2), today)
        assertTrue(rows.contains("2026-11-07|9|0|0|0|0"))
        assertTrue(rows.contains("2026-11-08|2|0|0|0|0"))
    }
    @Test fun repeatedDailySaveReplacesInsteadOfAdding() {
        val rows = guardBlockHistory(emptySet(), GuardDaily(), daily(0, 2), today)
        val updated = guardBlockHistory(rows, daily(0, 2), daily(0, 3), today)
        assertEquals(setOf("2026-11-08|3|0|0|0|0"), updated)
    }
    @Test fun keepsExactlySevenLocalDaysAndDropsFutureAndMalformedRows() {
        val rows = (0L..8L).map { "${today.minusDays(it)}|1|2|3|4" }.toSet() + "${today.plusDays(1)}|1|0|0|0" + "bad"
        val kept = guardBlockHistory(rows, GuardDaily(), null, today)
        assertEquals(7, kept.size)
        assertTrue(kept.any { it.startsWith(today.minusDays(6).toString()) })
        assertFalse(kept.any { it.startsWith(today.minusDays(7).toString()) })
    }
    @Test fun rolloverWithoutNewBlocksPrunesAndPreservesMigration() {
        val kept = guardBlockHistory(emptySet(), daily(0, 7), null, today.plusDays(7))
        assertTrue(kept.isEmpty())
    }
    @Test fun migrationDoesNotReplaceNewerRingCount() {
        assertEquals(setOf("2026-11-08|8|0|0|0|0"), guardBlockHistory(setOf("2026-11-08|8|0|0|0"), daily(0, 2), null, today))
    }
    @Test fun resetWithNoCountersStaysEmpty() {
        assertTrue(guardBlockHistory(emptySet(), GuardDaily(), null, today).isEmpty())
    }
    @Test fun legacyFourSurfaceRowsKeepTheirCountsAndDefaultStatusToZero() {
        assertEquals(setOf("2026-11-08|1|2|3|4|0"),
            guardBlockHistory(setOf("2026-11-08|1|2|3|4"), GuardDaily(), null, today))
        assertEquals(listOf(GuardSurface.YOUTUBE, GuardSurface.INSTAGRAM,
            GuardSurface.BROWSER_YOUTUBE, GuardSurface.BROWSER_INSTAGRAM), GuardSurface.entries.take(4))
    }

}
