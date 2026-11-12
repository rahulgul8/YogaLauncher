package `in`.tuwi.yogaphone.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.guard.*
import `in`.tuwi.yogaphone.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LearningChannelsScreen(prefs: LearningPrefs, onBack: () -> Unit) {
    val state by prefs.preferences.collectAsState(initial = LearningPreferences())
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val snackbar = remember { SnackbarHostState() }
    var entry by remember { mutableStateOf("") }
    fun add(name: String) {
        if (LearningChannels.normalize(name).isEmpty()) return
        scope.launch { prefs.add(name) }
        entry = ""
        keyboard?.hide()
    }

    Box(Modifier.fillMaxSize()) {
        YogaScreen("Learning videos", onBack, eyebrow = "Yoga Pro") {
            item { YogaSection("Your channels") }
            if (state.channels.isEmpty()) item { YogaNote("No channels yet. Add the exact channel name shown by YouTube.") }
            state.channels.forEach { channel -> item(key = channel.key) {
                YogaRow(channel.displayName, trailing = {
                    YogaTextButton("Remove", {
                        scope.launch {
                            prefs.remove(channel.key)
                            if (snackbar.showSnackbar("Removed ${channel.displayName}", "Undo") == SnackbarResult.ActionPerformed) {
                                prefs.add(channel.displayName)
                            }
                        }
                    }, muted = true)
                })
            } }
            item {
                YogaTextField(entry, { entry = it }, "Add a channel", placeholder = "Channel name",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { add(entry) }))
                YogaTextButton("Add channel", { add(entry) }, Modifier.padding(horizontal = TextButtonInset), enabled = entry.isNotBlank())
            }
            if (state.recent.isNotEmpty()) {
                item { YogaSection("Recently added") }
                item { YogaChipRow { state.recent.forEach { name -> YogaChip(name, false, onClick = { add(name) }) } } }
            }
            item { YogaSection("Starter channels") }
            item { YogaNote("Suggestions only. None are added until you tap them.") }
            item { YogaChipRow {
                LearningPrefs.starterChannels.forEach { name ->
                    val selected = state.channels.any { it.key == LearningChannels.normalize(name) }
                    YogaChip(name, selected, onClick = { if (!selected) add(name) })
                }
            } }
            item { YogaSection("During focus") }
            item { YogaSwitchRow("Allow YouTube home during focus", state.allowYouTubeHomeDuringFocus,
                { scope.launch { prefs.setAllowHome(it) } }, summary = "Off by default; learning channels can still play") }
            item { YogaSwitchRow("Show title hints", state.titleHints, { scope.launch { prefs.setTitleHints(it) } },
                summary = "Offer Count as learning for titles with study or work words") }
            item { YogaNote("Yoga recognises the channel name shown on a YouTube watch page. If it cannot read the channel, the time counts normally. Titles are checked only in memory for the optional hint and are never stored.") }
            item { Spacer(Modifier.height(72.dp)) }
        }
        SnackbarHost(snackbar, Modifier.align(androidx.compose.ui.Alignment.BottomCenter).padding(16.dp))
    }
}
