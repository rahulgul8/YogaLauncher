package `in`.tuwi.yogaphone.ui

import `in`.tuwi.yogaphone.pro.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.data.AppModel
import `in`.tuwi.yogaphone.ui.theme.*
import `in`.tuwi.yogaphone.usage.*

private fun minutesLabel(minutes: Long): String = if (minutes >= 60) "${minutes / 60} h ${minutes % 60} m" else "$minutes min"

@Composable
fun UsageDashboardScreen(state: UsageUiState, viewModel: UsageViewModel, onBack: () -> Unit, onSettings: () -> Unit,
    openDetails: Boolean = false, categoryNames: Map<String, String> = emptyMap(), blockedToday: Int = 0,
    resistedToday: Int = 0,
    onWeeklyReviewLeave: (daysWithin: Int, knownDays: Int, minutes: Long) -> Unit = { _, _, _ -> }) {
    var weekly by remember { mutableStateOf(false) }
    var weeklyOpened by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<AppModel?>(null) }
    var details by remember(openDetails) { mutableStateOf(openDetails) }
    val access = LocalProAccess.current
    val s = state.snapshot
    val p = state.preferences
    val allowance = state.rules.firstOrNull { it.scope == "TOTAL" && it.enabled }
    val rows = if (weekly) s.week else s.today
    val eligible = rows.filter { it.eligible }.sumOf { it.foregroundMinutes }
    val total = rows.sumOf { it.foregroundMinutes }
    // The headline counts only allowance apps, so say which, and give the all-apps total beside it.
    val scope = if (p.counts == AllowanceCounts.DISTRACTING) "on distracting apps" else "on apps except essentials"
    val byApp = rows.groupBy { it.packageName }.mapValues { (_, values) -> values.sumOf { it.foregroundMinutes } }.entries.sortedByDescending { it.value }
    val maximum = byApp.firstOrNull()?.value?.coerceAtLeast(1) ?: 1
    val excessWeek = allowance?.let { rule -> s.knownDays.sumOf { day ->
        (s.week.filter { it.date == day && it.eligible }.sumOf { it.foregroundMinutes } - rule.dailyAllowanceMinutes).coerceAtLeast(0)
    } } ?: 0
    val daysWithin = allowance?.let { rule -> s.knownDays.count { day ->
        s.week.filter { it.date == day && it.eligible }.sumOf { it.foregroundMinutes } <= rule.dailyAllowanceMinutes
    } } ?: 0
    val weeklyFacts by rememberUpdatedState(Triple(daysWithin, s.knownDays.size, s.week.filter { it.eligible }.sumOf { it.foregroundMinutes }))
    val weeklyExit by rememberUpdatedState(onWeeklyReviewLeave)
    DisposableEffect(Unit) { onDispose { if (weeklyOpened) weeklyFacts.let { weeklyExit(it.first, it.second, it.third) } } }

    YogaScreen("Usage", onBack) {
        item(key = "feature-introduction") { UsageIntroduction() }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = ScreenMargin, vertical = 12.dp)) {
                YogaEyebrow(if (weekly) "Last 7 days" else "Today")
                Text(minutesLabel(eligible), style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "lnum, tnum"),
                    color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(top = 8.dp))
                val muted = MaterialTheme.colorScheme.onSurfaceVariant
                val body = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "lnum, tnum")
                when {
                    allowance == null -> Text("$scope · daily allowance is off", style = body, color = muted)
                    weekly -> {
                        // A week's total is never compared against a daily allowance: count the days that went over instead.
                        val overDays = s.knownDays.count { day ->
                            s.week.filter { it.date == day && it.eligible }.sumOf { it.foregroundMinutes } > allowance.dailyAllowanceMinutes
                        }
                        Text("$scope · ${minutesLabel(allowance.dailyAllowanceMinutes.toLong())} daily allowance · $overDays over-limit ${if (overDays == 1) "day" else "days"}",
                            style = body, color = muted)
                    }
                    else -> {
                        Text("$scope · of ${minutesLabel(allowance.dailyAllowanceMinutes.toLong())} allowance", style = body, color = muted)
                        YogaProgress(eligible.toFloat() / allowance.dailyAllowanceMinutes.coerceAtLeast(1),
                            Modifier.padding(top = 16.dp), overLimit = eligible > allowance.dailyAllowanceMinutes)
                        val result = s.evaluation.overall
                        if (result?.state == RuleState.OVER) Text("${minutesLabel(result.excessMinutes)} past today's limit",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "lnum, tnum"),
                            color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(top = 10.dp))
                        else if (result?.state == RuleState.NEAR) Text("${minutesLabel(result.remaining)} left today",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "lnum, tnum"), color = muted,
                            modifier = Modifier.padding(top = 10.dp))
                    }
                }
                if (s.access && total > 0) Text("${minutesLabel(total)} in all apps", style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "lnum, tnum"),
                    color = muted, modifier = Modifier.padding(top = 10.dp))
                if (!weekly) s.unlocksToday?.let {
                    Text("$it ${if (it == 1) "unlock" else "unlocks"} today", style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "lnum, tnum"),
                        color = muted, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
        item { YogaChipRow { YogaChip("Today", !weekly, onClick = { weekly = false }); YogaChip("Last 7 days", weekly, onClick = { weekly = true; weeklyOpened = true }) } }
        item { YogaTextButton("Edit usage rules", onSettings, Modifier.padding(horizontal = TextButtonInset)) }
        if (!weekly && access.allows(ProFeature.LEARNING_VIDEOS)) item {
            YogaRow("Learning time today", trailing = { Text(minutesLabel(s.learningSecondsToday / 60),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) })
        }
        if (weekly && access.allows(ProFeature.LEARNING_VIDEOS)) item {
            YogaRow("Learning time", summary = "Last 7 days", trailing = { Text(minutesLabel(s.learningSecondsWeek / 60),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) })
        }
        if (blockedToday > 0) item { YogaNote("Shorts and Reels blocked $blockedToday ${if (blockedToday == 1) "time" else "times"} today.") }
        if (resistedToday > 0) item { YogaNote("Resisted the pause $resistedToday times today.") }
        when {
            state.loading -> item { YogaNote("Reading usage…") }
            !s.access -> item { UsageAccessCard() }
            !weekly && !s.available -> item { YogaNote(s.error ?: "Usage data is unavailable."); YogaTextButton("Try again", viewModel::refresh, Modifier.padding(horizontal = TextButtonInset)) }
            weekly && s.knownDays.isEmpty() -> item { YogaNote("No saved days yet. Your history will build as Yoga measures your days.") }
            else -> {
                if (access.allows(ProFeature.VALUE_OF_TIME) && p.timeValueEnabled && allowance != null) item {
                    val minutes = if (weekly) excessWeek else s.evaluation.overall?.excessMinutes ?: 0
                    Text("Time value · ${TimeValueCalculator().format(minutes, p.hourlyAmount(), p.currency)}",
                        style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "lnum, tnum"), color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(horizontal = ScreenMargin, vertical = 18.dp))
                    YogaNote("Counts only time past your daily allowance. $TIME_VALUE_EXPLANATION")
                    YogaTextButton("How this is calculated", { details = true }, Modifier.padding(horizontal = TextButtonInset), muted = true)
                }
                val categories = rows.groupBy { s.categoryOf[it.packageName] }
                    .map { (id, values) -> (categoryNames[id] ?: "Other") to values.sumOf { it.foregroundMinutes } }
                    .filter { it.second > 0 }.sortedByDescending { it.second }
                if (categories.isNotEmpty()) {
                    item { YogaSection("Categories") }
                    items(categories, key = { it.first }) { (name, minutes) -> YogaRow(name,
                        trailing = { Text(minutesLabel(minutes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }) }
                }
                item { YogaSection("Apps") }
                if (byApp.isEmpty()) item { YogaNote("No foreground app time was recorded in this period.") }
                items(byApp, key = { it.key }) { row ->
                    val app = state.apps.firstOrNull { it.appPackage == row.key }
                    Column(Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(enabled = app != null) { selected = app }
                        .padding(horizontal = ScreenMargin, vertical = 12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f, fill = false).padding(end = 12.dp)) {
                                Text(app?.displayLabel ?: s.appLabels[row.key] ?: "App", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                                Text(when { row.key in s.essentialPackages -> "Essential"; app?.isDistracting == true -> "Distracting"; else -> "Other" },
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(minutesLabel(row.value), style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "lnum, tnum"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        YogaProgress(row.value.toFloat() / maximum, Modifier.padding(top = 10.dp))
                    }
                }
                if (weekly) item { YogaNote("${s.knownDays.size} of 7 days recorded. Missing days are not treated as zero. Historical eligibility is saved when sampled; comparisons use your current daily allowance.") }
            }
        }
        item { YogaNote("App foreground time differs from screen-on time. Lock screen, Yoga and home apps are excluded; multi-window apps can overlap. Android may delay or omit events.") }
    }
    selected?.let { UsageAppSheet(it, state, viewModel) { selected = null } }
    if (details && access.allows(ProFeature.VALUE_OF_TIME)) TimeValueDetailsSheet(state, { details = false }, viewModel::snooze)
}
