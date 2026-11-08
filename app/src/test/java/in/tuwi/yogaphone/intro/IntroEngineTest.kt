package `in`.tuwi.yogaphone.intro

import `in`.tuwi.yogaphone.guard.GuardSurface
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class IntroEngineTest {
    private val day = LocalDate.of(2026, 11, 8)
    private val now = 1_000_000_000L
    private val youtube = IntroSignals(youtubeInstalled = true, youtubeMinutes7d = 180)
    private val instagram = IntroSignals(instagramInstalled = true, instagramMinutes7d = 180)
    private fun pick(s: IntroSignals = youtube, state: IntroState = IntroState(), surface: IntroSurface = IntroSurface.USAGE,
        today: LocalDate = day, time: Long = now) = IntroEngine.pick(surface, s, state, today, time)
    private fun only(id: IntroId, s: IntroSignals) = s.copy(allowed = setOf(id))

    @Test fun shortsWeeklyBoundary() {
        assertNull(pick(youtube.copy(youtubeMinutes7d = 179)))
        assertEquals(IntroId.SHORTS_BLOCK, pick()!!.id)
    }
    @Test fun reelsWeeklyBoundary() {
        assertNull(pick(instagram.copy(instagramMinutes7d = 179)))
        assertEquals(IntroId.REELS_BLOCK, pick(instagram)!!.id)
    }
    @Test fun averageRequiresThreeDaysAndThirtyMinutes() {
        for (s in listOf(youtube, instagram)) {
            val isYoutube = s.youtubeInstalled
            fun values(minutes: Int, days: Int) = s.copy(recordedDays = days,
                youtubeMinutes7d = if (isYoutube) minutes else 0, instagramMinutes7d = if (isYoutube) 0 else minutes)
            assertNull(pick(values(89, 3)))
            assertNotNull(pick(values(90, 3)))
            assertNull(pick(values(60, 2)))
            assertNull(pick(values(119, 4)))
            assertNotNull(pick(values(120, 4)))
        }
    }
    @Test fun absentAppsAreNeverOffered() {
        assertNull(pick(youtube.copy(youtubeInstalled = false)))
        assertNull(pick(instagram.copy(instagramInstalled = false)))
    }
    @Test fun enabledSurfacesAreNeverOffered() {
        assertNull(pick(youtube.copy(surfacesEnabled = setOf(GuardSurface.YOUTUBE))))
        assertNull(pick(instagram.copy(surfacesEnabled = setOf(GuardSurface.INSTAGRAM))))
    }
    @Test fun shortsCopyAndImpact() {
        val intro = pick(youtube.copy(youtubeMinutes7d = 400))!!
        assertEquals("6 h 40 m on YouTube this week", intro.evidence)
        assertEquals("Turn off Shorts", intro.actionLabel)
        assertEquals("Regular videos still work.", intro.note)
        assertEquals(200, intro.impactMinutesPerWeek)
        assertEquals(GuardSurface.YOUTUBE, intro.targetSurface)
    }
    @Test fun reelsCopyAndImpact() {
        val intro = pick(instagram.copy(instagramMinutes7d = 250))!!
        assertEquals("4 h 10 m on Instagram this week", intro.evidence)
        assertEquals("Turn off Reels", intro.actionLabel)
        assertEquals(125, intro.impactMinutesPerWeek)
    }
    private fun browser(app: GuardSurface) = IntroSignals(guardEnabled = true,
        surfacesEnabled = setOf(app), blocks7d = mapOf(app to 5), browserMinutes7d = 60)
    @Test fun browserBoundariesForBothTargets() {
        listOf(GuardSurface.YOUTUBE to GuardSurface.BROWSER_YOUTUBE,
            GuardSurface.INSTAGRAM to GuardSurface.BROWSER_INSTAGRAM).forEach { (app, target) ->
            val s = browser(app)
            assertEquals(target, pick(s)!!.targetSurface)
            assertNull(pick(s.copy(blocks7d = mapOf(app to 4))))
            assertNull(pick(s.copy(browserMinutes7d = 59)))
            assertNull(pick(s.copy(surfacesEnabled = setOf(app, target))))
            assertNull(pick(s.copy(blockedSurfaces = emptySet())))
            assertNull(pick(s.copy(guardEnabled = false)))
        }
    }
    @Test fun browserEvidenceAndDeterministicTarget() {
        val s = browser(GuardSurface.YOUTUBE).copy(blocks7d = mapOf(GuardSurface.YOUTUBE to 14))
        assertEquals("Shorts blocked 14 times this week", pick(s)!!.evidence)
        assertEquals("Block in browsers too", pick(s)!!.actionLabel)
        val both = s.copy(surfacesEnabled = setOf(GuardSurface.YOUTUBE, GuardSurface.INSTAGRAM),
            blockedSurfaces = setOf(GuardSurface.YOUTUBE, GuardSurface.INSTAGRAM),
            blocks7d = mapOf(GuardSurface.YOUTUBE to 14, GuardSurface.INSTAGRAM to 14))
        assertEquals(GuardSurface.BROWSER_YOUTUBE, pick(both)!!.targetSurface)
    }
    private val daily = IntroSignals(recordedDays = 3, avgDistractingPerDay = 120.0)
    @Test fun dailyAllowanceBoundaries() {
        assertNull(pick(daily.copy(recordedDays = 2)))
        assertNull(pick(daily.copy(avgDistractingPerDay = null)))
        assertNull(pick(daily.copy(avgDistractingPerDay = 119.9)))
        assertNull(pick(daily.copy(allowanceMinutes = 120)))
        assertEquals(IntroId.DAILY_ALLOWANCE, pick(daily)!!.id)
        assertEquals(90, pick(daily)!!.targetMinutes)
    }
    @Test fun dailyRoundsDownAndUsesExistingEditorRange() {
        val intro = pick(daily.copy(avgDistractingPerDay = 160.0))!!
        assertEquals("2 h 40 m a day in distracting apps", intro.evidence)
        assertEquals("Set 2 h a day", intro.actionLabel)
        assertEquals(120, intro.targetMinutes)
        assertEquals(280, intro.impactMinutesPerWeek)
        assertEquals(480, pick(daily.copy(avgDistractingPerDay = 1000.0))!!.targetMinutes)
    }
    @Test fun lowerUsesSuggestedValueAndRealEvidenceCount() {
        val s = IntroSignals(allowanceMinutes = 90, suggestedAllowance = 75, daysUnderSuggestedAllowance = 6)
        val intro = pick(s)!!
        assertEquals(IntroId.LOWER_ALLOWANCE, intro.id)
        assertEquals("6 of the last 7 days under 1 h 15 m", intro.evidence)
        assertEquals("Lower to 1 h 15 m", intro.actionLabel)
        assertEquals(105, intro.impactMinutesPerWeek)
        assertNull(pick(s.copy(suggestedAllowance = null)))
        assertNull(pick(s.copy(allowanceMinutes = null)))
        assertNull(pick(s.copy(suggestedAllowance = 90)))
    }
    @Test fun greyBoundariesAndCopy() {
        val s = IntroSignals(allowanceMinutes = 90, daysOverLimitThisWeek = 3)
        assertNotNull(pick(s))
        assertNull(pick(s.copy(daysOverLimitThisWeek = 2)))
        assertNull(pick(s.copy(allowanceMinutes = null)))
        assertNull(pick(s.copy(greyOverLimitOn = true)))
        assertEquals("Past your limit on 4 days this week", pick(s.copy(daysOverLimitThisWeek = 4))!!.evidence)
    }
    @Test fun quietPeriodExactBoundaryAndLegacyMissingTime() {
        assertNull(pick(state = IntroState(onboardingFinishedAtMillis = now - IntroEngine.QUIET_MILLIS + 1)))
        assertNotNull(pick(state = IntroState(onboardingFinishedAtMillis = now - IntroEngine.QUIET_MILLIS)))
        assertNotNull(pick(state = IntroState(onboardingFinishedAtMillis = 0)))
        assertNull(pick(state = IntroState(onboardingFinishedAtMillis = now + 1)))
    }
    @Test fun quietPeriodIsElapsedTimeNotCalendarDate() {
        assertNull(pick(state = IntroState(onboardingFinishedAtMillis = now - 60_000), today = day.plusDays(1)))
    }
    @Test fun oneNewPerDayAndMidnight() {
        val shown = IntroEngine.shown(IntroState(), pick()!!, day)
        assertEquals(day, shown.lastNewIntroDate)
        assertNull(pick(instagram, shown))
        assertNotNull(pick(instagram, shown, today = day.plusDays(1)))
        assertNotNull(pick(youtube, shown))
    }
    @Test fun existingIntroCanWinAfterNewDailyQuotaUsed() {
        val s = youtube.copy(instagramInstalled = true, instagramMinutes7d = 600)
        val state = IntroState(records = mapOf(IntroId.SHORTS_BLOCK to IntroRecord(shownCount = 1)), lastNewIntroDate = day)
        assertEquals(IntroId.SHORTS_BLOCK, pick(s, state)!!.id)
    }
    @Test fun firstDismissalSnoozesExactlySevenDays() {
        val record = IntroEngine.dismissed(IntroRecord(shownCount = 1), day)
        val state = IntroState(records = mapOf(IntroId.SHORTS_BLOCK to record))
        assertFalse(record.retired)
        assertEquals(1, record.dismissals)
        assertNull(pick(state = state, today = day.plusDays(6)))
        assertNotNull(pick(state = state, today = day.plusDays(7)))
    }
    @Test fun secondDismissalRetiresPermanently() {
        val record = IntroEngine.dismissed(IntroEngine.dismissed(IntroRecord(), day), day.plusDays(7))
        assertTrue(record.retired)
        assertEquals(2, record.dismissals)
        assertNull(pick(state = IntroState(records = mapOf(IntroId.SHORTS_BLOCK to record)), today = day.plusYears(1)))
    }
    @Test fun actedNeverAppears() {
        assertNull(pick(state = IntroState(records = mapOf(IntroId.SHORTS_BLOCK to IntroRecord(acted = true)))))
    }
    @Test fun homeOnlyHighValueIntroductions() {
        assertNotNull(pick(youtube, surface = IntroSurface.HOME))
        assertNotNull(pick(instagram, surface = IntroSurface.HOME))
        assertNotNull(pick(daily, surface = IntroSurface.HOME))
        assertNull(pick(browser(GuardSurface.YOUTUBE), surface = IntroSurface.HOME))
        assertNull(pick(IntroSignals(allowanceMinutes = 90, suggestedAllowance = 75), surface = IntroSurface.HOME))
        assertNull(pick(IntroSignals(allowanceMinutes = 90, daysOverLimitThisWeek = 3), surface = IntroSurface.HOME))
    }
    @Test fun homeOncePerLocalDayWithoutBlockingUsage() {
        val intro = pick(surface = IntroSurface.HOME)!!
        val state = IntroEngine.shown(IntroState(), intro, day)
        // The same introduction stays on home for the rest of the day; no second one appears.
        assertEquals(intro.id, pick(state = state, surface = IntroSurface.HOME)?.id)
        val other = if (intro.id == IntroId.SHORTS_BLOCK) instagram else youtube
        assertNull(pick(other, state = state, surface = IntroSurface.HOME))
        assertNotNull(pick(state = state, surface = IntroSurface.USAGE))
        assertNotNull(pick(state = state, surface = IntroSurface.HOME, today = day.plusDays(1)))
    }
    @Test fun usageImpressionDoesNotConsumeHomeQuota() {
        val state = IntroEngine.shown(IntroState(), pick()!!, day)
        assertNull(state.homeShownDate)
        assertNotNull(pick(state = state, surface = IntroSurface.HOME))
    }
    @Test fun blockCardOnlySiblingIgnoringTimeThreshold() {
        listOf(GuardSurface.YOUTUBE, GuardSurface.BROWSER_YOUTUBE).forEach { blocked ->
            val s = IntroSignals(guardEnabled = true, instagramInstalled = true, blockedSurface = blocked)
            assertEquals(IntroId.REELS_BLOCK, pick(s, surface = IntroSurface.BLOCK_CARD)!!.id)
            assertNull(pick(s.copy(instagramInstalled = false), surface = IntroSurface.BLOCK_CARD))
            assertNull(pick(s.copy(guardEnabled = false), surface = IntroSurface.BLOCK_CARD))
        }
        listOf(GuardSurface.INSTAGRAM, GuardSurface.BROWSER_INSTAGRAM).forEach { blocked ->
            val s = IntroSignals(guardEnabled = true, youtubeInstalled = true, blockedSurface = blocked)
            assertEquals(IntroId.SHORTS_BLOCK, pick(s, surface = IntroSurface.BLOCK_CARD)!!.id)
            assertNull(pick(s.copy(youtubeInstalled = false), surface = IntroSurface.BLOCK_CARD))
        }
        assertNull(pick(youtube, surface = IntroSurface.BLOCK_CARD))
    }
    @Test fun blockCardRespectsAlreadyEnabledAndDailyQuotaAndQuietPeriod() {
        val s = IntroSignals(guardEnabled = true, instagramInstalled = true, blockedSurface = GuardSurface.YOUTUBE)
        assertNull(pick(s.copy(surfacesEnabled = setOf(GuardSurface.INSTAGRAM)), surface = IntroSurface.BLOCK_CARD))
        assertNull(pick(s, IntroState(lastNewIntroDate = day), IntroSurface.BLOCK_CARD))
        assertNull(pick(s, IntroState(onboardingFinishedAtMillis = now), IntroSurface.BLOCK_CARD))
    }
    @Test fun highestImpactWinsAndTiesUseEnumOrder() {
        val s = youtube.copy(instagramInstalled = true, instagramMinutes7d = 181)
        assertEquals(IntroId.SHORTS_BLOCK, pick(s)!!.id) // integer division: both 90
        assertEquals(IntroId.REELS_BLOCK, pick(s.copy(instagramMinutes7d = 182))!!.id)
        assertEquals(IntroId.DAILY_ALLOWANCE, pick(s.copy(recordedDays = 3, avgDistractingPerDay = 160.0))!!.id)
    }
    @Test fun permissionAndProMaskAppliesToEverySurface() {
        assertNull(pick(youtube.copy(allowed = emptySet())))
        assertNull(pick(youtube.copy(allowed = emptySet()), surface = IntroSurface.HOME))
        assertNull(pick(IntroSignals(guardEnabled = true, instagramInstalled = true, blockedSurface = GuardSurface.YOUTUBE, allowed = emptySet()), surface = IntroSurface.BLOCK_CARD))
    }
    @Test fun repeatedShowsPreserveFirstDateAndLastNewDate() {
        val intro = pick()!!
        val first = IntroEngine.shown(IntroState(), intro, day)
        val second = IntroEngine.shown(first, intro, day.plusDays(1))
        assertEquals(2, second.records[intro.id]!!.shownCount)
        assertEquals(day, second.records[intro.id]!!.firstShownDate)
        assertEquals(day, second.lastNewIntroDate)
    }
    @Test fun durations() {
        assertEquals("0 min", introDuration(0))
        assertEquals("40 min", introDuration(40))
        assertEquals("1 h", introDuration(60))
        assertEquals("6 h 40 m", introDuration(400))
    }
}
