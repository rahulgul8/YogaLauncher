package `in`.tuwi.yogaphone.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.insights.*
import `in`.tuwi.yogaphone.ui.theme.*
import kotlinx.coroutines.launch
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun InsightsScreen(repository: InsightsRepository, prefs: InsightsPreferences, learningSecondsToday: Long = 0,
    onAcceptSuggestion: (Int) -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<InsightsData?>(null) }
    var reload by remember { mutableStateOf(0) }
    LaunchedEffect(reload, prefs.tone) { data = runCatching { repository.load() }.getOrDefault(InsightsData()) }
    val d = data
    YogaScreen("Insights", onBack) {
        if (!prefs.insightsEnabled) { item { YogaNote("Insights are off. Turn them on in Privacy and data.") }; return@YogaScreen }
        if (d == null) { item { YogaNote("Reading your week…") }; return@YogaScreen }
        d.suggestion?.let { suggested -> item {
            YogaCard {
                YogaEyebrow("A gentler target")
                Text("You stayed well under your allowance most days last week.", modifier = Modifier.padding(top = 10.dp))
                Text("Try $suggested minutes a day?", style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "lnum, tnum"),
                    modifier = Modifier.padding(vertical = 12.dp))
                YogaPrimaryButton("Use $suggested min", { onAcceptSuggestion(suggested); reload++ })
                YogaTextButton("Not now", { scope.launch { repository.dismissSuggestion("${repository.weekKey(java.time.LocalDate.now())}-$suggested"); reload++ } }, muted = true)
            }
        } }
        if (d.cards.isEmpty()) item { YogaNote("Insights appear after a few days of use. Comparisons wait until there are at least five recorded days in each week.") }
        items(d.cards, key = { it.id }) { card -> YogaCard(Modifier.padding(vertical = 6.dp)) {
            YogaEyebrow(card.title); Text(card.body, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
        } }
        item { YogaSection("Last 7 days") }
        item { WeekBars(d) }
        item { YogaSection("When you use distracting apps (4 weeks)") }
        item { Heatmap(d) }
        item { YogaSection("Goals") }
        item { YogaCard { YogaStat("${learningSecondsToday / 60} min", "Learning today") } }
        item {
            YogaCard {
                YogaStat("${d.focusMinutesToday} / ${prefs.focusGoalMinutes} min", "Focus today")
                YogaProgress(d.focusMinutesToday.toFloat() / prefs.focusGoalMinutes.coerceAtLeast(1), Modifier.padding(top = 12.dp))
            }
            YogaChipRow { listOf(30, 60, 90, 120).forEach { goal -> YogaChip("$goal min", prefs.focusGoalMinutes == goal,
                onClick = { scope.launch { repository.setFocusGoal(goal) } }) } }
            if (d.allowance != null) {
                YogaCard { YogaStat("${d.daysWithinAllowance} / ${prefs.allowanceDaysGoal}", "Days within allowance this week")
                    YogaProgress(d.daysWithinAllowance.toFloat() / prefs.allowanceDaysGoal.coerceAtLeast(1), Modifier.padding(top = 12.dp)) }
                YogaChipRow { listOf(3, 5, 7).forEach { goal -> YogaChip("$goal days", prefs.allowanceDaysGoal == goal,
                    onClick = { scope.launch { repository.setDaysGoal(goal) } }) } }
            }
        }
        item { YogaSection("Coaching") }
        item { YogaChipRow {
            YogaChip("Gentle", prefs.tone == CoachingTone.GENTLE, onClick = { scope.launch { repository.setTone(CoachingTone.GENTLE) } })
            YogaChip("Just the numbers", prefs.tone == CoachingTone.DATA_DRIVEN, onClick = { scope.launch { repository.setTone(CoachingTone.DATA_DRIVEN) } })
        } }
        item { YogaSwitchRow("Weekly summary", prefs.weeklySummary, { scope.launch { repository.setWeeklySummary(it) } },
            summary = "A short note on Sunday evening") }
        item { YogaSwitchRow("Reflect after focus", prefs.reflections, { scope.launch { repository.setReflections(it) } },
            summary = "One tap on how a focus session felt") }
        item { YogaNote("Insights are worked out on this phone from your own usage. Percent changes are shown only when both weeks have at least five recorded days.") }
    }
}

