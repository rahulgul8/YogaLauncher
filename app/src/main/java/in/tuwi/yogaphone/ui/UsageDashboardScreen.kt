package `in`.tuwi.yogaphone.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.data.AppModel
import `in`.tuwi.yogaphone.usage.*

@Composable
fun UsageDashboardScreen(state: UsageUiState, viewModel: UsageViewModel, onBack: () -> Unit, onSettings: () -> Unit,
    openDetails: Boolean = false, categoryNames: Map<String, String> = emptyMap()) {
    var weekly by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<AppModel?>(null) }
    var details by remember(openDetails) { mutableStateOf(openDetails) }
    val s = state.snapshot
    val p = state.preferences
    val total = state.rules.firstOrNull { it.scope == "TOTAL" && it.enabled }
    val rows = if (weekly) s.week else s.today
    val byApp = rows.groupBy { it.packageName }.mapValues { (_, values) -> values.sumOf { it.foregroundMinutes } }.entries.sortedByDescending { it.value }
    val dailyEligible = s.week.filter { it.eligible }.groupBy { it.date }.mapValues { (_, values) -> values.sumOf { it.foregroundMinutes } }
    val overDays = if (total == null) 0 else s.knownDays.count { (dailyEligible[it] ?: 0) > total.dailyAllowanceMinutes }
    val excessWeek = if (total == null) 0 else s.knownDays.sumOf { ((dailyEligible[it] ?: 0) - total.dailyAllowanceMinutes).coerceAtLeast(0) }
    LazyColumn {
        item { SettingsHeader("Usage", onBack) }
        item {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!weekly, { weekly = false }, label = { Text("Today") })
                FilterChip(weekly, { weekly = true }, label = { Text("Last 7 days") })
            }
        }
        item { TextButton(onClick = onSettings, modifier = Modifier.padding(horizontal = 16.dp)) { Text("Edit usage rules") } }
        when {
            state.loading -> item { Text("Reading usage…", modifier = Modifier.padding(16.dp)) }
            !s.access -> item { UsageAccessCard() }
            !weekly && !s.available -> item {
                Text(s.error ?: "Usage data is unavailable.", modifier = Modifier.padding(16.dp))
                TextButton(onClick = viewModel::refresh) { Text("Try again") }
            }
            weekly && s.knownDays.isEmpty() -> item { Text("No saved days yet. Usage history will build as Yoga measures your days.", modifier = Modifier.padding(16.dp)) }
            else -> {
                item {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val eligible = rows.filter { it.eligible }.sumOf { it.foregroundMinutes }
                        Text("$eligible eligible minutes", style = MaterialTheme.typography.headlineSmall)
                        Text(if (total == null) "Daily allowance is off" else if (weekly) "${total.dailyAllowanceMinutes} min daily allowance · $overDays over-limit days" else "of ${total.dailyAllowanceMinutes} minutes today")
                        Text("${rows.sumOf { it.foregroundMinutes }} min all app foreground time")
                        if (!weekly && total != null) {
                            LinearProgressIndicator(progress = { (eligible.toFloat() / total.dailyAllowanceMinutes).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                            val result = s.evaluation.overall
                            if (result != null && result.state == RuleState.OVER) Text("${result.excessMinutes} min past today's limit", color = MaterialTheme.colorScheme.error)
                            else if (result?.state == RuleState.NEAR) Text("Approaching today's allowance · ${result.remaining} min remaining")
                        }
                        if (weekly) {
                            Text("${s.date.minusDays(6)} to ${s.date} · ${s.knownDays.size} of 7 days recorded.")
                            Text("Missing days are not treated as zero. Historical eligibility is saved when sampled; comparisons use your current daily allowance.", style = MaterialTheme.typography.bodySmall)
                            if (p.timeValueEnabled && total != null) Text("Weekly time value ≈ ${TimeValueCalculator().format(excessWeek, p.hourlyAmount(), p.currency)}")
                        }
                        if (p.timeValueEnabled) {
                            Text(TIME_VALUE_EXPLANATION, style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = { details = true }) { Text("Time value details for today") }
                        }
                    }
                }
                val byCategory = rows.groupBy { s.categoryOf[it.packageName] }
                    .map { (id, values) -> (categoryNames[id] ?: "Other") to values.sumOf { it.foregroundMinutes } }
                    .filter { it.second > 0 }.sortedByDescending { it.second }
                if (byCategory.isNotEmpty()) item {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("By category", style = MaterialTheme.typography.titleSmall)
                        byCategory.forEach { (name, minutes) -> Text("$name · $minutes min", modifier = Modifier.padding(vertical = 4.dp)) }
                    }
                }
                if (byApp.isEmpty()) item { Text("No foreground app time recorded in this period.", modifier = Modifier.padding(16.dp)) }
                items(byApp, key = { it.key }) { row ->
                    val app = state.apps.firstOrNull { it.appPackage == row.key }
                    val essential = row.key in s.essentialPackages
                    Column(Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(enabled = app != null) { selected = app }.padding(16.dp)) {
                        Text("${app?.displayLabel ?: row.key} · ${row.value} min")
                        Text(when { essential -> "Essential"; app?.isDistracting == true -> "Distracting"; else -> "Other" }, style = MaterialTheme.typography.bodySmall)
                        LinearProgressIndicator(progress = { (row.value.toFloat() / (byApp.firstOrNull()?.value ?: 1).coerceAtLeast(1)).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(2.dp))
                    }
                }
            }
        }
        item { Text("App foreground time is time apps report being in the foreground. It differs from screen-on time: lock screen and Yoga are excluded, and multi-window apps can overlap. Android may delay or omit events; sessions are reconstructed with a 24-hour lookback.",
            modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall) }
    }
    selected?.let { UsageAppSheet(it, state, viewModel) { selected = null } }
    if (details) TimeValueDetailsSheet(state, { details = false }, viewModel::snooze)
}
