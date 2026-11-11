package `in`.tuwi.yogaphone.focus

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import `in`.tuwi.yogaphone.resist.ResistanceLadder

private const val MINUTE_MS = 60_000L

/**
 * Timebox timing. A running timebox has [endAt]; a paused one keeps [pausedRemainingMs] instead,
 * so pausing never needs to remember how long it was paused.
 */
data class TimeboxClock(val endAt: Long, val pausedRemainingMs: Long?) {
    val isPaused: Boolean get() = pausedRemainingMs != null

    fun remainingMs(now: Long): Long = (pausedRemainingMs ?: (endAt - now)).coerceAtLeast(0)
    fun pause(now: Long): TimeboxClock = if (isPaused) this else TimeboxClock(endAt, remainingMs(now))
    fun resume(now: Long): TimeboxClock = pausedRemainingMs?.let { TimeboxClock(now + it, null) } ?: this

    /** Extending a finished timebox restarts the countdown from now. */
    fun extend(minutes: Int, now: Long): TimeboxClock {
        val add = minutes * MINUTE_MS
        return when {
            pausedRemainingMs != null -> copy(pausedRemainingMs = pausedRemainingMs + add)
            endAt <= now -> TimeboxClock(now + add, null)
            else -> copy(endAt = endAt + add)
        }
    }

    companion object {
        fun start(minutes: Int, now: Long) = TimeboxClock(now + minutes * MINUTE_MS, null)
    }
}

/** A recurring focus window. When [end] is not after [start] the window runs past midnight. */
data class FocusWindow(
    val id: Long,
    val name: String,
    val days: Set<DayOfWeek>,
    val start: LocalTime,
    val end: LocalTime,
    val enabled: Boolean = true,
)

data class ScheduledSpan(val window: FocusWindow, val start: Instant, val end: Instant)

object ScheduleEvaluator {
    /** Every occurrence of [window] whose start falls on a day in [from]..[to] (inclusive), in [zone]. */
    fun occurrences(window: FocusWindow, from: LocalDate, to: LocalDate, zone: ZoneId): List<ScheduledSpan> {
        if (!window.enabled || window.days.isEmpty() || window.start == window.end) return emptyList()
        val spans = mutableListOf<ScheduledSpan>()
        var day = from
        while (!day.isAfter(to)) {
            if (day.dayOfWeek in window.days) {
                val endDay = if (window.end.isAfter(window.start)) day else day.plusDays(1)
                // ZonedDateTime.of moves times inside a DST gap forward, and picks the earlier offset in an overlap.
                val start = ZonedDateTime.of(day, window.start, zone).toInstant()
                val end = ZonedDateTime.of(endDay, window.end, zone).toInstant()
                if (end.isAfter(start)) spans += ScheduledSpan(window, start, end)
            }
            day = day.plusDays(1)
        }
        return spans
    }

    /** The span active at [at], preferring the one that ends last when windows overlap. */
    fun activeAt(windows: List<FocusWindow>, at: Instant, zone: ZoneId): ScheduledSpan? {
        val today = at.atZone(zone).toLocalDate()
        return windows.flatMap { occurrences(it, today.minusDays(1), today, zone) }
            .filter { !it.start.isAfter(at) && it.end.isAfter(at) }
            .maxByOrNull { it.end }
    }

    /** The next instant strictly after [at] when any window starts or ends, within the coming 8 days. */
    fun nextTransition(windows: List<FocusWindow>, at: Instant, zone: ZoneId): Instant? {
        val today = at.atZone(zone).toLocalDate()
        return windows.flatMap { occurrences(it, today.minusDays(1), today.plusDays(8), zone) }
            .flatMap { listOf(it.start, it.end) }
            .filter { it.isAfter(at) }
            .minOrNull()
    }
}

object StreakCalculator {
    /**
     * Consecutive days with at least one completed session, ending today, or ending yesterday when
     * nothing has been completed yet today (a streak is not broken until the day is over).
     */
    fun streak(completedDays: Set<LocalDate>, today: LocalDate): Int {
        var day = if (today in completedDays) today else today.minusDays(1)
        var count = 0
        while (day in completedDays) { count++; day = day.minusDays(1) }
        return count
    }
}

object DelayGate {
    /** True when the mindful pause should be shown, given when the user last chose to continue to this app. */
    fun shouldPause(lastContinuedAt: Long?, now: Long, dontAskMinutes: Int): Boolean {
        if (lastContinuedAt == null || dontAskMinutes <= 0) return true
        val elapsed = now - lastContinuedAt
        return elapsed < 0 || elapsed >= dontAskMinutes * MINUTE_MS
    }

    fun encode(continued: Map<String, Long>): Set<String> = continued.map { (pkg, at) -> "$pkg|$at" }.toSet()
    fun decode(raw: Set<String>): Map<String, Long> = raw.mapNotNull { entry ->
        val split = entry.lastIndexOf('|')
        if (split <= 0) return@mapNotNull null
        entry.substring(split + 1).toLongOrNull()?.let { entry.substring(0, split) to it }
    }.toMap()

    /** Drops entries older than a day so the stored set stays small. */
    fun record(continued: Map<String, Long>, packageName: String, now: Long): Map<String, Long> =
        continued.filterValues { now - it < 24 * 60 * MINUTE_MS } + (packageName to now)
}

/** Pure persistence and selection rules for a mindful-pause pass. */
object PausePassGate {
    fun isActive(until: Long?, now: Long): Boolean = until != null && until > now

    fun encode(passes: Map<String, Long>): Set<String> =
        passes.map { (pkg, until) -> "$pkg|$until" }.toSet()

    fun decode(raw: Set<String>): Map<String, Long> = raw.mapNotNull { entry ->
        val split = entry.lastIndexOf('|')
        if (split <= 0) return@mapNotNull null
        entry.substring(split + 1).toLongOrNull()?.let { entry.substring(0, split) to it }
    }.toMap()

    fun active(passes: Map<String, Long>, now: Long): Map<String, Long> =
        passes.filterValues { it > now }

    fun record(passes: Map<String, Long>, packageName: String, until: Long, now: Long): Map<String, Long> =
        active(passes, now) + (packageName to until)

    /** The saved per-app choice wins; the old global setting is only a first-use migration fallback. */
    fun preselect(lastMinutes: Int?, legacyMinutes: Int, choices: List<Int>): Int =
        ResistanceLadder.preselect(lastMinutes ?: legacyMinutes, choices)
}

/** Newest first, case-insensitively de-duplicated, capped (default 3). */
fun updateRecentList(current: List<String>, entry: String, cap: Int = 3): List<String> {
    val clean = entry.trim()
    if (clean.isEmpty()) return current.take(cap)
    return (listOf(clean) + current.filterNot { it.equals(clean, ignoreCase = true) }).take(cap)
}

fun formatMinutesLeft(remainingMs: Long): String {
    val minutes = (remainingMs + MINUTE_MS - 1) / MINUTE_MS
    return if (minutes >= 60) "${minutes / 60} h ${minutes % 60} min left" else "$minutes min left"
}
