package `in`.tuwi.yogaphone.review

import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPolicyTest {
    private val day = ReviewPolicy.DAY_MILLIS
    private val now = 200L * day

    private fun signals(
        installedAt: Long = now - 4 * day,
        sessions: Int = 5,
        defaultHome: Boolean = true,
        onboarding: Boolean = false,
        interruption: Long? = null,
        overLimit: Boolean = false,
        asks: List<Long> = emptyList(),
    ) = ReviewSignals(now, installedAt, sessions, defaultHome, onboarding, interruption, overLimit, asks)

    @Test fun allGuardsPassingAllowsAsk() = assertTrue(ReviewPolicy.mayAsk(signals()))

    @Test fun installationAgeAloneBlocks() = assertFalse(ReviewPolicy.mayAsk(signals(installedAt = now - 3 * day + 1)))
    @Test fun sessionCountAloneBlocks() = assertFalse(ReviewPolicy.mayAsk(signals(sessions = 4)))
    @Test fun defaultHomeAloneBlocks() = assertFalse(ReviewPolicy.mayAsk(signals(defaultHome = false)))
    @Test fun onboardingAloneBlocks() = assertFalse(ReviewPolicy.mayAsk(signals(onboarding = true)))
    @Test fun recentInterruptionAloneBlocks() = assertFalse(ReviewPolicy.mayAsk(signals(interruption = now - 10 * 60_000 + 1)))
    @Test fun overLimitAloneBlocks() = assertFalse(ReviewPolicy.mayAsk(signals(overLimit = true)))

    @Test fun ninetyDayCapHasAnExactBoundary() {
        assertFalse(ReviewPolicy.mayAsk(signals(asks = listOf(now - 90 * day + 1))))
        assertTrue(ReviewPolicy.mayAsk(signals(asks = listOf(now - 90 * day))))
    }

    @Test fun threeEverCapBlocksEvenWhenOld() {
        assertFalse(ReviewPolicy.mayAsk(signals(asks = listOf(now - 100 * day, now - 110 * day, now - 120 * day))))
    }

    @Test fun goodWeekRequiresFiveKnownDays() {
        assertFalse(ReviewPolicy.isGoodWeek(5, 4, 100, null))
        assertTrue(ReviewPolicy.isGoodWeek(5, 5, 100, null))
        assertTrue(ReviewPolicy.isGoodWeek(0, 5, 80, 100))
        assertFalse(ReviewPolicy.isGoodWeek(0, 5, 81, 100))
    }

    @Test fun strongDayRequiresFiveBlocksAndBeingWithinLimit() {
        assertFalse(ReviewPolicy.isStrongBlockDay(4, false))
        assertFalse(ReviewPolicy.isStrongBlockDay(5, true))
        assertTrue(ReviewPolicy.isStrongBlockDay(5, false))
    }

    @Test fun strongDayWaitsForANextEveningAndFiresOnce() {
        val today = LocalDate.of(2026, 11, 11)
        assertFalse(ReviewPolicy.shouldAskStrongBlockDay(today, today, 18, false))
        assertFalse(ReviewPolicy.shouldAskStrongBlockDay(today.minusDays(1), today, 17, false))
        assertTrue(ReviewPolicy.shouldAskStrongBlockDay(today.minusDays(1), today, 18, false))
        assertFalse(ReviewPolicy.shouldAskStrongBlockDay(today.minusDays(2), today, 21, false))
        assertTrue(ReviewPolicy.shouldAskStrongBlockDay(today.minusDays(1), today, 21, false))
        assertFalse(ReviewPolicy.shouldAskStrongBlockDay(today.minusDays(1), today, 22, false))
        assertFalse(ReviewPolicy.shouldAskStrongBlockDay(today.minusDays(1), today, 18, true))
    }

    @Test fun queuedMomentsExpireAfterADay() {
        val day = ReviewPolicy.DAY_MILLIS
        assertFalse(ReviewPolicy.isFresh(null, day))
        assertTrue(ReviewPolicy.isFresh(0L, day - 1))
        assertFalse(ReviewPolicy.isFresh(0L, day))
        assertFalse(ReviewPolicy.isFresh(day, 0L))
    }

    @Test fun feedbackWindowIsSevenDaysInclusive() {
        assertFalse(ReviewPolicy.shouldAskWhatDidntWork(null, now))
        assertTrue(ReviewPolicy.shouldAskWhatDidntWork(now - 7 * day, now))
        assertFalse(ReviewPolicy.shouldAskWhatDidntWork(now - 7 * day - 1, now))
        assertFalse(ReviewPolicy.shouldAskWhatDidntWork(now + 1, now))
    }
}
