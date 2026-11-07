package `in`.tuwi.yogaphone.quiet

import `in`.tuwi.yogaphone.pro.*
import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import `in`.tuwi.yogaphone.YogaApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Holds notifications the user chose to quiet. Active only after the user turns on "Quiet notifications"
 * and grants listener access; without both it does nothing.
 */
class YogaNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val app = applicationContext as YogaApplication
        val notification = sbn.notification ?: return
        val extras = notification.extras
        val incoming = IncomingNotification(
            packageName = sbn.packageName,
            category = notification.category,
            ongoing = sbn.isOngoing,
            foregroundService = notification.flags and Notification.FLAG_FOREGROUND_SERVICE != 0,
            mediaSession = extras?.containsKey(Notification.EXTRA_MEDIA_SESSION) == true,
            groupSummary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0,
        )
        scope.launch {
            if (!app.proRepository.allows(ProFeature.QUIET_NOTIFICATIONS)) return@launch
            val repository = app.quietRepository
            if (!repository.preferences.first().quietEnabled) return@launch
            val focusActive = app.database.focusDao().activeFocus() != null
            val policy = repository.policyFor(sbn.packageName)
            if (!HoldDecider.shouldHold(incoming, policy, focusActive, packageName)) return@launch
            val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
            val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
            repository.hold(HeldItem(sbn.key, sbn.packageName, title.take(120), text.take(240), sbn.postTime))
            runCatching { cancelNotification(sbn.key) }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
