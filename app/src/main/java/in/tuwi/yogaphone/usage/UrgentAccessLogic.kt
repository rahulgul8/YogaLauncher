package `in`.tuwi.yogaphone.usage

data class UrgentOpenRecord(val date: String, val packageName: String, val count: Int)
data class UrgentWindow(val packageName: String, val until: Long)

data class UrgentAccessState(
    val records: List<UrgentOpenRecord> = emptyList(),
    val windows: Map<String, UrgentWindow> = emptyMap(),
) {
    fun usedOn(date: String): Int = records.filter { it.date == date }.sumOf { it.count }
    fun remainingOn(date: String): Int = (UrgentAccessPolicy.MAX_PER_DAY - usedOn(date)).coerceAtLeast(0)
    fun usedByApp(date: String, packageName: String): Int =
        records.firstOrNull { it.date == date && it.packageName == packageName }?.count ?: 0
}

object UrgentAccessPolicy {
    const val MAX_PER_DAY = 3
    const val WINDOW_MILLIS = 5 * 60_000L
    const val HOLD_MILLIS = 3_000L
    const val REMINDER_MILLIS = 5 * 60_000L

    fun canGrant(records: List<UrgentOpenRecord>, date: String): Boolean =
        records.filter { it.date == date }.sumOf { it.count } < MAX_PER_DAY

    fun record(records: List<UrgentOpenRecord>, date: String, packageName: String): List<UrgentOpenRecord> {
        if (!canGrant(records, date)) return records
        val current = records.firstOrNull { it.date == date && it.packageName == packageName }
        return records.filterNot { it.date == date && it.packageName == packageName } +
            UrgentOpenRecord(date, packageName, (current?.count ?: 0) + 1)
    }
}

data class UrgentInterventionDecision(
    val grey: Boolean,
    val remind: Boolean,
    val clearPackages: Set<String>,
)

/** Decides what an expired urgent window needs without reading Android state. */
object UrgentWindowPolicy {
    fun decide(
        windows: Collection<UrgentWindow>,
        foregroundPackage: String,
        now: Long,
        canGrey: Boolean,
        lastReminderAt: Long?,
    ): UrgentInterventionDecision {
        val expired = windows.filter { now >= it.until }
        val current = expired.firstOrNull { it.packageName == foregroundPackage }
        return UrgentInterventionDecision(
            grey = current != null && canGrey,
            remind = current != null && !canGrey &&
                (lastReminderAt == null || now - lastReminderAt >= UrgentAccessPolicy.REMINDER_MILLIS),
            clearPackages = expired.filterNot { it.packageName == foregroundPackage }.mapTo(linkedSetOf()) { it.packageName },
        )
    }

    fun isLive(window: UrgentWindow?, now: Long): Boolean = window != null && now < window.until
}

/** Compact preference encoding; package names cannot contain the separator. */
object UrgentAccessCodec {
    private const val SEPARATOR = "|"

    fun encodeRecords(records: List<UrgentOpenRecord>): Set<String> = records
        .filter { it.count > 0 }
        .mapTo(linkedSetOf()) { "${it.date}$SEPARATOR${it.packageName}$SEPARATOR${it.count}" }

    fun decodeRecords(values: Set<String>): List<UrgentOpenRecord> = values.mapNotNull { value ->
        val parts = value.split(SEPARATOR)
        val count = parts.getOrNull(2)?.toIntOrNull() ?: return@mapNotNull null
        val date = parts.getOrNull(0).orEmpty()
        val packageName = parts.getOrNull(1).orEmpty()
        UrgentOpenRecord(date, packageName, count).takeIf { date.isNotBlank() && packageName.isNotBlank() && count > 0 }
    }

    fun encodeWindows(windows: Collection<UrgentWindow>): Set<String> = windows
        .mapTo(linkedSetOf()) { "${it.packageName}$SEPARATOR${it.until}" }

    fun decodeWindows(values: Set<String>): Map<String, UrgentWindow> = values.mapNotNull { value ->
        val split = value.lastIndexOf(SEPARATOR)
        if (split <= 0) return@mapNotNull null
        val until = value.substring(split + 1).toLongOrNull() ?: return@mapNotNull null
        UrgentWindow(value.substring(0, split), until)
    }.associateBy { it.packageName }
}

