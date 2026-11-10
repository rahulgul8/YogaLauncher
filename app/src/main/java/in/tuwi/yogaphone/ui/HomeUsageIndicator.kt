package `in`.tuwi.yogaphone.ui

import `in`.tuwi.yogaphone.data.db.UsageRuleEntity
import `in`.tuwi.yogaphone.pro.*
import `in`.tuwi.yogaphone.ui.theme.*
import `in`.tuwi.yogaphone.usage.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.LocalDate

data class HomeUsageNumbers(
    val todayMinutes: Int,
    val allowanceMinutes: Int?,
    val over: Boolean,
    val excessMinutes: Int,
    val week: List<Int?>,
)

fun homeUsageNumbers(snapshot: UsageSnapshot, rules: List<UsageRuleEntity>, today: LocalDate): HomeUsageNumbers {
    val allowance = rules.firstOrNull { it.scope == "TOTAL" && it.enabled }
    val eligibleToday = snapshot.today.filter { it.eligible && it.packageName !in snapshot.excludedPackages }
        .sumOf { it.foregroundMinutes }.toInt()
    val todayMinutes = if (allowance != null) snapshot.evaluation.overall?.used?.toInt() ?: eligibleToday else eligibleToday
    val allowanceMinutes = allowance?.dailyAllowanceMinutes
    val week = (6L downTo 0L).map { offset ->
        val date = today.minusDays(offset).toString()
        if (date !in snapshot.knownDays && snapshot.week.none { it.date == date }) null
        else snapshot.week.filter { it.date == date && it.eligible && it.packageName !in snapshot.excludedPackages }
            .sumOf { it.foregroundMinutes }.toInt()
    }
    val excess = allowanceMinutes?.let { (todayMinutes - it).coerceAtLeast(0) } ?: 0
    return HomeUsageNumbers(todayMinutes, allowanceMinutes, excess > 0, excess, week)
}

internal fun usageMinutesLabel(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} m"
}

internal fun HomeUsageNumbers.accessibilityLabel(): String = buildString {
    append(usageMinutesLabel(todayMinutes)).append(" used today")
    allowanceMinutes?.let { append(" of ").append(usageMinutesLabel(it)) }
}

@Composable
fun HomeUsageDisplay(state: UsageUiState, onOpenUsage: () -> Unit, onDetails: () -> Unit) {
    if (state.preferences.homeStyle == HomeDisplayStyle.OFF) return
    if (!state.snapshot.access) {
        val context = androidx.compose.ui.platform.LocalContext.current
        YogaTextButton("Allow Usage Access to see today's usage", { UsageAccess.openSettings(context) }, muted = true)
        return
    }
    if (state.loading || !state.snapshot.available) {
        YogaTextButton(if (state.loading) "Reading today's usage…" else "Usage unavailable · view details", onOpenUsage, muted = true)
        return
    }
    val numbers = homeUsageNumbers(state.snapshot, state.rules, state.snapshot.date)
    val value = LocalProAccess.current.allows(ProFeature.VALUE_OF_TIME) && numbers.over &&
        state.preferences.timeValueEnabled && state.preferences.valueOnHome
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onOpenUsage).padding(vertical = 8.dp)) {
            HomeUsageStyle(state.preferences.homeStyle, numbers)
        }
        if (value) YogaTextButton("· ${TimeValueCalculator().format(numbers.excessMinutes.toLong(), state.preferences.hourlyAmount(), state.preferences.currency)} time value", onDetails, muted = true)
    }
}

@Composable
fun HomeUsageIndicator(state: UsageUiState, onOpenUsage: () -> Unit, onDetails: () -> Unit) =
    HomeUsageDisplay(state, onOpenUsage, onDetails)

@Composable
internal fun HomeUsageStyle(style: HomeDisplayStyle, numbers: HomeUsageNumbers) {
    when (style) {
        HomeDisplayStyle.LINE -> HomeUsageLine(numbers)
        HomeDisplayStyle.NUMBER -> HomeUsageNumber(numbers)
        HomeDisplayStyle.RING -> HomeUsageRing(numbers)
        HomeDisplayStyle.WEEK -> HomeUsageWeek(numbers)
        HomeDisplayStyle.OFF -> Unit
    }
}

@Composable
fun HomeUsageLine(numbers: HomeUsageNumbers) {
    Column(Modifier.fillMaxWidth().semantics { contentDescription = numbers.accessibilityLabel() }) {
        Text(
            if (numbers.over) "${numbers.excessMinutes} min past today's limit"
            else numbers.allowanceMinutes?.let { "${numbers.todayMinutes} min of $it today" } ?: "${usageMinutesLabel(numbers.todayMinutes)} today",
            color = if (numbers.over) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "lnum, tnum"),
        )
        numbers.allowanceMinutes?.let {
            YogaProgress(numbers.todayMinutes.toFloat() / it.coerceAtLeast(1), Modifier.padding(top = 6.dp), numbers.over)
        }
    }
}

@Composable
fun HomeUsageNumber(numbers: HomeUsageNumbers) {
    Column(Modifier.semantics { contentDescription = numbers.accessibilityLabel() }) {
        Text(usageMinutesLabel(numbers.todayMinutes), style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "lnum, tnum"),
            color = if (numbers.over) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant)
        numbers.allowanceMinutes?.let {
            Text("of ${usageMinutesLabel(it)}", style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "lnum, tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun HomeUsageRing(numbers: HomeUsageNumbers) {
    val track = MaterialTheme.colorScheme.outlineVariant
    val fill = if (numbers.over) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    Box(Modifier.size(56.dp).semantics { contentDescription = numbers.accessibilityLabel() }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 2.dp.toPx()
            val inset = stroke / 2
            drawArc(track, -90f, 360f, false, Offset(inset, inset), Size(size.width - stroke, size.height - stroke), style = Stroke(stroke))
            numbers.allowanceMinutes?.let {
                drawArc(fill, -90f, 360f * (numbers.todayMinutes.toFloat() / it.coerceAtLeast(1)).coerceIn(0f, 1f), false,
                    Offset(inset, inset), Size(size.width - stroke, size.height - stroke), style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        Text(usageMinutesLabel(numbers.todayMinutes), style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "lnum, tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun HomeUsageWeek(numbers: HomeUsageNumbers) {
    val maximum = (numbers.week.filterNotNull() + listOfNotNull(numbers.allowanceMinutes)).maxOrNull()?.coerceAtLeast(1) ?: 1
    Column(Modifier.semantics { contentDescription = numbers.accessibilityLabel() }) {
        Box(Modifier.width(78.dp).height(40.dp)) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                numbers.week.forEachIndexed { index, minutes ->
                    Box(Modifier.width(6.dp).fillMaxHeight().clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant), contentAlignment = Alignment.BottomCenter) {
                        if (minutes != null && minutes > 0) Box(Modifier.fillMaxWidth().fillMaxHeight((minutes.toFloat() / maximum).coerceIn(0f, 1f))
                            .background(if (index == 6) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)))
                    }
                }
            }
            numbers.allowanceMinutes?.let {
                val y = 40.dp * (1f - (it.toFloat() / maximum).coerceIn(0f, 1f))
                Box(Modifier.fillMaxWidth().offset(y = y).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
            }
        }
        Text("${usageMinutesLabel(numbers.todayMinutes)} today", style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "lnum, tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
    }
}
