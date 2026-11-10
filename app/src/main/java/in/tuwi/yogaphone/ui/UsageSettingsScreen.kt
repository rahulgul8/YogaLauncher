package `in`.tuwi.yogaphone.ui

import `in`.tuwi.yogaphone.pro.*
import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.data.AppModel
import `in`.tuwi.yogaphone.data.db.CategoryEntity
import `in`.tuwi.yogaphone.ui.theme.*
import `in`.tuwi.yogaphone.usage.*

@Composable
fun UsageSettingsScreen(state: UsageUiState, viewModel: UsageViewModel, onBack: () -> Unit, onDashboard: () -> Unit, onValue: () -> Unit,
    categories: List<CategoryEntity> = emptyList()) {
    var editCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var pickCategory by remember { mutableStateOf(false) }
    var editTotal by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<AppModel?>(null) }
    var query by remember { mutableStateOf("") }
    var denied by remember { mutableStateOf(false) }
    var clearHistory by remember { mutableStateOf(false) }
    val access = LocalProAccess.current
    fun canEdit(id: String) = access.allows(ProFeature.EXTRA_ALLOWANCES) || state.rules.none { it.enabled } || id in freeQuota(state.rules.filter { it.enabled }.map { it.id }, 1)
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        denied = !granted; viewModel.preferences { it.copy(reminders = granted) }
    }
    val total = state.rules.firstOrNull { it.scope == "TOTAL" }
    YogaScreen("Usage rules", onBack) {
        item { UsageAccessCard() }
        item { YogaTextButton("Open usage dashboard", onDashboard, Modifier.padding(horizontal = TextButtonInset)) }
        item { YogaRow("Daily allowance", summary = total?.let { "${it.dailyAllowanceMinutes} min a day" } ?: "Off · suggested 90 minutes", onClick = { if (canEdit("total")) editTotal = true else access.open() }) }
        item { YogaSection("Allowance counts") }
        items(AllowanceCounts.entries) { option ->
            YogaRadioRow(if (option == AllowanceCounts.DISTRACTING) "Distracting apps" else "All apps except essential",
                state.preferences.counts == option, onClick = { viewModel.preferences { it.copy(counts = option) } })
        }
        item { YogaNote("Essential apps never count towards total or app allowances.") }
        item { YogaSection("Warning threshold") }
        items(listOf(0, 70, 80, 90)) { percent ->
            YogaRadioRow(if (percent == 0) "Off (over-limit reminders only)" else "$percent%",
                state.preferences.warnAtPercent == percent, onClick = { viewModel.threshold(percent) })
        }
        item { ProSwitchRow("Usage reminders", ProFeature.REMINDERS, state.preferences.reminders, { enabled ->
            if (enabled && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else viewModel.preferences { it.copy(reminders = enabled) }
        }) }
        if (denied) item {
            YogaNote("Notifications are off. You can allow them in Android settings or keep using the dashboard.")
            YogaTextButton("Notification settings", { runCatching { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) } }, Modifier.padding(horizontal = TextButtonInset))
        }
        item { YogaNote("Checks run when Yoga resumes and about every 15 minutes. Android may delay background work.") }
        item { YogaSection("Usage on home") }
        item {
            HomeDisplayPicker(
                state.preferences.homeStyle,
                if (state.snapshot.available) homeUsageNumbers(state.snapshot, state.rules, state.snapshot.date) else null,
            ) { style -> viewModel.preferences { it.copy(homeStyle = style) } }
        }
        item { ProRow("Value of time settings", ProFeature.VALUE_OF_TIME, onClick = onValue) }
        item { YogaSection("Per-app limits") }
        if (!access.allows(ProFeature.EXTRA_ALLOWANCES)) item { YogaNote("One allowance stays active on Free. Additional saved allowances resume with Pro.") }
        if (state.rules.none { it.scope == "APP" }) item { YogaNote("No app limits yet. Choose an app below to add one.") }
        items(state.rules.filter { it.scope == "APP" }, key = { it.id }) { rule ->
            val app = state.apps.firstOrNull { it.appPackage == rule.targetPackage }
            YogaRow(app?.displayLabel ?: rule.targetPackage.orEmpty(), summary = "${rule.dailyAllowanceMinutes} min a day${if (app?.isEssential == true) " · excluded (essential)" else ""}",
                enabled = app != null, onClick = { selected = app }, trailing = { YogaTextButton("Remove", { viewModel.saveRule(rule.targetPackage, null) }, muted = true) })
        }
        item { YogaSection("Category limits") }
        items(state.rules.filter { it.scope == "CATEGORY" }, key = { it.id }) { rule ->
            val category = categories.firstOrNull { it.id == rule.targetPackage }
            YogaRow(category?.name ?: rule.targetPackage.orEmpty(), summary = "${rule.dailyAllowanceMinutes} min a day", enabled = category != null,
                onClick = { if (canEdit(rule.id)) editCategory = category else access.open() }, trailing = { YogaTextButton("Remove", { viewModel.saveCategoryRule(rule.targetPackage.orEmpty(), null) }, muted = true) })
        }
        item { if (canEdit("category:new")) YogaTextButton("Add a category limit", { pickCategory = true }, Modifier.padding(horizontal = TextButtonInset))
            else ProRow("Add a category limit", ProFeature.EXTRA_ALLOWANCES, onClick = access.open) }
        item { YogaSection("Apps · classification and limits") }
        item { YogaTextField(query, { query = it }, "Find an app") }
        items(state.apps.distinctBy { it.appPackage }.filter { it.displayLabel.contains(query, true) }, key = { it.appPackage }) { app ->
            YogaRow(app.displayLabel, summary = when { app.isEssential -> "Essential"; app.isDistracting -> "Distracting"; else -> "Other" }, onClick = { selected = app })
        }
        item { YogaTextButton("Delete saved usage history", { clearHistory = true }, Modifier.padding(horizontal = TextButtonInset), muted = true) }
    }
    if (pickCategory) YogaDialog("Limit a category", "Close", { pickCategory = false }, { pickCategory = false }, dismissText = "Cancel", text = {
        LazyColumn(Modifier.heightIn(max = 360.dp)) {
            items(categories, key = { it.id }) { category -> YogaRow(category.name, onClick = { pickCategory = false; editCategory = category }) }
        }
    })
    editCategory?.let { category ->
        val current = state.rules.firstOrNull { it.scope == "CATEGORY" && it.targetPackage == category.id }
        AllowanceEditor("${category.name} a day", current?.dailyAllowanceMinutes, { editCategory = null }) { viewModel.saveCategoryRule(category.id, it) }
    }
    if (editTotal) AllowanceEditor("Daily allowance", total?.dailyAllowanceMinutes, { editTotal = false }) { viewModel.saveRule(null, it) }
    selected?.let { UsageAppSheet(it, state, viewModel) { selected = null } }
    if (clearHistory) YogaDialog("Delete saved history?", "Delete", { viewModel.clearHistory(); clearHistory = false }, { clearHistory = false },
        text = { androidx.compose.material3.Text("Past saved days will be removed. Today and yesterday can be measured again from Android while Usage Access is on. Turn off Usage Access to stop collection.") })
}
