package `in`.tuwi.yogaphone.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.usage.*

@Composable
fun HomeUsageIndicator(state: UsageUiState, onOpenUsage: () -> Unit, onDetails: () -> Unit) {
    val total = state.rules.firstOrNull { it.scope == "TOTAL" && it.enabled } ?: return
    if (!state.preferences.showHome) return
    if (!state.snapshot.access) { UsageAccessCard(); return }
    if (state.loading || !state.snapshot.available) {
        TextButton(onClick = onOpenUsage) { Text(if (state.loading) "Reading today's usage…" else "Usage unavailable · view details") }
        return
    }
    val result = state.snapshot.evaluation.overall ?: return
    val over = result.state == RuleState.OVER
    val value = over && state.preferences.timeValueEnabled && state.preferences.valueOnHome
    Column(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onOpenUsage).padding(vertical = 8.dp)) {
        Text(if (over) "${result.excessMinutes} min past today's limit" else "${result.used} min of ${total.dailyAllowanceMinutes} today",
            color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium)
        LinearProgressIndicator(progress = { (result.used.toFloat() / result.allowance).coerceIn(0f, 1f) },
            color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(2.dp))
    }
    if (value) TextButton(onClick = onDetails, contentPadding = PaddingValues(0.dp)) {
        Text("· ${TimeValueCalculator().format(result.excessMinutes, state.preferences.hourlyAmount(), state.preferences.currency)} time value")
    }
}
