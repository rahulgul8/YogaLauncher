package `in`.tuwi.yogaphone.guard

enum class GateAction { NONE, BACK, HOME }

/** All times are monotonic milliseconds; dates come from the local calendar. */
class ShortsGate(initialDaily: GuardDaily = GuardDaily()) {
    var daily: GuardDaily = initialDaily
        private set
    private var previous: GuardSurface? = null
    private var previousAt: Long? = null
    private var lastActionAt: Long? = null
    private val backs = mutableListOf<Long>()
    private var escalated = false

    fun evaluate(detected: GuardSurface?, nowMillis: Long, date: String,
        settings: Map<GuardSurface, SurfaceSettings>): GateAction {
        if (daily.date != date) {
            daily = GuardDaily(date = date)
            previousAt = null
            previous = null
            lastActionAt = null
            backs.clear()
            escalated = false
        }
        // Charge the preceding interval to the surface that was actually visible.
        val before = previous
        val elapsed = previousAt?.let { (nowMillis - it).coerceAtLeast(0) } ?: 0
        if (before != null && settings[before]?.enabled == true && settings[before]!!.allowanceMinutes > 0) {
            val used = daily.usedMillis[before] ?: 0
            val allowance = settings[before]!!.allowanceMinutes.toLong() * 60_000
            daily = daily.copy(usedMillis = daily.usedMillis + (before to (used + elapsed).coerceAtMost(allowance)))
        }
        previousAt = nowMillis
        if (detected != previous) { backs.clear(); lastActionAt = null; escalated = false }
        previous = detected
        if (detected == null) return GateAction.NONE
        val preference = settings[detected] ?: return GateAction.NONE
        if (!preference.enabled) return GateAction.NONE
        if (preference.allowanceMinutes > 0 && (daily.usedMillis[detected] ?: 0) < preference.allowanceMinutes.toLong() * 60_000) {
            return GateAction.NONE
        }
        if (lastActionAt?.let { nowMillis - it < 1_000 } == true) return GateAction.NONE
        backs.removeAll { nowMillis - it > 3_000 }
        // One attempt to open Shorts/Reels counts once, however many Back/Home steps it takes.
        val newAttempt = backs.isEmpty() && !escalated
        val action = if (backs.size >= 2) GateAction.HOME else GateAction.BACK
        if (action == GateAction.HOME) { backs.clear(); escalated = true } else backs += nowMillis
        lastActionAt = nowMillis
        if (newAttempt) daily = daily.copy(blocks = daily.blocks + (detected to ((daily.blocks[detected] ?: 0) + 1)))
        return action
    }
}
