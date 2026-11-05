package `in`.tuwi.yogaphone.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import `in`.tuwi.yogaphone.data.AppModel
import `in`.tuwi.yogaphone.data.db.FocusScheduleEntity
import `in`.tuwi.yogaphone.data.db.HeldNotificationEntity
import `in`.tuwi.yogaphone.data.db.NotificationRuleEntity
import `in`.tuwi.yogaphone.quiet.Digest
import `in`.tuwi.yogaphone.quiet.GreyReason
import `in`.tuwi.yogaphone.quiet.GreyscalePolicy
import `in`.tuwi.yogaphone.quiet.HeldItem
import `in`.tuwi.yogaphone.quiet.HoldDecider
import `in`.tuwi.yogaphone.quiet.NotificationPolicy
import `in`.tuwi.yogaphone.quiet.QuietPreferences
import `in`.tuwi.yogaphone.quiet.QuietRepository
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
private fun rememberOnResume(read: () -> Boolean): Boolean {
    var value by remember { mutableStateOf(read()) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) value = read() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return value
}

@Composable
fun HomeGreyIndicator(reason: GreyReason?, onSnooze: () -> Unit) {
    if (reason == null) return
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Greyscale · ${GreyscalePolicy.label(reason)}", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        TextButton(onClick = onSnooze, modifier = Modifier.heightIn(min = 48.dp)) { Text("Snooze 30 min") }
    }
}

@Composable
fun VisualInterventionsScreen(
    prefs: QuietPreferences,
    focusGreyscale: Boolean,
    schedules: List<FocusScheduleEntity>,
    repository: QuietRepository,
    onFocusGreyscale: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val canWrite = rememberOnResume { repository.canWriteSecureSettings() }
    var advanced by remember { mutableStateOf(false) }
    LazyColumn {
        item { SettingsHeader("Visual interventions", onBack) }
        item {
            Text("Greyscale takes some of the pull out of bright, rewarding screens. Yoga can always turn its own screens grey; " +
                "the rest of the phone needs Android's own setting.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
        item { SectionTitle("Turn Yoga grey") }
        item { SwitchListItem("When I'm past today's limit", prefs.greyOverLimit) { scope.launch { repository.setGreyOverLimit(it) } } }
        item { SwitchListItem("During focus", focusGreyscale, onChange = onFocusGreyscale) }
        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("During a bedtime schedule")
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { scope.launch { repository.setGreyBedtime(null) } }) {
                    RadioButton(selected = prefs.greyBedtimeScheduleId == null, onClick = { scope.launch { repository.setGreyBedtime(null) } })
                    Text("Off")
                }
                schedules.forEach { schedule ->
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { scope.launch { repository.setGreyBedtime(schedule.id) } }) {
                        RadioButton(selected = prefs.greyBedtimeScheduleId == schedule.id, onClick = { scope.launch { repository.setGreyBedtime(schedule.id) } })
                        Text(schedule.name)
                    }
                }
                if (schedules.isEmpty()) Text("Add a schedule (for example Bedtime 22:00–07:00) in Focus and timebox settings.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { SectionTitle("The whole phone") }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text("Android can show everything in greyscale. Open its settings and turn it on:")
                Text("Accessibility → Colour and motion (or Visibility enhancements) → Colour correction → Greyscale. " +
                    "Digital Wellbeing's Bedtime mode can also turn the screen grey on a schedule.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } }) {
                        Text("Accessibility settings")
                    }
                    val wellbeing = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.wellbeing")
                    if (wellbeing != null) TextButton(onClick = { runCatching { context.startActivity(wellbeing) } }) { Text("Digital Wellbeing") }
                }
            }
        }
        item {
            ListItem(
                headlineContent = { Text("For advanced users: let Yoga switch it") },
                supportingContent = { Text(if (canWrite) "Permission granted" else "Needs a one-time permission from a computer") },
                modifier = Modifier.clickable { advanced = !advanced },
            )
        }
        if (advanced) item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text("With this permission Yoga turns on Android's greyscale on the triggers above and restores your previous setting afterwards. " +
                    "Connect the phone to a computer with USB debugging on and run:", style = MaterialTheme.typography.bodySmall)
                Text(QuietRepository.ADB_GRANT, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 8.dp))
                TextButton(onClick = {
                    context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("adb", QuietRepository.ADB_GRANT))
                }) { Text("Copy command") }
                SwitchListItem("Use device-wide greyscale", prefs.deviceWideGrey && canWrite, enabled = canWrite) {
                    scope.launch { repository.setDeviceWide(it) }
                }
            }
        }
    }
}

