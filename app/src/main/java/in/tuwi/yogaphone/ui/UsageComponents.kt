package `in`.tuwi.yogaphone.ui

import `in`.tuwi.yogaphone.pro.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.data.AppModel
import `in`.tuwi.yogaphone.ui.theme.*
import `in`.tuwi.yogaphone.usage.*
import kotlin.math.roundToInt

const val TIME_VALUE_EXPLANATION = "This is an illustrative value of your time — not money spent or income lost."

@Composable
fun UsageToggle(title: String, checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) =
    YogaSwitchRow(title, checked, onChange, enabled = enabled)

@Composable
fun AllowanceEditor(title: String, initial: Int?, onDismiss: () -> Unit, onSave: (Int?) -> Unit) {
    var enabled by remember { mutableStateOf(initial != null) }
    var minutes by remember { mutableIntStateOf(initial ?: 90) }
    YogaDialog(title, "Save", { onSave(if (enabled) minutes else null); onDismiss() }, onDismiss, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            UsageToggle("Allowance on", enabled, { enabled = it })
            Text(if (enabled) "$minutes minutes a day" else "Off", style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "lnum, tnum"))
            YogaSlider(minutes.toFloat(), { minutes = (it / 15).roundToInt() * 15 }, valueRange = 15f..480f, steps = 30,
                enabled = enabled, modifier = Modifier.semantics { contentDescription = "Daily allowance in minutes" })
            Text("15–480 minutes in 15-minute steps. Suggested: 90 minutes.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    })
}

/** The per-app sheet from Usage screens; it is the same sheet as Your apps and the long-press "App settings…". */
@Composable
fun UsageAppSheet(app: AppModel, state: UsageUiState, viewModel: UsageViewModel, onDismiss: () -> Unit) =
    AppSettingsSheet(app, state, viewModel, onDismiss)

@Composable
fun TimeValueDetailsSheet(state: UsageUiState, onDismiss: () -> Unit, onSnooze: () -> Unit) {
    val snapshot = state.snapshot
    val total = snapshot.evaluation.overall
    YogaSheet(onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Value of time", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = ScreenMargin))
            YogaNote(TIME_VALUE_EXPLANATION)
            YogaNote("Period: ${snapshot.date}, local midnight to now (${java.time.ZoneId.systemDefault().id}).")
            when {
                !snapshot.access -> UsageAccessCard()
                !snapshot.available -> YogaNote(snapshot.error ?: "Usage data is unavailable.")
                total == null -> YogaNote("Set a daily allowance to calculate time past your limit.")
                else -> {
                    YogaCard { YogaStat("${total.excessMinutes} min", "Past your limit") }
                    YogaSection("Contributing apps")
                    val rows = snapshot.allowanceToday().filter { it.eligible && it.foregroundMinutes > 0 }.sortedByDescending { it.foregroundMinutes }
                    if (rows.isEmpty()) YogaNote("No eligible foreground usage recorded yet.")
                    rows.forEach { row -> YogaRow(state.apps.firstOrNull { it.appPackage == row.packageName }?.displayLabel
                        ?: snapshot.appLabels[row.packageName] ?: "App", trailing = { Text("${row.foregroundMinutes} min") }) }
                    YogaNote("The allowance applies to their combined time. The value uses only time past the allowance.")
                }
            }
            YogaTextButton("Snooze notification for today", { onSnooze(); onDismiss() }, Modifier.padding(horizontal = TextButtonInset), muted = true)
        }
    }
}
