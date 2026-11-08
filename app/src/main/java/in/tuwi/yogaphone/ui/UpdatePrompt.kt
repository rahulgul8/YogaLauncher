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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.MainActivity
import `in`.tuwi.yogaphone.update.UpdateController
import `in`.tuwi.yogaphone.update.UpdateUi
import `in`.tuwi.yogaphone.update.WhatsNewEntry
import `in`.tuwi.yogaphone.ui.theme.ScreenMargin
import `in`.tuwi.yogaphone.ui.theme.YogaEyebrow
import `in`.tuwi.yogaphone.ui.theme.YogaSheet
import `in`.tuwi.yogaphone.ui.theme.YogaTextButton

/** Owns every update-related home surface so MainActivity only needs one line. */
@Composable
fun UpdatePrompt(activity: MainActivity, controller: UpdateController) {
    val update by controller.ui.collectAsState()
    val whatsNew by controller.whatsNew.collectAsState()
    var sheetEntry by remember { mutableStateOf<WhatsNewEntry?>(null) }

    Column {
        when (val value = update) {
            UpdateUi.Hidden -> Unit
            is UpdateUi.Ready -> UpdateLine(
                text = "Update ready",
                versionCode = value.versionCode,
                action = "Update",
                onAction = { controller.startFlexible(activity) },
                onDismiss = controller::dismissUpdate,
            )
            is UpdateUi.Downloading -> UpdateLine(
                text = "Downloading update…",
                versionCode = value.versionCode,
                onDismiss = controller::dismissUpdate,
            )
            is UpdateUi.Restart -> UpdateLine(
                text = "Restart to update",
                versionCode = value.versionCode,
                action = "Restart",
                onAction = controller::completeUpdate,
                onDismiss = controller::dismissUpdate,
            )
        }
        whatsNew?.let { ui ->
            HomePromptLine(
                text = "New in ${ui.entry.versionName} ·",
                action = "See what's new",
                onAction = { sheetEntry = ui.entry; controller.markWhatsNewSeen() },
                onLongPress = controller::markWhatsNewSeen,
            )
        }
    }

    sheetEntry?.let { entry ->
        WhatsNewSheet(entry.versionName, entry.lines) {
            controller.markWhatsNewSeen()
            sheetEntry = null
        }
    }
}

@Composable
private fun UpdateLine(
    text: String,
    versionCode: Int,
    action: String? = null,
    onAction: () -> Unit = {},
    onDismiss: (Int) -> Unit,
) {
    HomePromptLine(
        text = text,
        action = action,
        onAction = onAction,
        secondaryAction = "Not now",
        onSecondaryAction = { onDismiss(versionCode) },
        onLongPress = { onDismiss(versionCode) },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomePromptLine(
    text: String,
    action: String?,
    onAction: () -> Unit,
    secondaryAction: String? = null,
    onSecondaryAction: () -> Unit = {},
    onLongPress: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 4.dp)
            .combinedClickable(onClick = {}, onLongClick = onLongPress),
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
private fun WhatsNewSheet(versionName: String, lines: List<String>, onDismiss: () -> Unit) {
    YogaSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = ScreenMargin).padding(bottom = 28.dp)) {
            // The display serif draws "1" like "I", so the version sits in the sans eyebrow.
            YogaEyebrow("Version $versionName", Modifier.padding(top = 8.dp, bottom = 10.dp))
            Text("What's new", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.size(20.dp))
            lines.take(3).forEach { line ->
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
