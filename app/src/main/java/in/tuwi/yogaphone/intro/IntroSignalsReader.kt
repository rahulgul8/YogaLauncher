package `in`.tuwi.yogaphone.intro

import android.content.Intent
import `in`.tuwi.yogaphone.BuildConfig
import `in`.tuwi.yogaphone.YogaApplication
import `in`.tuwi.yogaphone.guard.*
import `in`.tuwi.yogaphone.insights.InsightEngine
import `in`.tuwi.yogaphone.pro.*
import `in`.tuwi.yogaphone.usage.UsageAccess
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate

/** One application-owned reader. Queries run on IO, never during home recomposition. */
class IntroSignalsReader(private val app: YogaApplication) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshes = MutableStateFlow(0L)
    fun refresh() { refreshes.value += 1 }

    /** Debug and profile builds: fake evidence for checking introductions on a phone without real history. */
    @Volatile private var preview: Map<String, Int> = emptyMap()

    /**
     * `youtube=400,instagram=300,browser=90,days=7,avg=200`, or `off`. Keys left out keep real values.
     * `reset=1` also forgets every introduction shown, snoozed or acted on, so one can be checked again today.
     */
    fun setPreview(spec: String) {
        if (!BuildConfig.INTRO_PREVIEW) return
        val parsed = if (spec == "off") emptyMap() else spec.split(',').mapNotNull { part ->
            part.split('=').takeIf { it.size == 2 }?.let { (k, v) -> v.trim().toIntOrNull()?.let { k.trim() to it } }
        }.toMap()
        preview = parsed - "reset"
        if (parsed["reset"] == 1) scope.launch { app.introStore.clearForPreview(); refresh() } else refresh()
    }

    private fun withPreview(real: IntroSignals): IntroSignals {
        val p = preview
        if (!BuildConfig.INTRO_PREVIEW || p.isEmpty()) return real
        return real.copy(
            youtubeMinutes7d = p["youtube"] ?: real.youtubeMinutes7d,
            youtubeInstalled = real.youtubeInstalled || "youtube" in p,
            instagramMinutes7d = p["instagram"] ?: real.instagramMinutes7d,
            instagramInstalled = real.instagramInstalled || "instagram" in p,
            browserMinutes7d = p["browser"] ?: real.browserMinutes7d,
            recordedDays = p["days"] ?: real.recordedDays,
            avgDistractingPerDay = p["avg"]?.toDouble() ?: real.avgDistractingPerDay,
        )
    }
    private val clock = flow { while (true) { emit(System.currentTimeMillis()); delay(30_000) } }
    private val changes = combine(clock, refreshes, app.guardPrefs.preferences,
        app.quietRepository.preferences, app.usageRepository.rules) { _, _, _, _, _ -> Unit }
    val signals: StateFlow<IntroSignals?> = combine(changes, app.proRepository.state,
        app.insightsRepository.preferences) { _, _, _ ->
            try { read() } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { null }
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), null)

    suspend fun read(): IntroSignals = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val dao = app.database.usageDao()
        val prefs = app.insightsRepository.preferences.first()
        val usable = prefs.usageTracking && UsageAccess.granted(app)
        val known = if (usable) dao.days(today.minusDays(7).toString(), today.toString()).map { LocalDate.parse(it.date) }.toSet() else emptySet()
        val excluded = app.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            .map { it.activityInfo.packageName }.toSet() + app.packageName
        val rows = if (usable) dao.aggregates(today.minusDays(7).toString(), today.toString())
            .filterNot { it.packageName in excluded } else emptyList()
        val weekDays = known.filter { !it.isBefore(today.minusDays(6)) }
        val week = rows.filter { it.date >= today.minusDays(6).toString() }
        val eligibleByDay = known.associateWith { date -> rows.filter { it.date == date.toString() && it.eligible }.sumOf { it.foregroundMinutes } }
        fun minutes(packages: Set<String>) = week.filter { it.packageName in packages }.sumOf { it.foregroundMinutes }.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        fun installed(pkg: String) = runCatching { app.packageManager.getApplicationInfo(pkg, 0).enabled }.getOrDefault(false)
        val guard = app.guardPrefs.preferences.first()
        val enabled = BuildConfig.GUARD && YogaGuardService.isEnabled(app)
        val pro = app.proRepository.current()
        val rules = dao.rules()
        val free = freeQuota(rules.filter { it.enabled }.map { it.id }, 1)
        val extra = ProGate.allows(ProFeature.EXTRA_ALLOWANCES, pro)
        val total = rules.firstOrNull { it.scope == "TOTAL" && it.enabled && (extra || it.id in free) }
        val allowance = total?.dailyAllowanceMinutes
        val suggestion = InsightEngine.suggestedAllowance(allowance, eligibleByDay, today)
        app.guardPrefs.pruneBlockHistory(today)
        val history = app.guardPrefs.weeklyBlocks.first().filterKeys { !it.isBefore(today.minusDays(6)) && !it.isAfter(today) }
        val allowed = IntroId.entries.toMutableSet()
        if (!BuildConfig.GUARD || !ProGate.allows(ProFeature.SHORTS_BLOCKING, pro)) {
            allowed.removeAll(setOf(IntroId.SHORTS_BLOCK, IntroId.REELS_BLOCK, IntroId.BROWSER_SURFACES))
        }
        // A free app allowance can occupy the one active rule slot. Do not offer an ineffective TOTAL.
        if (!extra && rules.any { it.enabled && it.id != "total" }) allowed.remove(IntroId.DAILY_ALLOWANCE)
        if (prefs.onboardingVersion < 1) allowed.clear()
        withPreview(IntroSignals(
            youtubeMinutes7d = minutes(setOf("com.google.android.youtube")),
            instagramMinutes7d = minutes(setOf("com.instagram.android")),
            browserMinutes7d = minutes(ShortsDetector.browserAddressIds.keys), recordedDays = weekDays.size,
            youtubeInstalled = installed("com.google.android.youtube"), instagramInstalled = installed("com.instagram.android"),
            guardEnabled = enabled,
            surfacesEnabled = if (enabled) guard.surfaces.filterValues { it.enabled }.keys else emptySet(),
            blockedSurfaces = if (enabled) guard.surfaces.filterValues { it.enabled && it.allowanceMinutes == 0 }.keys else emptySet(),
            blocks7d = GuardSurface.entries.associateWith { surface -> history.values.sumOf { it[surface] ?: 0 } },
            allowanceMinutes = allowance,
            avgDistractingPerDay = if (weekDays.size >= 3) weekDays.map { eligibleByDay[it] ?: 0L }.average() else null,
            suggestedAllowance = suggestion,
            daysUnderSuggestedAllowance = if (suggestion == null) 0 else (1L..7L).count { (eligibleByDay[today.minusDays(it)] ?: Long.MAX_VALUE) <= suggestion },
            daysOverLimitThisWeek = if (allowance == null) 0 else weekDays.count { (eligibleByDay[it] ?: 0) > allowance },
            greyOverLimitOn = app.quietRepository.preferences.first().greyOverLimit, allowed = allowed,
            countsAllApps = app.usagePrefs.preferences.first().counts == `in`.tuwi.yogaphone.usage.AllowanceCounts.ALL_EXCEPT_ESSENTIAL,
        ))
    }
}
