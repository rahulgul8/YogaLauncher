package `in`.tuwi.yogaphone.usage

import android.content.Context
import androidx.datastore.preferences.core.*
import `in`.tuwi.yogaphone.data.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale

enum class AllowanceCounts { DISTRACTING, ALL_EXCEPT_ESSENTIAL }
data class UsagePreferences(
    val counts: AllowanceCounts = AllowanceCounts.DISTRACTING,
    val reminders: Boolean = false,
    val showHome: Boolean = true,
    val warnAtPercent: Int = 80,
    val timeValueEnabled: Boolean = false,
    val hourlyValue: String = "500",
    val currency: String = defaultCurrency(),
    val valueOnHome: Boolean = true,
    val ongoingValue: Boolean = false,
) {
    fun hourlyAmount(): BigDecimal = hourlyValue.toBigDecimalOrNull()?.max(BigDecimal.ZERO) ?: BigDecimal.ZERO
}
fun defaultCurrency(): String = runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrDefault("USD")

class UsagePrefs(private val context: Context) {
    private val counts = stringPreferencesKey("usage_counts")
    private val reminders = booleanPreferencesKey("usage_reminders")
    private val home = booleanPreferencesKey("usage_home")
    private val threshold = intPreferencesKey("usage_threshold")
    private val value = booleanPreferencesKey("time_value_enabled")
    private val hourly = stringPreferencesKey("time_value_hourly")
    private val currency = stringPreferencesKey("time_value_currency")
    private val valueHome = booleanPreferencesKey("time_value_home")
    private val ongoing = booleanPreferencesKey("time_value_ongoing")
    private val sent = stringSetPreferencesKey("usage_sent_by_date")
    private val snoozed = stringPreferencesKey("usage_snoozed_date")
    val preferences = context.dataStore.data.map { p -> UsagePreferences(
        counts = runCatching { AllowanceCounts.valueOf(p[counts].orEmpty()) }.getOrDefault(AllowanceCounts.DISTRACTING),
        reminders = p[reminders] ?: false, showHome = p[home] ?: true, warnAtPercent = p[threshold] ?: 80,
        timeValueEnabled = p[value] ?: false, hourlyValue = p[hourly] ?: "500", currency = p[currency] ?: defaultCurrency(),
        valueOnHome = p[valueHome] ?: true, ongoingValue = p[ongoing] ?: false)
    }
    suspend fun update(transform: (UsagePreferences) -> UsagePreferences) {
        // Serialize read/modify/write in DataStore; do not overwrite a concurrent setting change.
        context.dataStore.edit { p ->
            val before = UsagePreferences(
                runCatching { AllowanceCounts.valueOf(p[counts].orEmpty()) }.getOrDefault(AllowanceCounts.DISTRACTING),
                p[reminders] ?: false, p[home] ?: true, p[threshold] ?: 80, p[value] ?: false,
                p[hourly] ?: "500", p[currency] ?: defaultCurrency(), p[valueHome] ?: true, p[ongoing] ?: false)
            val next = transform(before)
            p[counts] = next.counts.name; p[reminders] = next.reminders; p[home] = next.showHome
            p[threshold] = next.warnAtPercent; p[value] = next.timeValueEnabled; p[hourly] = next.hourlyValue
            p[currency] = next.currency; p[valueHome] = next.valueOnHome; p[ongoing] = next.ongoingValue
        }
    }
    suspend fun sent(): Set<String> = context.dataStore.data.first()[sent].orEmpty()
    suspend fun markSent(date: String, ruleId: String, state: RuleState) {
        context.dataStore.edit { it[sent] = ReminderLedger.record(it[sent].orEmpty(), date, ruleId, state) }
    }
    suspend fun snooze(date: String) { context.dataStore.edit { it[snoozed] = date } }
    suspend fun isSnoozed(date: String) = context.dataStore.data.first()[snoozed] == date
}
