package `in`.tuwi.yogaphone.usage

/** Foreground time, not screen-on time. Timestamps are epoch milliseconds. */
data class ForegroundEvent(
    val packageName: String,
    val timestamp: Long,
    val type: Type,
    /** Activity class; lets one app's activity transitions overlap without ending the app's session early. */
    val className: String? = null,
) {
    enum class Type { RESUMED, PAUSED, STOPPED, SCREEN_OFF, SHUTDOWN }
}

class UsageCalculator(private val ownPackage: String) {
    fun calculate(events: List<ForegroundEvent>, start: Long, end: Long, now: Long = end): Map<String, Long> {
        val stop = minOf(end, now)
        if (stop <= start) return emptyMap()
        val openSince = mutableMapOf<String, Long>()
        val openActivities = mutableMapOf<String, MutableSet<String>>()
        val totals = mutableMapOf<String, Long>()
        fun closeSession(pkg: String, time: Long) {
            openActivities.remove(pkg)
            val began = openSince.remove(pkg) ?: return
            val duration = (minOf(time, stop) - maxOf(began, start)).coerceAtLeast(0)
            if (duration > 0 && pkg != ownPackage) totals[pkg] = (totals[pkg] ?: 0) + duration
        }
        events.sortedBy { it.timestamp }.takeWhile { it.timestamp < stop }.forEach { event ->
            val pkg = event.packageName
            val activity = event.className.orEmpty()
            when (event.type) {
                ForegroundEvent.Type.RESUMED -> {
                    openSince.putIfAbsent(pkg, event.timestamp)
                    openActivities.getOrPut(pkg) { mutableSetOf() } += activity
                }
                ForegroundEvent.Type.PAUSED, ForegroundEvent.Type.STOPPED -> {
                    val activities = openActivities[pkg] ?: return@forEach
                    // A STOPPED for an activity that is not open (e.g. after its PAUSED) changes nothing.
                    if (!activities.remove(activity)) return@forEach
                    if (activities.isEmpty()) closeSession(pkg, event.timestamp)
                }
                ForegroundEvent.Type.SCREEN_OFF, ForegroundEvent.Type.SHUTDOWN ->
                    openSince.keys.toList().forEach { closeSession(it, event.timestamp) }
            }
        }
        openSince.keys.toList().forEach { closeSession(it, stop) }
        return totals
    }
}

enum class RuleScope { TOTAL, APP }
enum class RuleState { UNDER, NEAR, OVER }
data class UsageRule(val id: String, val scope: RuleScope, val targetPackage: String?, val dailyAllowanceMinutes: Int,
    val warnAtPercent: Int = 80, val enabled: Boolean = true)
data class RuleResult(val rule: UsageRule, val used: Long, val allowance: Int, val remaining: Long,
    val state: RuleState, val excessMinutes: Long)
data class RuleEvaluation(val rules: List<RuleResult>, val overall: RuleResult?)

class RuleEvaluator {
    fun evaluate(rules: List<UsageRule>, eligibleMinutes: Map<String, Long>, appMinutes: Map<String, Long> = eligibleMinutes): RuleEvaluation {
        val results = rules.filter { it.enabled && it.dailyAllowanceMinutes > 0 }.map { rule ->
            val used = if (rule.scope == RuleScope.TOTAL) eligibleMinutes.values.sum() else appMinutes[rule.targetPackage] ?: 0
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

