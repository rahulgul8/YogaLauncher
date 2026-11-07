package `in`.tuwi.yogaphone.pro

import android.app.Activity
import android.content.Context
import androidx.datastore.preferences.core.*
import com.android.billingclient.api.Purchase
import `in`.tuwi.yogaphone.BuildConfig
import `in`.tuwi.yogaphone.data.dataStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZoneId
import `in`.tuwi.yogaphone.ui.AppIcon
import `in`.tuwi.yogaphone.ui.AppIcons

class ProRepository(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val ready = CompletableDeferred<Unit>()
    private val refreshLock = Mutex()
    private val mutableState = MutableStateFlow(ProState())
    val state: StateFlow<ProState> = mutableState.asStateFlow()
    private val mutableProducts = MutableStateFlow<List<ProProduct>>(emptyList())
    val products = mutableProducts.asStateFlow()
    private val mutableMessage = MutableStateFlow<String?>(null)
    val message = mutableMessage.asStateFlow()
    val trialDismissed = context.dataStore.data.map { it[DISMISSED] ?: false }
    val debugOverride = context.dataStore.data.map { if (BuildConfig.DEBUG) it[OVERRIDE] else null }
    val selectedIcon = context.dataStore.data.map { p -> p[ICON]?.let { runCatching { AppIcon.valueOf(it) }.getOrNull() } ?: AppIcon.TEN_PAST_TEN }
    private val billing = BillingClientWrapper(context, scope, ::acceptPurchases) { mutableMessage.value = it }

    init {
        scope.launch {
            advanceClock()
            context.dataStore.edit { if (it[ICON] == null) it[ICON] = AppIcons.current(context).name }
            updateState(context.dataStore.data.first())
            ready.complete(Unit)
            launch { context.dataStore.data.collect { updateState(it) } }
            launch { while (isActive) { delay(60_000); advanceClock() } }
            launch {
                combine(state, selectedIcon) { state, chosen -> if (ProGate.allows(ProFeature.COLON_ICON, state)) chosen else AppIcon.TEN_PAST_TEN }
                    .distinctUntilChanged().collect { icon -> if (AppIcons.current(context) != icon) AppIcons.set(context, icon) }
            }
            launch {
                state.map { it.entitlement }.distinctUntilChanged().collect {
                    val app = context.applicationContext as `in`.tuwi.yogaphone.YogaApplication
                    runCatching { app.focusRepository.reconcile() }
                    runCatching { app.quietRepository.scheduleDigest(); app.quietRepository.reconcileGreyscale() }
                    runCatching { app.insightsRepository.scheduleWeekly() }
                    runCatching { app.usageRepository.check() }
                }
            }
            refresh()
        }
    }

    /** Reads persisted entitlement before a cold-start worker makes a policy decision. */
    suspend fun current(): ProState { ready.await(); advanceClock(); return state.value }
    suspend fun allows(feature: ProFeature) = ProGate.allows(feature, current())

    private fun updateState(p: Preferences) {
        val start = p[START] ?: return
        val clock = trialClock(start, System.currentTimeMillis(), ZoneId.systemDefault(), p[ELAPSED] ?: 0)
        val plan = p[PLAN]?.let { runCatching { ProPlan.valueOf(it) }.getOrNull() }
        val actual = resolveProState(clock.daysLeft, plan)
        val override = if (BuildConfig.DEBUG) p[OVERRIDE]?.let { runCatching { Entitlement.valueOf(it) }.getOrNull() } else null
        mutableState.value = when (override) {
            Entitlement.PRO -> ProState(Entitlement.PRO, clock.daysLeft, plan ?: ProPlan.YEARLY)
            Entitlement.TRIAL -> ProState(Entitlement.TRIAL, clock.daysLeft.coerceAtLeast(1))
            Entitlement.FREE -> ProState(Entitlement.FREE)
            null -> actual
        }
    }

    private suspend fun advanceClock() {
        val updated = context.dataStore.edit { p ->
            if (p[START] == null) p[START] = System.currentTimeMillis()
            val clock = trialClock(p[START]!!, System.currentTimeMillis(), ZoneId.systemDefault(), p[ELAPSED] ?: 0)
            p[ELAPSED] = clock.elapsedDays
        }
        updateState(updated)
    }

    fun onResume() { scope.launch { advanceClock(); refresh() } }
    private suspend fun refresh() = refreshLock.withLock {
        billing.restore()
        mutableProducts.value = billing.products()
    }
    fun restore() { scope.launch {
        ready.await()
        refreshLock.withLock {
            mutableMessage.value = if (billing.restore()) {
                if (state.value.entitlement == Entitlement.PRO) "Purchases restored" else "No Yoga Pro purchase found"
            } else "Couldn't reach Google Play. Your saved access is unchanged."
            mutableProducts.value = billing.products()
        }
    } }
    fun buy(activity: Activity, product: ProProduct) = billing.buy(activity, product)

    private suspend fun acceptPurchases(purchases: List<Purchase>, authoritative: Boolean) {
        val owned = purchases.map { OwnedProduct(it.products, it.purchaseState == Purchase.PurchaseState.PURCHASED) }
        val restored = restoredPlan(owned)
        context.dataStore.edit { p ->
            if (restored != null) {
                // An incremental yearly callback must not downgrade a previously restored lifetime.
                if (authoritative || p[PLAN] != ProPlan.LIFETIME.name) p[PLAN] = restored.name
            } else if (authoritative) p.remove(PLAN)
        }.also(::updateState)
        if (purchases.any { it.purchaseState == Purchase.PurchaseState.PENDING }) mutableMessage.value = "Purchase pending. Yoga Pro unlocks when Google Play confirms payment."
        purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && restoredPlan(listOf(OwnedProduct(it.products, true))) != null }
            .forEach { if (!billing.acknowledge(it)) mutableMessage.value = "Purchase received. Google Play confirmation will retry when you return." }
    }

    fun dismissTrialCard() { scope.launch { context.dataStore.edit { it[DISMISSED] = true } } }
    fun selectIcon(icon: AppIcon) { scope.launch { context.dataStore.edit { it[ICON] = icon.name } } }
    fun setDebugOverride(value: Entitlement?) { if (BuildConfig.DEBUG) scope.launch {
        context.dataStore.edit { if (value == null) it.remove(OVERRIDE) else it[OVERRIDE] = value.name }
    } }

    companion object {
        val START = longPreferencesKey("pro_trial_started_at")
        val ELAPSED = intPreferencesKey("pro_trial_elapsed_days")
        val DISMISSED = booleanPreferencesKey("pro_trial_card_dismissed")
        val PLAN = stringPreferencesKey("pro_cached_plan")
        val OVERRIDE = stringPreferencesKey("pro_debug_override")
        val ICON = stringPreferencesKey("selected_app_icon")
        val durableKeys = setOf(START.name, ELAPSED.name, DISMISSED.name)
        val deviceKeys = setOf(PLAN.name, OVERRIDE.name)
    }
}
