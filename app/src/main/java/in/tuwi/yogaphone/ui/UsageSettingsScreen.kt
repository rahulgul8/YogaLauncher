package `in`.tuwi.yogaphone.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.data.AppModel
import `in`.tuwi.yogaphone.usage.*

@Composable
fun UsageSettingsScreen(state: UsageUiState, viewModel: UsageViewModel, onBack: () -> Unit, onDashboard: () -> Unit, onValue: () -> Unit,
    categories: List<`in`.tuwi.yogaphone.data.db.CategoryEntity> = emptyList()) {
    var editCategory by remember { mutableStateOf<`in`.tuwi.yogaphone.data.db.CategoryEntity?>(null) }
    var pickCategory by remember { mutableStateOf(false) }
    var editTotal by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<AppModel?>(null) }
    var query by remember { mutableStateOf("") }
    var denied by remember { mutableStateOf(false) }
    var clearHistory by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        denied = !granted
        viewModel.preferences { it.copy(reminders = granted) }
    }
    val total = state.rules.firstOrNull { it.scope == "TOTAL" }
    LazyColumn {
        item { SettingsHeader("Usage rules", onBack) }
        item { UsageAccessCard() }
        item { TextButton(onClick = onDashboard, modifier = Modifier.padding(horizontal = 16.dp)) { Text("Open usage dashboard") } }
        item {
            ListItem(headlineContent = { Text("Daily allowance") }, supportingContent = { Text(total?.let { "${it.dailyAllowanceMinutes} min a day" } ?: "Off · suggested 90 minutes") },
                modifier = Modifier.heightIn(min = 64.dp).clickable { editTotal = true })
        }
        item { SectionTitle("Allowance counts") }
        items(AllowanceCounts.entries) { option ->
            ListItem(headlineContent = { Text(if (option == AllowanceCounts.DISTRACTING) "Distracting apps" else "All apps except essential") },
                leadingContent = { RadioButton(state.preferences.counts == option, onClick = { viewModel.preferences { it.copy(counts = option) } }) },
                modifier = Modifier.clickable { viewModel.preferences { it.copy(counts = option) } })
        }
        item { Text("Essential apps never count towards total or app allowances.", modifier = Modifier.padding(16.dp)) }
        item { SectionTitle("Warning threshold") }
        items(listOf(0, 70, 80, 90)) { percent ->
            ListItem(headlineContent = { Text(if (percent == 0) "Off (over-limit reminders only)" else "$percent%") },
                leadingContent = { RadioButton(state.preferences.warnAtPercent == percent, { viewModel.threshold(percent) }) },
                modifier = Modifier.clickable { viewModel.threshold(percent) })
        }
        item {
            UsageToggle("Usage reminders", state.preferences.reminders, { enabled ->
                if (enabled && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else viewModel.preferences { it.copy(reminders = enabled) }
            })
        }
        if (denied) item {
            Text("Notifications are off. You can allow them in Android settings or keep using the dashboard.", modifier = Modifier.padding(16.dp))
            TextButton(onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) } }) { Text("Notification settings") }
        }
        item { Text("Checks run when Yoga resumes and about every 15 minutes. Android may delay background work.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall) }
        item { UsageToggle("Show usage on home", state.preferences.showHome, { enabled -> viewModel.preferences { it.copy(showHome = enabled) } }) }
        item { TextButton(onClick = onValue, modifier = Modifier.padding(horizontal = 16.dp)) { Text("Value of time settings") } }
        item { SectionTitle("Per-app limits") }
        if (state.rules.none { it.scope == "APP" }) item { Text("No app limits yet. Choose an app below to add one.", modifier = Modifier.padding(16.dp)) }
        items(state.rules.filter { it.scope == "APP" }, key = { it.id }) { rule ->
            val app = state.apps.firstOrNull { it.appPackage == rule.targetPackage }
            ListItem(headlineContent = { Text(app?.displayLabel ?: rule.targetPackage.orEmpty()) },
                supportingContent = { Text("${rule.dailyAllowanceMinutes} min a day${if (app?.isEssential == true) " · excluded (essential)" else ""}") },
                trailingContent = { TextButton(onClick = { viewModel.saveRule(rule.targetPackage, null) }) { Text("Remove") } },
                modifier = Modifier.clickable(enabled = app != null) { selected = app })
        }
        item { SectionTitle("Category limits") }
        items(state.rules.filter { it.scope == "CATEGORY" }, key = { it.id }) { rule ->
            val category = categories.firstOrNull { it.id == rule.targetPackage }
            ListItem(headlineContent = { Text(category?.name ?: rule.targetPackage.orEmpty()) },
                supportingContent = { Text("${rule.dailyAllowanceMinutes} min a day") },
                trailingContent = { TextButton(onClick = { viewModel.saveCategoryRule(rule.targetPackage.orEmpty(), null) }) { Text("Remove") } },
                modifier = Modifier.clickable(enabled = category != null) { editCategory = category })
        }
        item { TextButton(onClick = { pickCategory = true }, modifier = Modifier.padding(horizontal = 16.dp)) { Text("Add a category limit") } }
        item { SectionTitle("Apps · classification and limits") }
        item { OutlinedTextField(query, { query = it }, label = { Text("Find an app") }, modifier = Modifier.fillMaxWidth().padding(16.dp), singleLine = true) }
        items(state.apps.distinctBy { it.appPackage }.filter { it.displayLabel.contains(query, true) }, key = { it.appPackage }) { app ->
            ListItem(headlineContent = { Text(app.displayLabel) }, supportingContent = { Text(when { app.isEssential -> "Essential"; app.isDistracting -> "Distracting"; else -> "Other" }) },
                modifier = Modifier.heightIn(min = 64.dp).clickable { selected = app })
        }
        item { TextButton(onClick = { clearHistory = true }, modifier = Modifier.padding(16.dp)) { Text("Delete saved usage history") } }
    }
    if (pickCategory) AlertDialog(onDismissRequest = { pickCategory = false }, title = { Text("Limit a category") },
        text = {
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                items(categories, key = { it.id }) { category ->
                    Text(category.name, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .clickable { pickCategory = false; editCategory = category }.padding(vertical = 12.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = { pickCategory = false }) { Text("Close") } })
    editCategory?.let { category ->
        val current = state.rules.firstOrNull { it.scope == "CATEGORY" && it.targetPackage == category.id }
        AllowanceEditor("${category.name} a day", current?.dailyAllowanceMinutes, { editCategory = null }) {
            viewModel.saveCategoryRule(category.id, it)
        }
    }
    if (editTotal) AllowanceEditor("Daily allowance", total?.dailyAllowanceMinutes, { editTotal = false }) { viewModel.saveRule(null, it) }
    selected?.let { UsageAppSheet(it, state, viewModel) { selected = null } }
    if (clearHistory) AlertDialog(onDismissRequest = { clearHistory = false }, title = { Text("Delete saved history?") },
        text = { Text("Past saved days will be removed. Today and yesterday can be measured again from Android while Usage Access is on. Turn off Usage Access to stop collection.") },
        confirmButton = { TextButton(onClick = { viewModel.clearHistory(); clearHistory = false }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { clearHistory = false }) { Text("Cancel") } })
}
