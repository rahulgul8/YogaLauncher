package `in`.tuwi.yogaphone.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.usage.*
import `in`.tuwi.yogaphone.guard.*
import kotlinx.coroutines.launch
import java.util.Currency

@Composable
fun ValueOfTimeSettingsScreen(state: UsageUiState, viewModel: UsageViewModel, guardPrefs: GuardPrefs, onGuard: () -> Unit, onBack: () -> Unit) {
    val p = state.preferences
    val guard by guardPrefs.preferences.collectAsState(initial = GuardPreferences())
    val scope = rememberCoroutineScope()
    var hourly by remember(p.hourlyValue) { mutableStateOf(p.hourlyValue) }
    var denied by remember { mutableStateOf(false) }
    val amount = hourly.toBigDecimalOrNull()
    val valid = amount != null && amount.signum() >= 0 && amount <= java.math.BigDecimal("1000000000")
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        denied = !granted; viewModel.preferences { it.copy(ongoingValue = granted) }
    }
    LazyColumn {
        item { SettingsHeader("Value of time", onBack) }
        item { Text(TIME_VALUE_EXPLANATION, modifier = Modifier.padding(16.dp)) }
        item { UsageToggle("Value of time", p.timeValueEnabled, { enabled -> viewModel.preferences { it.copy(timeValueEnabled = enabled) } }) }
        item {
            OutlinedTextField(hourly, { hourly = it }, label = { Text("Hourly value (${p.currency})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
                isError = !valid, supportingText = { Text(if (valid) "Choose a value that is meaningful to you." else "Enter a number from 0 to 1,000,000,000.") },
                modifier = Modifier.fillMaxWidth().padding(16.dp))
            TextButton(onClick = { viewModel.preferences { it.copy(hourlyValue = amount!!.toPlainString()) } }, enabled = valid,
                modifier = Modifier.padding(horizontal = 16.dp)) { Text("Save hourly value") }
        }
        item { SectionTitle("Currency") }
        items((listOf(p.currency, "INR", "USD", "EUR", "GBP", "JPY", "AUD", "CAD") + defaultCurrency()).distinct()) { code ->
            Row(Modifier.fillMaxWidth().selectable(p.currency == code, role = Role.RadioButton) { viewModel.preferences { it.copy(currency = code) } }
                .padding(horizontal = 16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                RadioButton(p.currency == code, null)
                Text("$code · ${Currency.getInstance(code).symbol}", Modifier.padding(start = 8.dp))
            }
        }
        item { Text("20 min past your limit ≈ ${TimeValueCalculator().format(20, if (valid) amount!! else p.hourlyAmount(), p.currency)} of your time",
            modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium) }
        item { UsageToggle("Show time value on home", p.valueOnHome, { enabled -> viewModel.preferences { it.copy(valueOnHome = enabled) } }) }
        item { UsageToggle("Ongoing silent notification while over", p.ongoingValue, { enabled ->
            if (enabled && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else viewModel.preferences { it.copy(ongoingValue = enabled) }
        }) }
        if (denied) item { Text("Notifications were not allowed. The home indicator and dashboard are still available.", modifier = Modifier.padding(16.dp)) }
        item { Text("The notification can be snoozed for today. A daily allowance is needed to calculate excess time. The home indicator and notification use whole currency units.", modifier = Modifier.padding(16.dp)) }
        item { SectionTitle("Live indicator") }
        item { UsageToggle("Show live indicator over apps", guard.liveIndicator, { value -> scope.launch { guardPrefs.update { it.copy(liveIndicator = value) } } }) }
        item {
            Text("Needs Yoga guard and Value of time. Illustrative, not money lost or spent. You can turn it off any time.", modifier = Modifier.padding(16.dp))
            TextButton(onClick = onGuard, modifier = Modifier.padding(horizontal = 16.dp)) { Text("Shorts and live time value · Yoga guard") }
        }
        item { SectionTitle("Style") }
        item {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GuardStyle.entries.forEach { style ->
                    FilterChip(guard.style == style, { scope.launch { guardPrefs.update { it.copy(style = style) } } },
                        label = { Text(if (style == GuardStyle.BURNING) "Burning" else "Gentle") })
                }
            }
        }
        item { SectionTitle("Start counting") }
        items(StartCounting.entries.toList()) { start ->
            Row(Modifier.fillMaxWidth().selectable(guard.startCounting == start, role = Role.RadioButton) {
                    scope.launch { guardPrefs.update { it.copy(startCounting = start) } } }
                .padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                RadioButton(guard.startCounting == start, null)
                Text(if (start == StartCounting.AFTER_LIMIT) "After my limit" else "From the first minute", Modifier.padding(start = 8.dp))
            }
        }
        item { Text("After my limit needs a total daily allowance. From the first minute counts all eligible app time. The live indicator uses currency minor units for rates below one unit per minute. Usage measuring must be on; snoozing today also hides it.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall) }
    }
}
