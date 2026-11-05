package `in`.tuwi.yogaphone.insights

import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

enum class CoachingTone { GENTLE, DATA_DRIVEN }

/** Everything the engine needs, already aggregated per local day. Missing days are absent, never zero. */
data class InsightInput(
    val today: LocalDate,
    /** Eligible (counted) minutes per sampled day. */
    val eligibleByDay: Map<LocalDate, Long>,
    /** Eligible minutes per day and hour (0..23), for sampled days. */
    val hourly: Map<LocalDate, LongArray>,
    val allowanceMinutes: Int?,
    val timeboxesCompleted: Int,
    val timeboxesExtended: Int,
    val focusMinutesThisWeek: Long,
    val streak: Int,
    /** Category name to eligible minutes this week. */
    val categoryMinutesThisWeek: Map<String, Long>,
    val weeklyTimeValue: String?,
    val nowHour: Int,
    /** Unlock count per sampled day (Android 9+). */
    val unlocksByDay: Map<LocalDate, Int> = emptyMap(),
)

data class InsightCard(val id: String, val title: String, val body: String)

object InsightEngine {
    const val MIN_DAYS_FOR_COMPARISON = 5
    private val LATE_HOURS = listOf(23, 0, 1, 2, 3, 4)

    fun thisWeek(today: LocalDate) = (0L..6L).map { today.minusDays(it) }.toSet()
    fun lastWeek(today: LocalDate) = (7L..13L).map { today.minusDays(it) }.toSet()

    /** Average per sampled day, or null when fewer than [MIN_DAYS_FOR_COMPARISON] days were sampled. */
    fun averageOver(days: Set<LocalDate>, values: Map<LocalDate, Long>): Double? {
        val known = days.mapNotNull { values[it] }
        return if (known.size < MIN_DAYS_FOR_COMPARISON) null else known.average()
    }

    /** Percent change from [before] to [after], or null when there is no valid baseline. */
    fun percentChange(before: Double?, after: Double?): Int? {
        if (before == null || after == null || before <= 0.0) return null
        return ((after - before) / before * 100).roundToInt()
    }

    fun lateNightMinutes(hours: LongArray): Long = LATE_HOURS.sumOf { hours[it] }

    /** Longest run of whole waking hours (07–23) with no eligible use, up to [untilHour] (exclusive). */
    fun longestBreakHours(hours: LongArray, untilHour: Int): Int {
        var best = 0
        var run = 0
        for (h in 7 until untilHour.coerceIn(7, 23)) {
            if (hours[h] == 0L) { run++; best = maxOf(best, run) } else run = 0
        }
        return best
    }

    fun cards(input: InsightInput, tone: CoachingTone): List<InsightCard> {
        val cards = mutableListOf<InsightCard>()
        val gentle = tone == CoachingTone.GENTLE
        val week = thisWeek(input.today)
        val prior = lastWeek(input.today)

        percentChange(averageOver(prior, input.eligibleByDay), averageOver(week, input.eligibleByDay))?.let { change ->
            val body = when {
                change < 0 && gentle -> "This week you spent ${abs(change)}% less time in distracting apps than last week. That's real progress."
                change < 0 -> "Distracting-app time: ${abs(change)}% lower per day than last week."
                change == 0 -> "About the same time in distracting apps as last week."
                gentle -> "A little more time in distracting apps than last week (${change}%). Tomorrow is a fresh start."
                else -> "Distracting-app time: ${change}% higher per day than last week."
            }
            cards += InsightCard("week_change", "This week", body)
        }

        val lateNow = averageOver(week, input.hourly.mapValues { lateNightMinutes(it.value) })
        val lateBefore = averageOver(prior, input.hourly.mapValues { lateNightMinutes(it.value) })
        percentChange(lateBefore, lateNow)?.takeIf { it != 0 }?.let { change ->
            val body = if (change < 0) "Late-night use (11 pm–5 am) is down ${abs(change)}% compared with last week."
            else "Late-night use (11 pm–5 am) is up ${change}% compared with last week."
            cards += InsightCard("late_night", "Late nights", body)
        }

        if (input.allowanceMinutes != null) {
            val sampled = week.filter { it in input.eligibleByDay }
            if (sampled.isNotEmpty()) {
                val within = sampled.count { (input.eligibleByDay[it] ?: 0) <= input.allowanceMinutes }
                cards += InsightCard("within_allowance", "Within your allowance",
                    "$within of ${sampled.size} recorded days this week stayed within ${input.allowanceMinutes} minutes.")
            }
        }

        if (input.timeboxesCompleted + input.timeboxesExtended > 0) {
            cards += InsightCard("timeboxes", "Timeboxes",
                "${input.timeboxesCompleted} completed this week" + if (input.timeboxesExtended > 0) ", ${input.timeboxesExtended} extended." else ".")
        }

        if (input.focusMinutesThisWeek > 0 || input.streak > 0) {
            val streak = if (input.streak > 1) " Current streak: ${input.streak} days." else ""
            cards += InsightCard("focus", "Focus", "${input.focusMinutesThisWeek} minutes of focus this week.$streak")
        }

        input.categoryMinutesThisWeek.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.let { (name, minutes) ->
            cards += InsightCard("top_category", "Where the time went", "$name took the most time this week: $minutes minutes.")
        }

        input.weeklyTimeValue?.let {
            cards += InsightCard("time_value", "Value of time", "Time past your limit this week ≈ $it. An illustrative value, not money spent.")
        }

        val weekUnlocks = week.mapNotNull { input.unlocksByDay[it] }.filter { it > 0 }
        if (weekUnlocks.size >= 3) {
            cards += InsightCard("unlocks", "Unlocks", "About ${weekUnlocks.average().roundToInt()} unlocks a day this week.")
        }

        input.hourly[input.today]?.let { hours ->
            val gap = longestBreakHours(hours, input.nowHour)
            if (gap >= 2) cards += InsightCard("break", "Longest break today",
                "About $gap hours in a row without distracting apps." + if (gentle) " Nice." else "")
        }
        return cards
    }

    /** Suggests lowering the allowance by 15 minutes after a strong week. Never applied automatically. */
    fun suggestedAllowance(allowance: Int?, eligibleByDay: Map<LocalDate, Long>, today: LocalDate): Int? {
        if (allowance == null || allowance <= 15) return null
        val lastSeven = (1L..7L).map { today.minusDays(it) }.mapNotNull { eligibleByDay[it] }
        if (lastSeven.size < MIN_DAYS_FOR_COMPARISON) return null
        return if (lastSeven.count { it <= allowance - 15 } >= 5) allowance - 15 else null
    }
}
