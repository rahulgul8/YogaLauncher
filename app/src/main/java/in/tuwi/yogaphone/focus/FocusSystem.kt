package `in`.tuwi.yogaphone.focus

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
import android.os.Bundle
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import `in`.tuwi.yogaphone.MainActivity
import `in`.tuwi.yogaphone.R
import `in`.tuwi.yogaphone.YogaApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Alarms, notifications and Do Not Disturb for timeboxes and focus sessions. */
class FocusSystem(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val notifications = NotificationManagerCompat.from(context)

    fun canScheduleExact(): Boolean = Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()

    fun exactAlarmSettingsIntent(): Intent? = if (Build.VERSION.SDK_INT >= 31)
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, android.net.Uri.parse("package:${context.packageName}")) else null

    fun setAlarm(action: String, at: Long, exactIfAllowed: Boolean, extras: Bundle? = null) {
        val pending = alarmIntent(action, extras)
        runCatching {
            if (exactIfAllowed && canScheduleExact()) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            else alarms.setWindow(AlarmManager.RTC_WAKEUP, at, 60_000, pending)
        }
    }

    fun cancelAlarm(action: String) = alarms.cancel(alarmIntent(action))

    /** One replaceable, inexact pass-ending alarm per package. */
    fun setPausePassAlarm(packageName: String, label: String, minutes: Int, at: Long) {
        val action = ACTION_PAUSE_PASS_END_PREFIX + packageName
        setAlarm(action, at, exactIfAllowed = false, extras = Bundle().apply {
            putString(EXTRA_APP_LABEL, label)
            putInt(EXTRA_PASS_MINUTES, minutes)
        })
    }

    private fun alarmIntent(action: String, extras: Bundle? = null): PendingIntent = PendingIntent.getBroadcast(
        context, action.hashCode(), Intent(context, FocusReceiver::class.java).setAction(action).apply {
            if (extras != null) putExtras(extras)
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun actionIntent(action: String, code: Int): PendingIntent = PendingIntent.getBroadcast(
        context, code, Intent(context, FocusReceiver::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun openLauncher(): PendingIntent = PendingIntent.getActivity(
        context, 40, Intent(context, MainActivity::class.java).setAction(ACTION_OPEN_TIMEBOX)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun openHome(): PendingIntent = PendingIntent.getActivity(
        context, 45, Intent(context, MainActivity::class.java).setAction(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun canPost(): Boolean = Build.VERSION.SDK_INT < 33 ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun ensureChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_TIMEBOX, "Timebox", NotificationManager.IMPORTANCE_LOW)
            .apply { description = "The running timebox countdown" })
        manager.createNotificationChannel(NotificationChannel(CHANNEL_DONE, "Timebox and focus endings", NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = "A gentle note when a timebox or focus session ends" })
    }

    fun showTimeboxRunning(label: String, endAt: Long, pausedRemainingMs: Long?) {
        if (!canPost()) return
        ensureChannels()
        val paused = pausedRemainingMs != null
        val builder = NotificationCompat.Builder(context, CHANNEL_TIMEBOX)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(label)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openLauncher())
            .addAction(0, if (paused) "Resume" else "Pause", actionIntent(if (paused) ACTION_RESUME else ACTION_PAUSE, 41))
            .addAction(0, "Stop", actionIntent(ACTION_STOP, 42))
        if (paused) builder.setContentText("Paused · ${formatMinutesLeft(pausedRemainingMs!!)}").setShowWhen(false)
        else builder.setContentText("Timebox running").setUsesChronometer(true).setChronometerCountDown(true).setWhen(endAt).setShowWhen(true)
        runCatching { notifications.notify(ID_TIMEBOX, builder.build()) }
    }

    fun showTimeboxDone(label: String, minutes: Int) {
        notifications.cancel(ID_TIMEBOX)
        if (!canPost()) return
        ensureChannels()
        val notification = NotificationCompat.Builder(context, CHANNEL_DONE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Your $minutes-minute timebox is done")
            .setContentText(label)
            .setAutoCancel(true)
            .setContentIntent(openLauncher())
            .addAction(0, "+5 min", actionIntent(ACTION_EXTEND_5, 43))
            .addAction(0, "Done", actionIntent(ACTION_DONE, 44))
            .build()
        runCatching { notifications.notify(ID_TIMEBOX_DONE, notification) }
    }

    fun cancelTimeboxNotifications() {
        notifications.cancel(ID_TIMEBOX)
        notifications.cancel(ID_TIMEBOX_DONE)
    }

    fun showFocusDone(text: String) {
        if (!canPost()) return
        ensureChannels()
        val notification = NotificationCompat.Builder(context, CHANNEL_DONE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(text)
            .setAutoCancel(true)
            .setContentIntent(openLauncher())
            .build()
        runCatching { notifications.notify(ID_FOCUS_DONE, notification) }
    }

    fun showPausePassDone(packageName: String, label: String, minutes: Int) {
        if (!canPost()) return
        ensureChannels()
        val notification = NotificationCompat.Builder(context, CHANNEL_DONE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Your $minutes minutes on $label are up")
            .setAutoCancel(true)
            .setContentIntent(openHome())
            .build()
        runCatching { notifications.notify(ID_PAUSE_PASS_BASE xor packageName.hashCode(), notification) }
    }

    fun hasDndAccess(): Boolean = context.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted

    /** Returns the filter that was active before, or null when nothing was changed. */
    fun enableDnd(): Int? {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (!manager.isNotificationPolicyAccessGranted) return null
        val previous = manager.currentInterruptionFilter
        if (previous != NotificationManager.INTERRUPTION_FILTER_ALL) return null // the user already chose a quieter mode
        return runCatching { manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY); previous }.getOrNull()
    }

    fun restoreDnd(previous: Int) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (!manager.isNotificationPolicyAccessGranted) return
        // Only undo our own change: if the user moved to another mode meanwhile, leave it.
        if (manager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY) {
            runCatching { manager.setInterruptionFilter(previous) }
        }
    }

    companion object {
        const val CHANNEL_TIMEBOX = "timebox"
        const val CHANNEL_DONE = "timebox_done"
        const val ID_TIMEBOX = 3101
        const val ID_TIMEBOX_DONE = 3102
        const val ID_FOCUS_DONE = 3103
        const val ID_PAUSE_PASS_BASE = 3200
        const val ACTION_TIMEBOX_END = "in.tuwi.yogaphone.TIMEBOX_END"
        const val ACTION_FOCUS_END = "in.tuwi.yogaphone.FOCUS_END"
        const val ACTION_SCHEDULE = "in.tuwi.yogaphone.FOCUS_SCHEDULE"
        const val ACTION_PAUSE = "in.tuwi.yogaphone.TIMEBOX_PAUSE"
        const val ACTION_RESUME = "in.tuwi.yogaphone.TIMEBOX_RESUME"
        const val ACTION_STOP = "in.tuwi.yogaphone.TIMEBOX_STOP"
        const val ACTION_EXTEND_5 = "in.tuwi.yogaphone.TIMEBOX_EXTEND_5"
        const val ACTION_DONE = "in.tuwi.yogaphone.TIMEBOX_DONE"
        const val ACTION_OPEN_TIMEBOX = "in.tuwi.yogaphone.OPEN_TIMEBOX"
        const val ACTION_PAUSE_PASS_END_PREFIX = "in.tuwi.yogaphone.PAUSE_PASS_END."
        const val EXTRA_APP_LABEL = "pause_app_label"
        const val EXTRA_PASS_MINUTES = "pause_pass_minutes"
    }
}

/** Handles alarm firings, notification actions, boot and clock changes. */
class FocusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val repository = (context.applicationContext as YogaApplication).focusRepository
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val action = intent.action
                when {
                    action == FocusSystem.ACTION_PAUSE -> repository.pauseTimebox()
                    action == FocusSystem.ACTION_RESUME -> repository.resumeTimebox()
                    action == FocusSystem.ACTION_STOP -> repository.stopTimebox()
                    action == FocusSystem.ACTION_EXTEND_5 -> repository.extendTimebox(5)
                    action == FocusSystem.ACTION_DONE -> repository.acknowledgeTimebox()
                    action != null && action.startsWith(FocusSystem.ACTION_PAUSE_PASS_END_PREFIX) -> {
                        val packageName = action.removePrefix(FocusSystem.ACTION_PAUSE_PASS_END_PREFIX)
                        val label = intent.getStringExtra(FocusSystem.EXTRA_APP_LABEL) ?: "this app"
                        val minutes = intent.getIntExtra(FocusSystem.EXTRA_PASS_MINUTES, 0)
                        if (minutes > 0) repository.system().showPausePassDone(packageName, label, minutes)
                    }
                    // Alarm firings, boot, time and timezone changes all re-check state and reschedule.
                    else -> {
                        repository.reconcile()
                        val app = context.applicationContext as YogaApplication
                        app.organizeRepository.reconcileSchedules()?.profile?.let { profile ->
                            if (profile.focusOnActivate) repository.startFocus(null)
                        }
                        app.quietRepository.scheduleDigest()
                        app.insightsRepository.scheduleWeekly()
                        app.quietRepository.reconcileGreyscale()
                    }
                }
            } catch (_: Exception) {
            } finally {
                pending.finish()
            }
        }
    }
}
