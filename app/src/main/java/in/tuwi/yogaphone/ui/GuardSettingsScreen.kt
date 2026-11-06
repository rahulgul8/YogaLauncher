package `in`.tuwi.yogaphone.ui

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import `in`.tuwi.yogaphone.guard.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Keep date-sensitive counts correct even when this screen stays open across midnight. */
@Composable
internal fun rememberGuardDate(): String {
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                date = LocalDate.now().toString()
                delay(1_000)
            }
        }
    }
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
    var disclosure by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) enabled = YogaGuardService.isEnabled(context)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    fun updateSurface(surface: GuardSurface, transform: (SurfaceSettings) -> SurfaceSettings) {
        scope.launch { prefs.update { it.copy(surfaces = it.surfaces + (surface to transform(it.surface(surface)))) } }
    }
    LazyColumn {
        item { SettingsHeader("Shorts and live time value", onBack) }
        item {
            ListItem(headlineContent = { Text("Yoga guard · ${if (enabled) "on" else "off"}") },
                supportingContent = { Text("Optional accessibility service for live time value and Shorts/Reels blocking") })
            TextButton(onClick = { disclosure = true }, modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(if (enabled) "Manage Yoga guard" else "Enable Yoga guard")
            }
        }
        if (Build.VERSION.SDK_INT >= 33) item {
            Text("If Yoga was not installed from Play, you may first need App info → ⋮ → Allow restricted settings.",
                modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall)
        }
        error?.let { message -> item { Text(message, modifier = Modifier.padding(16.dp)) } }
        item { SectionTitle("Live time value") }
        item { UsageToggle("Show live indicator over apps", p.liveIndicator, { value -> scope.launch { prefs.update { it.copy(liveIndicator = value) } } }) }
        item {
            Text("Needs Yoga guard and Value of time. Illustrative, not money lost or spent. Hidden in sensitive apps, on calls, and while locked.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            if (!timeValueEnabled) Text("Value of time is currently off.", modifier = Modifier.padding(horizontal = 16.dp))
            TextButton(onClick = onValueOfTime, modifier = Modifier.padding(horizontal = 16.dp)) { Text("Value, style and start counting") }
        }
        item { SectionTitle("Shorts and Reels") }
        item { Text("Turn on only the surfaces you want Yoga to recognise. Regular videos still work. Browser detection is optional and off by default.", modifier = Modifier.padding(16.dp)) }
        GuardSurface.entries.forEach { surface ->
            val setting = p.surface(surface)
            val label = when (surface) {
                GuardSurface.YOUTUBE -> "YouTube Shorts"
                GuardSurface.INSTAGRAM -> "Instagram Reels"
                GuardSurface.BROWSER_YOUTUBE -> "YouTube Shorts in browsers"
                GuardSurface.BROWSER_INSTAGRAM -> "Instagram Reels in browsers"
            }
            item(key = surface.name) {
                UsageToggle(label, setting.enabled, { value -> updateSurface(surface) { it.copy(enabled = value) } })
                if (setting.enabled) {
                    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(setting.allowanceMinutes == 0, { updateSurface(surface) { it.copy(allowanceMinutes = 0) } }, label = { Text("Block") })
                        FilterChip(setting.allowanceMinutes > 0, { updateSurface(surface) { it.copy(allowanceMinutes = it.lastAllowanceMinutes) } }, label = { Text("Allow daily") })
                    }
                    if (setting.allowanceMinutes > 0) {
                        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(5, 10, 15, 30).forEach { minutes ->
                                FilterChip(setting.allowanceMinutes == minutes,
                                    { updateSurface(surface) { it.copy(allowanceMinutes = minutes, lastAllowanceMinutes = minutes) } },
                                    label = { Text("$minutes min") })
                            }
                        }
                        val used = if (daily.date == today) daily.usedMillis[surface] ?: 0L else 0L
                        Text("${used / 60_000} of ${setting.allowanceMinutes} minutes used today", modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
                Text("Blocked ${blocks[surface] ?: 0} times today", style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }
        item { Text("Selected browser surfaces use the address bar in Chrome, Samsung Internet and Firefox. Recognition can change after app updates.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall) }
        item { Text("Shorts and Reels blocked today: ${blocks.values.sum()}", modifier = Modifier.padding(16.dp)) }
    }
    if (disclosure) AlertDialog(
        onDismissRequest = { disclosure = false },
        title = { Text("Allow Yoga guard accessibility access?") },
        text = {
            Text("Yoga guard reads which app is open to show the optional live time-value indicator. For the surfaces you enable, it reads the screen structure of YouTube, Instagram and the selected browsers (Chrome, Samsung Internet and Firefox), only to recognise Shorts and Reels. It can go back or home to block those surfaces.\n\nScreen content is never stored, logged or sent anywhere. Only your settings, daily allowance time and block counts are saved on this phone. You can turn Yoga guard off any time in Android accessibility settings.",
                modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()))
        },
        confirmButton = {
            TextButton(onClick = {
                disclosure = false
                runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                    .onFailure { error = "Couldn't open accessibility settings. Open Android Settings → Accessibility → Yoga guard." }
            }) { Text("Agree and open settings") }
        },
        dismissButton = { TextButton(onClick = { disclosure = false }) { Text("Not now") } },
    )
}
