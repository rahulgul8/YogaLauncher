package `in`.tuwi.yogaphone.insights

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import `in`.tuwi.yogaphone.MainActivity
import `in`.tuwi.yogaphone.R
import `in`.tuwi.yogaphone.YogaApplication
import `in`.tuwi.yogaphone.data.dataStore
import `in`.tuwi.yogaphone.data.db.FocusStatus
import `in`.tuwi.yogaphone.data.db.InterventionEventEntity
import `in`.tuwi.yogaphone.data.db.TimeboxStatus
import `in`.tuwi.yogaphone.data.db.YogaDatabase
import `in`.tuwi.yogaphone.usage.TimeValueCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

data class InsightsPreferences(
    val tone: CoachingTone = CoachingTone.GENTLE,
    val focusGoalMinutes: Int = 60,
    val allowanceDaysGoal: Int = 5,
    val weeklySummary: Boolean = true,
    val reflections: Boolean = true,
    val insightsEnabled: Boolean = true,
    val usageTracking: Boolean = true,
    val retentionDays: Int = 90,
    val onboardingVersion: Int = 0,
    val tipsSeen: Set<String> = emptySet(),
    val dismissedSuggestion: String? = null,
    val lastReflectedFocusId: Long = 0,
)

data class InsightsData(
    val cards: List<InsightCard> = emptyList(),
    /** Last 7 days, oldest first; null for days that were not recorded. */
    val week: List<Pair<LocalDate, Long?>> = emptyList(),
    val allowance: Int? = null,
    /** Last 28 days, oldest first, each with 24 hourly eligible-minute buckets (null when not recorded). */
    val heatmap: List<Pair<LocalDate, LongArray?>> = emptyList(),
    val focusMinutesToday: Long = 0,
    val daysWithinAllowance: Int = 0,
    val suggestion: Int? = null,
)

class InsightsRepository(private val context: Context, private val database: YogaDatabase) {
    private object Keys {
        val tone = stringPreferencesKey("insights_tone")
        val focusGoal = intPreferencesKey("insights_focus_goal")
        val daysGoal = intPreferencesKey("insights_days_goal")
        val weekly = booleanPreferencesKey("insights_weekly_summary")
        val reflections = booleanPreferencesKey("insights_reflections")
        val enabled = booleanPreferencesKey("insights_enabled")
        val tracking = booleanPreferencesKey("usage_tracking_enabled")
        val retention = intPreferencesKey("data_retention_days")
        val onboarding = intPreferencesKey("onboarding_version")
        val tips = stringSetPreferencesKey("tips_seen")
        val dismissed = stringPreferencesKey("insights_dismissed_suggestion")
        val reflected = longPreferencesKey("insights_last_reflected_focus")
    }

    val preferences: Flow<InsightsPreferences> = context.dataStore.data.map { p ->
        InsightsPreferences(
            tone = runCatching { CoachingTone.valueOf(p[Keys.tone].orEmpty()) }.getOrDefault(CoachingTone.GENTLE),
            focusGoalMinutes = p[Keys.focusGoal] ?: 60,
            allowanceDaysGoal = p[Keys.daysGoal] ?: 5,
            weeklySummary = p[Keys.weekly] ?: true,
            reflections = p[Keys.reflections] ?: true,
            insightsEnabled = p[Keys.enabled] ?: true,
            usageTracking = p[Keys.tracking] ?: true,
            retentionDays = p[Keys.retention] ?: 90,
            onboardingVersion = p[Keys.onboarding] ?: 0,
            tipsSeen = p[Keys.tips].orEmpty(),
            dismissedSuggestion = p[Keys.dismissed],
            lastReflectedFocusId = p[Keys.reflected] ?: 0,
        )
    }

