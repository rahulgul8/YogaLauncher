package `in`.tuwi.yogaphone.insights

import `in`.tuwi.yogaphone.data.db.FocusSessionEntity
import `in`.tuwi.yogaphone.data.db.FocusStatus
import `in`.tuwi.yogaphone.data.db.TimeboxSessionEntity
import `in`.tuwi.yogaphone.data.db.TimeboxStatus
import `in`.tuwi.yogaphone.data.db.UsageAggregateEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DayAchievementsTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val date = LocalDate.of(2026, 11, 16)

    private fun at(day: LocalDate, hour: Int, minute: Int = 0): Long =
        day.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun usage(day: LocalDate = date, minutes: Long, eligible: Boolean = true, pkg: String = "com.example.app") =
        UsageAggregateEntity(day.toString(), pkg, minutes, eligible)

    private fun timebox(
        startAt: Long = at(date, 9),
        minutes: Int = 30,
        extended: Int = 0,
        purpose: String? = "Yoga",
        status: String = TimeboxStatus.COMPLETED,
        endedAt: Long? = null,
    ) = TimeboxSessionEntity(
        purpose = purpose,
        plannedMinutes = minutes,
        startedAt = startAt,
        endAt = startAt + minutes * 60_000L,
        endedAt = endedAt,
        status = status,
        extendedMinutes = extended,
    )

    private fun focus(
        startAt: Long = at(date, 18),
        minutes: Int = 30,
        endedAfterMinutes: Int? = null,
        status: String = FocusStatus.COMPLETED,
    ) = FocusSessionEntity(
        startAt = startAt,
        plannedEndAt = startAt + minutes * 60_000L,
        endedAt = endedAfterMinutes?.let { startAt + it * 60_000L },
        status = status,
    )

    private fun build(
        aggregates: List<UsageAggregateEntity> = emptyList(),
        allowance: Int? = 60,
        timeboxes: List<TimeboxSessionEntity> = emptyList(),
        focus: List<FocusSessionEntity> = emptyList(),
        wentBack: Int = 0,
    ) = DayAchievementsBuilder.build(date, zone, aggregates, allowance, timeboxes, focus, wentBack)

    @Test fun noDataHasNoContent() {
        val day = build(allowance = null)
        assertFalse(day.hasContent)
        assertEquals("Yesterday", day.headline())
        assertTrue(day.lines().isEmpty())
        assertTrue(day.goals.isEmpty())
        assertNull(day.distractingMinutes)
        assertNull(day.underAllowance)
        assertNull(day.lessThanDayBefore)
    }

    @Test fun underAllowance() {
        val day = build(aggregates = listOf(usage(minutes = 40)), allowance = 60)
        assertEquals(40, day.distractingMinutes)
        assertEquals(true, day.underAllowance)
        assertEquals("Yesterday: Under your limit", day.headline())
        assertEquals("40 min on distracting apps, within your 1 h limit", day.lines().first())
    }

    @Test fun overAllowance() {
        val day = build(aggregates = listOf(usage(minutes = 90)), allowance = 60)
        assertEquals(false, day.underAllowance)
        assertEquals("Yesterday: 1 h 30 min on distracting apps", day.headline())
        assertEquals("1 h 30 min on distracting apps, 30 min past your limit", day.lines().first())
    }

    @Test fun noAllowance() {
        val day = build(aggregates = listOf(usage(minutes = 45)), allowance = null)
        assertNull(day.underAllowance)
        assertEquals("Yesterday: 45 min on distracting apps", day.headline())
        assertEquals("45 min on distracting apps", day.lines().single())
    }

    @Test fun rowsFromOtherDatesAreIgnored() {
        val day = build(
            aggregates = listOf(usage(day = date.plusDays(1), minutes = 40), usage(day = date.minusDays(2), minutes = 70)),
            allowance = 60,
        )
        assertNull(day.distractingMinutes)
        assertNull(day.previousDistractingMinutes)
        assertFalse(day.hasContent)
        assertEquals("Yesterday", day.headline())
    }

    @Test fun nonEligibleRowsAreIgnored() {
        val day = build(
            aggregates = listOf(usage(minutes = 30, pkg = "a"), usage(minutes = 500, eligible = false, pkg = "b")),
            allowance = 60,
        )
        assertEquals(30, day.distractingMinutes)
        assertEquals(true, day.underAllowance)
    }

    @Test fun onlyNonEligibleRowsCountAsZero() {
        val day = build(aggregates = listOf(usage(minutes = 500, eligible = false)), allowance = 60)
        assertEquals(0, day.distractingMinutes)
        assertEquals(true, day.underAllowance)
        assertTrue(day.hasContent)
    }

    @Test fun timeboxOutsideTheDayIsIgnored() {
        val day = build(
            timeboxes = listOf(
                timebox(startAt = at(date.plusDays(1), 0, 5)),
                timebox(startAt = at(date.minusDays(1), 23, 0)),
            ),
        )
        assertTrue(day.goals.isEmpty())
        assertFalse(day.hasContent)
    }

    @Test fun cancelledTimeboxIsIgnored() {
        val day = build(
            timeboxes = listOf(
                timebox(status = TimeboxStatus.CANCELLED),
                timebox(startAt = at(date, 11), status = TimeboxStatus.RUNNING),
            ),
            focus = listOf(focus(status = FocusStatus.ENDED_EARLY)),
        )
        assertTrue(day.goals.isEmpty())
    }

    @Test fun blankPurposeFallsBackToTimebox() {
        val day = build(
            timeboxes = listOf(
                timebox(startAt = at(date, 8), purpose = "   "),
                timebox(startAt = at(date, 10), purpose = null),
            ),
        )
        assertEquals(listOf("Timebox", "Timebox"), day.goals.map { it.title })
    }

    @Test fun timeboxUsesPlannedPlusExtendedAndActualMinutes() {
        val start = at(date, 9)
        val day = build(timeboxes = listOf(timebox(startAt = start, minutes = 25, extended = 5, endedAt = start + 20 * 60_000L)))
        val goal = day.goals.single()
        assertEquals("Yoga", goal.title)
        assertEquals(GoalDone.Kind.TIMEBOX, goal.kind)
        assertEquals(30, goal.plannedMinutes)
        assertEquals(20, goal.actualMinutes)
        assertTrue(day.lines().contains("Yoga: 20 min of 30 min"))
    }

    @Test fun completedFocusIsAGoal() {
        val day = build(focus = listOf(focus(minutes = 50, endedAfterMinutes = 30)))
        val goal = day.goals.single()
        assertEquals("Focus", goal.title)
        assertEquals(GoalDone.Kind.FOCUS, goal.kind)
        assertEquals(50, goal.plannedMinutes)
        assertEquals(30, goal.actualMinutes)
    }

    @Test fun goalsAreSortedByStartTime() {
        val day = build(
            timeboxes = listOf(timebox(startAt = at(date, 12), purpose = "Noon")),
            focus = listOf(focus(startAt = at(date, 20)), focus(startAt = at(date, 8))),
        )
        assertEquals(
            listOf(GoalDone.Kind.FOCUS, GoalDone.Kind.TIMEBOX, GoalDone.Kind.FOCUS),
            day.goals.map { it.kind },
        )
        assertEquals("Noon", day.goals[1].title)
    }

    @Test fun minutesAreFormatted() {
        val long = at(date, 9)
        val day = build(
            aggregates = listOf(usage(minutes = 45)),
            allowance = 45,
            timeboxes = listOf(timebox(startAt = long, minutes = 80, purpose = "Long")),
            focus = listOf(focus(minutes = 120)),
        )
        val lines = day.lines()
        assertEquals("45 min on distracting apps, within your 45 min limit", lines[0])
        assertTrue(lines.contains("Long: 1 h 20 min"))
        assertTrue(lines.contains("Focus: 2 h"))
        assertEquals("Yesterday: Under your limit · 2 goals done", day.headline())
    }

    @Test fun lessThanDayBefore() {
        val day = build(
            aggregates = listOf(usage(minutes = 30), usage(day = date.minusDays(1), minutes = 90)),
            allowance = null,
        )
        assertEquals(60, day.lessThanDayBefore)
        assertEquals("Yesterday: 30 min on distracting apps · 1 h less than the day before", day.headline())
        assertTrue(day.lines().contains("1 h less than the day before"))
    }

    @Test fun lessThanDayBeforeIsNullWhenNotLower() {
        val day = build(
            aggregates = listOf(usage(minutes = 50), usage(day = date.minusDays(1), minutes = 50)),
            allowance = null,
        )
        assertNull(day.lessThanDayBefore)
        assertEquals("Yesterday: 50 min on distracting apps", day.headline())
        assertFalse(day.lines().any { it.contains("day before") })
    }

    @Test fun headlinePutsAchievementSecond() {
        val under = build(aggregates = listOf(usage(minutes = 10)), allowance = 60, timeboxes = listOf(timebox()))
        assertEquals("Yesterday: Under your limit · 1 goal done", under.headline())

        val many = build(
            aggregates = listOf(usage(minutes = 10)),
            allowance = 60,
            timeboxes = listOf(timebox(startAt = at(date, 9)), timebox(startAt = at(date, 10)), timebox(startAt = at(date, 11))),
            wentBack = 4,
        )
        assertEquals("Yesterday: Under your limit · 3 goals done", many.headline())
    }

    @Test fun headlineFallsBackToWentBack() {
        val day = build(allowance = null, wentBack = 3)
        assertTrue(day.hasContent)
        assertEquals("Yesterday: went back 3×", day.headline())
        assertTrue(day.lines().contains("You went back instead of opening an app 3 times"))
        assertTrue(build(allowance = null, wentBack = 1).lines().contains("You went back instead of opening an app 1 time"))
    }

    @Test fun underAllowanceIsInclusive() {
        val day = build(aggregates = listOf(usage(minutes = 60)), allowance = 60)
        assertEquals(true, day.underAllowance)
        assertEquals("Yesterday: Under your limit", day.headline())
    }
}
