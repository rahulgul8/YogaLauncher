package `in`.tuwi.yogaphone.insights

import `in`.tuwi.yogaphone.data.db.FocusSessionEntity
import `in`.tuwi.yogaphone.data.db.FocusStatus
import `in`.tuwi.yogaphone.data.db.TimeboxSessionEntity
import `in`.tuwi.yogaphone.data.db.TimeboxStatus
import `in`.tuwi.yogaphone.data.db.UsageAggregateEntity
import java.time.LocalDate
import java.time.ZoneId

data class GoalDone(val title: String, val plannedMinutes: Int, val actualMinutes: Int, val kind: Kind) {
    enum class Kind { TIMEBOX, FOCUS }
}

data class DayAchievements(
    val date: LocalDate,
    /** Minutes on apps counted against the allowance (UsageAggregateEntity.eligible). Null when there is no usage data for the day. */
    val distractingMinutes: Int?,
    /** The overall daily allowance in minutes, or null when none is set. */
    val allowanceMinutes: Int?,
    /** The same figure for the day before, or null when unknown. */
    val previousDistractingMinutes: Int?,
    val goals: List<GoalDone>,
    /** How many times the user chose to go back at a pause screen. */
    val wentBack: Int,
) {
    val underAllowance: Boolean?
        get() {
            val distracting = distractingMinutes ?: return null
            val allowance = allowanceMinutes ?: return null
            return distracting <= allowance
        }

    val lessThanDayBefore: Int?
        get() {
            val previous = previousDistractingMinutes ?: return null
            val current = distractingMinutes ?: return null
            return if (previous > current) previous - current else null
        }

    val hasContent: Boolean
        get() = distractingMinutes != null || goals.isNotEmpty() || wentBack > 0

    fun headline(): String {
        val parts = mutableListOf<String>()
        when {
            underAllowance == true -> parts += "Under your limit"
            distractingMinutes != null -> parts += "${minutes(distractingMinutes)} on distracting apps"
        }
        when {
            goals.size == 1 -> parts += "1 goal done"
            goals.size > 1 -> parts += "${goals.size} goals done"
            else -> {
                val less = lessThanDayBefore
                when {
                    less != null -> parts += "${minutes(less)} less than the day before"
                    wentBack > 0 -> parts += "went back ${wentBack}×"
                }
            }
        }
        if (parts.isEmpty()) return "Yesterday"
        return "Yesterday: " + parts.take(2).joinToString(" · ")
    }

    fun lines(): List<String> {
        val out = mutableListOf<String>()
        val distracting = distractingMinutes
        if (distracting != null) {
            val allowance = allowanceMinutes
            out += when {
                allowance == null -> "${minutes(distracting)} on distracting apps"
                distracting <= allowance -> "${minutes(distracting)} on distracting apps, within your ${minutes(allowance)} limit"
                else -> "${minutes(distracting)} on distracting apps, ${minutes(distracting - allowance)} past your limit"
            }
        }
        lessThanDayBefore?.let { out += "${minutes(it)} less than the day before" }
        goals.forEach { goal ->
            out += goal.title + ": " + minutes(goal.actualMinutes) +
                if (goal.actualMinutes != goal.plannedMinutes) " of ${minutes(goal.plannedMinutes)}" else ""
        }
        if (wentBack > 0) {
            out += "You went back instead of opening an app " + if (wentBack == 1) "1 time" else "$wentBack times"
        }
        return out
    }
}

private fun minutes(m: Int): String = when {
    m < 60 -> "$m min"
    m % 60 == 0 -> "${m / 60} h"
    else -> "${m / 60} h ${m % 60} min"
}

object DayAchievementsBuilder {
    fun build(
        date: LocalDate,
        zone: ZoneId,
        aggregates: List<UsageAggregateEntity>,
        allowanceMinutes: Int?,
        timeboxes: List<TimeboxSessionEntity>,
        focus: List<FocusSessionEntity>,
        wentBack: Int,
    ): DayAchievements {
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val nextStart = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        fun minutesOn(day: LocalDate): Int? {
            val key = day.toString()
            val rows = aggregates.filter { it.date == key }
            if (rows.isEmpty()) return null
            return rows.filter { it.eligible }.sumOf { it.foregroundMinutes }.toInt()
        }

        val found = mutableListOf<Pair<Long, GoalDone>>()

        timeboxes.forEach { session ->
            if (session.status != TimeboxStatus.COMPLETED) return@forEach
            if (session.startedAt < dayStart || session.startedAt >= nextStart) return@forEach
            val title = session.purpose?.trim()?.takeIf { it.isNotEmpty() } ?: "Timebox"
            found += session.startedAt to GoalDone(
                title = title,
                plannedMinutes = session.plannedMinutes + session.extendedMinutes,
                actualMinutes = (((session.endedAt ?: session.endAt) - session.startedAt).coerceAtLeast(0L) / 60_000).toInt(),
                kind = GoalDone.Kind.TIMEBOX,
            )
        }

        focus.forEach { session ->
            if (session.status != FocusStatus.COMPLETED) return@forEach
            if (session.startAt < dayStart || session.startAt >= nextStart) return@forEach
            val planned = ((session.plannedEndAt ?: session.endedAt ?: session.startAt) - session.startAt).coerceAtLeast(0L)
            val actual = ((session.endedAt ?: session.plannedEndAt ?: session.startAt) - session.startAt).coerceAtLeast(0L)
            found += session.startAt to GoalDone(
                title = "Focus",
                plannedMinutes = (planned / 60_000).toInt(),
                actualMinutes = (actual / 60_000).toInt(),
                kind = GoalDone.Kind.FOCUS,
            )
        }

        return DayAchievements(
            date = date,
            distractingMinutes = minutesOn(date),
            allowanceMinutes = allowanceMinutes,
            previousDistractingMinutes = minutesOn(date.minusDays(1)),
            goals = found.sortedBy { it.first }.map { it.second },
            wentBack = wentBack,
        )
    }
}
