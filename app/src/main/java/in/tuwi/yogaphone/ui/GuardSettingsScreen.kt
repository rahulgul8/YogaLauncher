package `in`.tuwi.yogaphone.ui

import `in`.tuwi.yogaphone.pro.*
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import `in`.tuwi.yogaphone.guard.*
import `in`.tuwi.yogaphone.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
internal fun rememberGuardDate(): String {
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner) { owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        while (true) { date = LocalDate.now().toString(); delay(1_000) }
    } }
    return date
}

@Composable
fun GuardSettingsScreen(prefs: GuardPrefs, timeValueEnabled: Boolean, onValueOfTime: () -> Unit,
    onLearning: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val p by prefs.preferences.collectAsState(initial = GuardPreferences())
    val daily by prefs.daily.collectAsState(initial = GuardDaily())
    val today = rememberGuardDate()
    val blocks = daily.blocks.takeIf { daily.date == today }.orEmpty()
    var enabled by remember { mutableStateOf(YogaGuardService.isEnabled(context)) }
    var disclosure by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, context) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) enabled = YogaGuardService.isEnabled(context) }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    fun updateSurface(surface: GuardSurface, transform: (SurfaceSettings) -> SurfaceSettings) {
        scope.launch { prefs.update { it.copy(surfaces = it.surfaces + (surface to transform(it.surface(surface)))) } }
    }

    YogaScreen("Shorts and live time value", onBack) {
        item {
            YogaRow("Yoga guard · ${if (enabled) "on" else "off"}", summary = "Optional accessibility service for live time value and Shorts/Reels blocking")
            YogaTextButton(if (enabled) "Manage Yoga guard" else "Enable Yoga guard", { disclosure = true }, Modifier.padding(horizontal = TextButtonInset))
        }
        if (Build.VERSION.SDK_INT >= 33) item { YogaNote("If Yoga was not installed from Play, you may first need App info → ⋮ → Allow restricted settings.") }
        error?.let { message -> item { YogaNote(message) } }
        item { YogaSection("Live time value") }
        item { ProSwitchRow("Show live indicator over apps", ProFeature.VALUE_OF_TIME, p.liveIndicator, { value -> scope.launch { prefs.update { it.copy(liveIndicator = value) } } }) }
        item {
            YogaNote("Needs Yoga guard and Value of time. Illustrative, not money lost or spent. Hidden in sensitive apps, on calls, and while locked.")
            if (!timeValueEnabled) YogaNote("Value of time is currently off.")
            ProRow("Value, style and start counting", ProFeature.VALUE_OF_TIME, onClick = onValueOfTime)
        }
        item { YogaSection("Shorts and Reels") }
        item { ProRow("Learning videos", ProFeature.LEARNING_VIDEOS, "Allow chosen YouTube channels without using allowance", onClick = onLearning) }
        item { YogaNote("Turn on only the surfaces you want Yoga to recognise. Regular videos still work. Browser detection is optional and off by default.") }
        GuardSurface.entries.forEach { surface ->
            val setting = p.surface(surface)
            val label = when (surface) {
                GuardSurface.YOUTUBE -> "YouTube Shorts"
                GuardSurface.INSTAGRAM -> "Instagram Reels"
                GuardSurface.BROWSER_YOUTUBE -> "YouTube Shorts in browsers"
                GuardSurface.BROWSER_INSTAGRAM -> "Instagram Reels in browsers"
            }
            item(key = surface.name) {
                YogaSwitchRow(label, setting.enabled, { value -> updateSurface(surface) { it.copy(enabled = value) } })
                if (setting.enabled) {
                    YogaChipRow {
                        YogaChip("Block", setting.allowanceMinutes == 0, onClick = { updateSurface(surface) { it.copy(allowanceMinutes = 0) } })
                        YogaChip("Allow daily", setting.allowanceMinutes > 0, onClick = { updateSurface(surface) { it.copy(allowanceMinutes = it.lastAllowanceMinutes) } })
                    }
                    if (setting.allowanceMinutes > 0) {
                        YogaChipRow {
                            listOf(5, 10, 15, 30).forEach { minutes ->
                                YogaChip("$minutes min", setting.allowanceMinutes == minutes,
                                    onClick = { updateSurface(surface) { it.copy(allowanceMinutes = minutes, lastAllowanceMinutes = minutes) } })
                            }
                        }
                        val used = if (daily.date == today) daily.usedMillis[surface] ?: 0L else 0L
                        YogaNote("${used / 60_000} of ${setting.allowanceMinutes} minutes used today")
                    }
                }
                YogaNote("Blocked ${blocks[surface] ?: 0} times today")
            }
        }
        item { YogaNote("Selected browser surfaces use the address bar in Chrome, Samsung Internet and Firefox. Recognition can change after app updates.") }
        item { YogaCard { YogaStat(blocks.values.sum().toString(), "Shorts and Reels blocked today") } }
    }
    if (disclosure) YogaDialog(
        title = "Allow Yoga guard accessibility access?", confirmText = "Agree and open settings",
        onConfirm = {
            disclosure = false
            runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                .onFailure { error = "Couldn't open accessibility settings. Open Android Settings → Accessibility → Yoga guard." }
        },
        onDismissRequest = { disclosure = false }, dismissText = "Not now",
        text = { Text("Yoga guard reads which app is open to show the optional live time-value indicator. For the surfaces you enable, it reads the screen structure of YouTube, Instagram and the selected browsers (Chrome, Samsung Internet and Firefox) to recognise Shorts and Reels. For Learning videos, it reads the YouTube channel name and video title in memory so it can match channels you added and offer an optional title hint. It can go back or home to block those surfaces during focus.\n\nTitles are never stored. Screen content is never logged or sent anywhere. Only your settings, channels you add, daily learning totals, daily allowance time and block counts are saved on this phone. You can turn Yoga guard off any time in Android accessibility settings.", modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) },
    )
}
