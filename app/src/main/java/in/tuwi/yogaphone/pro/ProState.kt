package `in`.tuwi.yogaphone.pro

import java.time.Instant
import java.time.ZoneId

enum class Entitlement { TRIAL, PRO, FREE }
enum class ProPlan { YEARLY, LIFETIME }
data class ProState(val entitlement: Entitlement = Entitlement.FREE, val trialDaysLeft: Int = 0, val plan: ProPlan? = null)

enum class ProFeature {
    LAUNCHER, THEMES, TODAY_SCREEN_TIME, SHORTS_BLOCKING, BASIC_MINDFUL_PAUSE, FIRST_ALLOWANCE,
    LOOKS, COLON_ICON, VALUE_OF_TIME, EXTRA_ALLOWANCES, EXTRA_MINDFUL_APPS, REMINDERS,
    FOCUS, TIMEBOX, DO_NOT_DISTURB, INSIGHTS, WEEKLY_REPORTS, EXTENDED_HISTORY,
    QUIET_NOTIFICATIONS, PROFILES, WIDGETS, LEARNING_VIDEOS,
}

object ProGate {
    fun allows(feature: ProFeature, state: ProState): Boolean = state.entitlement != Entitlement.FREE || when (feature) {
        ProFeature.LAUNCHER, ProFeature.THEMES, ProFeature.TODAY_SCREEN_TIME, ProFeature.SHORTS_BLOCKING,
        ProFeature.BASIC_MINDFUL_PAUSE, ProFeature.FIRST_ALLOWANCE -> true
        else -> false
    }
}

data class TrialClock(val daysLeft: Int, val elapsedDays: Int)

/** Re-evaluate both instants in the current zone; retain the highest observed elapsed local day.
 * A rollback never gives back a used day. Zone travel can shift a boundary by at most one day.
 */
fun trialClock(startedAt: Long, now: Long, zone: ZoneId, elapsedHighWater: Int = 0): TrialClock {
    val start = Instant.ofEpochMilli(startedAt).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val elapsed = maxOf(elapsedHighWater.coerceIn(0, 14), (today.toEpochDay() - start.toEpochDay()).coerceIn(0, 14).toInt())
    return TrialClock((14 - elapsed).coerceIn(0, 14), elapsed)
}

/** A callback is a delta, whereas a successful query of BOTH product types is authoritative. */
data class OwnedProduct(val products: List<String>, val purchased: Boolean)
fun restoredPlan(purchases: List<OwnedProduct>): ProPlan? {
    val ids = purchases.filter { it.purchased }.flatMap { it.products }.toSet()
    return when {
        "yoga_pro_lifetime" in ids -> ProPlan.LIFETIME
        "yoga_pro_yearly" in ids -> ProPlan.YEARLY
        else -> null
    }
}

fun resolveProState(daysLeft: Int, cachedPlan: ProPlan?, purchases: List<OwnedProduct>? = null): ProState {
    val plan = if (purchases == null) cachedPlan else restoredPlan(purchases)
    return ProState(if (plan != null) Entitlement.PRO else if (daysLeft > 0) Entitlement.TRIAL else Entitlement.FREE, daysLeft, plan)
}

/** Stable quotas use persisted identifiers, never usage, labels or list encounter order. */
fun freeQuota(ids: Collection<String>, limit: Int): Set<String> = ids.distinct().sorted().take(limit).toSet()
