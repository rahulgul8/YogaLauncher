package `in`.tuwi.yogaphone.intro

import `in`.tuwi.yogaphone.guard.GuardSurface
import java.time.LocalDate

enum class IntroId { SHORTS_BLOCK, REELS_BLOCK, BROWSER_SURFACES, DAILY_ALLOWANCE, LOWER_ALLOWANCE, GREY_PAST_LIMIT }
enum class IntroSurface { USAGE, HOME, BLOCK_CARD }
data class IntroSignals(
    val youtubeMinutes7d: Int = 0, val instagramMinutes7d: Int = 0, val browserMinutes7d: Int = 0,
    val recordedDays: Int = 0, val youtubeInstalled: Boolean = false, val instagramInstalled: Boolean = false,
    val guardEnabled: Boolean = false,
    /** Effective enabled surfaces, including a daily Shorts/Reels allowance. */
    val surfacesEnabled: Set<GuardSurface> = emptySet(),
    val blockedSurfaces: Set<GuardSurface> = surfacesEnabled,
    val blocks7d: Map<GuardSurface, Int> = emptyMap(),
    val allowanceMinutes: Int? = null, val avgDistractingPerDay: Double? = null,
    val suggestedAllowance: Int? = null, val daysUnderSuggestedAllowance: Int = 0,
    val daysOverLimitThisWeek: Int = 0, val greyOverLimitOn: Boolean = false,
    /** The allowance counts every app except essential ones, not only distracting apps. */
    val countsAllApps: Boolean = false,
    val allowed: Set<IntroId> = IntroId.entries.toSet(),
    val blockedSurface: GuardSurface? = null,
)
data class IntroRecord(val shownCount: Int = 0, val firstShownDate: LocalDate? = null,
    val snoozedUntil: LocalDate? = null, val dismissals: Int = 0, val retired: Boolean = false,
    val acted: Boolean = false)
data class IntroState(val records: Map<IntroId, IntroRecord> = emptyMap(),
    val lastNewIntroDate: LocalDate? = null, val onboardingFinishedAtMillis: Long = 0,
    val homeShownDate: LocalDate? = null, val homeShownId: IntroId? = null)
data class Intro(val id: IntroId, val surface: IntroSurface, val evidence: String,
    val actionLabel: String, val note: String? = null, val impactMinutesPerWeek: Int,
    val homeWorthy: Boolean = false, val targetSurface: GuardSurface? = null,
    val targetMinutes: Int? = null)

fun introDuration(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} m"
}
