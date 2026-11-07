package `in`.tuwi.yogaphone.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import `in`.tuwi.yogaphone.guard.*
import `in`.tuwi.yogaphone.ui.theme.*
import `in`.tuwi.yogaphone.usage.*
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
    YogaScreen("Value of time", onBack) {
        item { YogaNote(TIME_VALUE_EXPLANATION) }
        item { YogaSwitchRow("Value of time", p.timeValueEnabled, { enabled -> viewModel.preferences { it.copy(timeValueEnabled = enabled) } }) }
        item {
            YogaTextField(hourly, { hourly = it }, "Hourly value (${p.currency})",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            YogaNote(if (valid) "Choose a value that is meaningful to you." else "Enter a number from 0 to 1,000,000,000.")
            YogaTextButton("Save hourly value", { viewModel.preferences { it.copy(hourlyValue = amount!!.toPlainString()) } },
                Modifier.padding(horizontal = TextButtonInset), enabled = valid)
        }
        item { YogaSection("Currency") }
        items((listOf(p.currency, "INR", "USD", "EUR", "GBP", "JPY", "AUD", "CAD") + defaultCurrency()).distinct()) { code ->
            YogaRadioRow("$code · ${Currency.getInstance(code).symbol}", p.currency == code,
                onClick = { viewModel.preferences { it.copy(currency = code) } })
        }
        item { YogaCard { YogaStat(TimeValueCalculator().format(20, if (valid) amount!! else p.hourlyAmount(), p.currency), "20 min past your limit") } }
        item { YogaSwitchRow("Show time value on home", p.valueOnHome, { enabled -> viewModel.preferences { it.copy(valueOnHome = enabled) } }) }
        item { YogaSwitchRow("Ongoing silent notification while over", p.ongoingValue, { enabled ->
            if (enabled && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else viewModel.preferences { it.copy(ongoingValue = enabled) }
        }) }
        if (denied) item { YogaNote("Notifications were not allowed. The home indicator and dashboard are still available.") }
        item { YogaNote("The notification can be snoozed for today. A daily allowance is needed to calculate excess time. The home indicator and notification use whole currency units.") }
        if (`in`.tuwi.yogaphone.BuildConfig.GUARD) {
            item { YogaSection("Live indicator") }
            item { YogaSwitchRow("Show live indicator over apps", guard.liveIndicator, { value -> scope.launch { guardPrefs.update { it.copy(liveIndicator = value) } } }) }
            item {
                YogaNote("Needs Yoga guard and Value of time. Illustrative, not money lost or spent. You can turn it off any time.")
                YogaTextButton("Shorts and live time value · Yoga guard", onGuard, Modifier.padding(horizontal = TextButtonInset))
            }
            item { YogaSection("Style") }
            item { YogaChipRow {
                GuardStyle.entries.forEach { style -> YogaChip(if (style == GuardStyle.BURNING) "Burning" else "Gentle", guard.style == style,
                    onClick = { scope.launch { guardPrefs.update { it.copy(style = style) } } }) }
            } }
            item { YogaSection("Start counting") }
            items(listOf(StartCounting.FROM_FIRST_MINUTE, StartCounting.AFTER_LIMIT)) { start ->
                YogaRadioRow(if (start == StartCounting.AFTER_LIMIT) "After my limit" else "From the first minute", guard.startCounting == start,
                    onClick = { scope.launch { guardPrefs.update { it.copy(startCounting = start) } } })
            }
            item { YogaNote("After my limit needs a total daily allowance. From the first minute counts all eligible app time. The live indicator uses currency minor units for rates below one unit per minute. Usage measuring must be on; snoozing today also hides it.") }
        }
    }
}
