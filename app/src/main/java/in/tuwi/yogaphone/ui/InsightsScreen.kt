package `in`.tuwi.yogaphone.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.insights.CoachingTone
import `in`.tuwi.yogaphone.insights.InsightsData
import `in`.tuwi.yogaphone.insights.InsightsPreferences
import `in`.tuwi.yogaphone.insights.InsightsRepository
import kotlinx.coroutines.launch
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun InsightsScreen(
    repository: InsightsRepository,
    prefs: InsightsPreferences,
    onAcceptSuggestion: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<InsightsData?>(null) }
    var reload by remember { mutableStateOf(0) }
    LaunchedEffect(reload, prefs.tone) { data = runCatching { repository.load() }.getOrDefault(InsightsData()) }
    val d = data

    LazyColumn {
        item { SettingsHeader("Insights", onBack) }
        if (!prefs.insightsEnabled) {
            item {
                Text("Insights are off. Turn them on in Privacy and data.", modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@LazyColumn
        }
        if (d == null) {
            item { Text("Reading your week…", modifier = Modifier.padding(16.dp)) }
            return@LazyColumn
        }
        d.suggestion?.let { suggested ->
            item {
                Card(Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("You stayed well under your allowance most days last week.")
                        Text("Try $suggested minutes a day?", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onAcceptSuggestion(suggested); reload++ }) { Text("Use $suggested min") }
                            TextButton(onClick = {
                                scope.launch { repository.dismissSuggestion("${repository.weekKey(java.time.LocalDate.now())}-$suggested"); reload++ }
                            }) { Text("Not now") }
                        }
                    }
                }
            }
        }
        if (d.cards.isEmpty()) item {
            Text("Insights appear after a few days of use. Comparisons wait until there are at least five recorded days in each week.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
        }
        items(d.cards, key = { it.id }) { card ->
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp)) {
                    Text(card.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(card.body, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        item { SectionTitle("Last 7 days") }
        item { WeekBars(d) }
        item { SectionTitle("When you use distracting apps (4 weeks)") }
        item { Heatmap(d) }
        item { SectionTitle("Goals") }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text("Focus today: ${d.focusMinutesToday} of ${prefs.focusGoalMinutes} min")
                LinearProgressIndicator(progress = { (d.focusMinutesToday.toFloat() / prefs.focusGoalMinutes).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(30, 60, 90, 120).forEach { goal ->
                        FilterChip(selected = prefs.focusGoalMinutes == goal, onClick = { scope.launch { repository.setFocusGoal(goal) } },
                            label = { Text("$goal min") })
                    }
                }
                if (d.allowance != null) {
                    Text("Days within allowance this week: ${d.daysWithinAllowance} of ${prefs.allowanceDaysGoal}", modifier = Modifier.padding(top = 12.dp))
                    LinearProgressIndicator(progress = { (d.daysWithinAllowance.toFloat() / prefs.allowanceDaysGoal).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(3, 5, 7).forEach { goal ->
                            FilterChip(selected = prefs.allowanceDaysGoal == goal, onClick = { scope.launch { repository.setDaysGoal(goal) } },
                                label = { Text("$goal days") })
                        }
                    }
                }
            }
        }
        item { SectionTitle("Coaching") }
        item {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(prefs.tone == CoachingTone.GENTLE, { scope.launch { repository.setTone(CoachingTone.GENTLE) } }, label = { Text("Gentle") })
                FilterChip(prefs.tone == CoachingTone.DATA_DRIVEN, { scope.launch { repository.setTone(CoachingTone.DATA_DRIVEN) } }, label = { Text("Just the numbers") })
            }
        }
        item {
            ListItem(headlineContent = { Text("Weekly summary") }, supportingContent = { Text("A short note on Sunday evening") },
                trailingContent = { Switch(prefs.weeklySummary, { scope.launch { repository.setWeeklySummary(it) } }) },
                modifier = Modifier.clickable { scope.launch { repository.setWeeklySummary(!prefs.weeklySummary) } })
        }
        item {
            ListItem(headlineContent = { Text("Reflect after focus") }, supportingContent = { Text("One tap on how a focus session felt") },
                trailingContent = { Switch(prefs.reflections, { scope.launch { repository.setReflections(it) } }) },
                modifier = Modifier.clickable { scope.launch { repository.setReflections(!prefs.reflections) } })
        }
        item {
            Text("Insights are worked out on this phone from your own usage. Percent changes are shown only when both weeks have at least five recorded days.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
private fun WeekBars(d: InsightsData) {
    val bar = MaterialTheme.colorScheme.primary
    val over = MaterialTheme.colorScheme.error
    val line = MaterialTheme.colorScheme.onSurfaceVariant
    val max = (d.week.mapNotNull { it.second }.maxOrNull() ?: 0L).coerceAtLeast(d.allowance?.toLong() ?: 0L).coerceAtLeast(1L)
    val summary = d.week.joinToString("; ") { (day, minutes) ->
        "${day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${minutes?.let { "$it minutes" } ?: "not recorded"}"
    } + (d.allowance?.let { ". Allowance $it minutes." } ?: "")
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).semantics { contentDescription = summary }) {
        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
            val slot = size.width / 7f
            d.week.forEachIndexed { index, (_, minutes) ->
                if (minutes == null) return@forEachIndexed
                val h = size.height * minutes / max
                val overLimit = d.allowance != null && minutes > d.allowance
                drawRect(if (overLimit) over else bar, topLeft = Offset(index * slot + slot * 0.2f, size.height - h), size = Size(slot * 0.6f, h))
            }
            d.allowance?.let { allowance ->
                val y = size.height - size.height * allowance / max
                drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            d.week.forEach { (day, minutes) ->
                Column(Modifier.weight(1f)) {
                    Text(day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), style = MaterialTheme.typography.labelSmall)
                    Text(minutes?.toString() ?: "–", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (d.allowance != null) Text("Dashed line: your ${d.allowance}-minute allowance. Bars past it are marked in the warning colour.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Heatmap(d: InsightsData) {
    val base = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.surfaceVariant
    val max = d.heatmap.mapNotNull { it.second?.maxOrNull() }.maxOrNull()?.coerceAtLeast(1) ?: 1
    val recorded = d.heatmap.count { it.second != null }
    val peak = (0..23).maxByOrNull { h -> d.heatmap.sumOf { it.second?.get(h) ?: 0 } }
    val summary = if (recorded == 0) "No hourly data yet." else "$recorded days recorded. Busiest hour: ${peak}:00."
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).semantics { contentDescription = summary }) {
        Canvas(Modifier.fillMaxWidth().height(168.dp)) {
            val cellW = size.width / 24f
            val cellH = size.height / 28f
            d.heatmap.forEachIndexed { row, (_, hours) ->
                for (h in 0 until 24) {
                    val v = hours?.get(h) ?: 0
                    val color = if (hours == null || v == 0L) empty else base.copy(alpha = 0.2f + 0.8f * v / max)
                    drawRect(color, Offset(h * cellW + 1, row * cellH + 1), Size(cellW - 2, cellH - 2))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("0", "6", "12", "18", "23").forEach { Text(it, style = MaterialTheme.typography.labelSmall) }
        }
        Text("$summary Each row is a day (oldest at the top), each column an hour; darker means more time.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReflectionSheet(onAnswer: (String?) -> Unit) {
    ModalBottomSheet(onDismissRequest = { onAnswer(null) }) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text("How did that focus session feel?", style = MaterialTheme.typography.titleLarge)
            Row(Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Calmer", "About the same", "Restless").forEach { feeling ->
                    FilterChip(selected = false, onClick = { onAnswer(feeling) }, label = { Text(feeling) },
                        modifier = Modifier.heightIn(min = 48.dp))
                }
            }
            TextButton(onClick = { onAnswer(null) }) { Text("Skip") }
        }
    }
}
