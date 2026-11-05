package `in`.tuwi.yogaphone.ui

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import `in`.tuwi.yogaphone.insights.InsightsPreferences
import `in`.tuwi.yogaphone.insights.InsightsRepository
import `in`.tuwi.yogaphone.privacy.BackupException
import `in`.tuwi.yogaphone.privacy.BackupManager
import `in`.tuwi.yogaphone.privacy.BackupPayload
import `in`.tuwi.yogaphone.privacy.BackupCodec
import `in`.tuwi.yogaphone.usage.UsageAccess
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class PermissionRow(val name: String, val purpose: String, val granted: Boolean, val intent: Intent?)

@Composable
fun PrivacyScreen(
    prefs: InsightsPreferences,
    insights: InsightsRepository,
    backup: BackupManager,
    onReset: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var refresh by remember { mutableIntStateOf(0) }
    var counts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var pending by remember { mutableStateOf<Pair<String, Job>?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var includeHistory by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf<BackupPayload?>(null) }
    LaunchedEffect(refresh) { counts = runCatching { backup.counts() }.getOrDefault(emptyMap()) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh++ }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) scope.launch {
            message = runCatching { "Saved: " + backup.export(uri, includeHistory) }.getOrElse { it.message ?: "Couldn't save the backup." }
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) scope.launch {
            runCatching { backup.read(uri) }.onSuccess { importing = it }
                .onFailure { message = (it as? BackupException)?.message ?: "Couldn't read that file." }
        }
    }

    /** Destructive actions wait five seconds with an undo, instead of asking "are you sure?". */
    fun later(label: String, action: suspend () -> Unit) {
        pending?.second?.cancel()
        val job = scope.launch {
            delay(5_000)
            action()
            pending = null
            refresh++
        }
        pending = label to job
    }

    val permissions = run {
        refresh // re-read on resume
        val nm = context.getSystemService(NotificationManager::class.java)
        listOf(
            PermissionRow("Usage Access", "Measures app time for allowances and insights", UsageAccess.granted(context),
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)),
            PermissionRow("Notifications", "Reminders, timebox endings, digests",
                Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)),
            PermissionRow("Notification access", "Quiet notifications (holds chosen apps' notifications)",
                NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName),
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)),
            PermissionRow("Do Not Disturb access", "Priority-only mode during focus", nm.isNotificationPolicyAccessGranted,
                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)),
            PermissionRow("Exact alarms", "Timeboxes end on the minute",
                Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms(),
                if (Build.VERSION.SDK_INT >= 31) Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")) else null),
            PermissionRow("Secure settings (adb)", "Device-wide greyscale",
                context.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED, null),
        )
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn {
            item { SettingsHeader("Privacy and data", onBack) }
            item {
                Text("Everything Yoga measures stays on this phone. Nothing is sent to a server.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            item { SectionTitle("What is stored") }
            counts.forEach { (label, count) ->
                item(key = "count:$label") { ListItem(headlineContent = { Text(label) }, trailingContent = { Text("$count") }) }
            }
            item { SectionTitle("Keep history for") }
            item {
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30, 90, 365).forEach { days ->
                        FilterChip(prefs.retentionDays == days, { scope.launch { insights.setRetention(days); refresh++ } },
                            label = { Text(if (days == 365) "1 year" else "$days days") })
                    }
                }
            }
            item {
                ToggleListItem("Measure usage", "Turn off to stop reading app usage entirely", prefs.usageTracking) {
                    scope.launch { insights.setUsageTracking(it) }
                }
            }
            item { ToggleListItem("Insights", "Weekly summaries and insight cards", prefs.insightsEnabled) { scope.launch { insights.setInsightsEnabled(it) } } }
            item { SectionTitle("Permissions") }
            permissions.forEach { row ->
                item(key = "perm:${row.name}") {
                    ListItem(
                        headlineContent = { Text(row.name) },
                        supportingContent = { Text(row.purpose + if (row.granted) " · allowed" else " · not allowed") },
                        trailingContent = {
                            row.intent?.let { intent ->
                                TextButton(onClick = { runCatching { context.startActivity(intent) } }) { Text(if (row.granted) "Change" else "Allow") }
                            }
                        },
                    )
                }
            }
            item { SectionTitle("Backup") }
            item {
                Row(Modifier.fillMaxWidth().clickable { includeHistory = !includeHistory }.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(includeHistory, { includeHistory = it })
                    Text("Include usage and session history")
                }
            }
            item {
                Row(Modifier.padding(horizontal = 8.dp)) {
                    TextButton(onClick = { exporter.launch("yoga-launcher-backup.json") }) { Text("Export to a file") }
                    TextButton(onClick = { importer.launch(arrayOf("application/json", "text/plain", "*/*")) }) { Text("Import") }
                }
            }
            item {
                Text("Android's automatic cloud backup is off for Yoga, so your usage history never leaves the phone unless you export it.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
            }
            item { SectionTitle("Delete") }
            item {
                TextButton(onClick = { later("Usage history deleted") { backup.deleteUsageHistory() } }, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text("Delete usage history")
                }
            }
            item {
                TextButton(onClick = { later("Everything reset") { backup.resetEverything(); onReset() } }, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text("Reset everything", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        pending?.let { (label, job) ->
            Snackbar(
                action = { TextButton(onClick = { job.cancel(); pending = null }) { Text("Undo") } },
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            ) { Text("$label in 5 seconds") }
        }
        message?.let {
            Snackbar(
                action = { TextButton(onClick = { message = null }) { Text("OK") } },
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            ) { Text(it) }
        }
    }

    importing?.let { payload ->
        AlertDialog(
            onDismissRequest = { importing = null },
            title = { Text("Import this backup?") },
            text = { Text("It contains ${BackupCodec.summary(payload)}. Matching settings and lists on this phone will be replaced.") },
            confirmButton = {
                TextButton(onClick = {
                    importing = null
                    scope.launch { message = runCatching { backup.apply(payload); "Backup imported." }.getOrElse { "Import failed: ${it.message}" }; refresh++ }
                }) { Text("Import") }
            },
            dismissButton = { TextButton(onClick = { importing = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ToggleListItem(title: String, summary: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(headlineContent = { Text(title) }, supportingContent = { Text(summary) },
        trailingContent = { Switch(checked, onChange) }, modifier = Modifier.clickable { onChange(!checked) })
}

@Composable
fun HelpScreen(onReplaySetup: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val faqs = listOf(
        "Why do some apps open without the pause?" to
            "Yoga can only pause apps you open from Yoga itself. Apps opened from a notification, another app or the recents screen skip it.",
        "Why are my numbers different from Digital Wellbeing?" to
            "Yoga counts the time apps are in the foreground, from Android's usage events, and leaves out essential apps. " +
            "Digital Wellbeing may count screen time differently or include apps Yoga excludes.",
        "How do I stop using Yoga as my home screen?" to
            "Settings → Leave Yoga Launcher opens Android's home-app setting, where you can pick another launcher.",
        "Reminders or timebox endings arrive late" to
            "Some phones pause background work to save battery. Letting Yoga run without battery restrictions makes reminders more reliable.",
    )
    LazyColumn {
        item { SettingsHeader("Help", onBack) }
        item { ListItem(headlineContent = { Text("Replay setup") }, supportingContent = { Text("Walk through the first-run steps again") },
            modifier = Modifier.clickable(onClick = onReplaySetup)) }
        item {
            ListItem(headlineContent = { Text("Battery optimisation") },
                supportingContent = { Text("Open Android's battery optimisation list") },
                modifier = Modifier.clickable { runCatching { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) } })
        }
        item { SectionTitle("Questions") }
        faqs.forEach { (q, a) ->
            item(key = q) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(q, style = MaterialTheme.typography.titleSmall)
                    Text(a, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
