package `in`.tuwi.yogaphone.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import `in`.tuwi.yogaphone.YogaApplication
import `in`.tuwi.yogaphone.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun IntroActionHost(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as YogaApplication
    val actions = app.introActions
    val scope = rememberCoroutineScope()
    val disclosure by actions.disclosure.collectAsState()
    val notice by actions.notice.collectAsState()
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) scope.launch { runCatching { actions.resume() }.onFailure { actions.failure() } }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { runCatching { actions.resume() }.onFailure { actions.failure() } }
    // Confirmations clear themselves; Undo stays reachable for a few seconds.
    LaunchedEffect(notice) { val shown = notice ?: return@LaunchedEffect; kotlinx.coroutines.delay(6_000); if (actions.notice.value === shown) actions.clearNotice() }
    Box(Modifier.fillMaxSize()) {
        content()
        notice?.let { message ->
            Snackbar(modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(ScreenMargin),
                action = { YogaTextButton(message.actionLabel ?: "OK", {
                    actions.clearNotice()
                    scope.launch { runCatching { message.action?.invoke() }.onFailure { actions.failure() } }
                }) }, dismissAction = { YogaTextButton("Dismiss", actions::clearNotice) }) { Text(message.text) }
        }
    }
    if (disclosure != null) GuardDisclosureDialog(onConfirm = {
        scope.launch {
            runCatching {
                if (actions.agree()) runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                    .onFailure { actions.settingsFailure() }
            }.onFailure { actions.failure() }
        }
    }, onDismissRequest = actions::closeDisclosure)
}
