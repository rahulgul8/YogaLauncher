package `in`.tuwi.yogaphone.usage

import `in`.tuwi.yogaphone.data.db.UsageAggregateEntity
import `in`.tuwi.yogaphone.guard.LearningAccounting
import `in`.tuwi.yogaphone.guard.LearningDetector
import java.time.LocalDate
import java.time.ZoneId

internal const val USAGE_EVENT_LOOKBACK_DAYS = 1L

internal fun usageEventQueryStart(date: LocalDate, zone: ZoneId): Long =
    date.minusDays(USAGE_EVENT_LOOKBACK_DAYS).atStartOfDay(zone).toInstant().toEpochMilli()

internal fun usageBackfillDates(today: LocalDate): List<LocalDate> = (1L..6L).map(today::minusDays)

internal data class LocalHourWindow(val hour: Int, val start: Long, val end: Long)

/** Timeline-hour windows labelled with their local clock hour. Repeated DST hours are deliberately returned twice. */
internal fun localHourWindows(date: LocalDate, zone: ZoneId): List<LocalHourWindow> {
    val dayStart = date.atStartOfDay(zone).toInstant()
    val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant()
    val result = mutableListOf<LocalHourWindow>()
    var cursor = dayStart
    while (cursor < dayEnd) {
        val next = minOf(cursor.plusSeconds(3_600), dayEnd)
        result += LocalHourWindow(cursor.atZone(zone).hour, cursor.toEpochMilli(), next.toEpochMilli())
        cursor = next
    }
    return result
}

internal fun roundedMinutes(millis: Long): Long = (millis.coerceAtLeast(0) + 30_000L) / 60_000L

/**
 * Allocates whole minutes without rounding every app independently. The eligible subtotal and all-app total
 * both equal their independently rounded millisecond totals; the unavoidable sub-minute remainder is assigned
 * deterministically to the apps with the largest remainders.
 */
internal fun allocateUsageMinutes(usageMillis: Map<String, Long>, eligiblePackages: Set<String>): Map<String, Long> {
    val clean = usageMillis.mapValues { it.value.coerceAtLeast(0) }
    val eligibleTarget = roundedMinutes(clean.filterKeys { it in eligiblePackages }.values.sum())
    val totalTarget = roundedMinutes(clean.values.sum())

    fun allocate(packages: Set<String>, target: Long): Map<String, Long> {
        if (packages.isEmpty()) return emptyMap()
        val base = packages.associateWith { (clean[it] ?: 0L) / 60_000L }.toMutableMap()
        var extras = (target - base.values.sum()).coerceAtLeast(0)
        packages.sortedWith(compareByDescending<String> { (clean[it] ?: 0L) % 60_000L }.thenBy { it })
            .forEach { pkg -> if (extras > 0) { base[pkg] = base.getValue(pkg) + 1; extras-- } }
        return base
    }

    val packages = clean.keys
    val eligible = packages.filterTo(mutableSetOf()) { it in eligiblePackages }
    val other = packages - eligible
    return allocate(eligible, eligibleTarget) + allocate(other, totalTarget - eligibleTarget)
}

/** Rows used by allowances. Learning video remains in raw all-app totals but does not count against the user. */
internal fun applyLearningExemption(
    rows: List<UsageAggregateEntity>,
    learningSecondsByKey: Map<String, Long>,
    enabled: Boolean,
): List<UsageAggregateEntity> {
    if (!enabled || learningSecondsByKey.isEmpty()) return rows
    return rows.map { row ->
        if (row.packageName != LearningDetector.YOUTUBE_PACKAGE) row
        else row.copy(foregroundMinutes = LearningAccounting.effectiveMinutes(
            row.foregroundMinutes,
            LearningAccounting.totalFor(learningSecondsByKey, row.date, row.packageName),
        ))
    }
}
