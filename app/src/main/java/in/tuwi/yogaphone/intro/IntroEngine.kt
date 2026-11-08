package `in`.tuwi.yogaphone.intro

import `in`.tuwi.yogaphone.guard.GuardSurface
import java.time.LocalDate

object IntroEngine {
    const val QUIET_MILLIS = 24 * 60 * 60_000L
    fun pick(surface: IntroSurface, signals: IntroSignals, state: IntroState, today: LocalDate, nowMillis: Long): Intro? {
        if (state.onboardingFinishedAtMillis > 0 && nowMillis - state.onboardingFinishedAtMillis < QUIET_MILLIS) return null
        // Home gets one introduction a day; the same one stays for the rest of that day.
        val homeToday = surface == IntroSurface.HOME && state.homeShownDate == today
        return candidates(surface, signals).firstOrNull { intro ->
            if (homeToday && intro.id != state.homeShownId) return@firstOrNull false
            val record = state.records[intro.id] ?: IntroRecord()
            !record.retired && !record.acted && (record.snoozedUntil == null || !today.isBefore(record.snoozedUntil)) &&
                (record.shownCount > 0 || state.lastNewIntroDate != today)
        }
    }

    fun candidates(surface: IntroSurface, s: IntroSignals): List<Intro> {
        val list = mutableListOf<Intro>()
        fun app(id: IntroId, target: GuardSurface, installed: Boolean, minutes: Int, name: String, short: String) {
            val sibling = when (s.blockedSurface) {
                GuardSurface.YOUTUBE, GuardSurface.BROWSER_YOUTUBE -> target == GuardSurface.INSTAGRAM
                GuardSurface.INSTAGRAM, GuardSurface.BROWSER_INSTAGRAM -> target == GuardSurface.YOUTUBE
                null -> false
            }
            val threshold = minutes >= 180 || (s.recordedDays >= 3 && minutes.toDouble() / s.recordedDays >= 30)
            if (installed && target !in s.surfacesEnabled &&
                (if (surface == IntroSurface.BLOCK_CARD) sibling && s.guardEnabled else threshold)) {
                list += Intro(id, surface, "${introDuration(minutes)} on $name this week", "Turn off $short",
                    if (id == IntroId.SHORTS_BLOCK) "Regular videos still work." else "Posts, stories and messages still work.",
                    minutes / 2, true, target)
            }
        }
        app(IntroId.SHORTS_BLOCK, GuardSurface.YOUTUBE, s.youtubeInstalled, s.youtubeMinutes7d, "YouTube", "Shorts")
        app(IntroId.REELS_BLOCK, GuardSurface.INSTAGRAM, s.instagramInstalled, s.instagramMinutes7d, "Instagram", "Reels")
        if (surface != IntroSurface.BLOCK_CARD) {
            listOf(GuardSurface.YOUTUBE to GuardSurface.BROWSER_YOUTUBE,
                GuardSurface.INSTAGRAM to GuardSurface.BROWSER_INSTAGRAM).forEach { (app, browser) ->
                val count = s.blocks7d[app] ?: 0
                if (s.guardEnabled && app in s.blockedSurfaces && browser !in s.surfacesEnabled && count >= 5 && s.browserMinutes7d >= 60) {
                    val name = if (app == GuardSurface.YOUTUBE) "Shorts" else "Reels"
                    list += Intro(IntroId.BROWSER_SURFACES, surface, "$name blocked $count times this week",
                        "Block in browsers too", impactMinutesPerWeek = s.browserMinutes7d / 2, targetSurface = browser)
                }
            }
            val average = s.avgDistractingPerDay
            if (s.allowanceMinutes == null && s.recordedDays >= 3 && average != null && average >= 120) {
                // The existing allowance editor supports 15..480 minutes.
                val minutes = ((average * 0.8 / 15).toInt() * 15).coerceIn(30, 480)
                list += Intro(IntroId.DAILY_ALLOWANCE, surface, "${introDuration(average.toInt())} a day in distracting apps",
                    "Set ${introDuration(minutes)} a day", impactMinutesPerWeek = ((average - minutes) * 7).toInt().coerceAtLeast(0),
                    homeWorthy = true, targetMinutes = minutes)
            }
            s.suggestedAllowance?.takeIf { s.allowanceMinutes != null && it < s.allowanceMinutes }?.let { minutes ->
                list += Intro(IntroId.LOWER_ALLOWANCE, surface,
                    "${s.daysUnderSuggestedAllowance} of the last 7 days under ${introDuration(minutes)}",
                    "Lower to ${introDuration(minutes)}", impactMinutesPerWeek = (s.allowanceMinutes!! - minutes) * 7,
                    targetMinutes = minutes)
            }
            if (s.allowanceMinutes != null && s.daysOverLimitThisWeek >= 3 && !s.greyOverLimitOn) {
                list += Intro(IntroId.GREY_PAST_LIMIT, surface, "Past your limit on ${s.daysOverLimitThisWeek} days this week",
                    "Turn grey past my limit", impactMinutesPerWeek = s.daysOverLimitThisWeek * 15)
            }
        }
        return list.filter { it.id in s.allowed && (surface != IntroSurface.HOME || it.homeWorthy) }
            .sortedWith(compareByDescending<Intro> { it.impactMinutesPerWeek }.thenBy { it.id.ordinal })
    }

    fun shown(state: IntroState, intro: Intro, today: LocalDate): IntroState {
        val old = state.records[intro.id] ?: IntroRecord()
        return state.copy(records = state.records + (intro.id to old.copy(shownCount = old.shownCount + 1,
            firstShownDate = old.firstShownDate ?: today)),
            lastNewIntroDate = if (old.shownCount == 0) today else state.lastNewIntroDate,
            homeShownDate = if (intro.surface == IntroSurface.HOME) today else state.homeShownDate,
            homeShownId = if (intro.surface == IntroSurface.HOME) intro.id else state.homeShownId)
    }
    fun dismissed(record: IntroRecord, today: LocalDate): IntroRecord = record.copy(
        dismissals = record.dismissals + 1, snoozedUntil = today.plusDays(7), retired = record.dismissals >= 1)
}
