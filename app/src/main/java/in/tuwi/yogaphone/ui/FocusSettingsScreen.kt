package `in`.tuwi.yogaphone.ui

import androidx.compose.foundation.clickable
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import `in`.tuwi.yogaphone.data.AppModel
import `in`.tuwi.yogaphone.data.db.FocusScheduleEntity
import `in`.tuwi.yogaphone.focus.FocusUiState
import `in`.tuwi.yogaphone.focus.FocusViewModel
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun FocusSettingsScreen(state: FocusUiState, apps: List<AppModel>, viewModel: FocusViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = state.preferences
    val system = viewModel.system()
    var dndAccess by remember { mutableStateOf(system.hasDndAccess()) }
    var exact by remember { mutableStateOf(system.canScheduleExact()) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) { dndAccess = system.hasDndAccess(); exact = system.canScheduleExact() }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    var editing by remember { mutableStateOf<FocusScheduleEntity?>(null) }
    val distracting = apps.filter { state.isDistracting(it.appPackage) }.distinctBy { it.appPackage }

    LazyColumn {
        item { SettingsHeader("Focus and timebox", onBack) }

        item { SectionTitle("Mindful pause") }
        item { SwitchRow("Pause before distracting apps", "A few seconds to choose on purpose", prefs.delayEnabled, viewModel::setDelayEnabled) }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text("Pause length: ${prefs.delaySeconds} seconds (5 more during focus)")
                Slider(value = prefs.delaySeconds.toFloat(), onValueChange = { viewModel.setDelaySeconds(it.toInt()) },
                    valueRange = 2f..10f, steps = 7)
                Text("After you continue, don't ask again for that app for")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 5, 15, 30).forEach { minutes ->
                        FilterChip(selected = prefs.dontAskMinutes == minutes, onClick = { viewModel.setDontAskMinutes(minutes) },
                            label = { Text(if (minutes == 0) "Always ask" else "$minutes min") })
                    }
                }
                Text("Apps opened from notifications or other apps skip this pause.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp))
            }
        }

        item { SectionTitle("During focus") }
        item { SwitchRow("Greyscale launcher", "Switch Yoga to greyscale while focus is on", prefs.focusGreyscale, viewModel::setFocusGreyscale) }
        item {
            if (dndAccess) SwitchRow("Do Not Disturb", "Priority-only notifications while focus is on; restored afterwards",
                prefs.focusDnd, viewModel::setFocusDnd)
            else ListItem(
                headlineContent = { Text("Do Not Disturb during focus") },
                supportingContent = { Text("Needs Do Not Disturb access, so Yoga can turn on priority-only mode and turn it off again.") },
                trailingContent = {
                    TextButton(onClick = {
                        runCatching { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) }
                    }) { Text("Allow") }
                },
            )
        }

        item { SectionTitle("Focus schedules") }
        if (state.schedules.isEmpty()) item {
            Text("No schedules yet. For example: Work 09:00–13:00 on weekdays, or Bedtime 22:00–07:00.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
        items(state.schedules, key = { it.id }) { schedule ->
            ListItem(
                headlineContent = { Text(schedule.name) },
                supportingContent = { Text("${time(schedule.startMinuteOfDay)}–${time(schedule.endMinuteOfDay)} · ${daysLabel(schedule.days)}" + if (schedule.startsFocus) "" else " · profile only") },
                trailingContent = {
                    Row {
                        Switch(checked = schedule.enabled, onCheckedChange = { viewModel.saveSchedule(schedule.copy(enabled = it)) })
                        TextButton(onClick = { editing = schedule }) { Text("Edit") }
                    }
                },
            )
        }
        item {
            TextButton(onClick = { editing = FocusScheduleEntity(name = "Work", days = "1,2,3,4,5", startMinuteOfDay = 9 * 60, endMinuteOfDay = 13 * 60) },
                modifier = Modifier.padding(horizontal = 8.dp)) { Text("Add schedule") }
        }

        item { SectionTitle("Timebox") }
        item {
            if (exact) ListItem(headlineContent = { Text("Precise endings") }, supportingContent = { Text("Timeboxes end on the minute.") })
            else ListItem(
                headlineContent = { Text("Precise endings (optional)") },
                supportingContent = { Text("Without exact alarms Android may deliver the ending note up to a minute late.") },
                trailingContent = {
                    system.exactAlarmSettingsIntent()?.let { intent ->
                        TextButton(onClick = { runCatching { context.startActivity(intent) } }) { Text("Allow") }
                    }
                },
            )
        }

        item { SectionTitle("Distracting apps") }
        item {
            Text("Change which apps count as distracting in Usage rules.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
        }
        items(distracting, key = { "focus:" + it.appPackage }) { app ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text(app.displayLabel, style = MaterialTheme.typography.bodyLarge)
                CheckRow("Pause before opening", app.appPackage !in prefs.delayOptOut) { viewModel.setDelayOptOut(app.appPackage, !it) }
                CheckRow("Allowed during focus", app.appPackage in prefs.focusAllowed) { viewModel.setFocusAllowed(app.appPackage, it) }
            }
        }
    }

    editing?.let { schedule ->
        ScheduleDialog(schedule, onDismiss = { editing = null },
            onSave = { viewModel.saveSchedule(it); editing = null },
            onDelete = if (schedule.id != 0L) ({ viewModel.deleteSchedule(schedule.id); editing = null }) else null)
    }
}

