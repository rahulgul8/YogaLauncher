package `in`.tuwi.yogaphone.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.ui.theme.*
import `in`.tuwi.yogaphone.usage.*

@Composable
fun HomeUsageIndicator(state: UsageUiState, onOpenUsage: () -> Unit, onDetails: () -> Unit) {
    val total = state.rules.firstOrNull { it.scope == "TOTAL" && it.enabled } ?: return
    if (!state.preferences.showHome) return
    if (!state.snapshot.access) {
        val context = androidx.compose.ui.platform.LocalContext.current
        YogaTextButton("Allow Usage Access to see today's usage", { UsageAccess.openSettings(context) }, muted = true)
        return
    }
    if (state.loading || !state.snapshot.available) {
        YogaTextButton(if (state.loading) "Reading today's usage…" else "Usage unavailable · view details", onOpenUsage, muted = true)
        return
    }
    val result = state.snapshot.evaluation.overall ?: return
    val over = result.state == RuleState.OVER
    val value = over && state.preferences.timeValueEnabled && state.preferences.valueOnHome
    Column(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onOpenUsage).padding(vertical = 8.dp)) {
        Text(if (over) "${result.excessMinutes} min past today's limit" else "${result.used} min of ${total.dailyAllowanceMinutes} today",
            color = if (over) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "lnum, tnum"))
        YogaProgress(result.used.toFloat() / result.allowance.coerceAtLeast(1), Modifier.padding(top = 6.dp), over)
    }
    if (value) YogaTextButton("· ${TimeValueCalculator().format(result.excessMinutes, state.preferences.hourlyAmount(), state.preferences.currency)} time value", onDetails, muted = true)
}
