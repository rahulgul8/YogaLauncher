package `in`.tuwi.yogaphone.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import `in`.tuwi.yogaphone.YogaApplication
import `in`.tuwi.yogaphone.intro.*
import `in`.tuwi.yogaphone.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SuggestionsScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as YogaApplication
    val signals by app.introSignals.signals.collectAsState()
    val state by app.introStore.state.collectAsState(initial = IntroState())
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { app.introSignals.refresh() }
    val suggestions = signals?.let { current -> IntroEngine.candidates(IntroSurface.USAGE, current).filter { intro ->
        state.records[intro.id]?.let { !it.acted && (it.retired || it.snoozedUntil != null) } == true
    }.distinctBy { it.id } }.orEmpty()
    YogaScreen("Suggestions", onBack) {
        if (suggestions.isEmpty()) item { YogaNote("Nothing to suggest right now.") }
        suggestions.forEach { intro -> item(key = intro.id.name) {
            IntroCard(intro, onAction = { scope.launch { runCatching { app.introActions.request(intro) }.onFailure { app.introActions.failure() } } }, onDismiss = null)
        } }
    }
}
