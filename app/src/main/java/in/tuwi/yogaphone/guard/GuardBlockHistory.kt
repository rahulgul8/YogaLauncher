package `in`.tuwi.yogaphone.guard

import java.time.LocalDate

/** Replace a day's aggregate; repeated saves never add the same blocks twice. */
fun guardBlockHistory(rows: Set<String>, previous: GuardDaily, next: GuardDaily?, today: LocalDate): Set<String> {
    val counts = rows.mapNotNull { row ->
        val parts = row.split('|')
        val day = runCatching { LocalDate.parse(parts[0]) }.getOrNull() ?: return@mapNotNull null
        day to GuardSurface.entries.associateWith { parts.getOrNull(it.ordinal + 1)?.toIntOrNull()?.coerceAtLeast(0) ?: 0 }
    }.toMap().toMutableMap()
    // Migrate the pre-ring daily counter without replacing a newer row.
    runCatching { LocalDate.parse(previous.date) }.getOrNull()?.let { counts.putIfAbsent(it, previous.blocks) }
    next?.let { value -> runCatching { LocalDate.parse(value.date) }.getOrNull()?.let { counts[it] = value.blocks } }
    return counts.filterKeys { !it.isBefore(today.minusDays(6)) && !it.isAfter(today) }.map { (day, values) ->
        (listOf(day.toString()) + GuardSurface.entries.map { (values[it] ?: 0).coerceAtLeast(0).toString() }).joinToString("|")
    }.toSet()
}
