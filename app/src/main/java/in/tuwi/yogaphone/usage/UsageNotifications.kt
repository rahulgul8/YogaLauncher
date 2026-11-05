package `in`.tuwi.yogaphone.usage

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import `in`.tuwi.yogaphone.MainActivity
import `in`.tuwi.yogaphone.R
import `in`.tuwi.yogaphone.YogaApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class UsageNotifications(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)
    private fun canPost() = (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) && manager.areNotificationsEnabled()
    private fun builder() = NotificationCompat.Builder(context, CHANNEL)
        .setSmallIcon(R.drawable.ic_launcher_foreground).setContentTitle("Yoga · Usage")
        .setPriority(NotificationCompat.PRIORITY_LOW).setOnlyAlertOnce(true).setSilent(true)
        .setContentIntent(PendingIntent.getActivity(context, 20,
            Intent(context, MainActivity::class.java).setAction(ACTION_DETAILS), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
    suspend fun check(date: String, evaluation: RuleEvaluation, p: UsagePreferences, prefs: UsagePrefs) {
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Usage reminders", NotificationManager.IMPORTANCE_LOW))
        if (!canPost()) { cancelAll(); return }
        if (!p.reminders) cancelReminders()
        if (p.reminders) evaluation.rules.forEach { result ->
            if (ReminderLedger.shouldSend(prefs.sent(), date, result.rule.id, result.state)) {
                val label = result.rule.targetPackage?.let { pkg ->
                    runCatching { context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
                }
                val copy = if (result.state == RuleState.OVER) "You're ${result.excessMinutes} minutes past today's limit."
                    else "You've used ${result.used} of your ${result.allowance} minutes today."
                val text = if (label == null) copy else "$label: $copy"
                try {
                    manager.notify("usage:${result.rule.id}", REMINDER, builder().setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text)).setAutoCancel(true).build())
                    prefs.markSent(date, result.rule.id, result.state)
                } catch (_: SecurityException) { /* Permission can be revoked during a check. */ }
            }
        }
        val total = evaluation.overall
        if (p.timeValueEnabled && p.ongoingValue && total != null && total.state == RuleState.OVER && !prefs.isSnoozed(date)) {
            val text = "${total.excessMinutes} min past today's limit · ${TimeValueCalculator().format(total.excessMinutes, p.hourlyAmount(), p.currency)} time value"
            val snooze = PendingIntent.getBroadcast(context, 21, Intent(context, UsageSnoozeReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            try {
                manager.notify(ONGOING, builder().setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    .setOngoing(true).addAction(0, "Snooze for today", snooze).build())
            } catch (_: SecurityException) { }
        } else cancelOngoing()
    }
    fun cancelOngoing() = manager.cancel(ONGOING)
    private fun cancelReminders() {
        manager.activeNotifications.filter { it.id == REMINDER }.forEach { manager.cancel(it.tag, it.id) }
    }
    fun cancelAll() { cancelReminders(); cancelOngoing() }
    companion object {
        const val ACTION_DETAILS = "in.tuwi.yogaphone.USAGE_DETAILS"
        private const val CHANNEL = "usage_reminders"
        private const val REMINDER = 201
        private const val ONGOING = 202
    }
}

class UsageSnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { (context.applicationContext as YogaApplication).usageRepository.snoozeToday() }
            finally { pending.finish() }
        }
    }
}

