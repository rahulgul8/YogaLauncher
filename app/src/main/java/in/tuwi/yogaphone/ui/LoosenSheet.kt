package `in`.tuwi.yogaphone.ui

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.resist.ResistanceLadder
import `in`.tuwi.yogaphone.ui.theme.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A button that acts only after it is held for [ResistanceLadder.HOLD_MILLIS]. TalkBack users get [actionLabel]
 * as the click action, so the hold never locks anyone out.
 */
@Composable
fun YogaHoldButton(text: String, actionLabel: String, enabled: Boolean, onHeld: () -> Unit, modifier: Modifier = Modifier) {
    var progress by remember { mutableFloatStateOf(0f) }
    val current by rememberUpdatedState(onHeld)
    val fill = MaterialTheme.colorScheme.primary
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                if (!enabled) disabled()
                onClick(label = actionLabel) { if (enabled) { current(); true } else false }
            }
            .pointerInput(enabled) {
                if (enabled) detectTapGestures(onPress = {
                    coroutineScope {
                        val hold = launch {
                            val start = SystemClock.elapsedRealtime()
                            while (true) {
                                progress = ((SystemClock.elapsedRealtime() - start).toFloat() / ResistanceLadder.HOLD_MILLIS).coerceIn(0f, 1f)
                                if (progress >= 1f) { current(); break }
                                delay(16)
                            }
                        }
                        try { tryAwaitRelease() } finally { hold.cancel(); progress = 0f }
                    }
                })
            }) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text, style = MaterialTheme.typography.labelLarge,
                color = if (enabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant)
            Canvas(Modifier.fillMaxWidth().padding(top = 6.dp).height(2.dp)) {
                drawLine(fill, Offset.Zero, Offset(size.width * progress, 0f), strokeWidth = size.height)
            }
        }
    }
}

/**
 * Making a rule looser (turning a block off, raising a limit) waits [ResistanceLadder.LOOSEN_SECONDS] and takes a
 * hold. Keeping the rule is the primary, one-tap action. Tightening never comes here.
 */
@Composable
fun LoosenSheet(title: String, detail: String?, confirmText: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    var remaining by remember { mutableIntStateOf(ResistanceLadder.LOOSEN_SECONDS) }
    LaunchedEffect(Unit) { while (remaining > 0) { delay(1_000); remaining-- } }
    YogaSheet(onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = ScreenMargin).padding(bottom = 24.dp)) {
            YogaEyebrow("Take a breath")
            Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 8.dp))
            detail?.let { Text(it, style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "lnum, tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp)) }
            Text(if (remaining > 0) "You can change it in $remaining s" else "You can change it now",
                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "lnum, tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp).semantics { liveRegion = LiveRegionMode.Polite })
            Spacer(Modifier.height(20.dp))
            YogaPrimaryButton("Keep it", onDismiss, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            YogaHoldButton("Hold to $confirmText", confirmText.replaceFirstChar { it.uppercase() }, remaining == 0,
                { onConfirm(); onDismiss() })
        }
    }
}
