package `in`.tuwi.yogaphone.ui

import `in`.tuwi.yogaphone.pro.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.data.AppModel
import `in`.tuwi.yogaphone.data.db.UsageAggregateEntity
import `in`.tuwi.yogaphone.insights.GoalDone
import `in`.tuwi.yogaphone.ui.theme.*
import `in`.tuwi.yogaphone.usage.*
import java.time.LocalDate

private fun minutesLabel(minutes: Long): String = if (minutes >= 60) "${minutes / 60} h ${minutes % 60} m" else "$minutes min"

/**
 * Usage details (L-F4). Day view: one day with an hourly chart, stepping back through the week. Week view: one bar
 * per day; tapping a bar narrows everything below to that day. Both list apps, finished goals and unlocks.
 */
@Composable
fun UsageDashboardScreen(state: UsageUiState, viewModel: UsageViewModel, onBack: () -> Unit, onSettings: () -> Unit,
    openDetails: Boolean = false, categoryNames: Map<String, String> = emptyMap(), blockedToday: Int = 0,
    resistedToday: Int = 0, startWeekly: Boolean = false,
    onWeeklyReviewLeave: (daysWithin: Int, knownDays: Int, minutes: Long) -> Unit = { _, _, _ -> }) {
    val s = state.snapshot
    val today = s.date
    var weekly by remember { mutableStateOf(startWeekly) }
    var weeklyOpened by remember { mutableStateOf(startWeekly) }
    var day by remember(today) { mutableStateOf(today) }
    var weekPick by remember(today) { mutableStateOf<LocalDate?>(null) }
    var hourPick by remember(day) { mutableStateOf<Int?>(null) }
    var selected by remember { mutableStateOf<AppModel?>(null) }
    var details by remember(openDetails) { mutableStateOf(openDetails) }
    val access = LocalProAccess.current
    val p = state.preferences
    val allowance = state.rules.firstOrNull { it.scope == "TOTAL" && it.enabled }
    val weekDays = remember(today) { UsageDetails.weekDays(today) }
    val dayDetails by produceState(emptyMap<LocalDate, UsageDayDetails>(), s) { value = viewModel.weekDetails(today) }

    // The day everything below describes; null means the whole week.
    val focusDay: LocalDate? = if (weekly) weekPick else day
    fun rawFor(d: LocalDate): List<UsageAggregateEntity> = if (d == today) s.today else s.week.filter { it.date == d.toString() }
    fun countedFor(d: LocalDate): List<UsageAggregateEntity> =
        if (d == today) s.allowanceToday() else s.allowanceWeek().filter { it.date == d.toString() }
    val rows = focusDay?.let(::rawFor) ?: s.week
    val countedRows = focusDay?.let(::countedFor) ?: s.allowanceWeek()
    val eligible = countedRows.filter { it.eligible }.sumOf { it.foregroundMinutes }
    val total = rows.sumOf { it.foregroundMinutes }
    val recorded = focusDay == null || focusDay == today || focusDay.toString() in s.knownDays || rows.isNotEmpty()
    // The headline counts only allowance apps, so say which, and give the all-apps total beside it.
    val scope = if (p.counts == AllowanceCounts.DISTRACTING) "on distracting apps" else "on apps except essentials"
    val byApp = UsageDetails.appMinutes(rows)
    val maximum = byApp.firstOrNull()?.second?.coerceAtLeast(1) ?: 1
    val dailyEligible = UsageDetails.dailyEligible(s.allowanceWeek(), weekDays, s.knownDays)
    val excessWeek = allowance?.let { rule -> dailyEligible.filterNotNull().sumOf { (it - rule.dailyAllowanceMinutes).coerceAtLeast(0) } } ?: 0
    val daysWithin = allowance?.let { rule -> s.knownDays.count { known ->
        s.allowanceWeek().filter { it.date == known && it.eligible }.sumOf { it.foregroundMinutes } <= rule.dailyAllowanceMinutes
    } } ?: 0
    val goals: List<Pair<LocalDate, GoalDone>> = (focusDay?.let { listOf(it) } ?: weekDays)
        .flatMap { d -> dayDetails[d]?.goals.orEmpty().map { d to it } }
    val unlocks: Int? = when {
        focusDay == today -> s.unlocksToday ?: dayDetails[today]?.unlocks
        focusDay != null -> dayDetails[focusDay]?.unlocks
        else -> weekDays.mapNotNull { dayDetails[it]?.unlocks }.takeIf { it.isNotEmpty() }?.sum()
    }
    val weeklyFacts by rememberUpdatedState(Triple(daysWithin, s.knownDays.size, s.allowanceWeek().filter { it.eligible }.sumOf { it.foregroundMinutes }))
    val weeklyExit by rememberUpdatedState(onWeeklyReviewLeave)
    DisposableEffect(Unit) { onDispose { if (weeklyOpened) weeklyFacts.let { weeklyExit(it.first, it.second, it.third) } } }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val numbers = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "lnum, tnum")

    YogaScreen("Usage", onBack) {
        item(key = "feature-introduction") { UsageIntroduction() }
        item(key = "range") {
            YogaChipRow {
                YogaChip("Day", !weekly, onClick = { weekly = false })
                YogaChip("Last 7 days", weekly, onClick = { weekly = true; weeklyOpened = true })
            }
        }
        item(key = "picker") {
            if (!weekly) DayStepper(UsageDetails.dayTitle(day, today),
                UsageDetails.canStepBack(day, today), UsageDetails.canStepForward(day, today),
                onBack = { day = UsageDetails.step(day, -1, today) }, onForward = { day = UsageDetails.step(day, 1, today) })
            else Row(Modifier.fillMaxWidth().padding(horizontal = ScreenMargin).heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(weekPick?.let { UsageDetails.dayTitle(it, today) } ?: "Last 7 days", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                if (weekPick != null) YogaTextButton("Whole week", { weekPick = null }, muted = true)
            }
        }
        item(key = "headline") {
            Column(Modifier.fillMaxWidth().padding(horizontal = ScreenMargin, vertical = 8.dp)) {
                Text(if (recorded) minutesLabel(eligible) else "—", style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "lnum, tnum"),
                    color = MaterialTheme.colorScheme.onBackground)
                val body = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "lnum, tnum")
                when {
                    !recorded -> Text("This day wasn't recorded", style = body, color = muted)
                    allowance == null -> Text("$scope · daily allowance is off", style = body, color = muted)
                    focusDay == null -> {
                        // A week's total is never compared against a daily allowance: count the days that went over instead.
                        val overDays = dailyEligible.count { it != null && it > allowance.dailyAllowanceMinutes }
                        Text("$scope · ${minutesLabel(allowance.dailyAllowanceMinutes.toLong())} daily allowance · $overDays over-limit ${if (overDays == 1) "day" else "days"}",
                            style = body, color = muted)
                    }
                    else -> {
                        Text("$scope · of ${minutesLabel(allowance.dailyAllowanceMinutes.toLong())} allowance", style = body, color = muted)
                        YogaProgress(eligible.toFloat() / allowance.dailyAllowanceMinutes.coerceAtLeast(1),
                            Modifier.padding(top = 16.dp), overLimit = eligible > allowance.dailyAllowanceMinutes)
                        val over = eligible - allowance.dailyAllowanceMinutes
                        val result = s.evaluation.overall
                        when {
                            focusDay == today && result?.state == RuleState.OVER -> Text("${minutesLabel(result.excessMinutes)} past today's limit",
                                style = numbers, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(top = 10.dp))
                            focusDay == today && result?.state == RuleState.NEAR -> Text("${minutesLabel(result.remaining)} left today",
                                style = numbers, color = muted, modifier = Modifier.padding(top = 10.dp))
                            focusDay != today && over > 0 -> Text("${minutesLabel(over)} past the limit",
                                style = numbers, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(top = 10.dp))
                            focusDay != today -> Text("Within the limit", style = numbers, color = muted, modifier = Modifier.padding(top = 10.dp))
                        }
                    }
                }
                if (s.access && recorded && total > 0) Text("${minutesLabel(total)} in all apps", style = numbers,
                    color = muted, modifier = Modifier.padding(top = 10.dp))
                unlocks?.let {
                    Text("$it ${if (it == 1) "unlock" else "unlocks"}", style = numbers, color = muted, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
        if (s.access) item(key = "chart") {
            if (!weekly) {
                val hours = dayDetails[day]?.hours ?: List(24) { 0L }
                UsageBarChart(hours, List(24) { if (it % 6 == 0) "%02d".format(it) else "" }, hourPick, null,
                    "Minutes $scope each hour", { hourPick = if (hourPick == it) null else it }, Modifier.padding(top = 12.dp))
                Text(hourPick?.let { "${UsageDetails.hourRange(it)} · ${minutesLabel(hours[it])} $scope" } ?: "Tap a bar to see that hour",
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "lnum, tnum"), color = muted, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenMargin, vertical = 8.dp))
            } else {
                val picked = weekPick?.let { weekDays.indexOf(it) }?.takeIf { it >= 0 }
                UsageBarChart(dailyEligible, weekDays.map(UsageDetails::dayInitial), picked,
                    allowance?.dailyAllowanceMinutes?.toLong(), "Minutes $scope each day of the last week",
                    { index -> weekPick = weekDays[index].takeIf { it != weekPick } }, Modifier.padding(top = 12.dp))
                Text(if (allowance != null) "Dashed line: your daily allowance. Tap a day to see it." else "Tap a day to see it.",
                    style = MaterialTheme.typography.bodySmall, color = muted, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenMargin, vertical = 8.dp))
            }
        }
        item(key = "rules") { YogaTextButton("Edit usage rules", onSettings, Modifier.padding(horizontal = TextButtonInset)) }
        if (focusDay == today) {
            item(key = "urgent") { YogaNote("Urgent opens today: ${state.urgentAccess.usedOn(today.toString())} of ${UrgentAccessPolicy.MAX_PER_DAY}") }
            if (access.allows(ProFeature.LEARNING_VIDEOS)) item(key = "learning") {
                YogaRow("Learning time today", trailing = { Text(minutesLabel(s.learningSecondsToday / 60),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) })
            }
            if (blockedToday > 0) item(key = "blocked") { YogaNote("Shorts, Reels and Status blocked $blockedToday ${if (blockedToday == 1) "time" else "times"} today.") }
            if (resistedToday > 0) item(key = "resisted") { YogaNote("Resisted the pause $resistedToday times today.") }
        }
        if (focusDay == null && access.allows(ProFeature.LEARNING_VIDEOS)) item(key = "learning-week") {
            YogaRow("Learning time", summary = "Last 7 days", trailing = { Text(minutesLabel(s.learningSecondsWeek / 60),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) })
        }
        if (goals.isNotEmpty()) {
            item(key = "goals-title") { YogaSection("Goals done") }
            items(goals.withIndex().toList(), key = { "goal:${it.index}" }) { (_, entry) ->
                val (d, goal) = entry
                YogaRow(goal.title, summary = buildString {
                    if (focusDay == null) append(UsageDetails.dayTitle(d, today)).append(" · ")
                    append(if (goal.kind == GoalDone.Kind.FOCUS) "Focus · " else "Timebox · ")
                    append(UsageDetails.goalLine(goal))
                })
            }
        }
        when {
            state.loading -> item { YogaNote("Reading usage…") }
            !s.access -> item { UsageAccessCard() }
            focusDay == today && !s.available -> item { YogaNote(s.error ?: "Usage data is unavailable."); YogaTextButton("Try again", viewModel::refresh, Modifier.padding(horizontal = TextButtonInset)) }
            focusDay == null && s.knownDays.isEmpty() -> item { YogaNote("No saved days yet. Your history will build as Yoga measures your days.") }
            !recorded -> item { YogaNote("Yoga has no saved usage for this day. Days are saved while Usage access is on.") }
            else -> {
                if (access.allows(ProFeature.VALUE_OF_TIME) && p.timeValueEnabled && allowance != null && (focusDay == null || focusDay == today)) item(key = "value") {
                    val minutes = if (focusDay == null) excessWeek else s.evaluation.overall?.excessMinutes ?: 0
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
                    item(key = "categories-title") { YogaSection("Categories") }
                    items(categories, key = { "category:${it.first}" }) { (name, minutes) -> YogaRow(name,
                        trailing = { Text(minutesLabel(minutes), style = MaterialTheme.typography.bodyMedium, color = muted) }) }
                }
                item(key = "apps-title") { YogaSection("Apps") }
                if (byApp.isEmpty()) item(key = "apps-empty") { YogaNote("No foreground app time was recorded in this period.") }
                items(byApp, key = { "app:${it.first}" }) { (pkg, minutes) ->
                    val app = state.apps.firstOrNull { it.appPackage == pkg }
                    Column(Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(enabled = app != null) { selected = app }
                        .padding(horizontal = ScreenMargin, vertical = 12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f, fill = false).padding(end = 12.dp)) {
                                Text(app?.displayLabel ?: s.appLabels[pkg] ?: "App", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
                                Text(when { pkg in s.essentialPackages -> "Essential"; app?.isDistracting == true -> "Distracting"; else -> "Normal" },
                                    style = MaterialTheme.typography.bodySmall, color = muted)
                            }
                            Text(minutesLabel(minutes), style = numbers, color = muted)
                        }
                        YogaProgress(minutes.toFloat() / maximum, Modifier.padding(top = 10.dp))
                    }
                }
                if (focusDay == null) item(key = "week-note") { YogaNote("${s.knownDays.size} of 7 days recorded. Missing days are not treated as zero. Historical eligibility is saved when sampled; comparisons use your current daily allowance.") }
            }
        }
        item(key = "method") { YogaNote("Yoga counts foreground app time from Android's activity events. Lock screen, Yoga, home apps and System UI are excluded; simultaneous multi-window apps can overlap. Learning video stays in all-app time but not the allowance. Whole-minute storage can differ by up to about a minute.") }
    }
    selected?.let { UsageAppSheet(it, state, viewModel) { selected = null } }
    if (details && access.allows(ProFeature.VALUE_OF_TIME)) TimeValueDetailsSheet(state, { details = false }, viewModel::snooze)
}

/** "‹ Yesterday ›": steps one day at a time through the stored week, like Digital Wellbeing's date switcher. */
@Composable
private fun DayStepper(title: String, canBack: Boolean, canForward: Boolean, onBack: () -> Unit, onForward: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = ScreenMargin - 12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onBack, enabled = canBack) {
            Icon(YogaIcons.Back, contentDescription = "Previous day",
                tint = if (canBack) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outlineVariant)
        }
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        IconButton(onForward, enabled = canForward) {
            Icon(YogaIcons.ChevronRight, contentDescription = "Next day",
                tint = if (canForward) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outlineVariant)
        }
    }
}
