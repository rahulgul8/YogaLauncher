package `in`.tuwi.yogaphone.focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class TimeboxClockTest {
    private val min = 60_000L

    @Test fun pauseAndResumeKeepRemainingTime() {
        val clock = TimeboxClock.start(25, 0)
        val paused = clock.pause(10 * min)
        assertEquals(15 * min, paused.remainingMs(100 * min))
        val resumed = paused.resume(100 * min)
        assertEquals(115 * min, resumed.endAt)
        assertEquals(5 * min, resumed.remainingMs(110 * min))
    }

    @Test fun extendRunningPausedAndFinished() {
        assertEquals(30 * min, TimeboxClock.start(25, 0).extend(5, 10 * min).endAt)
        assertEquals(20 * min, TimeboxClock.start(25, 0).pause(10 * min).extend(5, 50 * min).remainingMs(99 * min))
        val finished = TimeboxClock.start(25, 0).extend(10, 40 * min)
        assertEquals(50 * min, finished.endAt)
    }

    @Test fun remainingNeverNegativeAndDoublePauseIsNoop() {
        val clock = TimeboxClock.start(1, 0)
        assertEquals(0, clock.remainingMs(5 * min))
        val paused = clock.pause(0)
        assertEquals(paused, paused.pause(30_000))
    }
}

class ScheduleEvaluatorTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
    private val work = FocusWindow(1, "Work", weekdays, LocalTime.of(9, 0), LocalTime.of(13, 0))
    private val bedtime = FocusWindow(2, "Bedtime", DayOfWeek.entries.toSet(), LocalTime.of(22, 0), LocalTime.of(7, 0))
    private fun at(text: String, z: ZoneId = zone) = LocalDateTime.parse(text).atZone(z).toInstant()

    @Test fun weekdayWindow() {
        // 2026-11-06 is a Friday, 2026-11-07 a Saturday.
        assertEquals(work, ScheduleEvaluator.activeAt(listOf(work), at("2026-11-06T10:00"), zone)?.window)
        assertNull(ScheduleEvaluator.activeAt(listOf(work), at("2026-11-06T13:00"), zone))
        assertNull(ScheduleEvaluator.activeAt(listOf(work), at("2026-11-07T10:00"), zone))
    }

    @Test fun midnightCrossingWindowBelongsToPreviousDay() {
        val sundayOnly = bedtime.copy(days = setOf(DayOfWeek.SUNDAY))
        // Sunday 22:00 to Monday 07:00.
        assertEquals(at("2026-11-09T07:00"), ScheduleEvaluator.activeAt(listOf(sundayOnly), at("2026-11-09T02:00"), zone)?.end)
        assertNull(ScheduleEvaluator.activeAt(listOf(sundayOnly), at("2026-11-10T02:00"), zone))
    }

    @Test fun nextTransition() {
        assertEquals(at("2026-11-06T13:00"), ScheduleEvaluator.nextTransition(listOf(work, bedtime), at("2026-11-06T10:00"), zone))
        assertEquals(at("2026-11-06T22:00"), ScheduleEvaluator.nextTransition(listOf(work, bedtime), at("2026-11-06T13:00"), zone))
        // Friday after work: next work start is Monday, but bedtime comes first.
        assertEquals(at("2026-11-09T09:00"), ScheduleEvaluator.nextTransition(listOf(work), at("2026-11-06T14:00"), zone))
    }

    @Test fun dstGapAndDisabledOrEmptyWindows() {
        val ny = ZoneId.of("America/New_York")
        // 2026-03-08 02:00 does not exist in New York; the window starts at 03:00 instead.
        val early = FocusWindow(3, "Early", setOf(DayOfWeek.SUNDAY), LocalTime.of(2, 0), LocalTime.of(4, 0))
        val span = ScheduleEvaluator.occurrences(early, LocalDate.of(2026, 3, 8), LocalDate.of(2026, 3, 8), ny).single()
        assertEquals(at("2026-03-08T03:00", ny), span.start)
        assertEquals(3_600_000L, span.end.toEpochMilli() - span.start.toEpochMilli())
        assertTrue(ScheduleEvaluator.occurrences(work.copy(enabled = false), LocalDate.of(2026, 11, 6), LocalDate.of(2026, 11, 6), zone).isEmpty())
        assertTrue(ScheduleEvaluator.occurrences(work.copy(end = work.start), LocalDate.of(2026, 11, 6), LocalDate.of(2026, 11, 6), zone).isEmpty())
    }
}

class StreakAndDelayTest {
    private val today = LocalDate.of(2026, 11, 6)

    @Test fun streakCountsBackFromTodayOrYesterday() {
        assertEquals(3, StreakCalculator.streak(setOf(today, today.minusDays(1), today.minusDays(2), today.minusDays(4)), today))
        assertEquals(2, StreakCalculator.streak(setOf(today.minusDays(1), today.minusDays(2)), today))
        assertEquals(0, StreakCalculator.streak(setOf(today.minusDays(2)), today))
    }

    @Test fun delayWindow() {
        assertTrue(DelayGate.shouldPause(null, 0, 5))
        assertFalse(DelayGate.shouldPause(0, 4 * 60_000, 5))
        assertTrue(DelayGate.shouldPause(0, 5 * 60_000, 5))
        assertTrue(DelayGate.shouldPause(0, 60_000, 0))
        assertTrue(DelayGate.shouldPause(10_000, 0, 5)) // clock moved backwards
    }

    @Test fun delayRecordRoundTripsAndPrunes() {
        val day = 24 * 60 * 60_000L
        val stored = DelayGate.record(mapOf("old.app" to 0L, "a|b" to day), "x.app", day + 1)
        assertEquals(mapOf("a|b" to day, "x.app" to day + 1), DelayGate.decode(DelayGate.encode(stored)))
    }

    @Test fun pausePassRoundTripsDropsExpiredAndDetectsLivePasses() {
        val now = 10_000L
        val passes = PausePassGate.record(mapOf("old.app" to now, "a|b" to now + 1), "x.app", now + 5_000, now)
        assertEquals(mapOf("a|b" to now + 1, "x.app" to now + 5_000), PausePassGate.decode(PausePassGate.encode(passes)))
        assertTrue(PausePassGate.isActive(passes["x.app"], now))
        assertFalse(PausePassGate.isActive(passes["old.app"], now))
        assertFalse(PausePassGate.isActive(null, now))
    }

    @Test fun pausePassSelectionUsesPerAppThenLegacyThenLadderDefault() {
        val choices = listOf(5, 10, 15)
        assertEquals(15, PausePassGate.preselect(15, 5, choices))
        assertEquals(5, PausePassGate.preselect(null, 5, choices))
        assertEquals(10, PausePassGate.preselect(null, 30, choices))
        assertEquals(5, PausePassGate.preselect(15, 30, listOf(5)))
    }

    @Test fun recentListIsNewestFirstDedupedAndCapped() {
        assertEquals(listOf("Read", "Email", "Plan"), updateRecentList(listOf("Email", "read", "Plan", "Walk"), " Read "))
        assertEquals(listOf("a", "b"), updateRecentList(listOf("a", "b"), "  "))
    }
}
