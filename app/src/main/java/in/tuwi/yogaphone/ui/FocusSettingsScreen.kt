package `in`.tuwi.yogaphone.ui

import `in`.tuwi.yogaphone.pro.*
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
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
import `in`.tuwi.yogaphone.YogaApplication
import `in`.tuwi.yogaphone.review.FeedbackFeature
import `in`.tuwi.yogaphone.ui.theme.*
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun FocusSettingsScreen(state: FocusUiState, apps: List<AppModel>, viewModel: FocusViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val access = LocalProAccess.current
    val prefs = state.preferences
    val system = viewModel.system()
    val review = (context.applicationContext as YogaApplication).reviewController
    val scope = rememberCoroutineScope()
    var pauseFeedback by remember { mutableStateOf(false) }
    var dndAccess by remember { mutableStateOf(system.hasDndAccess()) }
    var exact by remember { mutableStateOf(system.canScheduleExact()) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) {
            dndAccess = system.hasDndAccess(); exact = system.canScheduleExact()
        } }
        owner.lifecycle.addObserver(observer); onDispose { owner.lifecycle.removeObserver(observer) }
    }
    var editing by remember { mutableStateOf<FocusScheduleEntity?>(null) }
    val distracting = apps.filter { state.isDistracting(it.appPackage) }.distinctBy { it.appPackage }
    val recentNames = rememberRecentEntries(RECENT_SCHEDULE_NAMES)

    YogaScreen("Focus and timebox", onBack) {
        item { YogaSection("Mindful pause") }
        item { YogaSwitchRow("Pause before distracting apps", prefs.delayEnabled, { enabled ->
            viewModel.setDelayEnabled(enabled)
            scope.launch { if (review.featureChanged(FeedbackFeature.PAUSE, enabled)) pauseFeedback = true }
        },
            summary = "A few seconds to choose on purpose") }
        if (pauseFeedback) item {
            FeedbackQuestion(FeedbackFeature.PAUSE, listOf("Too slow", "Annoying", "Didn't help")) { pauseFeedback = false }
        }
        item {
            YogaRow("Pause length", summary = "${prefs.delaySeconds} seconds · 5 more during focus")
            YogaSlider(prefs.delaySeconds.toFloat(), { viewModel.setDelaySeconds(it.toInt()) },
                Modifier.padding(horizontal = ScreenMargin), valueRange = 2f..10f, steps = 7)
            YogaNote("Each time, choose a 5, 10 or 15 minute pass.")
            YogaNote("Apps opened from notifications or other apps skip this pause.")
        }
        if (!access.allows(ProFeature.FOCUS)) {
            item { ProRow("Focus schedules, timebox and Do Not Disturb", ProFeature.FOCUS, "Your saved settings resume with Pro", access.open) }
        } else {
        item { YogaSection("During focus") }
        item { YogaSwitchRow("Greyscale launcher", prefs.focusGreyscale, viewModel::setFocusGreyscale,
            summary = "Switch Yoga to greyscale while focus is on") }
        item {
            if (dndAccess) YogaSwitchRow("Do Not Disturb", prefs.focusDnd, viewModel::setFocusDnd,
                summary = "Priority-only notifications while focus is on; restored afterwards")
            else YogaRow("Do Not Disturb during focus", summary = "Needs Do Not Disturb access, so Yoga can turn on priority-only mode and turn it off again.",
                trailing = { YogaTextButton("Allow", { runCatching { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) } }) })
        }
        item { YogaSection("Focus schedules") }
        if (state.schedules.isEmpty()) item { YogaNote("No schedules yet. For example: Work 09:00–13:00 on weekdays, or Bedtime 22:00–07:00.") }
        items(state.schedules, key = { it.id }) { schedule ->
            YogaRow(schedule.name, summary = "${time(schedule.startMinuteOfDay)}–${time(schedule.endMinuteOfDay)} · ${daysLabel(schedule.days)}${if (schedule.startsFocus) "" else " · profile only"}",
                onClick = { editing = schedule }, trailing = {
                    YogaSwitch(schedule.enabled, { viewModel.saveSchedule(schedule.copy(enabled = it)) })
                })
        }
        item { YogaTextButton("Add schedule", { editing = FocusScheduleEntity(name = "Work", days = "1,2,3,4,5", startMinuteOfDay = 9 * 60, endMinuteOfDay = 13 * 60) }, Modifier.padding(horizontal = TextButtonInset)) }
        item { YogaSection("Timebox") }
        item {
            if (exact) YogaRow("Precise endings", summary = "Timeboxes end on the minute.")
            else YogaRow("Precise endings (optional)", summary = "Without exact alarms Android may deliver the ending note up to a minute late.",
                trailing = { system.exactAlarmSettingsIntent()?.let { intent -> YogaTextButton("Allow", { runCatching { context.startActivity(intent) } }) } })
        }
        }
        item { YogaSection("Distracting apps") }
        item { YogaNote("Change which apps count as distracting in Usage rules.") }
        items(distracting, key = { "focus:${it.appPackage}" }) { app ->
            YogaSection(app.displayLabel)
            val activeIds = distracting.filter { it.appPackage !in prefs.delayOptOut }.map { it.appPackage }
            val freeIds = freeQuota(activeIds, 3)
            val quota = app.appPackage in freeIds || activeIds.size < 3
            if (!access.allows(ProFeature.EXTRA_MINDFUL_APPS) && !quota) ProRow("Pause before opening", ProFeature.EXTRA_MINDFUL_APPS, onClick = access.open)
            else YogaCheckRow("Pause before opening", app.appPackage !in prefs.delayOptOut) {
                viewModel.setDelayOptOut(app.appPackage, app.appPackage !in prefs.delayOptOut)
            }
            if (access.allows(ProFeature.FOCUS)) YogaCheckRow("Allowed during focus", app.appPackage in prefs.focusAllowed) {
                viewModel.setFocusAllowed(app.appPackage, app.appPackage !in prefs.focusAllowed)
            }
        }
    }
    editing?.let { schedule -> ScheduleDialog(schedule, recentNames.value, { editing = null },
        { recordRecentEntry(context, RECENT_SCHEDULE_NAMES, recentNames, it.name); viewModel.saveSchedule(it); editing = null },
        if (schedule.id != 0L) ({ viewModel.deleteSchedule(schedule.id); editing = null }) else null) }
}

