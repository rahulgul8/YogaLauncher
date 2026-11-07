package `in`.tuwi.yogaphone.ui

import `in`.tuwi.yogaphone.pro.*
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import `in`.tuwi.yogaphone.data.AppModel
import `in`.tuwi.yogaphone.data.db.*
import `in`.tuwi.yogaphone.quiet.*
import `in`.tuwi.yogaphone.ui.theme.*
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
private fun rememberOnResume(read: () -> Boolean): Boolean {
    var value by remember { mutableStateOf(read()) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) value = read() }
        owner.lifecycle.addObserver(observer); onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return value
}

@Composable
fun HomeGreyIndicator(reason: GreyReason?, onSnooze: () -> Unit) {
    if (reason == null) return
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text("Greyscale · ${GreyscalePolicy.label(reason)}", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        YogaTextButton("Snooze 30 min", onSnooze)
    }
}

@Composable
fun VisualInterventionsScreen(prefs: QuietPreferences, focusGreyscale: Boolean, schedules: List<FocusScheduleEntity>,
    repository: QuietRepository, onFocusGreyscale: (Boolean) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val canWrite = rememberOnResume { repository.canWriteSecureSettings() }
    var advanced by remember { mutableStateOf(false) }
    YogaScreen("Visual interventions", onBack) {
        item { YogaNote("Greyscale takes some of the pull out of bright, rewarding screens. Yoga can always turn its own screens grey; the rest of the phone needs Android's own setting.") }
        item { YogaSection("Turn Yoga grey") }
        item { YogaSwitchRow("When I'm past today's limit", prefs.greyOverLimit, { scope.launch { repository.setGreyOverLimit(it) } }) }
        item { ProSwitchRow("During focus", ProFeature.FOCUS, focusGreyscale, onFocusGreyscale) }
        item { YogaSection("During a bedtime schedule") }
        item { YogaRadioRow("Off", prefs.greyBedtimeScheduleId == null, onClick = { scope.launch { repository.setGreyBedtime(null) } }) }
        items(schedules, key = { it.id }) { schedule ->
            val access = LocalProAccess.current
            if (!access.allows(ProFeature.FOCUS)) ProRow(schedule.name, ProFeature.FOCUS, onClick = access.open)
            else YogaRadioRow(schedule.name, prefs.greyBedtimeScheduleId == schedule.id, onClick = { scope.launch { repository.setGreyBedtime(schedule.id) } })
        }
        if (schedules.isEmpty()) item { YogaNote("Add a schedule (for example Bedtime 22:00–07:00) in Focus and timebox settings.") }
        item { YogaSection("The whole phone") }
        item {
            YogaNote("Android can show everything in greyscale. Open its settings and turn it on:")
            YogaNote("Accessibility → Colour and motion (or Visibility enhancements) → Colour correction → Greyscale. Digital Wellbeing's Bedtime mode can also turn the screen grey on a schedule.")
            Row(Modifier.padding(horizontal = ScreenMargin), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                YogaTextButton("Accessibility settings", { runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } })
                context.packageManager.getLaunchIntentForPackage("com.google.android.apps.wellbeing")?.let { wellbeing ->
                    YogaTextButton("Digital Wellbeing", { runCatching { context.startActivity(wellbeing) } })
                }
            }
        }
        item { YogaRow("For advanced users: let Yoga switch it", summary = if (canWrite) "Permission granted" else "Needs a one-time permission from a computer",
            onClick = { advanced = !advanced }) }
        if (advanced) item {
            YogaNote("With this permission Yoga turns on Android's greyscale on the triggers above and restores your previous setting afterwards. Connect the phone to a computer with USB debugging on and run:")
            Text(QuietRepository.ADB_GRANT, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = ScreenMargin, vertical = 8.dp))
            YogaTextButton("Copy command", { context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("adb", QuietRepository.ADB_GRANT)) },
                Modifier.padding(horizontal = TextButtonInset))
            YogaSwitchRow("Use device-wide greyscale", prefs.deviceWideGrey && canWrite, { scope.launch { repository.setDeviceWide(it) } }, enabled = canWrite)
        }
    }
}

