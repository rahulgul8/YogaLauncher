package `in`.tuwi.yogaphone.ui

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment
import `in`.tuwi.yogaphone.ui.theme.YogaCard
import `in`.tuwi.yogaphone.ui.theme.YogaEyebrow
import `in`.tuwi.yogaphone.ui.theme.YogaHeader
import `in`.tuwi.yogaphone.ui.theme.YogaNote
import `in`.tuwi.yogaphone.ui.theme.YogaPrimaryButton
import `in`.tuwi.yogaphone.ui.theme.YogaRow
import `in`.tuwi.yogaphone.ui.theme.YogaScreen
import `in`.tuwi.yogaphone.ui.theme.YogaSecondaryButton
import `in`.tuwi.yogaphone.ui.theme.YogaSection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAppearance: () -> Unit,
    onHiddenApps: () -> Unit,
    onResetNames: () -> Unit,
    onUsage: () -> Unit,
    onUsageRules: () -> Unit,
    onValueOfTime: () -> Unit,
    onGuard: () -> Unit,
    onFocus: () -> Unit,
    onProfiles: () -> Unit,
    onCategories: () -> Unit,
    onVisual: () -> Unit,
    onQuiet: () -> Unit,
    onInsights: () -> Unit,
    onPrivacy: () -> Unit,
    onHelp: () -> Unit,
    onWidgets: () -> Unit,
    onAdvanced: () -> Unit,
) {
    val context = LocalContext.current
    var isDefault by remember { mutableStateOf(isDefaultHome(context)) }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefault = isDefaultHome(context)
    }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) isDefault = isDefaultHome(context)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    YogaScreen("Settings", onBack, eyebrow = "Yoga Launcher") {
        // Only while Yoga isn't the home app. Leaving Yoga lives under Advanced, a few taps away on purpose.
        if (!isDefault) item(key = "home-card") {
            YogaCard(Modifier.padding(top = 12.dp)) {
                YogaEyebrow("Home screen")
                Text("Make Yoga your home screen", style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(top = 8.dp))
                Text("Android asks once.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp))
                YogaPrimaryButton("Make Yoga home", { runCatching { roleLauncher.launch(defaultHomeIntent(context)) } })
            }
        }
        item { YogaSection("Awareness") }
        item { YogaRow("Usage dashboard", summary = "Today and the last 7 days", onClick = onUsage) }
        item { YogaRow("Insights", summary = "Trends, late nights, goals", onClick = onInsights) }
        item { YogaRow("Usage rules", summary = "Allowances, essential apps, reminders", onClick = onUsageRules) }
        item { YogaRow("Value of time", summary = "Your hour, in your currency", onClick = onValueOfTime) }
        if (`in`.tuwi.yogaphone.BuildConfig.GUARD) item { YogaRow("Shorts and live time value", summary = "Yoga guard", onClick = onGuard) }
        item { YogaSection("Focus") }
        item { YogaRow("Focus and timebox", summary = "Mindful pause, schedules, Do Not Disturb", onClick = onFocus) }
        item { YogaRow("Visual interventions", summary = "Greyscale past your limit, in focus, at bedtime", onClick = onVisual) }
        item { YogaRow("Quiet notifications", summary = "Hold chosen apps for a digest", onClick = onQuiet) }
        item { YogaSection("Home") }
        item { YogaRow("Home and appearance", summary = "Theme, type, clock, date, layout", onClick = onAppearance) }
        item { YogaRow("Widgets", summary = "Add, resize, reorder, remove", onClick = onWidgets) }
        item { YogaNote("To change a shortcut in the dock, press and hold it on the home screen.") }
        item { YogaSection("Apps") }
        item { YogaRow("Profiles", summary = "Different homes for work, reading and more", onClick = onProfiles) }
        item { YogaRow("Categories", summary = "Automatic and your own, drawer grouping", onClick = onCategories) }
        item { YogaRow("Hidden apps", summary = "Review and unhide", onClick = onHiddenApps) }
        item { YogaSection("Privacy and help") }
        item { YogaRow("Privacy and data", summary = "What is stored, permissions, backup, delete", onClick = onPrivacy) }
        item { YogaRow("Help", summary = "Replay setup, questions, battery tips", onClick = onHelp) }
        item { YogaRow("Advanced", summary = "Android settings, app names, home screen", onClick = onAdvanced) }
        item(key = "about") {
            Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                BrandColon(28.sp)
                Text("Yoga Launcher", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 14.dp))
                Text("Version ${versionName(context)}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
fun SettingsHeader(title: String, onBack: () -> Unit) = YogaHeader(title, onBack)

@Composable
fun SectionTitle(text: String) = YogaSection(text)

internal fun defaultHomeIntent(context: Context): Intent {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roles = context.getSystemService(RoleManager::class.java)
        if (roles.isRoleAvailable(RoleManager.ROLE_HOME)) return roles.createRequestRoleIntent(RoleManager.ROLE_HOME)
    }
    return Intent(Settings.ACTION_HOME_SETTINGS)
}

internal fun isDefaultHome(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roles = context.getSystemService(RoleManager::class.java)
        if (roles.isRoleAvailable(RoleManager.ROLE_HOME)) return roles.isRoleHeld(RoleManager.ROLE_HOME)
    }
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val resolved = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
    return resolved?.activityInfo?.packageName == context.packageName
}

private fun versionName(context: Context): String = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0)).versionName
    } else {
        @Suppress("DEPRECATION") context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }
}.getOrNull() ?: "—"
