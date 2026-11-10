package `in`.tuwi.yogaphone.usage

import `in`.tuwi.yogaphone.pro.*
import android.content.Context
import androidx.room.withTransaction
import `in`.tuwi.yogaphone.data.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

data class UsageSnapshot(
    val date: LocalDate = LocalDate.now(),
    val access: Boolean = false,
    val available: Boolean = false,
    val today: List<UsageAggregateEntity> = emptyList(),
    val week: List<UsageAggregateEntity> = emptyList(),
    val knownDays: Set<String> = emptySet(),
    val excludedPackages: Set<String> = emptySet(),
    val essentialPackages: Set<String> = emptySet(),
    val evaluation: RuleEvaluation = RuleEvaluation(emptyList(), null),
    val categoryOf: Map<String, String> = emptyMap(),
    val appLabels: Map<String, String> = emptyMap(),
    val unlocksToday: Int? = null,
    val error: String? = null,
)

class UsageRepository(private val context: Context, private val database: YogaDatabase, val prefs: UsagePrefs) {
    private val reader = AndroidUsageReader(context)
    private val classifier = AppClassifier(context)
    private val dao = database.usageDao()
    // Shared by UI, workers and snooze receiver: no duplicate check/post race.
    private val mutex = Mutex()
    val rules = dao.observeRules()