    suspend fun setTone(value: CoachingTone) = context.dataStore.edit { it[Keys.tone] = value.name }
    suspend fun setFocusGoal(value: Int) = context.dataStore.edit { it[Keys.focusGoal] = value }
    suspend fun setDaysGoal(value: Int) = context.dataStore.edit { it[Keys.daysGoal] = value }
    suspend fun setWeeklySummary(value: Boolean) { context.dataStore.edit { it[Keys.weekly] = value }; scheduleWeekly() }
    suspend fun setReflections(value: Boolean) = context.dataStore.edit { it[Keys.reflections] = value }
    suspend fun setInsightsEnabled(value: Boolean) = context.dataStore.edit { it[Keys.enabled] = value }
    suspend fun setUsageTracking(value: Boolean) = context.dataStore.edit { it[Keys.tracking] = value }
    suspend fun setRetention(days: Int) { context.dataStore.edit { it[Keys.retention] = days }; prune() }
    suspend fun setOnboardingVersion(value: Int) = context.dataStore.edit { it[Keys.onboarding] = value }
    suspend fun markTip(id: String) = context.dataStore.edit { it[Keys.tips] = it[Keys.tips].orEmpty() + id }
    suspend fun dismissSuggestion(key: String) = context.dataStore.edit { it[Keys.dismissed] = key }

    // ---- Reflection after focus ----

    /** The id of a completed focus session still waiting for a reflection, if prompts are on. */
    suspend fun pendingReflection(): Long? = withContext(Dispatchers.IO) {
        val prefs = preferences.first()
        if (!prefs.reflections) return@withContext null
        val latest = database.focusDao().latestCompletedFocus() ?: return@withContext null
        val recent = System.currentTimeMillis() - (latest.endedAt ?: 0) < 6 * 60 * 60_000L
        latest.id.takeIf { recent && it > prefs.lastReflectedFocusId }
    }

    suspend fun recordReflection(focusId: Long, feeling: String?) = withContext(Dispatchers.IO) {
        if (feeling != null) database.focusDao().insertIntervention(InterventionEventEntity(type = "REFLECTION",
            packageName = null, shownAt = System.currentTimeMillis(), actionTaken = feeling))
        context.dataStore.edit { it[Keys.reflected] = focusId }
    }

    // ---- Data ----

    suspend fun load(): InsightsData = withContext(Dispatchers.IO) {
        val app = context.applicationContext as YogaApplication
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val usage = database.usageDao()
        val dayRows = usage.days(today.minusDays(27).toString(), today.toString())
        val sampled = dayRows.map { LocalDate.parse(it.date) }.toSet()
        val unlocksByDay = dayRows.associate { LocalDate.parse(it.date) to it.unlocks }
        val aggregates = usage.aggregates(today.minusDays(13).toString(), today.toString())
        val eligibleByDay = sampled.filter { !it.isBefore(today.minusDays(13)) }.associateWith { day ->
            aggregates.filter { it.date == day.toString() && it.eligible }.sumOf { it.foregroundMinutes }
        }
        val hourly = usage.hours(today.minusDays(27).toString(), today.toString()).groupBy { LocalDate.parse(it.date) }
            .filterKeys { it in sampled }
            .mapValues { (_, rows) -> LongArray(24).also { array -> rows.forEach { if (it.hour in 0..23) array[it.hour] = it.eligibleMinutes } } }
        val allowance = usage.rules().firstOrNull { it.scope == "TOTAL" && it.enabled }?.dailyAllowanceMinutes

        val weekStart = today.minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()
        val dayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val timeboxes = database.focusDao().timeboxesSince(weekStart)
        val focus = database.focusDao().focusSince(weekStart).filter { it.status != FocusStatus.ACTIVE }
        fun minutes(start: Long, end: Long?) = ((end ?: start) - start).coerceAtLeast(0) / 60_000
        val focusWeek = focus.sumOf { minutes(it.startAt, it.endedAt) }
        val focusToday = focus.filter { it.startAt >= dayStart }.sumOf { minutes(it.startAt, it.endedAt) }

        val organize = app.organizeRepository
        val names = database.organizeDao().categories().associate { it.id to it.name }
        val weekRows = aggregates.filter { it.eligible && !LocalDate.parse(it.date).isBefore(today.minusDays(6)) }
        val categoryMinutes = weekRows.groupBy { names[organize.categoryOf(it.packageName, "", database.appEntryDao().get(it.packageName))] ?: "Other" }
            .mapValues { (_, rows) -> rows.sumOf { it.foregroundMinutes } }

        val usagePrefs = app.usagePrefs.preferences.first()
        val weeklyValue = if (usagePrefs.timeValueEnabled && allowance != null) {
            val excess = InsightEngine.thisWeek(today).sumOf { ((eligibleByDay[it] ?: 0) - allowance).coerceAtLeast(0) }
            if (excess > 0) TimeValueCalculator().format(excess, usagePrefs.hourlyAmount(), usagePrefs.currency) else null
        } else null

        val input = InsightInput(
            today = today, eligibleByDay = eligibleByDay, hourly = hourly, allowanceMinutes = allowance,
            timeboxesCompleted = timeboxes.count { it.status == TimeboxStatus.COMPLETED },
            timeboxesExtended = timeboxes.count { it.extendedMinutes > 0 },
            focusMinutesThisWeek = focusWeek, streak = app.focusRepository.streak(),
            categoryMinutesThisWeek = categoryMinutes, weeklyTimeValue = weeklyValue, nowHour = ZonedDateTime.now(zone).hour,
            unlocksByDay = unlocksByDay,
        )
        val prefs = preferences.first()
        val suggestion = InsightEngine.suggestedAllowance(allowance, eligibleByDay, today)
            ?.takeIf { prefs.dismissedSuggestion != "$today-$it" && prefs.dismissedSuggestion != "${weekKey(today)}-$it" }
        InsightsData(
            cards = InsightEngine.cards(input, prefs.tone),
            week = (6L downTo 0L).map { today.minusDays(it) }.map { it to eligibleByDay[it] },
            allowance = allowance,
            heatmap = (27L downTo 0L).map { today.minusDays(it) }.map { it to hourly[it] },
            focusMinutesToday = focusToday,
            daysWithinAllowance = if (allowance == null) 0 else InsightEngine.thisWeek(today).count { (eligibleByDay[it] ?: Long.MAX_VALUE) <= allowance },
            suggestion = suggestion,
        )
    }