private fun time(minuteOfDay: Int) = "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)
private fun daysLabel(days: String): String {
    val set = days.split(',').mapNotNull { it.toIntOrNull() }.toSet()
    return when (set) {
        (1..7).toSet() -> "Every day"; (1..5).toSet() -> "Weekdays"; setOf(6, 7) -> "Weekends"
        else -> set.sorted().joinToString(" ") { DayOfWeek.of(it).getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleDialog(initial: FocusScheduleEntity, recentNames: List<String>, onDismiss: () -> Unit,
    onSave: (FocusScheduleEntity) -> Unit, onDelete: (() -> Unit)?) {
    var name by remember { mutableStateOf(initial.name) }
    var days by remember { mutableStateOf(initial.days.split(',').mapNotNull { it.toIntOrNull() }.toSet()) }
    var start by remember { mutableIntStateOf(initial.startMinuteOfDay) }
    var end by remember { mutableIntStateOf(initial.endMinuteOfDay) }
    var startsFocus by remember { mutableStateOf(initial.startsFocus) }
    var picking by remember { mutableStateOf<String?>(null) }
    val valid = name.isNotBlank() && days.isNotEmpty() && start != end
    YogaDialog(if (initial.id == 0L) "New focus schedule" else "Edit schedule", "Save", {
        onSave(initial.copy(name = name.trim(), days = days.sorted().joinToString(","), startMinuteOfDay = start, endMinuteOfDay = end, startsFocus = startsFocus))
    }, onDismiss, confirmEnabled = valid, text = {
        Column {
            YogaTextField(name, { name = it.take(24) }, "Name")
            if (recentNames.isNotEmpty()) YogaChipRow { recentNames.forEach { recent -> YogaChip(recent, name == recent, onClick = { name = recent }) } }
            YogaChipRow { (1..7).forEach { day -> YogaChip(DayOfWeek.of(day).getDisplayName(TextStyle.SHORT, Locale.getDefault()), day in days,
                onClick = { days = if (day in days) days - day else days + day }) } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = ScreenMargin)) {
                YogaSecondaryButton("From ${time(start)}", { picking = "start" }, Modifier.weight(1f))
                YogaSecondaryButton("To ${time(end)}", { picking = "end" }, Modifier.weight(1f))
            }
            YogaCheckRow("Turn on focus", startsFocus) { startsFocus = !startsFocus }
            if (!startsFocus) YogaNote("Use it to switch a profile without focus.")
            if (end < start) YogaNote("Runs past midnight into the next day.")
            if (start == end) Text("Start and end must differ.", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = ScreenMargin))
            onDelete?.let { YogaTextButton("Delete schedule", it, Modifier.padding(horizontal = TextButtonInset), muted = true) }
        }
    }, modifier = Modifier, dismissText = "Cancel", onDismissAction = onDismiss)
    picking?.let { which ->
        val current = if (which == "start") start else end
        val pickerState = rememberTimePickerState(initialHour = current / 60, initialMinute = current % 60)
        YogaDialog("Choose ${if (which == "start") "start" else "end"} time", "Set", {
            val value = pickerState.hour * 60 + pickerState.minute
            if (which == "start") start = value else end = value
            picking = null
        }, { picking = null }, text = { TimePicker(state = pickerState) })
    }
}