@Composable
fun QuietNotificationsScreen(prefs: QuietPreferences, apps: List<AppModel>, rules: List<NotificationRuleEntity>,
    isDistracting: (String) -> Boolean, isEssential: (String) -> Boolean, repository: QuietRepository,
    onOpenDigest: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val granted = rememberOnResume { repository.listenerGranted() }
    val ruleByPackage = rules.associate { it.packageName to it.policy }
    val presets = listOf(9 * 60, 12 * 60 + 30, 18 * 60 + 30, 21 * 60)
    YogaScreen("Quiet notifications", onBack) {
        item { YogaNote("Hold notifications from chosen apps and read them together later. Calls, alarms, ongoing and media notifications are never held. Yoga keeps only each held notification's title and text, on this phone, for at most a day.") }
        item {
            if (!granted) YogaRow("Allow notification access", summary = "Android asks you to allow Yoga to read notifications so it can hold them.",
                trailing = { YogaTextButton("Allow", { runCatching { context.startActivity(repository.listenerSettingsIntent()) } }) })
            else YogaSwitchRow("Quiet notifications", prefs.quietEnabled, { scope.launch { repository.setQuietEnabled(it) } })
        }
        item { YogaTextButton("Open held notifications", onOpenDigest, Modifier.padding(horizontal = TextButtonInset)) }
        item { YogaSection("Digest times") }
        item {
            YogaNote("Held notifications are summarised when focus ends and at:")
            YogaChipRow { presets.forEach { time ->
                val on = time in prefs.digestTimes
                YogaChip("%02d:%02d".format(time / 60, time % 60), on, onClick = {
                    scope.launch { repository.setDigestTimes(if (on) prefs.digestTimes - time else prefs.digestTimes + time) }
                })
            } }
        }
        item { YogaSection("Per app") }
        items(apps.distinctBy { it.appPackage }, key = { "quiet:${it.appPackage}" }) { app ->
            val policy = ruleByPackage[app.appPackage]?.let { runCatching { NotificationPolicy.valueOf(it) }.getOrNull() }
                ?: HoldDecider.defaultPolicy(isDistracting(app.appPackage), isEssential(app.appPackage))
            YogaRow(app.displayLabel)
            YogaChipRow {
                listOf(NotificationPolicy.ALLOW to "Allow", NotificationPolicy.HOLD_DURING_FOCUS to "Hold in focus", NotificationPolicy.ALWAYS_HOLD to "Always hold")
                    .forEach { (value, label) -> YogaChip(label, policy == value, onClick = { scope.launch { repository.setPolicy(app.appPackage, value) } }) }
            }
        }
    }
}

@Composable
fun DigestScreen(held: List<HeldNotificationEntity>, labelOf: (String) -> String, onOpenApp: (String) -> Unit,
    repository: QuietRepository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val groups = Digest.group(held.map { HeldItem(it.key, it.packageName, it.title, it.text, it.postedAt) }, System.currentTimeMillis())
    YogaScreen("Held notifications", onBack) {
        if (groups.isEmpty()) item { YogaNote("Nothing held right now.") }
        else item { YogaRow(Digest.summary(groups), trailing = { YogaTextButton("Clear all", { scope.launch { repository.clearHeld(null) } }, muted = true) }) }
        groups.forEach { group ->
            item(key = "group:${group.packageName}") { YogaSection(labelOf(group.packageName)); Row(Modifier.padding(horizontal = ScreenMargin)) {
                YogaTextButton("Open", { onOpenApp(group.packageName) }); YogaTextButton("Clear", { scope.launch { repository.clearHeld(group.packageName) } }, muted = true)
            } }
            items(group.items, key = { it.key }) { item -> YogaRow(item.title.ifBlank { labelOf(item.packageName) },
                summary = buildString {
                    if (item.text.isNotBlank()) append(item.text).append(" · ")
                    append(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(item.postedAt)))
                }, onClick = { onOpenApp(item.packageName) }) }
        }
    }
}