    fun weekKey(day: LocalDate): String = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()

    /** Deletes history older than the retention period. */
    suspend fun prune() = withContext(Dispatchers.IO) {
        val days = preferences.first().retentionDays
        val zone = ZoneId.systemDefault()
        val cutoffDay = LocalDate.now(zone).minusDays(days.toLong())
        val cutoff = cutoffDay.atStartOfDay(zone).toInstant().toEpochMilli()
        val usage = database.usageDao()
        usage.pruneAggregates(cutoffDay.toString())
        usage.pruneHours(cutoffDay.toString())
        usage.pruneDays(cutoffDay.toString())
        database.focusDao().pruneFocus(cutoff)
        database.focusDao().pruneTimeboxes(cutoff)
        database.focusDao().pruneInterventions(cutoff)
    }

    // ---- Weekly summary (Sunday 19:00) ----

    suspend fun scheduleWeekly() {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val pending = PendingIntent.getBroadcast(context, 71, Intent(context, InsightsReceiver::class.java).setAction(ACTION_WEEKLY),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val prefs = preferences.first()
        if (!prefs.weeklySummary || !prefs.insightsEnabled) { alarms.cancel(pending); return }
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        var next = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).with(LocalTime.of(19, 0))
        if (!next.isAfter(now)) next = next.plusWeeks(1)
        runCatching { alarms.setWindow(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), 15 * 60_000L, pending) }
    }

    suspend fun postWeeklySummary() {
        val prefs = preferences.first()
        if (!prefs.weeklySummary || !prefs.insightsEnabled) return
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val card = load().cards.firstOrNull() ?: return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Weekly summary", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(context, 72, Intent(context, MainActivity::class.java).setAction(ACTION_OPEN_INSIGHTS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Your week with Yoga")
            .setContentText(card.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(card.body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(3301, notification) }
    }

    companion object {
        const val CHANNEL = "weekly_summary"
        const val ACTION_WEEKLY = "in.tuwi.yogaphone.WEEKLY_SUMMARY"
        const val ACTION_OPEN_INSIGHTS = "in.tuwi.yogaphone.OPEN_INSIGHTS"
    }
}

class InsightsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val repository = (context.applicationContext as YogaApplication).insightsRepository
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (intent.action == InsightsRepository.ACTION_WEEKLY) repository.postWeeklySummary()
                repository.scheduleWeekly()
            } catch (_: Exception) {
            } finally {
                pending.finish()
            }
        }
    }
}
