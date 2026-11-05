package `in`.tuwi.yogaphone.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import java.time.LocalDate
import java.time.ZoneId

object UsageAccess {
    fun granted(context: Context): Boolean = runCatching {
        val ops = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= 29) ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        else @Suppress("DEPRECATION") ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        mode == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)

    fun openSettings(context: Context) {
        val specific = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:${context.packageName}"))
        if (runCatching { context.startActivity(specific.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) return
        runCatching { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}

class AndroidUsageReader(private val context: Context) {
    /** Null means access/data unavailable; it must never be represented as zero usage. */
    fun read(date: LocalDate, now: Long = System.currentTimeMillis()): Map<String, Long>? {
        if (!UsageAccess.granted(context)) return null
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = minOf(date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(), now)
        return runCatching {
            val source = context.getSystemService(UsageStatsManager::class.java)
                .queryEvents(start - 24 * 60 * 60 * 1000L, end) ?: return null
            val events = mutableListOf<ForegroundEvent>()
            val event = UsageEvents.Event()
            while (source.hasNextEvent()) {
                source.getNextEvent(event)
                val type = when {
                    event.eventType == UsageEvents.Event.SCREEN_NON_INTERACTIVE -> ForegroundEvent.Type.SCREEN_OFF
                    event.eventType == UsageEvents.Event.DEVICE_SHUTDOWN -> ForegroundEvent.Type.SHUTDOWN
                    Build.VERSION.SDK_INT >= 29 && event.eventType == UsageEvents.Event.ACTIVITY_STOPPED -> ForegroundEvent.Type.STOPPED
                    event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND -> ForegroundEvent.Type.RESUMED // Same values as ACTIVITY_RESUMED/PAUSED on 29+.
                    event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND -> ForegroundEvent.Type.PAUSED
                    else -> null
                }
                if (type != null) events += ForegroundEvent(event.packageName.orEmpty(), event.timeStamp, type, event.className)
            }
            // An empty event feed can mean a locked user or expired retention. Preserve prior aggregates.
            if (events.isEmpty()) null else UsageCalculator(context.packageName).calculate(events, start, end, now)
        }.getOrNull()
    }
}
