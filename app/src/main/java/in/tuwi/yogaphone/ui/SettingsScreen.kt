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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
    onFocus: () -> Unit,
    onProfiles: () -> Unit,
    onCategories: () -> Unit,
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

    LazyColumn {
        item { SettingsHeader("Settings", onBack) }
        item { SectionTitle("Launcher") }
        item {
            ListItem(
                headlineContent = { Text(if (isDefault) "Yoga is your home app" else "Set as default home app") },
                supportingContent = { Text(if (isDefault) "Active" else "Choose Yoga Launcher on the Android screen") },
                trailingContent = {
                    if (!isDefault) Button(onClick = { roleLauncher.launch(defaultHomeIntent(context)) }) { Text("Set") }
                },
            )
        }
        item {
            SettingsRow("Leave Yoga Launcher", "Choose another home app in Android settings") {
                context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            }
        }
        item {
            SettingsRow("Android system settings", "Open device settings") {
                context.startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        item { SectionTitle("Usage awareness") }
        item { SettingsRow("Usage dashboard", "Today and last 7 days", onUsage) }
        item { SettingsRow("Usage rules", "Allowances, essential apps and reminders", onUsageRules) }
        item { SettingsRow("Value of time", "Optional illustrative hourly value", onValueOfTime) }
        item { SectionTitle("Focus") }
        item { SettingsRow("Focus and timebox", "Mindful pause, focus schedules, Do Not Disturb", onFocus) }
        item { SectionTitle("Appearance") }
        item { SettingsRow("Home and appearance", "Theme, type, clock, date and layout", onAppearance) }
        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        item { SectionTitle("Apps") }
        item { SettingsRow("Profiles", "Different homes for work, reading and more", onProfiles) }
        item { SettingsRow("Categories", "Automatic categories, your own, drawer grouping", onCategories) }
        item { SettingsRow("Hidden apps", "Review and unhide apps", onHiddenApps) }
        item { SettingsRow("Reset custom names", "Restore every app's installed name", onResetNames) }
        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        item { SectionTitle("About") }
        item { ListItem(headlineContent = { Text("Yoga Launcher") }, supportingContent = { Text("Version ${versionName(context)}") }) }
    }
}

@Composable
fun SettingsHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(12.dp))
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp))
}

@Composable
private fun SettingsRow(title: String, summary: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(onClick = onClick),
    )
}

private fun defaultHomeIntent(context: Context): Intent {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roles = context.getSystemService(RoleManager::class.java)
        if (roles.isRoleAvailable(RoleManager.ROLE_HOME)) return roles.createRequestRoleIntent(RoleManager.ROLE_HOME)
    }
    return Intent(Settings.ACTION_HOME_SETTINGS)
}

private fun isDefaultHome(context: Context): Boolean {
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

