package `in`.tuwi.yogaphone.ui

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.data.AppModel
import `in`.tuwi.yogaphone.YogaApplication
import `in`.tuwi.yogaphone.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun MindfulDelayScreen(
    app: AppModel,
    seconds: Int,
    duringFocus: Boolean,
    usageLine: String?,
    recentPurposes: List<String>,
    onContinue: (purpose: String?) -> Unit,
    onGoBack: () -> Unit,
) {
    val context = LocalContext.current
    val reducedMotion = remember {
        runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }.getOrDefault(false)
    }
    var remaining by remember(app.appPackage) { mutableIntStateOf(seconds) }
    var purpose by remember(app.appPackage) { mutableStateOf("") }
    LaunchedEffect(app.appPackage) { (context.applicationContext as YogaApplication).reviewController.recordInterruption() }
    LaunchedEffect(app.appPackage) { while (remaining > 0) { delay(1_000); remaining-- } }
    BackHandler { onGoBack() }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = ScreenMargin, vertical = 24.dp),
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
            usageLine?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "lnum, tnum")) }
            YogaTextField(purpose, { purpose = it.take(40) }, "What for? (optional)", Modifier.padding(top = 16.dp))
            if (recentPurposes.isNotEmpty()) YogaChipRow {
                recentPurposes.take(3).forEach { recent -> YogaChip(recent, purpose == recent, onClick = { purpose = recent }) }
            }
            Spacer(Modifier.weight(1f))
            YogaPrimaryButton("Not now", onGoBack, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            YogaSecondaryButton("Open ${app.displayLabel}", { onContinue(purpose) },
                Modifier.fillMaxWidth(), enabled = remaining <= 0)
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
