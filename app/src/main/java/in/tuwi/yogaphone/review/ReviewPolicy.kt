package `in`.tuwi.yogaphone.review

import java.time.LocalDate

enum class ReviewMoment { R1_BLOCK_FROM_DATA, R2_GOOD_WEEK, R3_THIRD_SESSION, R4_STRONG_BLOCK_DAY }

data class ReviewSignals(
    val now: Long,
    val installedAt: Long,
    val homeSessions: Int,
    val isDefaultHome: Boolean,
    val onboarding: Boolean,
    val lastInterruptionAt: Long?,
    val overLimitToday: Boolean,
    val asks: List<Long>,
)

object ReviewPolicy {
    const val DAY_MILLIS = 24L * 60L * 60L * 1_000L
    const val FEEDBACK_WINDOW_MILLIS = 7L * DAY_MILLIS
    private const val INTERRUPTION_MILLIS = 10L * 60L * 1_000L
    private const val ASK_COOLDOWN_MILLIS = 90L * DAY_MILLIS

    fun mayAsk(signals: ReviewSignals): Boolean =
        signals.now - signals.installedAt >= 3L * DAY_MILLIS &&
            signals.homeSessions >= 5 &&
            signals.isDefaultHome &&
            !signals.onboarding &&
            signals.lastInterruptionAt?.let { signals.now - it < INTERRUPTION_MILLIS } != true &&
            !signals.overLimitToday &&
            signals.asks.size < 3 &&
            signals.asks.none { signals.now - it < ASK_COOLDOWN_MILLIS }

    fun isGoodWeek(
        daysWithin: Int,
        knownDays: Int,
        thisWeekMinutes: Long,
        lastWeekMinutes: Long?,
    ): Boolean {
        if (knownDays < 5) return false
        val improved = lastWeekMinutes?.takeIf { it > 0 }?.let { thisWeekMinutes * 5 <= it * 4 } == true
        return daysWithin >= 5 || improved
    }

    fun isStrongBlockDay(blocks: Int, overLimit: Boolean): Boolean = blocks >= 5 && !overLimit

    fun isEvening(hour: Int): Boolean = hour in 18..21

    fun shouldAskStrongBlockDay(
        pendingDay: LocalDate?,
        today: LocalDate,
        hour: Int,
        alreadyUsed: Boolean,
    ): Boolean = pendingDay == today.minusDays(1) && isEvening(hour) && !alreadyUsed

    /** A queued moment is still a moment for one day; after that it would be a random ask. */
    fun isFresh(queuedAt: Long?, now: Long): Boolean = queuedAt != null && now - queuedAt in 0 until DAY_MILLIS

    fun shouldAskWhatDidntWork(turnedOnAt: Long?, turnedOffAt: Long): Boolean =
        turnedOnAt != null && turnedOffAt >= turnedOnAt && turnedOffAt - turnedOnAt <= FEEDBACK_WINDOW_MILLIS
}
