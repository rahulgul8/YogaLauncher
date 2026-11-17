package `in`.tuwi.yogaphone.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import `in`.tuwi.yogaphone.YogaApplication
import `in`.tuwi.yogaphone.insights.DayAchievements
import `in`.tuwi.yogaphone.insights.MorningNoteController
import `in`.tuwi.yogaphone.ui.theme.ScreenMargin
import `in`.tuwi.yogaphone.ui.theme.YogaEyebrow
import `in`.tuwi.yogaphone.ui.theme.YogaSheet
import `in`.tuwi.yogaphone.ui.theme.YogaTextButton
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

/** The morning note about yesterday, shown on the first home view of the day. */
@Composable
fun MorningNote(app: YogaApplication) {
    val controller = remember { MorningNoteController(app) }
    val scope = rememberCoroutineScope()
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { controller.refresh() }
    }
    val note by controller.note.collectAsState()
    var openNote by remember { mutableStateOf<DayAchievements?>(null) }

    note?.let { value ->
        MorningPromptLine(
            text = value.headline(),
            action = "See",
            onAction = { openNote = value; scope.launch { controller.close() } },
            secondaryAction = "Not now",
            onSecondaryAction = { scope.launch { controller.close() } },
            onLongPress = { scope.launch { controller.close() } },
        )
    }

    openNote?.let { value ->
        MorningSheet(value) { openNote = null }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MorningPromptLine(
    text: String,
    action: String?,
    onAction: () -> Unit,
    secondaryAction: String? = null,
    onSecondaryAction: () -> Unit = {},
    onLongPress: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 4.dp)
            .combinedClickable(onClick = onAction, onLongClick = onLongPress),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (secondaryAction != null) YogaTextButton(secondaryAction, onSecondaryAction, muted = true)
        if (action != null) YogaTextButton(action, onAction)
    }
}

@Composable
private fun MorningSheet(note: DayAchievements, onDismiss: () -> Unit) {
    YogaSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = ScreenMargin).padding(bottom = 28.dp)) {
            val eyebrow = remember(note) {
                note.date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH))
            }
            YogaEyebrow(eyebrow, Modifier.padding(top = 8.dp, bottom = 10.dp))
            Text("Yesterday", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.size(20.dp))
            note.lines().forEach { line ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(5.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Text(line, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            YogaTextButton("Done", onDismiss, Modifier.align(Alignment.End))
        }
    }
}
