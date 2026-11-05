package `in`.tuwi.yogaphone.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.data.AppModel
import `in`.tuwi.yogaphone.usage.*
import kotlin.math.roundToInt

const val TIME_VALUE_EXPLANATION = "This is an illustrative value of your time — not money spent or income lost."

@Composable
fun UsageToggle(title: String, checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    ListItem(headlineContent = { Text(title) }, trailingContent = { Switch(checked, onChange, enabled = enabled, modifier = Modifier.semantics { contentDescription = title }) },
        modifier = Modifier.heightIn(min = 56.dp))
}

@Composable
fun AllowanceEditor(title: String, initial: Int?, onDismiss: () -> Unit, onSave: (Int?) -> Unit) {
    var enabled by remember { mutableStateOf(initial != null) }
    var minutes by remember { mutableIntStateOf(initial ?: 90) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            UsageToggle("Allowance on", enabled, { enabled = it })
            Text(if (enabled) "$minutes minutes a day" else "Off")
            Slider(value = minutes.toFloat(), onValueChange = { minutes = (it / 15).roundToInt() * 15 },
                valueRange = 15f..480f, steps = 30, enabled = enabled, modifier = Modifier.semantics { contentDescription = "Daily allowance in minutes" })
            Text("15–480 minutes, in 15 minute steps. Suggested: 90 minutes.", style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { TextButton(onClick = { onSave(if (enabled) minutes else null); onDismiss() }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageAppSheet(app: AppModel, state: UsageUiState, viewModel: UsageViewModel, onDismiss: () -> Unit) {
    var editLimit by remember { mutableStateOf(false) }
    val current = state.apps.firstOrNull { it.appPackage == app.appPackage } ?: app
    val rule = state.rules.firstOrNull { it.targetPackage == app.appPackage }
    val context = androidx.compose.ui.platform.LocalContext.current
    val mandatory = remember(app.appPackage) { app.appPackage in AppClassifier(context).mandatoryEssential() }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Text(current.displayLabel, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
            UsageToggle("Essential · excluded from allowances", current.isEssential,
                { viewModel.classify(app.appPackage, it, if (it) false else current.isDistracting) }, !mandatory)
            if (mandatory) Text("Phone, SMS, system settings and Yoga are always essential.", modifier = Modifier.padding(16.dp))
            UsageToggle("Distracting", current.isDistracting, { viewModel.classify(app.appPackage, false, it) }, !current.isEssential)
            ListItem(headlineContent = { Text("App allowance") },
                supportingContent = { Text(if (current.isEssential) "Essential apps are never counted" else rule?.let { "${it.dailyAllowanceMinutes} min a day" } ?: "Off") },
                modifier = Modifier.heightIn(min = 64.dp).clickable(enabled = !current.isEssential) { editLimit = true })
            if (rule != null) TextButton(onClick = { viewModel.saveRule(app.appPackage, null) }) { Text("Remove app limit") }
        }
    }
    if (editLimit) AllowanceEditor("${current.displayLabel} allowance", rule?.dailyAllowanceMinutes, { editLimit = false }) {
        viewModel.saveRule(app.appPackage, it)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeValueDetailsSheet(state: UsageUiState, onDismiss: () -> Unit, onSnooze: () -> Unit) {
    val snapshot = state.snapshot
    val total = snapshot.evaluation.overall
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Value of time", style = MaterialTheme.typography.headlineSmall)
            Text(TIME_VALUE_EXPLANATION)
            Text("Period: ${snapshot.date}, local midnight to now (${java.time.ZoneId.systemDefault().id}).")
            when {
                !snapshot.access -> UsageAccessCard()
                !snapshot.available -> Text(snapshot.error ?: "Usage data is unavailable.")
                total == null -> Text("Set a daily allowance to calculate time past your limit.")
                else -> {
                    Text("${total.used} eligible minutes − ${total.allowance} minute allowance = ${total.excessMinutes} minutes past your limit.")
                    Text("${total.excessMinutes} × ${state.preferences.hourlyValue} / 60 ≈ ${TimeValueCalculator().format(total.excessMinutes, state.preferences.hourlyAmount(), state.preferences.currency)}")
                    Text("Contributing apps", style = MaterialTheme.typography.titleMedium)
                    val rows = snapshot.today.filter { it.eligible && it.foregroundMinutes > 0 }.sortedByDescending { it.foregroundMinutes }
                    if (rows.isEmpty()) Text("No eligible foreground usage recorded yet.")
                    rows.forEach { row ->
                        Text("${state.apps.firstOrNull { it.appPackage == row.packageName }?.displayLabel ?: row.packageName}: ${row.foregroundMinutes} min")
                    }
                    Text("The allowance applies to their combined time; the value is calculated only from the excess.", style = MaterialTheme.typography.bodySmall)
                }
            }
            TextButton(onClick = { onSnooze(); onDismiss() }) { Text("Snooze notification for today") }
        }
    }
}

