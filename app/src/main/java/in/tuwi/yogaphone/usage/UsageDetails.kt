package `in`.tuwi.yogaphone.usage

import `in`.tuwi.yogaphone.data.db.UsageAggregateEntity
import `in`.tuwi.yogaphone.data.db.UsageHourEntity
import `in`.tuwi.yogaphone.insights.GoalDone
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** What the Usage view shows for one day besides app minutes: the hourly chart, unlocks and finished goals. */
data class UsageDayDetails(
    val date: LocalDate,
    /** 24 values, minutes on allowance apps per local clock hour. */
    val hours: List<Long>,
    /** Null when the day was never sampled. */
    val unlocks: Int?,
    val goals: List<GoalDone>,
)

/** Pure helpers for the Usage details view (L-F4). */
object UsageDetails {
    /** The view steps back through today and the six days before it, matching the stored week. */
    const val DAYS_BACK = 6L

    fun weekDays(today: LocalDate): List<LocalDate> = (DAYS_BACK downTo 0L).map(today::minusDays)

    fun canStepBack(day: LocalDate, today: LocalDate): Boolean = day > today.minusDays(DAYS_BACK)
    fun canStepForward(day: LocalDate, today: LocalDate): Boolean = day < today

    /** Moves [day] by [delta] days, kept inside the week the view can show. */
    fun step(day: LocalDate, delta: Long, today: LocalDate): LocalDate {
        val moved = day.plusDays(delta)
        val first = today.minusDays(DAYS_BACK)
        return when {
            moved < first -> first
            moved > today -> today
            else -> moved
        }
    }

    /** The value bars are scaled against: the tallest bar, or the allowance line when it is higher. */
    fun scaleTop(values: List<Long?>, reference: Long? = null): Long =
        maxOf(values.maxOfOrNull { it ?: 0L } ?: 0L, reference ?: 0L).coerceAtLeast(0L)

    /** Bar heights from 0 to 1. A null value (day not recorded) stays null so it is drawn as missing, not as zero. */
    fun barFractions(values: List<Long?>, reference: Long? = null): List<Float?> {
        val top = scaleTop(values, reference)
        return values.map { value ->
            value?.let { if (top <= 0L) 0f else (it.coerceAtLeast(0L).toFloat() / top).coerceIn(0f, 1f) }
        }
    }

    /** Height of the allowance hairline from 0 to 1, or null when there is no allowance or nothing to scale. */
    fun referenceFraction(values: List<Long?>, reference: Long?): Float? {
        val line = reference?.takeIf { it > 0 } ?: return null
        val top = scaleTop(values, reference)
        return if (top <= 0L) null else (line.toFloat() / top).coerceIn(0f, 1f)
    }

    /** 24 hourly values from stored hour rows; missing hours are zero. */
    fun hourValues(rows: List<UsageHourEntity>): List<Long> =
        (0..23).map { hour -> rows.filter { it.hour == hour }.sumOf { it.eligibleMinutes.coerceAtLeast(0L) } }

    /** Minutes per app for the given dates (all rows when null), largest first, apps with no time dropped. */
    fun appMinutes(rows: List<UsageAggregateEntity>, dates: Set<String>? = null): List<Pair<String, Long>> =
        rows.asSequence()
            .filter { dates == null || it.date in dates }
            .groupBy { it.packageName }
            .map { (pkg, values) -> pkg to values.sumOf { it.foregroundMinutes.coerceAtLeast(0L) } }
            .filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<String, Long>> { it.second }.thenBy { it.first })

    /** Minutes on allowance apps per day for [days]; null for a day that was never recorded. */
    fun dailyEligible(rows: List<UsageAggregateEntity>, days: List<LocalDate>, knownDays: Set<String>): List<Long?> =
        days.map { day ->
            val key = day.toString()
            val dayRows = rows.filter { it.date == key }
            if (key !in knownDays && dayRows.isEmpty()) null
            else dayRows.filter { it.eligible }.sumOf { it.foregroundMinutes.coerceAtLeast(0L) }
        }

    fun dayTitle(day: LocalDate, today: LocalDate): String = when (day) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> day.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault()))
    }

    /** One-letter weekday for the week chart. */
    fun dayInitial(day: LocalDate): String = day.format(DateTimeFormatter.ofPattern("EEEEE", Locale.getDefault()))

    fun hourRange(hour: Int): String = "%02d:00–%02d:00".format(hour, (hour + 1) % 24)

    /** "Finished in 25 of 30 min" style line for a goal. */
    fun goalLine(goal: GoalDone): String = when {
        goal.plannedMinutes <= 0 -> "${goal.actualMinutes} min"
        goal.actualMinutes == goal.plannedMinutes -> "${goal.actualMinutes} min, as planned"
        else -> "${goal.actualMinutes} of ${goal.plannedMinutes} min planned"
    }
}
