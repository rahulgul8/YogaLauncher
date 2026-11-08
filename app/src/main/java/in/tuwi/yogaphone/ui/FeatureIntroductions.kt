package `in`.tuwi.yogaphone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import `in`.tuwi.yogaphone.YogaApplication
import `in`.tuwi.yogaphone.intro.*
import `in`.tuwi.yogaphone.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
private fun rememberIntroduction(surface: IntroSurface): Intro? {
    val app = LocalContext.current.applicationContext as YogaApplication
    val owner = LocalLifecycleOwner.current
    var held by remember(surface) { mutableStateOf<Intro?>(null) }
    LaunchedEffect(app, owner, surface) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var heldDate: LocalDate? = null
            app.introSignals.refresh()
            try {
                val clock = flow { while (true) { emit(LocalDate.now()); delay(30_000) } }
                combine(app.introSignals.signals, app.introStore.state, clock) { signals, state, today -> Triple(signals, state, today) }
                    .collect { (signals, state, today) ->
                        if (signals == null) { held = null; return@collect }
                        val current = held
                        val record = current?.let { state.records[it.id] }
                        val relevant = current != null && IntroEngine.candidates(surface, signals).any {
                            it.id == current.id && it.targetSurface == current.targetSurface && it.targetMinutes == current.targetMinutes
                        }
                        if (heldDate != today || !relevant || record?.acted == true || record?.retired == true ||
                            record?.snoozedUntil?.let { today.isBefore(it) } == true) held = null
                        if (held == null) {
                            held = app.introStore.claim(surface, signals, today)
                            heldDate = today
                        }
                    }
            } finally { held = null }
        }
    }
    return held
}

@Composable
fun UsageIntroduction() {
    val intro = rememberIntroduction(IntroSurface.USAGE) ?: return
    val app = LocalContext.current.applicationContext as YogaApplication
    val scope = rememberCoroutineScope()
    IntroCard(intro, onAction = { scope.launch { runCatching { app.introActions.request(intro) }.onFailure { app.introActions.failure() } } },
        onDismiss = { scope.launch { app.introStore.dismiss(intro.id) } })
}

@Composable
fun HomeIntroduction() {
    val intro = rememberIntroduction(IntroSurface.HOME) ?: return
    val app = LocalContext.current.applicationContext as YogaApplication
    val scope = rememberCoroutineScope()
    val act = { scope.launch { runCatching { app.introActions.request(intro) }.onFailure { app.introActions.failure() } }; Unit }
    Column {
        Row(Modifier.fillMaxWidth().clickable(onClick = act).padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
            Spacer(Modifier.width(12.dp))
            Text(intro.evidence, style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "lnum, tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            YogaTextButton(intro.actionLabel, act)
        }
        YogaTextButton("Not now", { scope.launch { app.introStore.dismiss(intro.id) } }, muted = true)
    }
}
