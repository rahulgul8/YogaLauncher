package `in`.tuwi.yogaphone.ui

import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import `in`.tuwi.yogaphone.guard.*
import `in`.tuwi.yogaphone.resist.ResistanceLadder
import `in`.tuwi.yogaphone.ui.theme.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ResistScreen(surface: GuardSurface, daily: GuardDaily, lastPassMinutes: Int?, onNotNow: () -> Unit, onPass: (Int) -> Unit) {
    val passes = surface.family().sumOf { daily.passes[it] ?: 0 }
    val choices = ResistanceLadder.choices(passes)
    var minutes by rememberSaveable(surface) { mutableIntStateOf(ResistanceLadder.preselect(lastPassMinutes, choices)) }
    val wait = ResistanceLadder.waitSeconds(ResistanceLadder.SHORTS_BASE_SECONDS, passes, ResistanceLadder.SHORTS_CAP_SECONDS)
    val deadline = rememberSaveable(surface) { SystemClock.elapsedRealtime() + wait * 1_000L }
    var remaining by remember { mutableIntStateOf(wait) }
    LaunchedEffect(deadline) {
        do {
            remaining = ((deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0) + 999).div(1_000).toInt()
            if (remaining > 0) delay(100)
        } while (remaining > 0)
    }
    val context = LocalContext.current
    val reducedMotion = remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    val owner = LocalLifecycleOwner.current
    var resumed by remember { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, _ -> resumed = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    BackHandler(onBack = onNotNow)
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenMargin, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(24.dp))
            YogaEyebrow(surface.shortName())
            Text("Watch anyway?", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 12.dp))
            Box(Modifier.padding(vertical = 28.dp).size(176.dp), contentAlignment = Alignment.Center) {
                ResistBreathingClock(!reducedMotion)
                Text(if (remaining > 0) "$remaining" else "Breathe",
                    style = if (remaining > 0) MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "lnum, tnum") else MaterialTheme.typography.headlineMedium,
                    color = if (remaining > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite; contentDescription = if (remaining > 0) "$remaining seconds" else "Ready" })
            }
            Text("${ResistanceLadder.ordinal(passes + 1)} time today \u00b7 blocked ${times(surface.family().sumOf { daily.blocks[it] ?: 0 })} \u00b7 resisted ${times(surface.family().sumOf { daily.resisted[it] ?: 0 })}",
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "lnum, tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Text("For how long?", modifier = Modifier.padding(top = 24.dp), style = MaterialTheme.typography.titleMedium)
            YogaChipRow { choices.forEach { n -> YogaChip("$n min", minutes == n, { minutes = n }) } }
            Spacer(Modifier.height(24.dp))
            YogaPrimaryButton("Not now", onNotNow, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            HoldToWatch(minutes, remaining == 0 && resumed, onPass)
        }
    }
}

private fun times(n: Int) = when (n) { 1 -> "once"; 2 -> "twice"; else -> "$n times" }

@Composable
private fun HoldToWatch(minutes: Int, enabled: Boolean, onPass: (Int) -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }
    val currentPass by rememberUpdatedState(onPass)
    val color = MaterialTheme.colorScheme.primary
    Surface(shape = CircleShape, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                if (!enabled) disabled()
                onClick(label = "Watch for $minutes minutes") { if (enabled) { currentPass(minutes); true } else false }
            }.pointerInput(enabled, minutes) {
                if (enabled) detectTapGestures(onPress = {
                    coroutineScope {
                        val hold = launch {
                            val start = SystemClock.elapsedRealtime()
                            while (true) {
                                progress = ((SystemClock.elapsedRealtime() - start).toFloat() / ResistanceLadder.HOLD_MILLIS).coerceIn(0f, 1f)
                                if (progress >= 1f) { currentPass(minutes); break }
                                delay(16)
                            }
                        }
                        try { tryAwaitRelease() } finally { hold.cancel(); progress = 0f }
                    }
                })
            }) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Hold to watch for $minutes min", style = MaterialTheme.typography.labelLarge,
                color = if (enabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant)
            // Direct progress, with no interpolating animation, also serves reduced motion.
            Canvas(Modifier.fillMaxWidth().padding(top = 6.dp).height(2.dp)) {
                drawLine(color, androidx.compose.ui.geometry.Offset.Zero, androidx.compose.ui.geometry.Offset(size.width * progress, 0f), strokeWidth = size.height)
            }
        }
    }
}

@Composable
private fun ResistBreathingClock(animated: Boolean) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    val hand = MaterialTheme.colorScheme.primary
    val scale = if (animated) rememberInfiniteTransition(label = "breath").animateFloat(
        0.78f, 1f, infiniteRepeatable(tween(4_000), RepeatMode.Reverse), label = "scale").value else 1f
    Canvas(Modifier.fillMaxSize()) {
        val radius = size.minDimension / 2
        drawCircle(outline, radius = radius, style = Stroke(width = 1.dp.toPx()))
        drawCircle(outline.copy(alpha = .45f), radius = radius * scale, style = Stroke(width = 2.dp.toPx()))
        drawLine(hand, center, center.copy(y = center.y - radius * .66f), strokeWidth = 1.6.dp.toPx())
        drawCircle(hand, radius = 3.dp.toPx(), center = center)
    }
}