@Composable
private fun SwitchRow(title: String, summary: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(headlineContent = { Text(title) }, supportingContent = { Text(summary) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
        modifier = Modifier.clickable { onChange(!checked) })
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label)
    }
}

private fun time(minuteOfDay: Int) = "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)

private fun daysLabel(days: String): String {
    val set = days.split(',').mapNotNull { it.toIntOrNull() }.toSet()
    return when (set) {
        (1..7).toSet() -> "Every day"
        (1..5).toSet() -> "Weekdays"
        setOf(6, 7) -> "Weekends"
        else -> set.sorted().joinToString(" ") { DayOfWeek.of(it).getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ScheduleDialog(
    initial: FocusScheduleEntity,
    onDismiss: () -> Unit,
    onSave: (FocusScheduleEntity) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(initial.name) }
    var days by remember { mutableStateOf(initial.days.split(',').mapNotNull { it.toIntOrNull() }.toSet()) }
    var start by remember { mutableIntStateOf(initial.startMinuteOfDay) }
    var end by remember { mutableIntStateOf(initial.endMinuteOfDay) }
    var startsFocus by remember { mutableStateOf(initial.startsFocus) }
    var picking by remember { mutableStateOf<String?>(null) }
    val valid = name.isNotBlank() && days.isNotEmpty() && start != end

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id == 0L) "New focus schedule" else "Edit schedule") },
        text = {
            Column {
                OutlinedTextField(name, { name = it.take(24) }, label = { Text("Name") }, singleLine = true)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    (1..7).forEach { day ->
                        FilterChip(selected = day in days, onClick = { days = if (day in days) days - day else days + day },
                            label = { Text(DayOfWeek.of(day).getDisplayName(TextStyle.SHORT, Locale.getDefault())) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { picking = "start" }) { Text("From ${time(start)}") }
                    TextButton(onClick = { picking = "end" }) { Text("To ${time(end)}") }
                }
                CheckRow("Turn on focus", startsFocus) { startsFocus = it }
                if (!startsFocus) Text("Use it to switch a profile without focus.", style = MaterialTheme.typography.bodySmall)
                if (end < start) Text("Runs past midnight into the next day.", style = MaterialTheme.typography.bodySmall)
                if (start == end) Text("Start and end must differ.", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onSave(initial.copy(name = name.trim(), days = days.sorted().joinToString(","), startMinuteOfDay = start, endMinuteOfDay = end, startsFocus = startsFocus))
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                onDelete?.let { TextButton(onClick = it) { Text("Delete") } }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )

    picking?.let { which ->
        val current = if (which == "start") start else end
        val pickerState = rememberTimePickerState(initialHour = current / 60, initialMinute = current % 60)
        AlertDialog(
            onDismissRequest = { picking = null },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val value = pickerState.hour * 60 + pickerState.minute
                    if (which == "start") start = value else end = value
                    picking = null
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { picking = null }) { Text("Cancel") } },
        )
    }
}
