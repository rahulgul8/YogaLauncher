package `in`.tuwi.yogaphone.usage

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
    val essentialPackages: Set<String> = emptySet(),
    val evaluation: RuleEvaluation = RuleEvaluation(emptyList(), null),
    val error: String? = null,
)

class UsageRepository(private val context: Context, private val database: YogaDatabase, val prefs: UsagePrefs) {
    private val reader = AndroidUsageReader(context)
    private val classifier = AppClassifier(context)
    private val dao = database.usageDao()
    // Shared by UI, workers and snooze receiver: no duplicate check/post race.
    private val mutex = Mutex()
    val rules = dao.observeRules()

    private suspend fun collect(date: LocalDate, p: UsagePreferences): List<UsageAggregateEntity>? {
        val usage = reader.read(date) ?: return null
        val rows = usage.map { (pkg, millis) ->
            val classification = classifier.classify(pkg, database.appEntryDao().get(pkg))
            UsageAggregateEntity(date.toString(), pkg, (millis + 30_000) / 60_000,
                !classification.essential && (p.counts == AllowanceCounts.ALL_EXCEPT_ESSENTIAL || classification.distracting))
        }
        database.withTransaction {
            dao.clearDate(date.toString())
            dao.saveAggregates(rows)
            dao.saveDay(UsageDayEntity(date.toString(), System.currentTimeMillis()))
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
            val p = prefs.preferences.first()
            if (archiveYesterday) collect(today.minusDays(1), p)
            val rows = collect(today, p)
            val weekStart = today.minusDays(6).toString()
            val week = dao.aggregates(weekStart, today.toString())
            val known = dao.days(weekStart, today.toString()).map { it.date }.toSet()
            val essential = (week.map { it.packageName }.toSet() + classifier.mandatoryEssential()).filter {
                classifier.classify(it, database.appEntryDao().get(it)).essential
            }.toSet()
            val evaluation = RuleEvaluator().evaluate(dao.rules().map { it.toRule() },
                rows.orEmpty().filter { it.eligible }.associate { it.packageName to it.foregroundMinutes },
                rows.orEmpty().filterNot { it.packageName in essential }.associate { it.packageName to it.foregroundMinutes })
            if (rows != null) UsageNotifications(context).check(today.toString(), evaluation, p, prefs)
            else UsageNotifications(context).cancelOngoing()
            UsageSnapshot(today, true, rows != null, rows.orEmpty(), week, known, essential, evaluation,
                if (rows == null) "Android usage data is not available yet. Try again after using an app." else null)
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
    suspend fun setThreshold(value: Int) {
        prefs.update { it.copy(warnAtPercent = value) }
        database.withTransaction { dao.rules().forEach { dao.saveRule(it.copy(warnAtPercent = value)) } }
    }
    suspend fun classify(packageName: String, essential: Boolean, distracting: Boolean) {
        val old = database.appEntryDao().get(packageName) ?: AppEntryEntity(packageName)
        database.appEntryDao().upsert(old.copy(isEssential = essential, isDistracting = distracting && !essential, userClassified = true))
    }
    suspend fun clearHistory() = mutex.withLock {
        database.withTransaction { dao.clearHistory(); dao.clearDays() }
    }
}
