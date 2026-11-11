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
import `in`.tuwi.yogaphone.YogaApplication
import `in`.tuwi.yogaphone.review.FeedbackFeature

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
fun GuardSettingsScreen(prefs: GuardPrefs, timeValueEnabled: Boolean, onValueOfTime: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val p by prefs.preferences.collectAsState(initial = GuardPreferences())
    val daily by prefs.daily.collectAsState(initial = GuardDaily())
    val today = rememberGuardDate()
    val blocks = daily.blocks.takeIf { daily.date == today }.orEmpty()
    var enabled by remember { mutableStateOf(YogaGuardService.isEnabled(context)) }
    var lastEnabled by remember { mutableStateOf(enabled) }
    var guardFeedback by remember { mutableStateOf(false) }
    val review = (context.applicationContext as YogaApplication).reviewController
    var disclosure by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, context) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) {
            val next = YogaGuardService.isEnabled(context)
            if (next != lastEnabled) scope.launch {
                if (review.featureChanged(FeedbackFeature.GUARD, next)) guardFeedback = true
            }
            lastEnabled = next
            enabled = next
        } }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val todayDate = LocalDate.parse(today)
    val strictNow = isStrict(p, todayDate)
    // A "strict off" that has reached its day becomes plain preferences again.
    LaunchedEffect(p.strict, p.strictOffFrom, today) {
        if (strictEnded(p, todayDate)) prefs.update { it.copy(strict = false, strictOffFrom = null) }
    }
    var loosen by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    var strictNote by remember { mutableStateOf(false) }
    fun updateSurface(surface: GuardSurface, transform: (SurfaceSettings) -> SurfaceSettings) {
        val previous = p.surface(surface)
        val next = transform(previous)
        val apply = { scope.launch { prefs.update { it.copy(surfaces = it.surfaces + (surface to transform(it.surface(surface)))) } }; Unit }
        when {
            !isLooser(previous, next) -> apply()
            strictNow -> strictNote = true
            else -> loosen = (if (!next.enabled) "Turn this block off?" else "Allow more each day?") to apply
        }
    }

    YogaScreen("Shorts and live time value", onBack) {
        item {
            YogaRow("Yoga guard · ${if (enabled) "on" else "off"}", summary = "Optional accessibility service for live time value and Shorts/Reels blocking")
            YogaTextButton(if (enabled) "Manage Yoga guard" else "Enable Yoga guard", {
                // Turning the guard off happens in Android settings; going there is the loosening step.
                if (enabled) loosen = "Turn Yoga guard off?" to { disclosure = true } else disclosure = true
            }, Modifier.padding(horizontal = TextButtonInset))
        }
        if (guardFeedback) item {
            FeedbackQuestion(FeedbackFeature.GUARD, listOf("Blocked too much", "Didn't block enough", "Something else broke")) { guardFeedback = false }
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
        item { YogaNote("Turn on only the surfaces you want Yoga to recognise. Regular videos still work. Browser detection is optional and off by default.") }
        item {
            YogaSection("When I open Shorts or Reels")
            YogaRadioRow("Let me through after a pause", !p.strict, {
                // Leaving strict mode is a loosening: it waits until tomorrow.
                if (p.strict && p.strictOffFrom == null) scope.launch { prefs.update { it.copy(strictOffFrom = strictOffFrom(todayDate)) } }
            }, summary = "A wait that grows, then you choose 5, 10 or 15 minutes")
            YogaRadioRow("Never (strict)", p.strict && p.strictOffFrom == null, {
                scope.launch { prefs.update { it.copy(strict = true, strictOffFrom = null) } }
            }, summary = "No way through. Looser settings wait until strict mode ends")
            if (p.strict && p.strictOffFrom != null) {
                YogaNote("Strict mode ends tomorrow.")
                YogaTextButton("Keep strict mode", { scope.launch { prefs.update { it.copy(strictOffFrom = null) } } },
                    Modifier.padding(horizontal = TextButtonInset))
            }
            if (strictNote) YogaNote("Strict mode is on, so looser settings wait until it ends. Choose \"Let me through after a pause\" to end it tomorrow.")
        }
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
        val resisted = daily.resisted.takeIf { daily.date == today }.orEmpty().values.sum()
        if (resisted > 0) item { YogaNote("You said not now $resisted ${if (resisted == 1) "time" else "times"} today.") }
    }
    loosen?.let { (title, apply) ->
        LoosenSheet(title, resisted(daily, today).takeIf { it > 0 }?.let { "You said not now ${if (it == 1) "once" else "$it times"} today." },
            "change it", apply) { loosen = null }
    }
    if (disclosure) GuardDisclosureDialog(
        onConfirm = {
            disclosure = false
            runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                .onFailure { error = "Couldn't open accessibility settings. Open Android Settings → Accessibility → Yoga guard." }
        },
        onDismissRequest = { disclosure = false },
    )
}

private fun resisted(daily: GuardDaily, today: String) = daily.resisted.takeIf { daily.date == today }.orEmpty().values.sum()
