package `in`.tuwi.yogaphone.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import android.telecom.TelecomManager
import android.provider.Telephony
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.MainUiState
import `in`.tuwi.yogaphone.data.AppModel
import `in`.tuwi.yogaphone.data.ThemeMode
import `in`.tuwi.yogaphone.usage.UsageAccess
import `in`.tuwi.yogaphone.usage.UsageUiState

/** What the walkthrough writes; every step goes straight to the existing settings. */
class OnboardingActions(
    val setHomeApps: (List<AppModel>) -> Unit,
    val setAllowance: (Int?) -> Unit,
    val setDistracting: (String, Boolean) -> Unit,
    val setTimeValue: (Boolean, String) -> Unit,
    val setReminders: (Boolean) -> Unit,
    val setMindfulPause: (Boolean) -> Unit,
    val setGreyOverLimit: (Boolean) -> Unit,
    val setTheme: (ThemeMode) -> Unit,
    val finish: () -> Unit,
)

private const val STEPS = 9

@Composable
fun OnboardingScreen(
    ui: MainUiState,
    usage: UsageUiState,
    isDistracting: (String) -> Boolean,
    isEssential: (String) -> Boolean,
    actions: OnboardingActions,
) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    BackHandler(enabled = step > 0) { step-- }
    val next: () -> Unit = { if (step < STEPS - 1) step++ else actions.finish() }

    Column(Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(progress = { (step + 1f) / STEPS }, modifier = Modifier.weight(1f))
            TextButton(onClick = actions.finish) { Text("Skip setup") }
        }
        Column(Modifier.weight(1f).padding(top = 16.dp)) {
            when (step) {
                0 -> StepText("Welcome to Yoga", "A calmer home screen that helps you choose how you use your phone. " +
                    "It shows you what's happening first, and adds gentle friction only where you ask for it.")
                1 -> DefaultHomeStep()
                2 -> HomeAppsStep(ui, usage, isDistracting, actions)
                3 -> {
                    StepText("See your usage", "Yoga reads which apps were in the foreground and for how long. This stays on your phone. " +
                        "You can skip this and turn it on later.")
                    UsageAccessCard()
                    if (UsageAccess.granted(context)) Text("Usage Access is on.", color = MaterialTheme.colorScheme.primary)
                }
                4 -> AllowanceStep(ui, isDistracting, isEssential, actions)
                5 -> TimeValueStep(actions)
                6 -> InterventionsStep(actions)
                7 -> {
                    StepText("Choose a look", "You can change this any time in Settings.")
                    var theme by remember { mutableStateOf(ui.preferences.themeMode) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark", ThemeMode.GREYSCALE to "Greyscale", ThemeMode.FOLLOW_SYSTEM to "System")
                            .forEach { (mode, label) -> FilterChip(theme == mode, { theme = mode; actions.setTheme(mode) }, label = { Text(label) }) }
                    }
                }
                else -> StepText("You're set", "• Swipe up (or tap search) to see all your apps.\n" +
                    "• Long-press an app for options like rename, hide or timebox.\n" +
                    "• Timebox and Focus sit just above the search button.\n" +
                    "• Settings is at the top right; Help can replay this setup.")
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { if (step > 0) step-- }, enabled = step > 0) { Text("Back") }
            Button(onClick = next, modifier = Modifier.heightIn(min = 48.dp)) { Text(if (step == STEPS - 1) "Start" else "Next") }
        }
    }
}

@Composable
private fun StepText(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.headlineSmall)
    Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 12.dp))
}

