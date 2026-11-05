package `in`.tuwi.yogaphone.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.data.AppModel
import `in`.tuwi.yogaphone.data.db.TimeboxSessionEntity
import `in`.tuwi.yogaphone.data.db.TimeboxStatus
import `in`.tuwi.yogaphone.focus.FocusUiState
import `in`.tuwi.yogaphone.focus.formatMinutesLeft
import `in`.tuwi.yogaphone.focus.label
import java.text.DateFormat
import java.util.Date

@Composable
fun HomeQuickActions(onTimebox: () -> Unit, onFocus: () -> Unit, focusActive: Boolean, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        TextButton(onClick = onTimebox, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Timebox", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!focusActive) TextButton(onClick = onFocus, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Focus", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Quiet status lines for a running timebox and an active focus session. */
@Composable
fun HomeFocusStatus(state: FocusUiState, onTimebox: () -> Unit, onFocus: () -> Unit) {
    val focus = state.focus
    if (focus != null) {
        val until = focus.plannedEndAt
        val text = when {
            until == null -> "Focus · until you end it"
            until - state.now > 3 * 60 * 60_000L -> "Focus until ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(until))}"
            else -> "Focus · ${formatMinutesLeft(until - state.now)}"
        }
        StatusLine(text, onFocus)
    }
    val timebox = state.timebox
    val remaining = state.timeboxRemainingMs
    if (timebox != null && remaining != null) {
        val paused = timebox.status == TimeboxStatus.PAUSED
        StatusLine("${timebox.label} · ${formatMinutesLeft(remaining)}${if (paused) " · paused" else ""}", onTimebox)
    }
}

@Composable
private fun StatusLine(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick).padding(vertical = 12.dp),
    )
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

@Composable
private fun rememberNotificationRequest(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    return {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeboxStartSheet(
    lastMinutes: Int,
    recentPurposes: List<String>,
    boundApp: AppModel?,
    onStart: (minutes: Int, purpose: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val presets = listOf(5, 15, 25, 45, 60)
    var minutes by remember { mutableStateOf(lastMinutes.coerceIn(1, 180)) }
    var custom by remember { mutableStateOf(if (lastMinutes in presets) "" else lastMinutes.toString()) }
    var purpose by remember { mutableStateOf("") }
    val requestNotifications = rememberNotificationRequest()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text(if (boundApp == null) "Timebox" else "Timebox ${boundApp.displayLabel}", style = MaterialTheme.typography.titleLarge)
            Text("Set a bounded session. You'll get a gentle note when it ends.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
            ChipRow {
                presets.forEach { preset ->
                    FilterChip(selected = custom.isEmpty() && minutes == preset,
                        onClick = { minutes = preset; custom = "" }, label = { Text("$preset min") })
                }
            }
            OutlinedTextField(
                value = custom,
                onValueChange = { value ->
                    custom = value.filter(Char::isDigit).take(3)
                    custom.toIntOrNull()?.let { minutes = it.coerceIn(1, 180) }
                },
                label = { Text("Custom minutes (1–180)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )
            OutlinedTextField(
                value = purpose,
                onValueChange = { purpose = it.take(40) },
                label = { Text("What for? (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )
            if (recentPurposes.isNotEmpty()) ChipRow {
                recentPurposes.forEach { recent -> AssistChip(onClick = { purpose = recent }, label = { Text(recent) }) }
            }
            Button(
                onClick = { requestNotifications(); onStart(minutes, purpose); onDismiss() },
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).heightIn(min = 48.dp),
            ) { Text("Start $minutes min" + if (boundApp != null) " and open" else "") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeboxControlSheet(
    session: TimeboxSessionEntity,
    remainingMs: Long,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onExtend: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val paused = session.status == TimeboxStatus.PAUSED
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text(session.label, style = MaterialTheme.typography.titleLarge)
            Text(formatMinutesLeft(remainingMs) + if (paused) " · paused" else "",
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { if (paused) onResume() else onPause(); onDismiss() }) { Text(if (paused) "Resume" else "Pause") }
                OutlinedButton(onClick = { onExtend(5); onDismiss() }) { Text("+5 min") }
                OutlinedButton(onClick = { onStop(); onDismiss() }) { Text("Stop") }
            }
            Spacer16()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeboxCompletionSheet(
    session: TimeboxSessionEntity,
    streak: Int,
    onDone: () -> Unit,
    onExtend: (Int) -> Unit,
    onStartFocus: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDone, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text("Timebox done", style = MaterialTheme.typography.titleLarge)
            Text(
                "${session.plannedMinutes + session.extendedMinutes} minutes of ${session.label.lowercase()}." +
                    if (streak > 1) " $streak-day streak." else "",
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp),
            )
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Stop here") }
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onExtend(5) }) { Text("+5 min") }
                OutlinedButton(onClick = { onExtend(10) }) { Text("+10 min") }
                OutlinedButton(onClick = { onDone(); onStartFocus() }) { Text("Focus") }
            }
            Spacer16()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusStartSheet(lastMinutes: Int, onStart: (Int?) -> Unit, onDismiss: () -> Unit) {
    val options = listOf(25, 45, 60, 90, 0)
    var selected by remember { mutableStateOf(if (lastMinutes in options) lastMinutes else 45) }
    val requestNotifications = rememberNotificationRequest()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text("Focus", style = MaterialTheme.typography.titleLarge)
            Text("Distracting apps step aside and ask for a longer pause. Essential apps stay available.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
            ChipRow {
                options.forEach { option ->
                    FilterChip(selected = selected == option, onClick = { selected = option },
                        label = { Text(if (option == 0) "Until I stop" else "$option min") })
                }
            }
            Button(
                onClick = { requestNotifications(); onStart(selected.takeIf { it > 0 }); onDismiss() },
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).heightIn(min = 48.dp),
            ) { Text("Start focus") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusControlSheet(state: FocusUiState, onEnd: () -> Unit, onDismiss: () -> Unit) {
    val focus = state.focus ?: return
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text("Focus is on", style = MaterialTheme.typography.titleLarge)
            val minutes = ((state.now - focus.startAt) / 60_000).coerceAtLeast(0)
            Text(
                "$minutes min so far" + (focus.plannedEndAt?.let { " · ${formatMinutesLeft(it - state.now)}" } ?: ""),
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp),
            )
            Button(
                onClick = { onEnd(); onDismiss() },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { contentDescription = "End focus now" },
            ) { Text("End focus") }
            Spacer16()
        }
    }
}

@Composable
private fun Spacer16() = androidx.compose.foundation.layout.Spacer(Modifier.padding(8.dp))
