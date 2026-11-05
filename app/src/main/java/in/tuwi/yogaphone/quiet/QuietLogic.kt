package `in`.tuwi.yogaphone.quiet

/** Why the greyscale intervention is on, in the order they are checked. */
enum class GreyReason { FOCUS, BEDTIME, OVER_LIMIT }

data class GreyTriggers(val overLimit: Boolean, val duringFocus: Boolean, val bedtime: Boolean)

object GreyscalePolicy {
    /** Null when greyscale should be off. A snooze silences every trigger until it runs out. */
    fun reason(
        triggers: GreyTriggers,
        focusActive: Boolean,
        bedtimeActive: Boolean,
        overLimit: Boolean,
        snoozedUntil: Long,
        now: Long,
    ): GreyReason? {
        if (now < snoozedUntil) return null
        return when {
            triggers.duringFocus && focusActive -> GreyReason.FOCUS
            triggers.bedtime && bedtimeActive -> GreyReason.BEDTIME
            triggers.overLimit && overLimit -> GreyReason.OVER_LIMIT
            else -> null
        }
    }

    fun label(reason: GreyReason) = when (reason) {
        GreyReason.FOCUS -> "focus is on"
        GreyReason.BEDTIME -> "bedtime"
        GreyReason.OVER_LIMIT -> "past today's limit"
    }
}

enum class NotificationPolicy { ALLOW, HOLD_DURING_FOCUS, ALWAYS_HOLD }

data class IncomingNotification(
    val packageName: String,
    val category: String?,
    val ongoing: Boolean,
    val foregroundService: Boolean,
    val mediaSession: Boolean,
    val groupSummary: Boolean,
)

object HoldDecider {
    private val neverHoldCategories = setOf("call", "alarm", "navigation", "reminder", "sys", "transport", "stopwatch")

    /** True when the notification should be taken out of the shade and kept for the digest. */
    fun shouldHold(
        notification: IncomingNotification,
        policy: NotificationPolicy,
        focusActive: Boolean,
        ownPackage: String,
    ): Boolean {
        if (notification.packageName == ownPackage) return false
        if (notification.ongoing || notification.foregroundService || notification.mediaSession) return false
        if (notification.category in neverHoldCategories) return false
        // Summaries carry no content of their own; their children are judged individually.
        if (notification.groupSummary) return false
        return when (policy) {
            NotificationPolicy.ALLOW -> false
            NotificationPolicy.HOLD_DURING_FOCUS -> focusActive
            NotificationPolicy.ALWAYS_HOLD -> true
        }
    }

    fun defaultPolicy(distracting: Boolean, essential: Boolean): NotificationPolicy = when {
        essential -> NotificationPolicy.ALLOW
        distracting -> NotificationPolicy.HOLD_DURING_FOCUS
        else -> NotificationPolicy.ALLOW
    }
}

data class HeldItem(val key: String, val packageName: String, val title: String, val text: String, val postedAt: Long)

data class DigestGroup(val packageName: String, val items: List<HeldItem>)

object Digest {
    const val KEEP_MS = 24 * 60 * 60 * 1000L

    /** Groups by app, apps with the newest item first, items newest first; drops items older than a day. */
    fun group(items: List<HeldItem>, now: Long): List<DigestGroup> = items
        .filter { now - it.postedAt < KEEP_MS }
        .groupBy { it.packageName }
        .map { (pkg, list) -> DigestGroup(pkg, list.sortedByDescending { it.postedAt }) }
        .sortedByDescending { it.items.first().postedAt }

    fun summary(groups: List<DigestGroup>): String {
        val count = groups.sumOf { it.items.size }
        val apps = groups.size
        return "$count notification${if (count == 1) "" else "s"} held from $apps app${if (apps == 1) "" else "s"}"
    }

    /** The next digest time strictly after [nowMinuteOfDay], as minutes from now (wrapping to tomorrow). */
    fun minutesUntilNext(times: List<Int>, nowMinuteOfDay: Int): Int? {
        if (times.isEmpty()) return null
        return times.map { t -> ((t - nowMinuteOfDay) + 1440) % 1440 }.map { if (it == 0) 1440 else it }.min()
    }
}
