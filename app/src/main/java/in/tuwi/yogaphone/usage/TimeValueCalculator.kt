package `in`.tuwi.yogaphone.usage

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

class TimeValueCalculator {
    fun equivalent(excessMinutes: Long, hourlyValue: BigDecimal): BigDecimal =
        hourlyValue.max(BigDecimal.ZERO).multiply(BigDecimal.valueOf(excessMinutes.coerceAtLeast(0)))
            .divide(BigDecimal(60), 0, RoundingMode.HALF_UP)
    fun format(excessMinutes: Long, hourlyValue: BigDecimal, currencyCode: String, locale: Locale = Locale.getDefault()): String =
        NumberFormat.getCurrencyInstance(locale).apply {
            currency = runCatching { Currency.getInstance(currencyCode) }.getOrDefault(Currency.getInstance("USD"))
            minimumFractionDigits = 0
            maximumFractionDigits = 0
        }.format(equivalent(excessMinutes, hourlyValue))
}
