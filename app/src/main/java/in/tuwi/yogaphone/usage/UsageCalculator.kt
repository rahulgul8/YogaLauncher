package `in`.tuwi.yogaphone.usage

/** Foreground time, not screen-on time. Timestamps are epoch milliseconds. */
data class ForegroundEvent(
    val packageName: String,
    val timestamp: Long,
    val type: Type,
    /** Activity class; lets one app's activity transitions overlap without ending the app's session early. */
    val className: String? = null,
    /** Android's activity-instance id (API 29+). Classes alone are not unique across tasks/windows. */
    val instanceId: Int? = null,
) {
    enum class Type { RESUMED, PAUSED, STOPPED, SCREEN_OFF, SCREEN_ON, LOCKED, UNLOCKED, SHUTDOWN }
}

class UsageCalculator(private val ownPackage: String) {
    fun calculate(events: List<ForegroundEvent>, start: Long, end: Long, now: Long = end): Map<String, Long> {
        val stop = minOf(end, now)
        if (stop <= start) return emptyMap()
        val runningSince = mutableMapOf<String, Long>()
        val activeActivities = mutableMapOf<String, MutableSet<String>>()
        val totals = mutableMapOf<String, Long>()
        var screenInteractive = true
        var keyguardLocked = false
        fun foregroundAllowed() = screenInteractive && !keyguardLocked
        fun token(event: ForegroundEvent): String = event.instanceId?.takeIf { it != 0 }?.let { "instance:$it" }
            ?: event.className?.let { "class:$it" }
            ?: "package"
        fun startSession(pkg: String, time: Long) {
            if (foregroundAllowed() && pkg != ownPackage && pkg.isNotBlank()) runningSince.putIfAbsent(pkg, time)
        }
        fun closeSession(pkg: String, time: Long) {
            val began = runningSince.remove(pkg) ?: return
            val duration = (minOf(time, stop) - maxOf(began, start)).coerceAtLeast(0)
            if (duration > 0) totals[pkg] = (totals[pkg] ?: 0) + duration
        }
        fun allowForeground(time: Long) {
            activeActivities.filterValues { it.isNotEmpty() }.keys.forEach { startSession(it, time) }
        }
        events.sortedBy { it.timestamp }.takeWhile { it.timestamp < stop }.forEach { event ->
            val pkg = event.packageName
            val activity = token(event)
            when (event.type) {
                ForegroundEvent.Type.RESUMED -> {
                    // A resume itself proves the display is usable even if an OEM omitted SCREEN_ON/KEYGUARD_HIDDEN.
                    screenInteractive = true
                    keyguardLocked = false
                    activeActivities.getOrPut(pkg) { mutableSetOf() } += activity
                    startSession(pkg, event.timestamp)
                }
                ForegroundEvent.Type.PAUSED, ForegroundEvent.Type.STOPPED -> {
                    val activities = activeActivities[pkg] ?: return@forEach
                    // A STOPPED for an activity that is not open (e.g. after its PAUSED) changes nothing.
                    if (!activities.remove(activity)) return@forEach
                    if (activities.isEmpty()) {
                        activeActivities.remove(pkg)
                        closeSession(pkg, event.timestamp)
                    }
                }
                ForegroundEvent.Type.SCREEN_OFF -> {
                    runningSince.keys.toList().forEach { closeSession(it, event.timestamp) }
                    // Keep the activity set. Android often does not send another RESUMED after unlock.
                    screenInteractive = false
                }
                ForegroundEvent.Type.SCREEN_ON -> {
                    screenInteractive = true
                    if (!keyguardLocked) allowForeground(event.timestamp)
                }
                ForegroundEvent.Type.LOCKED -> {
                    keyguardLocked = true
                    runningSince.keys.toList().forEach { closeSession(it, event.timestamp) }
                }
                ForegroundEvent.Type.UNLOCKED -> {
                    screenInteractive = true
                    keyguardLocked = false
                    allowForeground(event.timestamp)
                }
                ForegroundEvent.Type.SHUTDOWN -> {
                    runningSince.keys.toList().forEach { closeSession(it, event.timestamp) }
                    activeActivities.clear()
                    screenInteractive = false
                    keyguardLocked = false
                }
            }
        }
        runningSince.keys.toList().forEach { closeSession(it, stop) }
        return totals
    }
}

enum class RuleScope { TOTAL, APP, CATEGORY }
enum class RuleState { UNDER, NEAR, OVER }
data class UsageRule(val id: String, val scope: RuleScope, val targetPackage: String?, val dailyAllowanceMinutes: Int,
    val warnAtPercent: Int = 80, val enabled: Boolean = true)
data class RuleResult(val rule: UsageRule, val used: Long, val allowance: Int, val remaining: Long,
    val state: RuleState, val excessMinutes: Long)
data class RuleEvaluation(val rules: List<RuleResult>, val overall: RuleResult?)

class RuleEvaluator {
    /** For CATEGORY rules, [UsageRule.targetPackage] holds the category id and [categoryOf] maps packages to categories. */
    fun evaluate(
        rules: List<UsageRule>,
        eligibleMinutes: Map<String, Long>,
        appMinutes: Map<String, Long> = eligibleMinutes,
        categoryOf: Map<String, String> = emptyMap(),
    ): RuleEvaluation {
        val results = rules.filter { it.enabled && it.dailyAllowanceMinutes > 0 }.map { rule ->
            val used = when (rule.scope) {
                RuleScope.TOTAL -> eligibleMinutes.values.sum()
                RuleScope.APP -> appMinutes[rule.targetPackage] ?: 0
                RuleScope.CATEGORY -> appMinutes.filterKeys { categoryOf[it] == rule.targetPackage }.values.sum()
            }
            val state = when {
                used > rule.dailyAllowanceMinutes -> RuleState.OVER
                rule.warnAtPercent in 1..100 && used * 100 >= rule.dailyAllowanceMinutes.toLong() * rule.warnAtPercent -> RuleState.NEAR
                else -> RuleState.UNDER
            }
            RuleResult(rule, used, rule.dailyAllowanceMinutes, (rule.dailyAllowanceMinutes - used).coerceAtLeast(0),
                state, (used - rule.dailyAllowanceMinutes).coerceAtLeast(0))
        }
        // TOTAL is the overall allowance, never a sum of overlapping app limits.
        return RuleEvaluation(results, results.firstOrNull { it.rule.scope == RuleScope.TOTAL })
    }
}

/** A day and rule state form the deduplication key. OVER also consumes NEAR, preventing stale warnings. */
object ReminderLedger {
    fun key(date: String, ruleId: String, state: RuleState) = "$date|$ruleId|${state.name}"
    fun shouldSend(sent: Set<String>, date: String, ruleId: String, state: RuleState): Boolean =
        state != RuleState.UNDER && key(date, ruleId, state) !in sent &&
            !(state == RuleState.NEAR && key(date, ruleId, RuleState.OVER) in sent)
    fun record(sent: Set<String>, date: String, ruleId: String, state: RuleState): Set<String> =
        sent.filter { it.startsWith("$date|") }.toSet() + key(date, ruleId, state)
}

