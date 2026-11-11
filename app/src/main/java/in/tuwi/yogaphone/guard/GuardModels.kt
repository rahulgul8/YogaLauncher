package `in`.tuwi.yogaphone.guard

import java.time.LocalDate

enum class GuardStyle { BURNING, GENTLE }
enum class StartCounting { AFTER_LIMIT, FROM_FIRST_MINUTE }
enum class GuardSurface { YOUTUBE, INSTAGRAM, BROWSER_YOUTUBE, BROWSER_INSTAGRAM }
fun GuardSurface.family(): Set<GuardSurface> = when (this) {
    GuardSurface.YOUTUBE, GuardSurface.BROWSER_YOUTUBE -> setOf(GuardSurface.YOUTUBE, GuardSurface.BROWSER_YOUTUBE)
    else -> setOf(GuardSurface.INSTAGRAM, GuardSurface.BROWSER_INSTAGRAM)
}
fun GuardSurface.shortName() = if (GuardSurface.YOUTUBE in family()) "Shorts" else "Reels"
fun isStrict(prefs: GuardPreferences, today: LocalDate): Boolean = prefs.strict &&
    (prefs.strictOffFrom?.let { runCatching { LocalDate.parse(it) }.getOrNull() }?.let { today.isBefore(it) } ?: true)
fun GuardPreferences.passExpiry(surface: GuardSurface): Long = surface.family().maxOf { passUntil[it] ?: 0L }
data class SurfaceSettings(val enabled: Boolean = false, val allowanceMinutes: Int = 0, val lastAllowanceMinutes: Int = 10)
data class GuardPreferences(
    val liveIndicator: Boolean = false,
    val style: GuardStyle = GuardStyle.BURNING,
    val startCounting: StartCounting = StartCounting.FROM_FIRST_MINUTE,
    val positionY: Int = 80,
    val surfaces: Map<GuardSurface, SurfaceSettings> = emptyMap(),
    val strict: Boolean = false,
    val strictOffFrom: String? = null,
    val passUntil: Map<GuardSurface, Long> = emptyMap(),
    val lastPassMinutes: Int? = null,
    // Per-family duration survives another family's pass and a service restart.
    val passMinutes: Map<GuardSurface, Int> = emptyMap(),
) {
    fun surface(surface: GuardSurface) = surfaces[surface] ?: SurfaceSettings()
    val active get() = liveIndicator || surfaces.values.any { it.enabled }
}
data class GuardDaily(val date: String = "", val blocks: Map<GuardSurface, Int> = emptyMap(),
    val usedMillis: Map<GuardSurface, Long> = emptyMap(),
    val passes: Map<GuardSurface, Int> = emptyMap(),
    val resisted: Map<GuardSurface, Int> = emptyMap())

/** Gate owns blocks/time; transactional preferences own choices. Never resurrect an older day. */
fun mergeGuardDaily(stored: GuardDaily, gate: GuardDaily): GuardDaily = when {
    stored.date > gate.date -> stored
    stored.date == gate.date -> gate.copy(passes = stored.passes, resisted = stored.resisted)
    else -> gate.copy(passes = emptyMap(), resisted = emptyMap())
}


/** True when [next] lets more Shorts or Reels through than [previous]: turned off, or a block became a daily allowance or a bigger one. */
fun isLooser(previous: SurfaceSettings, next: SurfaceSettings): Boolean = when {
    !previous.enabled -> false
    !next.enabled -> true
    previous.allowanceMinutes == 0 -> next.allowanceMinutes > 0
    else -> next.allowanceMinutes > previous.allowanceMinutes
}

/** Leaving strict mode takes effect from tomorrow (D6), so it really binds today. */
fun strictOffFrom(today: LocalDate): String = today.plusDays(1).toString()

/** A pending "strict off" whose day has come should be written back as plain, non-strict preferences. */
fun strictEnded(prefs: GuardPreferences, today: LocalDate): Boolean = prefs.strict && !isStrict(prefs, today)
