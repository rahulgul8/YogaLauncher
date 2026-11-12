package `in`.tuwi.yogaphone.ui

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import `in`.tuwi.yogaphone.BuildConfig
import `in`.tuwi.yogaphone.MainActivity
import `in`.tuwi.yogaphone.YogaApplication
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import `in`.tuwi.yogaphone.guard.YogaGuardService
import `in`.tuwi.yogaphone.insights.*
import `in`.tuwi.yogaphone.privacy.*
import `in`.tuwi.yogaphone.ui.theme.*
import `in`.tuwi.yogaphone.usage.UsageAccess
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import `in`.tuwi.yogaphone.review.buildFeedbackEmailBody

private data class PermissionRow(val name: String, val purpose: String, val granted: Boolean, val intent: Intent?)

@Composable
fun PrivacyScreen(prefs: InsightsPreferences, insights: InsightsRepository, backup: BackupManager, onReset: () -> Unit,
    onGuard: () -> Unit, onBack: () -> Unit) {
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
        owner.lifecycle.addObserver(observer); onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) scope.launch { message = runCatching { "Saved: " + backup.export(uri, includeHistory) }.getOrElse { it.message ?: "Couldn't save the backup." } }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) scope.launch { runCatching { backup.read(uri) }.onSuccess { importing = it }
            .onFailure { message = (it as? BackupException)?.message ?: "Couldn't read that file." } }
    }
    fun later(label: String, action: suspend () -> Unit) {
        pending?.second?.cancel()
        val job = scope.launch { delay(5_000); action(); pending = null; refresh++ }
        pending = label to job
    }
    val permissions = run {
        refresh
        val nm = context.getSystemService(NotificationManager::class.java)
        listOf(
            PermissionRow("Usage Access", "Measures app time for allowances and insights", UsageAccess.granted(context), Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)),
            PermissionRow("Notifications", "Reminders, timebox endings, digests", Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)),
            PermissionRow("Notification access", "Quiet notifications (holds chosen apps' notifications)", NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName), Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)),
            PermissionRow("Do Not Disturb access", "Priority-only mode during focus", nm.isNotificationPolicyAccessGranted, Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)),
            PermissionRow("Exact alarms", "Timeboxes end on the minute", Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms(),
                if (Build.VERSION.SDK_INT >= 31) Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")) else null),
            PermissionRow("Secure settings (adb)", "Device-wide greyscale", context.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED, null),
        )
    }
    Box(Modifier.fillMaxSize()) {
        YogaScreen("Privacy and data", onBack) {
            item { YogaNote("Everything Yoga measures stays on this phone. Nothing is sent to a server.") }
            item { YogaSection("What is stored") }
            counts.forEach { (label, count) -> item(key = "count:$label") { YogaRow(label, trailing = {
                Text(count.toString(), style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "lnum, tnum"))
            }) } }
            item { YogaSection("Keep history for") }
            item { YogaChipRow { listOf(30, 90, 365).forEach { days -> YogaChip(if (days == 365) "1 year" else "$days days", prefs.retentionDays == days,
                onClick = { scope.launch { insights.setRetention(days); refresh++ } }) } } }
            item { YogaSwitchRow("Measure usage", prefs.usageTracking, { scope.launch { insights.setUsageTracking(it) } }, summary = "Turn off to stop reading app usage entirely") }
            item { YogaSwitchRow("Insights", prefs.insightsEnabled, { scope.launch { insights.setInsightsEnabled(it) } }, summary = "Weekly summaries and insight cards") }
            item { YogaSection("Permissions") }
            if (`in`.tuwi.yogaphone.BuildConfig.GUARD) item {
                refresh
                val allowed = YogaGuardService.isEnabled(context)
                YogaRow("Yoga guard", summary = "Optional live time value and Shorts, Reels and Status recognition · ${if (allowed) "allowed" else "not allowed"}",
                    trailing = { YogaTextButton("Manage", onGuard) })
            }
            permissions.forEach { row -> item(key = "perm:${row.name}") { YogaRow(row.name,
                summary = "${row.purpose} · ${if (row.granted) "allowed" else "not allowed"}", trailing = row.intent?.let { intent -> ({
                    YogaTextButton(if (row.granted) "Change" else "Allow", { runCatching { context.startActivity(intent) } })
                }) }) } }
            item { YogaSection("Backup") }
            item { YogaCheckRow("Include usage and session history", includeHistory) { includeHistory = !includeHistory } }
            item { Row(Modifier.padding(horizontal = ScreenMargin), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                YogaSecondaryButton("Export to a file", { exporter.launch("yoga-launcher-backup.json") })
                YogaSecondaryButton("Import", { importer.launch(arrayOf("application/json", "text/plain", "*/*")) })
            } }
            item { YogaNote("Android's automatic cloud backup is off for Yoga, so your usage history never leaves the phone unless you export it.") }
            item { YogaSection("Delete") }
            item { YogaTextButton("Delete usage history", { later("Usage history deleted") { backup.deleteUsageHistory() } }, Modifier.padding(horizontal = TextButtonInset), muted = true) }
            item { YogaTextButton("Reset everything", { later("Everything reset") { backup.resetEverything(); onReset() } }, Modifier.padding(horizontal = TextButtonInset), muted = true) }
        }
        pending?.let { (label, job) -> Snackbar(action = { YogaTextButton("Undo", { job.cancel(); pending = null }) },
            modifier = Modifier.align(Alignment.BottomCenter).padding(ScreenMargin)) { Text("$label in 5 seconds") } }
        message?.let { Snackbar(action = { YogaTextButton("OK", { message = null }) },
            modifier = Modifier.align(Alignment.BottomCenter).padding(ScreenMargin)) { Text(it) } }
    }
    importing?.let { payload -> YogaDialog("Import this backup?", "Import", {
        importing = null
        scope.launch { message = runCatching { backup.apply(payload); "Backup imported." }.getOrElse { "Import failed: ${it.message}" }; refresh++ }
    }, { importing = null }, text = { Text("It contains ${BackupCodec.summary(payload)}. Matching settings and lists on this phone will be replaced.") }) }
}

