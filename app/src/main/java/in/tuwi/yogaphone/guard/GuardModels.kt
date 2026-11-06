package `in`.tuwi.yogaphone.guard

enum class GuardStyle { BURNING, GENTLE }
enum class StartCounting { AFTER_LIMIT, FROM_FIRST_MINUTE }
enum class GuardSurface { YOUTUBE, INSTAGRAM, BROWSER_YOUTUBE, BROWSER_INSTAGRAM }
data class SurfaceSettings(val enabled: Boolean = false, val allowanceMinutes: Int = 0, val lastAllowanceMinutes: Int = 10)
data class GuardPreferences(
    val liveIndicator: Boolean = false,
    val style: GuardStyle = GuardStyle.BURNING,
    val startCounting: StartCounting = StartCounting.AFTER_LIMIT,
    val positionY: Int = 80,
    val surfaces: Map<GuardSurface, SurfaceSettings> = emptyMap(),
) {
    fun surface(surface: GuardSurface) = surfaces[surface] ?: SurfaceSettings()
    val active get() = liveIndicator || surfaces.values.any { it.enabled }
}
data class GuardDaily(val date: String = "", val blocks: Map<GuardSurface, Int> = emptyMap(),
    val usedMillis: Map<GuardSurface, Long> = emptyMap())