@Composable
private fun DefaultHomeStep() {
    val context = LocalContext.current
    var isDefault by remember { mutableStateOf(isDefaultHome(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { isDefault = isDefaultHome(context) }
    StepText("Make Yoga your home screen", "Yoga works best as your home app. You can switch back any time from Settings → Leave Yoga Launcher.")
    if (isDefault) Text("Yoga is your home app.", color = MaterialTheme.colorScheme.primary)
    else Button(onClick = { runCatching { launcher.launch(defaultHomeIntent(context)) } }) { Text("Set as home app") }
    Text("Or tap Next to do it later.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun HomeAppsStep(ui: MainUiState, usage: UsageUiState, isDistracting: (String) -> Boolean, actions: OnboardingActions) {
    val context = LocalContext.current
    val apps = ui.drawerApps.distinctBy { it.appPackage }
    val suggested = remember(apps.size) {
        val byPackage = apps.associateBy { it.appPackage }
        val mostUsed = usage.snapshot.week.groupBy { it.packageName }.mapValues { (_, r) -> r.sumOf { it.foregroundMinutes } }
            .entries.sortedByDescending { it.value }.map { it.key }.filter { !isDistracting(it) }
        val essentials = listOfNotNull(
            runCatching { context.getSystemService(TelecomManager::class.java).defaultDialerPackage }.getOrNull(),
            runCatching { Telephony.Sms.getDefaultSmsPackage(context) }.getOrNull(),
            context.packageManager.resolveActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA), 0)?.activityInfo?.packageName,
            "com.google.android.apps.maps",
            context.packageManager.resolveActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://example.com")), 0)?.activityInfo?.packageName,
        )
        val current = ui.favoriteApps.map { it.appPackage }
        (current + mostUsed + essentials).distinct().filter { it in byPackage }.take(6)
    }
    var chosen by remember { mutableStateOf(suggested) }
    StepText("Your home apps", "We picked a few to start. Tap to change; up to 8.")
    LazyColumn(Modifier.heightIn(max = 420.dp)) {
        items((apps.filter { it.appPackage in chosen } + apps.filterNot { it.appPackage in chosen }), key = { it.appPackage }) { app ->
            val checked = app.appPackage in chosen
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(enabled = checked || chosen.size < 8) {
                chosen = if (checked) chosen - app.appPackage else chosen + app.appPackage
                actions.setHomeApps(chosen.mapNotNull { pkg -> apps.firstOrNull { it.appPackage == pkg } })
            }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked, null)
                Text(app.displayLabel, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
    // Apply the suggestion as soon as the step is shown, so "Next" alone keeps it.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (ui.favoriteApps.isEmpty()) actions.setHomeApps(chosen.mapNotNull { pkg -> apps.firstOrNull { it.appPackage == pkg } })
    }
}

@Composable
private fun AllowanceStep(ui: MainUiState, isDistracting: (String) -> Boolean, isEssential: (String) -> Boolean, actions: OnboardingActions) {
    var minutes by remember { mutableIntStateOf(90) }
    var enabled by remember { mutableStateOf(true) }
    val apps = ui.drawerApps.distinctBy { it.appPackage }.filterNot { isEssential(it.appPackage) }
    var distracting by remember { mutableStateOf(apps.filter { isDistracting(it.appPackage) }.map { it.appPackage }.toSet()) }
    androidx.compose.runtime.LaunchedEffect(enabled, minutes) { actions.setAllowance(if (enabled) minutes else null) }
    StepText("A daily allowance", "How much time in distracting apps feels right each day? Essential apps never count.")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(enabled, { enabled = it })
        Text(if (enabled) "  $minutes minutes a day" else "  No allowance for now")
    }
    if (enabled) Slider(minutes.toFloat(), { minutes = (it / 15).toInt() * 15 }, valueRange = 15f..240f, steps = 14)
    Text("These count as distracting — tap to change:", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
    LazyColumn(Modifier.heightIn(max = 300.dp)) {
        items(apps.sortedByDescending { it.appPackage in distracting }, key = { "d:" + it.appPackage }) { app ->
            val checked = app.appPackage in distracting
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable {
                distracting = if (checked) distracting - app.appPackage else distracting + app.appPackage
                actions.setDistracting(app.appPackage, !checked)
            }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked, null)
                Text(app.displayLabel, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun TimeValueStep(actions: OnboardingActions) {
    var on by remember { mutableStateOf(false) }
    var hourly by remember { mutableStateOf("500") }
    StepText("The value of your time (optional)", "If you like, give an hour of your time a value. When you go past your allowance, " +
        "Yoga can show the equivalent — for example \"20 min past your limit ≈ ₹167 of your time\".")
    Text("This is an illustrative value of your time — not money spent or income lost.", style = MaterialTheme.typography.bodySmall)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        Switch(on, { on = it; actions.setTimeValue(it, hourly) })
        Text("  Show the value of time")
    }
    if (on) OutlinedTextField(hourly, { hourly = it.filter { c -> c.isDigit() || c == '.' }.take(8); actions.setTimeValue(true, hourly) },
        label = { Text("Value of one hour") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
}

@Composable
private fun InterventionsStep(actions: OnboardingActions) {
    var reminders by remember { mutableStateOf(true) }
    var pause by remember { mutableStateOf(true) }
    var grey by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        reminders = granted; actions.setReminders(granted)
    }
    StepText("Gentle nudges", "Start soft. You can change any of these later.")
    Toggle("Reminders near and past your allowance", reminders) {
        reminders = it
        if (it && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS) else actions.setReminders(it)
    }
    Toggle("A short pause before distracting apps", pause) { pause = it; actions.setMindfulPause(it) }
    Toggle("Turn Yoga grey when you're past your limit", grey) { grey = it; actions.setGreyOverLimit(it) }
    Spacer(Modifier.heightIn(min = 8.dp))
}

@Composable
private fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { onChange(!checked) }, verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked, onChange)
    }
}