    private fun excludedPackages(): Set<String> = context.packageManager.queryIntentActivities(
        android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_HOME), 0
    ).map { it.activityInfo.packageName }.toSet() + context.packageName

    private fun labelOf(pkg: String): String = runCatching {
        context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(pkg, 0)).toString()
    }.getOrElse { "App" }

    private suspend fun collect(date: LocalDate, p: UsagePreferences): List<UsageAggregateEntity>? {
        val day = reader.readDay(date) ?: return null
        val usage = day.totals
        val hourly = day.hours
        val rows = usage.map { (pkg, millis) ->
            val classification = classifier.classify(pkg, database.appEntryDao().get(pkg))
            UsageAggregateEntity(date.toString(), pkg, (millis + 30_000) / 60_000,
                !classification.essential && (p.counts == AllowanceCounts.ALL_EXCEPT_ESSENTIAL || classification.distracting))
        }
        val eligible = rows.filter { it.eligible }.map { it.packageName }.toSet()
        val hourRows = hourly.map { (hour, perApp) ->
            UsageHourEntity(date.toString(), hour, (perApp.filterKeys { it in eligible }.values.sum() + 30_000) / 60_000)
        }
        database.withTransaction {
            dao.clearDate(date.toString())
            dao.clearHours(date.toString())
            dao.saveHours(hourRows)
            dao.saveAggregates(rows)
            dao.saveDay(UsageDayEntity(date.toString(), System.currentTimeMillis(), day.unlocks))
        }
        return rows
    }

    suspend fun check(archiveYesterday: Boolean = false): UsageSnapshot = withContext(Dispatchers.IO) {
        mutex.withLock {
            val today = LocalDate.now(ZoneId.systemDefault())
            if (!UsageAccess.granted(context)) {
                UsageNotifications(context).cancelAll()
                return@withLock UsageSnapshot(date = today)
            }
            val app = context.applicationContext as `in`.tuwi.yogaphone.YogaApplication
            if (!app.insightsRepository.preferences.first().usageTracking) {
                UsageNotifications(context).cancelAll()
                return@withLock UsageSnapshot(date = today, access = true, error = "Usage measuring is off in Privacy and data.")
            }
            val pro = app.proRepository.current()
            val saved = prefs.preferences.first()
            val p = if (ProGate.allows(ProFeature.REMINDERS, pro)) saved else saved.copy(reminders = false, timeValueEnabled = false)
            if (archiveYesterday) { collect(today.minusDays(1), p); runCatching { app.insightsRepository.prune() } }
            val rows = collect(today, p)
            val weekStart = today.minusDays(6).toString()
            val excluded = excludedPackages()
            val week = dao.aggregates(weekStart, today.toString()).filterNot { it.packageName in excluded }
            val visibleRows = rows.orEmpty().filterNot { it.packageName in excluded }
            val known = dao.days(weekStart, today.toString()).map { it.date }.toSet()
            val essential = (week.map { it.packageName }.toSet() + classifier.mandatoryEssential()).filter {
                classifier.classify(it, database.appEntryDao().get(it)).essential
            }.toSet()
            val organize = (context.applicationContext as `in`.tuwi.yogaphone.YogaApplication).organizeRepository
            val categoryOf = week.map { it.packageName }.toSet().associateWith { pkg ->
                organize.categoryOf(pkg, "", database.appEntryDao().get(pkg))
            }
            val savedRules = dao.rules()
            val freeIds = freeQuota(savedRules.filter { it.enabled }.map { it.id }, 1)
            val activeRules = if (ProGate.allows(ProFeature.EXTRA_ALLOWANCES, pro)) savedRules else savedRules.filter { it.id in freeIds }
            val evaluation = RuleEvaluator().evaluate(activeRules.map { it.toRule() },
                visibleRows.filter { it.eligible }.associate { it.packageName to it.foregroundMinutes },
                visibleRows.filterNot { it.packageName in essential }.associate { it.packageName to it.foregroundMinutes },
                categoryOf)
            if (rows != null) {
                val quiet = (context.applicationContext as `in`.tuwi.yogaphone.YogaApplication).quietRepository
                runCatching {
                    quiet.recordOverLimit(evaluation.overall?.state == RuleState.OVER)
                    quiet.reconcileGreyscale()
                }
            }
            if (!ProGate.allows(ProFeature.REMINDERS, pro)) UsageNotifications(context).cancelAll()
            else if (rows != null) UsageNotifications(context).check(today.toString(), evaluation, p, prefs)
            else UsageNotifications(context).cancelOngoing()
            val unlocksToday = dao.days(today.toString(), today.toString()).firstOrNull()?.unlocks
            val labels = week.map { it.packageName }.toSet().associateWith(::labelOf)
            UsageSnapshot(
                date = today,
                access = true,
                available = rows != null,
                today = visibleRows,
                week = week,
                knownDays = known,
                excludedPackages = excluded,
                essentialPackages = essential,
                evaluation = evaluation,
                categoryOf = categoryOf,
                appLabels = labels,
                unlocksToday = unlocksToday,
                error = if (rows == null) "Android usage data is not available yet. Try again after using an app." else null,
            )
        }
    }

    suspend fun snoozeToday() = mutex.withLock {
        prefs.snooze(LocalDate.now(ZoneId.systemDefault()).toString())
        UsageNotifications(context).cancelOngoing()
    }
    suspend fun saveRule(packageName: String?, minutes: Int?) {
        val id = packageName?.let { "app:$it" } ?: "total"
        if (minutes == null) dao.removeRule(id)
        else dao.saveRule(UsageRuleEntity(id, if (packageName == null) "TOTAL" else "APP", packageName,
            minutes.coerceIn(15, 480) / 15 * 15, prefs.preferences.first().warnAtPercent))
    }
    /** Restore the complete saved rule for introduction Undo, including disabled state and warning threshold. */
    suspend fun restoreTotalRule(previous: UsageRuleEntity?) {
        if (previous == null) dao.removeRule("total") else dao.saveRule(previous)
    }
    suspend fun saveCategoryRule(categoryId: String, minutes: Int?) {
        val id = "category:$categoryId"
        if (minutes == null) dao.removeRule(id)
        else dao.saveRule(UsageRuleEntity(id, "CATEGORY", categoryId, minutes.coerceIn(15, 480) / 15 * 15,
            prefs.preferences.first().warnAtPercent))
    }
    suspend fun setThreshold(value: Int) {
        prefs.update { it.copy(warnAtPercent = value) }
        database.withTransaction { dao.rules().forEach { dao.saveRule(it.copy(warnAtPercent = value)) } }
    }
    suspend fun classify(packageName: String, essential: Boolean, distracting: Boolean) {
        val old = database.appEntryDao().get(packageName) ?: AppEntryEntity(packageName)
        database.appEntryDao().upsert(old.copy(isEssential = essential, isDistracting = distracting && !essential, userClassified = true))
    }
    suspend fun clearHistory() = mutex.withLock {
        database.withTransaction { dao.clearHistory(); dao.clearDays(); dao.clearAllHours() }
    }
}
