package `in`.tuwi.yogaphone.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.data.AppModel
import `in`.tuwi.yogaphone.data.db.TimeboxSessionEntity
import `in`.tuwi.yogaphone.data.db.TimeboxStatus
import `in`.tuwi.yogaphone.focus.*
import `in`.tuwi.yogaphone.ui.theme.*
import java.text.DateFormat
import java.util.Date

@Composable
fun HomeQuickActions(onTimebox: () -> Unit, onFocus: () -> Unit, focusActive: Boolean, timeboxActive: Boolean = false) {
    HomeIconButton(YogaIcons.Timebox, if (timeboxActive) "Timebox running" else "Start a timebox", onTimebox, active = timeboxActive)
    HomeIconButton(YogaIcons.Focus, if (focusActive) "Focus is on" else "Start focus", onFocus, active = focusActive)
}

@Composable
fun HomeFocusStatus(state: FocusUiState, onTimebox: () -> Unit, onFocus: () -> Unit) {
    state.focus?.let { focus ->
        val until = focus.plannedEndAt
        StatusLine(when {
            until == null -> "Focus · until you end it"
            until - state.now > 3 * 60 * 60_000L -> "Focus until ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(until))}"
            else -> "Focus · ${formatMinutesLeft(until - state.now)}"
        }, onFocus)
    }
    val timebox = state.timebox
    val remaining = state.timeboxRemainingMs
    if (timebox != null && remaining != null) {
        StatusLine("${timebox.label} · ${formatMinutesLeft(remaining)}${if (timebox.status == TimeboxStatus.PAUSED) " · paused" else ""}", onTimebox)
    }
}

@Composable
private fun StatusLine(text: String, onClick: () -> Unit) {
    Text(text, style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "lnum, tnum"),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(onClick = onClick).padding(vertical = 20.dp))
}

@Composable
private fun rememberNotificationRequest(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    return {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@Composable
fun TimeboxStartSheet(lastMinutes: Int, recentPurposes: List<String>, boundApp: AppModel?,
    onStart: (minutes: Int, purpose: String?) -> Unit, onDismiss: () -> Unit) {
    val presets = listOf(5, 15, 25, 45, 60)
    var minutes by remember { mutableStateOf(lastMinutes.coerceIn(1, 180)) }
    var custom by remember { mutableStateOf(if (lastMinutes in presets) "" else lastMinutes.toString()) }
    var purpose by remember { mutableStateOf("") }
    val requestNotifications = rememberNotificationRequest()
    YogaSheet(onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Text(if (boundApp == null) "Timebox" else "Timebox ${boundApp.displayLabel}", style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = ScreenMargin, vertical = 12.dp))
            YogaNote("Set a bounded session. You'll get a gentle note when it ends.")
            YogaChipRow { presets.forEach { preset -> YogaChip("$preset min", custom.isEmpty() && minutes == preset,
                onClick = { minutes = preset; custom = "" }) } }
            YogaTextField(custom, { value -> custom = value.filter(Char::isDigit).take(3); custom.toIntOrNull()?.let { minutes = it.coerceIn(1, 180) } },
                "Custom minutes (1–180)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            YogaTextField(purpose, { purpose = it.take(40) }, "What for? (optional)")
            if (recentPurposes.isNotEmpty()) YogaChipRow {
                recentPurposes.take(3).forEach { recent -> YogaChip(recent, purpose == recent, onClick = { purpose = recent }) }
            }
            YogaPrimaryButton("Start $minutes min${if (boundApp != null) " and open" else ""}", {
                requestNotifications(); onStart(minutes, purpose); onDismiss()
            }, Modifier.fillMaxWidth().padding(horizontal = ScreenMargin, vertical = 12.dp))
        }
    }
}

@Composable
fun TimeboxControlSheet(session: TimeboxSessionEntity, remainingMs: Long, onPause: () -> Unit, onResume: () -> Unit,
    onStop: () -> Unit, onExtend: (Int) -> Unit, onDismiss: () -> Unit) {
    val paused = session.status == TimeboxStatus.PAUSED
    YogaSheet(onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Text(session.label, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = ScreenMargin, vertical = 12.dp))
            Text(formatMinutesLeft(remainingMs) + if (paused) " · paused" else "", style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "lnum, tnum"),
                color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(horizontal = ScreenMargin, vertical = 8.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = ScreenMargin), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                YogaSecondaryButton(if (paused) "Resume" else "Pause", { if (paused) onResume() else onPause(); onDismiss() }, Modifier.weight(1f))
                YogaSecondaryButton("+5 min", { onExtend(5); onDismiss() }, Modifier.weight(1f))
            }
            YogaTextButton("Stop", { onStop(); onDismiss() }, Modifier.padding(horizontal = TextButtonInset), muted = true)
        }
    }
}

@Composable
fun TimeboxCompletionSheet(session: TimeboxSessionEntity, streak: Int, onDone: () -> Unit, onExtend: (Int) -> Unit, onStartFocus: () -> Unit) {
    YogaSheet(onDone) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Text("Timebox done", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = ScreenMargin, vertical = 12.dp))
            YogaNote("${session.plannedMinutes + session.extendedMinutes} minutes of ${session.label.lowercase()}.${if (streak > 1) " $streak-day streak." else ""}")
            YogaPrimaryButton("Stop here", onDone, Modifier.fillMaxWidth().padding(horizontal = ScreenMargin, vertical = 8.dp))
            YogaChipRow {
                YogaChip("+5 min", false, onClick = { onExtend(5) })
                YogaChip("+10 min", false, onClick = { onExtend(10) })
                YogaChip("Focus", false, onClick = { onDone(); onStartFocus() })
            }
        }
    }
}

@Composable
fun FocusStartSheet(lastMinutes: Int, onStart: (Int?) -> Unit, onDismiss: () -> Unit) {
    val options = listOf(25, 45, 60, 90, 0)
    var selected by remember { mutableStateOf(if (lastMinutes in options) lastMinutes else 45) }
    val requestNotifications = rememberNotificationRequest()
    YogaSheet(onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Text("Focus", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = ScreenMargin, vertical = 12.dp))
            YogaNote("Distracting apps step aside and ask for a longer pause. Essential apps stay available.")
            YogaChipRow { options.forEach { option -> YogaChip(if (option == 0) "Until I stop" else "$option min", selected == option,
                onClick = { selected = option }) } }
            YogaPrimaryButton("Start focus", { requestNotifications(); onStart(selected.takeIf { it > 0 }); onDismiss() },
                Modifier.fillMaxWidth().padding(horizontal = ScreenMargin, vertical = 12.dp))
        }
    }
}

@Composable
fun FocusControlSheet(state: FocusUiState, onEnd: () -> Unit, onDismiss: () -> Unit) {
    val focus = state.focus ?: return
    YogaSheet(onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Text("Focus is on", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = ScreenMargin, vertical = 12.dp))
            val minutes = ((state.now - focus.startAt) / 60_000).coerceAtLeast(0)
            Text("$minutes min so far${focus.plannedEndAt?.let { " · ${formatMinutesLeft(it - state.now)}" } ?: ""}",
                style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "lnum, tnum"), color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(horizontal = ScreenMargin, vertical = 12.dp))
            YogaPrimaryButton("End focus", { onEnd(); onDismiss() }, Modifier.fillMaxWidth().padding(horizontal = ScreenMargin)
                .semantics { contentDescription = "End focus now" })
        }
    }
}