@Composable
private fun SwitchListItem(title: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    ListItem(headlineContent = { Text(title) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange, enabled = enabled) },
        modifier = Modifier.clickable(enabled = enabled) { onChange(!checked) })
}

@Composable
fun QuietNotificationsScreen(
    prefs: QuietPreferences,
    apps: List<AppModel>,
    rules: List<NotificationRuleEntity>,
    isDistracting: (String) -> Boolean,
    isEssential: (String) -> Boolean,
    repository: QuietRepository,
    onOpenDigest: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val granted = rememberOnResume { repository.listenerGranted() }
    val ruleByPackage = rules.associate { it.packageName to it.policy }
    val presets = listOf(9 * 60, 12 * 60 + 30, 18 * 60 + 30, 21 * 60)
    LazyColumn {
        item { SettingsHeader("Quiet notifications", onBack) }
        item {
            Text("Hold notifications from chosen apps and read them together later. Calls, alarms, ongoing and media notifications are never held. " +
                "Yoga keeps only each held notification's title and text, on this phone, for at most a day.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
        item {
            if (!granted) ListItem(
                headlineContent = { Text("Allow notification access") },
                supportingContent = { Text("Android asks you to allow Yoga to read notifications so it can hold them.") },
                trailingContent = { TextButton(onClick = { runCatching { context.startActivity(repository.listenerSettingsIntent()) } }) { Text("Allow") } },
            ) else SwitchListItem("Quiet notifications", prefs.quietEnabled) { scope.launch { repository.setQuietEnabled(it) } }
        }
        item { TextButton(onClick = onOpenDigest, modifier = Modifier.padding(horizontal = 8.dp)) { Text("Open held notifications") } }
        item { SectionTitle("Digest times") }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text("Held notifications are summarised when focus ends and at:", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { time ->
                        val on = time in prefs.digestTimes
                        FilterChip(selected = on, label = { Text("%02d:%02d".format(time / 60, time % 60)) }, onClick = {
                            scope.launch { repository.setDigestTimes(if (on) prefs.digestTimes - time else prefs.digestTimes + time) }
                        })
                    }
                }
            }
        }
        item { SectionTitle("Per app") }
        items(apps.distinctBy { it.appPackage }, key = { "quiet:" + it.appPackage }) { app ->
            val policy = ruleByPackage[app.appPackage]?.let { runCatching { NotificationPolicy.valueOf(it) }.getOrNull() }
                ?: HoldDecider.defaultPolicy(isDistracting(app.appPackage), isEssential(app.appPackage))
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                Text(app.displayLabel)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(NotificationPolicy.ALLOW to "Allow", NotificationPolicy.HOLD_DURING_FOCUS to "Hold in focus", NotificationPolicy.ALWAYS_HOLD to "Always hold")
                        .forEach { (value, label) ->
                            FilterChip(selected = policy == value, onClick = { scope.launch { repository.setPolicy(app.appPackage, value) } }, label = { Text(label) })
                        }
                }
            }
        }
    }
}

@Composable
fun DigestScreen(held: List<HeldNotificationEntity>, labelOf: (String) -> String, onOpenApp: (String) -> Unit, repository: QuietRepository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val groups = Digest.group(held.map { HeldItem(it.key, it.packageName, it.title, it.text, it.postedAt) }, System.currentTimeMillis())
    LazyColumn {
        item { SettingsHeader("Held notifications", onBack) }
        if (groups.isEmpty()) item {
            Text("Nothing held right now.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
        } else item {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(Digest.summary(groups), modifier = Modifier.weight(1f))
                TextButton(onClick = { scope.launch { repository.clearHeld(null) } }) { Text("Clear all") }
            }
        }
        groups.forEach { group ->
            item(key = "group:" + group.packageName) {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(labelOf(group.packageName), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onOpenApp(group.packageName) }) { Text("Open") }
                    TextButton(onClick = { scope.launch { repository.clearHeld(group.packageName) } }) { Text("Clear") }
                }
            }
            items(group.items, key = { it.key }) { item ->
                Column(Modifier.fillMaxWidth().clickable { onOpenApp(item.packageName) }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(item.title.ifBlank { labelOf(item.packageName) }, style = MaterialTheme.typography.bodyLarge)
                    if (item.text.isNotBlank()) Text(item.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(item.postedAt)), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
