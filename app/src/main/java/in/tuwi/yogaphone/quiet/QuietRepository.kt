package `in`.tuwi.yogaphone.quiet

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
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
import `in`.tuwi.yogaphone.data.db.HeldNotificationEntity
import `in`.tuwi.yogaphone.data.db.NotificationRuleEntity
import `in`.tuwi.yogaphone.data.db.YogaDatabase
import `in`.tuwi.yogaphone.focus.ScheduleEvaluator
import `in`.tuwi.yogaphone.focus.toWindow
import `in`.tuwi.yogaphone.usage.AppClassifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class QuietPreferences(
    val quietEnabled: Boolean = false,
    val digestTimes: List<Int> = listOf(12 * 60 + 30, 18 * 60 + 30),
    val greyOverLimit: Boolean = false,
    val greyBedtimeScheduleId: Long? = null,
    val deviceWideGrey: Boolean = false,
    val greySnoozedUntil: Long = 0,
    val overLimitDate: String? = null,
)

/** Notification quieting and the greyscale intervention (launcher theme and, when permitted, device-wide). */
class QuietRepository(private val context: Context, private val database: YogaDatabase) {
    private val dao = database.quietDao()
    private val classifier = AppClassifier(context)

    private object Keys {
        val quiet = booleanPreferencesKey("quiet_enabled")
        val digestTimes = stringSetPreferencesKey("quiet_digest_times")
        val greyOverLimit = booleanPreferencesKey("grey_over_limit")
        val greyBedtime = longPreferencesKey("grey_bedtime_schedule")
        val deviceWide = booleanPreferencesKey("grey_device_wide")
        val snoozed = longPreferencesKey("grey_snoozed_until")
        val overLimitDate = stringPreferencesKey("grey_over_limit_date")
        val savedEnabled = intPreferencesKey("grey_saved_daltonizer_enabled")
        val savedMode = intPreferencesKey("grey_saved_daltonizer_mode")
        val appliedByYoga = booleanPreferencesKey("grey_applied_by_yoga")
    }

    val preferences: Flow<QuietPreferences> = context.dataStore.data.map { p ->
        QuietPreferences(
            quietEnabled = p[Keys.quiet] ?: false,
            digestTimes = p[Keys.digestTimes]?.mapNotNull { it.toIntOrNull() }?.sorted() ?: listOf(12 * 60 + 30, 18 * 60 + 30),
            greyOverLimit = p[Keys.greyOverLimit] ?: false,
            greyBedtimeScheduleId = p[Keys.greyBedtime]?.takeIf { it > 0 },
            deviceWideGrey = p[Keys.deviceWide] ?: false,
            greySnoozedUntil = p[Keys.snoozed] ?: 0,
            overLimitDate = p[Keys.overLimitDate],
        )
    }
    val held = dao.observeHeld()
    val rules = dao.observeRules()

    suspend fun update(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) { context.dataStore.edit(block) }
    suspend fun setQuietEnabled(value: Boolean) { update { it[Keys.quiet] = value }; scheduleDigest() }
    suspend fun setDigestTimes(times: List<Int>) { update { it[Keys.digestTimes] = times.map(Int::toString).toSet() }; scheduleDigest() }
    suspend fun setGreyOverLimit(value: Boolean) = update { it[Keys.greyOverLimit] = value }
    suspend fun setGreyBedtime(id: Long?) = update { if (id == null) it.remove(Keys.greyBedtime) else it[Keys.greyBedtime] = id }
    suspend fun setDeviceWide(value: Boolean) { update { it[Keys.deviceWide] = value }; reconcileGreyscale() }
    suspend fun snoozeGrey(minutes: Int) { update { it[Keys.snoozed] = System.currentTimeMillis() + minutes * 60_000L }; reconcileGreyscale() }

    /** Called by usage checks so background work knows whether today's limit is passed. */
    suspend fun recordOverLimit(over: Boolean) {
        val today = LocalDate.now(ZoneId.systemDefault()).toString()
        update { if (over) it[Keys.overLimitDate] = today else if (it[Keys.overLimitDate] == today) it.remove(Keys.overLimitDate) }
    }

    // ---- Notification policies and the digest ----

