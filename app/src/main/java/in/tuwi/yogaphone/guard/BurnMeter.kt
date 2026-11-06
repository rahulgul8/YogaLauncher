package `in`.tuwi.yogaphone.guard

import `in`.tuwi.yogaphone.usage.RuleResult
import `in`.tuwi.yogaphone.usage.RuleState
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** Pure calculation; a missing/under-limit TOTAL rule never starts AFTER_LIMIT. */
object BurnMeter {
    fun amount(startCounting: StartCounting, overall: RuleResult?, eligibleMinutes: Long,
        liveSeconds: Long, hourlyValue: BigDecimal): BigDecimal? {
        val minutes = when (startCounting) {
            StartCounting.AFTER_LIMIT -> {
                if (overall == null || overall.state != RuleState.OVER) return null
                overall.excessMinutes
            }
            StartCounting.FROM_FIRST_MINUTE -> eligibleMinutes
        }
        val seconds = BigDecimal.valueOf(minutes.coerceAtLeast(0)).multiply(BigDecimal(60))
            .add(BigDecimal.valueOf(liveSeconds.coerceAtLeast(0)))
        return seconds.multiply(hourlyValue.max(BigDecimal.ZERO)).divide(BigDecimal(3600), 8, RoundingMode.HALF_UP)
    }

    fun fractionDigits(hourlyValue: BigDecimal, currencyCode: String): Int =
        if (hourlyValue < BigDecimal(60)) currency(currencyCode).defaultFractionDigits.coerceAtLeast(0) else 0

    fun format(amount: BigDecimal, hourlyValue: BigDecimal, currencyCode: String,
        locale: Locale = Locale.getDefault()): String = NumberFormat.getCurrencyInstance(locale).apply {
        currency = currency(currencyCode)
        minimumFractionDigits = fractionDigits(hourlyValue, currencyCode)
        maximumFractionDigits = minimumFractionDigits
        roundingMode = RoundingMode.HALF_UP
    }.format(amount)

    private fun currency(code: String): Currency = runCatching { Currency.getInstance(code) }
        .getOrDefault(Currency.getInstance("USD"))
}