@Composable
fun HelpScreen(onReplaySetup: () -> Unit, onSuggestions: () -> Unit = {}, onBack: () -> Unit) {
    val context = LocalContext.current
    val yoga = context.applicationContext as YogaApplication
    val updateController = yoga.updateController
    val updateNote by updateController.checkNote.collectAsState()
    val scope = rememberCoroutineScope()
    var notice by remember { mutableStateOf<String?>(null) }
    val faqs = listOf(
        "Why do some apps open without the pause?" to "Yoga can only pause apps you open from Yoga itself. Apps opened from a notification, another app or the recents screen skip it.",
        "Why are my numbers different from Digital Wellbeing?" to "Yoga counts the time apps are in the foreground, from Android's usage events, and leaves out essential apps. Digital Wellbeing may count screen time differently or include apps Yoga excludes.",
        "How do I stop using Yoga as my home screen?" to "Settings → Advanced → Leave Yoga Launcher opens Android's home-app setting, where you can pick another launcher.",
        "Reminders or timebox endings arrive late" to "Some phones pause background work to save battery. Letting Yoga run without battery restrictions makes reminders more reliable.",
    )
    Box(Modifier.fillMaxSize()) {
    YogaScreen("Help", onBack) {
        item { YogaRow("Suggestions", summary = "Ideas based on your phone", onClick = onSuggestions) }
        item { YogaRow("Replay setup", summary = "Walk through the first-run steps again", onClick = onReplaySetup) }
        item { YogaRow("Send feedback", summary = "Open an email you can review before sending", onClick = {
            scope.launch {
                val answers = runCatching { yoga.reviewController.store.read().feedbackAnswers }.getOrDefault(emptyList())
                val body = buildFeedbackEmailBody(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE,
                    Build.VERSION.RELEASE, "${Build.MANUFACTURER} ${Build.MODEL}".trim(), answers)
                // Gmail ignores the address in a mailto: link that carries query parameters, so the address,
                // subject and body also travel as extras (found on the moto g31: "To" came up empty).
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
                    .putExtra(Intent.EXTRA_EMAIL, arrayOf("tuwi.developer@gmail.com"))
                    .putExtra(Intent.EXTRA_SUBJECT, "Yoga Phone feedback")
                    .putExtra(Intent.EXTRA_TEXT, body)
                if (intent.resolveActivity(context.packageManager) != null) runCatching { context.startActivity(intent) }
                    .onFailure { notice = "No email app found" }
                else notice = "No email app found"
            }
        }) }
        item {
            YogaRow(
                "Check for updates",
                summary = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})" +
                    (updateNote?.let { "\n${it.text}" } ?: ""),
                trailing = {
                    val activity = context as? MainActivity
                    if (activity != null && updateNote == `in`.tuwi.yogaphone.update.UpdateCheckNote.UPDATE_READY)
                        YogaTextButton("Update", { updateController.startFlexible(activity) })
                    else if (activity != null) YogaTextButton("Check now", { updateController.forceCheck(activity) })
                },
            )
        }
        item { YogaRow("Battery optimisation", summary = "Open Android's battery optimisation list", onClick = {
            runCatching { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
        }) }
        item { YogaSection("Questions") }
        faqs.forEach { (q, a) -> item(key = q) { YogaRow(q, summary = a) } }
    }
        notice?.let { message -> Snackbar(action = { YogaTextButton("OK", { notice = null }) },
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(ScreenMargin)) { Text(message) } }
    }
}