    fun listenerGranted(): Boolean = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun listenerSettingsIntent(): Intent =
        if (Build.VERSION.SDK_INT >= 30) Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, ComponentName(context, YogaNotificationListener::class.java).flattenToString())
        else Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    suspend fun policyFor(pkg: String): NotificationPolicy = withContext(Dispatchers.IO) {
        dao.rule(pkg)?.policy?.let { runCatching { NotificationPolicy.valueOf(it) }.getOrNull() } ?: run {
            val c = classifier.classify(pkg, database.appEntryDao().get(pkg))
            HoldDecider.defaultPolicy(c.distracting, c.essential)
        }
    }

    suspend fun setPolicy(pkg: String, policy: NotificationPolicy) = dao.saveRule(NotificationRuleEntity(pkg, policy.name))

    suspend fun hold(item: HeldItem) = withContext(Dispatchers.IO) {
        dao.hold(HeldNotificationEntity(item.key, item.packageName, item.title, item.text, item.postedAt))
    }

    suspend fun clearHeld(pkg: String?) = withContext(Dispatchers.IO) { if (pkg == null) dao.clearAllHeld() else dao.clearHeld(pkg) }

    suspend fun prune() = withContext(Dispatchers.IO) { dao.pruneHeld(System.currentTimeMillis() - Digest.KEEP_MS) }

    /** Posts one summary notification when anything is being held. */
    suspend fun deliverDigest() = withContext(Dispatchers.IO) {
        prune()
        val groups = Digest.group(dao.held().map { HeldItem(it.key, it.packageName, it.title, it.text, it.postedAt) }, System.currentTimeMillis())
        if (groups.isEmpty()) return@withContext
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return@withContext
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_DIGEST, "Notification digest", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(context, 60, Intent(context, MainActivity::class.java).setAction(ACTION_OPEN_DIGEST)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL_DIGEST)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(Digest.summary(groups))
            .setContentText("Tap to read them in Yoga")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(ID_DIGEST, notification) }
    }

    suspend fun scheduleDigest() {
        val prefs = preferences.first()
        val alarms = context.getSystemService(AlarmManager::class.java)
        val pending = PendingIntent.getBroadcast(context, 61, Intent(context, QuietReceiver::class.java).setAction(ACTION_DIGEST),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val nowMinute = LocalTime.now().let { it.hour * 60 + it.minute }
        val minutes = Digest.minutesUntilNext(prefs.digestTimes, nowMinute)
        if (!prefs.quietEnabled || minutes == null) { alarms.cancel(pending); return }
        val at = (System.currentTimeMillis() / 60_000 + minutes) * 60_000
        runCatching { alarms.setWindow(AlarmManager.RTC_WAKEUP, at, 5 * 60_000L, pending) }
    }

    // ---- Greyscale ----

    fun canWriteSecureSettings(): Boolean =
        context.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED

    /** The reason greyscale should be on now, from stored state (usable from background work). */
    suspend fun currentGreyReason(focusActive: Boolean, overLimitNow: Boolean? = null, greyDuringFocus: Boolean): GreyReason? {
        val prefs = preferences.first()
        val today = LocalDate.now(ZoneId.systemDefault()).toString()
        val bedtimeActive = prefs.greyBedtimeScheduleId?.let { id ->
            val window = database.focusDao().schedules().firstOrNull { it.id == id }?.toWindow() ?: return@let false
            ScheduleEvaluator.activeAt(listOf(window), Instant.now(), ZoneId.systemDefault()) != null
        } ?: false
        return GreyscalePolicy.reason(
            GreyTriggers(prefs.greyOverLimit, greyDuringFocus, prefs.greyBedtimeScheduleId != null),
            focusActive, bedtimeActive, overLimitNow ?: (prefs.overLimitDate == today),
            prefs.greySnoozedUntil, System.currentTimeMillis(),
        )
    }

    /** Applies or restores device-wide greyscale when the user enabled it and granted the permission. */
    suspend fun reconcileGreyscale() = withContext(Dispatchers.IO) {
        val app = context.applicationContext as YogaApplication
        val focus = database.focusDao().activeFocus() != null
        val greyDuringFocus = app.focusPrefs.preferences.first().focusGreyscale
        val reason = currentGreyReason(focus, null, greyDuringFocus)
        applyDeviceGreyscale(reason != null && preferences.first().deviceWideGrey)
    }

    private suspend fun applyDeviceGreyscale(on: Boolean) {
        if (!canWriteSecureSettings()) return
        val resolver = context.contentResolver
        val p = context.dataStore.data.first()
        val applied = p[Keys.appliedByYoga] == true
        runCatching {
            if (on && !applied) {
                val enabled = Settings.Secure.getInt(resolver, DALTONIZER_ENABLED, 0)
                val mode = Settings.Secure.getInt(resolver, DALTONIZER, -1)
                if (enabled == 1 && mode == 0) return // the user already has greyscale on
                update { it[Keys.savedEnabled] = enabled; it[Keys.savedMode] = mode; it[Keys.appliedByYoga] = true }
                Settings.Secure.putInt(resolver, DALTONIZER_ENABLED, 1)
                Settings.Secure.putInt(resolver, DALTONIZER, 0)
            } else if (!on && applied) {
                Settings.Secure.putInt(resolver, DALTONIZER_ENABLED, p[Keys.savedEnabled] ?: 0)
                p[Keys.savedMode]?.takeIf { it >= 0 }?.let { Settings.Secure.putInt(resolver, DALTONIZER, it) }
                update { it[Keys.appliedByYoga] = false }
            }
        }
    }

    companion object {
        const val CHANNEL_DIGEST = "digest"
        const val ID_DIGEST = 3201
        const val ACTION_DIGEST = "in.tuwi.yogaphone.DIGEST"
        const val ACTION_OPEN_DIGEST = "in.tuwi.yogaphone.OPEN_DIGEST"
        private const val DALTONIZER_ENABLED = "accessibility_display_daltonizer_enabled"
        private const val DALTONIZER = "accessibility_display_daltonizer"
        const val ADB_GRANT = "adb shell pm grant in.tuwi.yogaphone android.permission.WRITE_SECURE_SETTINGS"
    }
}

class QuietReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val repository = (context.applicationContext as YogaApplication).quietRepository
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (intent.action == QuietRepository.ACTION_DIGEST) repository.deliverDigest()
                repository.scheduleDigest()
            } catch (_: Exception) {
            } finally {
                pending.finish()
            }
        }
    }
}