@Composable
private fun WeekBars(d: InsightsData) {
    val bar = MaterialTheme.colorScheme.onSurfaceVariant
    val over = MaterialTheme.colorScheme.tertiary
    val line = MaterialTheme.colorScheme.outline
    val max = (d.week.mapNotNull { it.second }.maxOrNull() ?: 0L).coerceAtLeast(d.allowance?.toLong() ?: 0L).coerceAtLeast(1L)
    val summary = d.week.joinToString("; ") { (day, minutes) ->
        "${day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${minutes?.let { "$it minutes" } ?: "not recorded"}"
    } + (d.allowance?.let { ". Allowance $it minutes." } ?: "")
    Column(Modifier.fillMaxWidth().padding(horizontal = ScreenMargin).semantics { contentDescription = summary }) {
        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
            val slot = size.width / 7f
            d.week.forEachIndexed { index, (_, minutes) ->
                if (minutes == null) return@forEachIndexed
                val h = size.height * minutes / max
                drawRect(if (d.allowance != null && minutes > d.allowance) over else bar,
                    Offset(index * slot + slot * .2f, size.height - h), Size(slot * .6f, h))
            }
            d.allowance?.let { allowance ->
                val y = size.height - size.height * allowance / max
                drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
            }
        }
        Row(Modifier.fillMaxWidth()) { d.week.forEach { (day, minutes) -> Column(Modifier.weight(1f)) {
            Text(day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), style = MaterialTheme.typography.labelSmall)
            Text(minutes?.toString() ?: "–", style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "lnum, tnum"), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } } }
        if (d.allowance != null) Text("Dashed line: your ${d.allowance}-minute allowance. Bars past it use the time colour.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Heatmap(d: InsightsData) {
    val base = MaterialTheme.colorScheme.onSurfaceVariant
    val empty = MaterialTheme.colorScheme.surfaceContainer
    val max = d.heatmap.mapNotNull { it.second?.maxOrNull() }.maxOrNull()?.coerceAtLeast(1) ?: 1
    val recorded = d.heatmap.count { it.second != null }
    val peak = (0..23).maxByOrNull { h -> d.heatmap.sumOf { it.second?.get(h) ?: 0 } }
    val summary = if (recorded == 0) "No hourly data yet." else "$recorded days recorded. Busiest hour: ${peak}:00."
    Column(Modifier.fillMaxWidth().padding(horizontal = ScreenMargin).semantics { contentDescription = summary }) {
        Canvas(Modifier.fillMaxWidth().height(168.dp)) {
            val cellW = size.width / 24f; val cellH = size.height / 28f
            d.heatmap.forEachIndexed { row, (_, hours) -> for (h in 0 until 24) {
                val v = hours?.get(h) ?: 0
                drawRect(if (hours == null || v == 0L) empty else base.copy(alpha = .2f + .8f * v / max),
                    Offset(h * cellW + 1, row * cellH + 1), Size(cellW - 2, cellH - 2))
            } }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("0", "6", "12", "18", "23").forEach { Text(it, style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "lnum, tnum")) }
        }
        Text("$summary Each row is a day (oldest at the top), each column an hour; darker means more time.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ReflectionSheet(onAnswer: (String?) -> Unit) {
    YogaSheet({ onAnswer(null) }) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text("How did that focus session feel?", style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = ScreenMargin, vertical = 12.dp))
            YogaChipRow { listOf("Calmer", "About the same", "Restless").forEach { feeling ->
                YogaChip(feeling, false, onClick = { onAnswer(feeling) })
            } }
            YogaTextButton("Skip", { onAnswer(null) }, Modifier.padding(horizontal = TextButtonInset), muted = true)
        }
    }
}
