package `in`.tuwi.yogaphone.ui

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.data.AppModel
import `in`.tuwi.yogaphone.YogaApplication
import `in`.tuwi.yogaphone.resist.ResistanceLadder
import `in`.tuwi.yogaphone.ui.theme.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MindfulDelayScreen(
    app: AppModel,
    seconds: Int,
    duringFocus: Boolean,
    costLine: String,
    allowanceLine: String?,
    resistedToday: Int,
    choices: List<Int>,
    preselected: Int,
    recentPurposes: List<String>,
    onContinue: (purpose: String?, minutes: Int) -> Unit,
    onGoBack: () -> Unit,
) {
    val context = LocalContext.current
    val reducedMotion = remember {
        runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }.getOrDefault(false)
    }
    var remaining by remember(app.appPackage) { mutableIntStateOf(seconds) }
    var purpose by remember(app.appPackage) { mutableStateOf("") }
    var selectedMinutes by remember(app.appPackage, preselected) { mutableIntStateOf(preselected) }
    LaunchedEffect(app.appPackage) { (context.applicationContext as YogaApplication).reviewController.recordInterruption() }
    LaunchedEffect(app.appPackage) { while (remaining > 0) { delay(1_000); remaining-- } }
    // Opened from search, the drawer's keyboard is still up and covers the buttons, so the first hold
    // landed on the keyboard (found on the moto g31). Close it as the pause appears.
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    LaunchedEffect(app.appPackage) { focus.clearFocus(force = true); keyboard?.hide() }
    BackHandler { onGoBack() }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        // Scrolls on small screens and with large text, so the way through is never pushed off the bottom.
        // With an unbounded height, Column shares its weights out of the minimum height, which is the screen.
        BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight).padding(horizontal = ScreenMargin, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            YogaEyebrow(if (duringFocus) "Focus is on" else "A short pause")
            Text(app.displayLabel, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp))
            Box(Modifier.padding(vertical = 36.dp).size(176.dp), contentAlignment = Alignment.Center) {
                BreathingClock(animated = !reducedMotion)
                Text(
                    if (remaining > 0) "$remaining" else "Breathe",
                    style = if (remaining > 0) MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "lnum, tnum") else MaterialTheme.typography.headlineMedium,
                    color = if (remaining > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = if (remaining > 0) "$remaining seconds" else "Ready"
                    },
                )
            }
            Text(costLine, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "lnum, tnum"))
            allowanceLine?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "lnum, tnum"), modifier = Modifier.padding(top = 4.dp)) }
            if (resistedToday > 0) Text("Resisted $resistedToday ${if (resistedToday == 1) "time" else "times"} today",
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "lnum, tnum"), modifier = Modifier.padding(top = 6.dp))
            YogaTextField(purpose, { purpose = it.take(40) }, "What for? (optional)", Modifier.padding(top = 16.dp))
            if (recentPurposes.isNotEmpty()) YogaChipRow {
                recentPurposes.take(3).forEach { recent -> YogaChip(recent, purpose == recent, onClick = { purpose = recent }) }
            }
            Text("For how long?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
            YogaChipRow(Modifier.padding(horizontal = 0.dp)) {
                choices.forEach { minutes -> YogaChip("$minutes min", selectedMinutes == minutes, onClick = { selectedMinutes = minutes }) }
            }
            Spacer(Modifier.weight(1f))
            YogaPrimaryButton("Not now", onGoBack, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            HoldToOpenButton(selectedMinutes, remaining <= 0, reducedMotion,
                onHeld = { onContinue(purpose.trim().takeIf(String::isNotEmpty), selectedMinutes) }, modifier = Modifier.fillMaxWidth())
        }
        }
    }
}

@Composable
private fun HoldToOpenButton(
    minutes: Int,
    enabled: Boolean,
    reducedMotion: Boolean,
    onHeld: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    val currentOnHeld by rememberUpdatedState(onHeld)
    val colours = MaterialTheme.colorScheme
    val gesture = Modifier.pointerInput(enabled, reducedMotion, minutes) {
        detectTapGestures(onPress = press@{
            if (!enabled) return@press
            coroutineScope {
                var completed = false
                val hold = launch {
                    if (reducedMotion) delay(ResistanceLadder.HOLD_MILLIS)
                    else progress.animateTo(1f, tween(ResistanceLadder.HOLD_MILLIS.toInt()))
                    completed = true
                    currentOnHeld()
                }
                tryAwaitRelease()
                if (!completed) {
                    hold.cancel()
                    progress.snapTo(0f)
                }
            }
        })
    }
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, colours.outline),
        modifier = modifier.heightIn(min = 52.dp)
            .then(gesture)
            .semantics {
                role = Role.Button
                if (!enabled) disabled()
                customActions = listOf(CustomAccessibilityAction("Open for $minutes minutes") {
                    if (enabled) { currentOnHeld(); true } else false
                })
            },
    ) {
        Box(Modifier.fillMaxWidth().heightIn(min = 52.dp), contentAlignment = Alignment.Center) {
            Text(
                "Hold to open for $minutes min",
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) colours.onBackground else colours.onSurfaceVariant,
            )
            if (!reducedMotion) Box(
                Modifier.align(Alignment.BottomStart).fillMaxWidth(progress.value).height(2.dp).background(colours.primary),
            )
        }
    }
}

@Composable
private fun BreathingClock(animated: Boolean) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    val hand = MaterialTheme.colorScheme.primary
    val scale = if (animated) rememberInfiniteTransition(label = "breath").animateFloat(
        0.78f, 1f, infiniteRepeatable(tween(4_000), RepeatMode.Reverse), label = "scale").value else 1f
    Canvas(Modifier.fillMaxSize()) {
        val radius = size.minDimension / 2
        drawCircle(outline, radius = radius, style = Stroke(width = 1.dp.toPx()))
        drawCircle(outline.copy(alpha = 0.45f), radius = radius * scale, style = Stroke(width = 2.dp.toPx()))
        drawLine(hand, center, center.copy(y = center.y - radius * .66f), strokeWidth = 1.6.dp.toPx())
        drawCircle(hand, radius = 3.dp.toPx(), center = center)
    }
}
