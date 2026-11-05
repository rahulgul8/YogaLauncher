package `in`.tuwi.yogaphone.ui

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.data.AppModel
import kotlinx.coroutines.delay

/**
 * Intervention level 2: a short pause before a distracting app opens. "Not now" is the primary action;
 * opening is enabled once the countdown ends.
 */
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
        runCatching { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
            .getOrDefault(false)
    }
    var remaining by remember(app.appPackage) { mutableIntStateOf(seconds) }
    var purpose by remember(app.appPackage) { mutableStateOf("") }
    LaunchedEffect(app.appPackage) {
        while (remaining > 0) { delay(1_000); remaining-- }
    }
    BackHandler { onGoBack() }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Text(if (duringFocus) "Focus is on" else "A short pause", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(app.displayLabel, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp))
            Box(Modifier.padding(vertical = 32.dp).size(160.dp), contentAlignment = Alignment.Center) {
                BreathingRing(animated = !reducedMotion)
                Text(
                    if (remaining > 0) "$remaining" else "Breathe",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = if (remaining > 0) "$remaining seconds" else "Ready"
                    },
                )
            }
            usageLine?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
            OutlinedTextField(
                value = purpose,
                onValueChange = { purpose = it.take(40) },
                label = { Text("What for? (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            )
            if (recentPurposes.isNotEmpty()) Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) { recentPurposes.forEach { AssistChip(onClick = { purpose = it }, label = { Text(it) }) } }
            Spacer(Modifier.weight(1f))
            Button(onClick = onGoBack, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Not now") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { onContinue(purpose) },
                enabled = remaining <= 0,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text("Open ${app.displayLabel}") }
        }
    }
}

@Composable
private fun BreathingRing(animated: Boolean) {
    val color = MaterialTheme.colorScheme.primary
    val scale = if (animated) {
        val transition = rememberInfiniteTransition(label = "breath")
        transition.animateFloat(0.7f, 1f, infiniteRepeatable(tween(4_000), RepeatMode.Reverse), label = "scale").value
    } else 1f
    Canvas(Modifier.fillMaxSize()) {
        drawCircle(color = color.copy(alpha = 0.35f), radius = size.minDimension / 2 * scale, style = Stroke(width = 3.dp.toPx()))
    }
}
