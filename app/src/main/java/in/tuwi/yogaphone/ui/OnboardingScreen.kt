package `in`.tuwi.yogaphone.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.MainUiState
import `in`.tuwi.yogaphone.data.AppModel
import `in`.tuwi.yogaphone.data.ThemeMode
import `in`.tuwi.yogaphone.ui.theme.ScreenMargin
import `in`.tuwi.yogaphone.ui.theme.YogaCard
import `in`.tuwi.yogaphone.ui.theme.YogaChip
import `in`.tuwi.yogaphone.ui.theme.YogaChipRow
import `in`.tuwi.yogaphone.ui.theme.YogaCheckRow
import `in`.tuwi.yogaphone.ui.theme.YogaEyebrow
import `in`.tuwi.yogaphone.ui.theme.YogaIcons
import `in`.tuwi.yogaphone.ui.theme.YogaNote
import `in`.tuwi.yogaphone.ui.theme.YogaPrimaryButton
import `in`.tuwi.yogaphone.ui.theme.YogaSecondaryButton
import `in`.tuwi.yogaphone.ui.theme.YogaSwitchRow
import `in`.tuwi.yogaphone.ui.theme.YogaTextButton
import `in`.tuwi.yogaphone.ui.theme.YogaTextField
import `in`.tuwi.yogaphone.usage.UsageAccess
import `in`.tuwi.yogaphone.usage.UsageUiState
import kotlinx.coroutines.delay

/** Bump to show the walkthrough again to everyone who finished an older one. */
const val ONBOARDING_VERSION = 2

/** What the walkthrough writes; every step goes straight to the existing settings. */
class OnboardingActions(
    val setHomeApps: (List<AppModel>) -> Unit,
    val setAllowance: (Int?) -> Unit,
    val setDistracting: (String, Boolean) -> Unit,
    val setTimeValue: (Boolean, String) -> Unit,
    val setReminders: (Boolean) -> Unit,
    val setMindfulPause: (Boolean) -> Unit,
    val setGreyOverLimit: (Boolean) -> Unit,
    val setTheme: (ThemeMode) -> Unit,
    val setPalette: (`in`.tuwi.yogaphone.data.Palette) -> Unit,
    val finish: () -> Unit,
)

/** Current settings, so replaying setup starts from what the person already chose. */
data class OnboardingInitial(
    val allowanceMinutes: Int?,
    val returning: Boolean,
    val timeValueOn: Boolean,
    val hourlyValue: String,
    val reminders: Boolean,
    val mindfulPause: Boolean,
    val greyOverLimit: Boolean,
)

private enum class Step { WELCOME, HOME, APPS, USAGE, ALLOWANCE, VALUE, NUDGES, LOOK, DONE }

@Composable
fun OnboardingScreen(
    ui: MainUiState,
    usage: UsageUiState,
    isDistracting: (String) -> Boolean,
    isEssential: (String) -> Boolean,
    initial: OnboardingInitial,
    actions: OnboardingActions,
) {
    var index by remember { mutableIntStateOf(0) }
    val steps = Step.entries
    val step = steps[index]
    BackHandler(enabled = index > 0) { index-- }
    val next: () -> Unit = { if (index < steps.lastIndex) index++ else actions.finish() }

    Column(Modifier.fillMaxSize().padding(top = 8.dp, bottom = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = ScreenMargin, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            StepDots(index, steps.size, Modifier.weight(1f))
            if (step != Step.DONE) YogaTextButton("Skip", actions.finish, muted = true)
        }
        AnimatedContent(
            targetState = index,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally(tween(320)) { w -> if (forward) w / 8 else -w / 8 } + fadeIn(tween(320)))
                    .togetherWith(slideOutHorizontally(tween(220)) { w -> if (forward) -w / 8 else w / 8 } + fadeOut(tween(180)))
            },
            modifier = Modifier.weight(1f),
            label = "onboarding",
        ) { shown ->
            Column(Modifier.fillMaxSize()) {
                when (steps[shown]) {
                    Step.WELCOME -> WelcomeStep()
                    Step.HOME -> DefaultHomeStep()
                    Step.APPS -> HomeAppsStep(ui, usage, isDistracting, actions)
                    Step.USAGE -> UsageStep()
                    Step.ALLOWANCE -> AllowanceStep(ui, isDistracting, isEssential, initial, actions)
                    Step.VALUE -> TimeValueStep(initial, actions)
                    Step.NUDGES -> InterventionsStep(initial, actions)
                    Step.LOOK -> LookStep(ui, actions)
                    Step.DONE -> DoneStep()
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = ScreenMargin),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (index > 0) YogaTextButton("Back", { index-- }, muted = true) else Spacer(Modifier.width(1.dp))
            YogaPrimaryButton(
                when (step) { Step.WELCOME -> "Begin"; Step.DONE -> "Go to my home"; else -> "Continue" },
                next,
            )
        }
    }
}

@Composable
private fun StepDots(current: Int, total: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { i ->
            val c = MaterialTheme.colorScheme
            Box(
                Modifier.height(3.dp).width(if (i == current) 22.dp else 10.dp)
                    .background(if (i <= current) c.primary.copy(alpha = if (i == current) 1f else 0.45f) else c.outlineVariant, CircleShape),
            )
        }
    }
}

@Composable
private fun ColumnScope.StepTitle(eyebrow: String, title: String, body: String? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = ScreenMargin).padding(top = 36.dp, bottom = 12.dp)) {
        YogaEyebrow(eyebrow)
        Text(title, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 12.dp))
        if (body != null) Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 14.dp))
    }
}

@Composable
private fun ColumnScope.WelcomeStep() {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1_000); now = System.currentTimeMillis() } }
    Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = ScreenMargin), verticalArrangement = Arrangement.Center) {
        BrandClock(now, onClick = {}, onLongClick = {})
        Spacer(Modifier.height(40.dp))
        YogaEyebrow("Welcome to Yoga")
        Text("Your phone, on your terms.", style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(top = 12.dp))
        Text("A quiet home screen that shows you where your time goes, and adds a little friction only where you ask for it. " +
            "Setup takes about a minute.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 14.dp))
    }
}

@Composable
private fun ColumnScope.DefaultHomeStep() {
    val context = LocalContext.current
    var isDefault by remember { mutableStateOf(isDefaultHome(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { isDefault = isDefaultHome(context) }
    StepTitle("Step one", "Make Yoga your home screen",
        "Yoga works as your home app, so it's there every time you press Home. You can switch back whenever you like.")
    Spacer(Modifier.height(12.dp))
    if (isDefault) DoneLine("Yoga is your home screen")
    else Box(Modifier.padding(horizontal = ScreenMargin)) {
        YogaSecondaryButton("Make Yoga home", { runCatching { launcher.launch(defaultHomeIntent(context)) } })
    }
}

@Composable
private fun DoneLine(text: String) {
    Row(Modifier.padding(horizontal = ScreenMargin, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
            Icon(YogaIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun ColumnScope.HomeAppsStep(ui: MainUiState, usage: UsageUiState, isDistracting: (String) -> Boolean, actions: OnboardingActions) {
    val context = LocalContext.current
    val apps = ui.drawerApps.distinctBy { it.appPackage }
    val suggested = remember(apps.size) {
        val byPackage = apps.associateBy { it.appPackage }
        val mostUsed = usage.snapshot.week.groupBy { it.packageName }.mapValues { (_, r) -> r.sumOf { it.foregroundMinutes } }
            .entries.sortedByDescending { it.value }.map { it.key }.filter { !isDistracting(it) }
        // The dock already holds phone, messages, camera and browser, so suggest everyday apps instead.
        val inDock = resolveDock(context, List(`in`.tuwi.yogaphone.data.DOCK_SIZE) { `in`.tuwi.yogaphone.data.DOCK_DEFAULT }, apps)
            .mapNotNull { it.app?.appPackage }.toSet()
        val everyday = listOf(Intent.CATEGORY_APP_CALENDAR, Intent.CATEGORY_APP_GALLERY, Intent.CATEGORY_APP_MAPS,
            Intent.CATEGORY_APP_EMAIL, Intent.CATEGORY_APP_MUSIC).mapNotNull { category ->
            runCatching { context.packageManager.resolveActivity(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, category), 0)
                ?.activityInfo?.packageName }.getOrNull()
        }
        val current = ui.favoriteApps.map { it.appPackage }
        current.ifEmpty { (mostUsed + everyday).distinct().filter { it in byPackage && it !in inDock && it != context.packageName } }
            .distinct().filter { it in byPackage }.take(6)
    }
    var chosen by remember { mutableStateOf(suggested) }
    StepTitle("Your home", "Pick the apps you reach for on purpose", "Up to 8. Everything else is one swipe away.")
    LazyColumn(Modifier.weight(1f)) {
        items(apps.filter { it.appPackage in chosen } + apps.filterNot { it.appPackage in chosen }, key = { it.appPackage }) { app ->
            val checked = app.appPackage in chosen
            YogaCheckRow(app.displayLabel, checked, enabled = checked || chosen.size < 8) {
                chosen = if (checked) chosen - app.appPackage else chosen + app.appPackage
                actions.setHomeApps(chosen.mapNotNull { pkg -> apps.firstOrNull { it.appPackage == pkg } })
            }
        }
    }
    // Apply the suggestion as soon as the step is shown, so "Continue" alone keeps it.
    LaunchedEffect(Unit) {
        if (ui.favoriteApps.isEmpty()) actions.setHomeApps(chosen.mapNotNull { pkg -> apps.firstOrNull { it.appPackage == pkg } })
    }
}

@Composable
private fun ColumnScope.UsageStep() {
    val context = LocalContext.current
    StepTitle("Awareness", "See where your time goes",
        "Yoga reads which app was open and for how long. It stays on this phone and is never sent anywhere. You can turn it on later.")
    if (UsageAccess.granted(context)) DoneLine("Usage access is on")
    else UsageAccessCard()
}

@Composable
private fun ColumnScope.AllowanceStep(
    ui: MainUiState,
    isDistracting: (String) -> Boolean,
    isEssential: (String) -> Boolean,
    initial: OnboardingInitial,
    actions: OnboardingActions,
) {
    val startOn = initial.allowanceMinutes != null || !initial.returning
    var minutes by remember { mutableIntStateOf(initial.allowanceMinutes ?: 90) }
    var enabled by remember { mutableStateOf(startOn) }
    val apps = ui.drawerApps.distinctBy { it.appPackage }.filterNot { isEssential(it.appPackage) }
    var distracting by remember { mutableStateOf(apps.filter { isDistracting(it.appPackage) }.map { it.appPackage }.toSet()) }
    // Write only what differs from today's setting, so replaying setup never overwrites a choice silently.
    LaunchedEffect(enabled, minutes) {
        val value = if (enabled) minutes else null
        if (value != initial.allowanceMinutes) actions.setAllowance(value)
    }
    StepTitle("Allowance", "How much is enough?", "A daily budget for distracting apps. Essential apps never count.")
    YogaSwitchRow(if (enabled) "${formatAllowance(minutes)} a day" else "No allowance for now", enabled, { enabled = it })
    if (enabled) Slider(
        minutes.toFloat(), { minutes = (it / 15).toInt() * 15 }, valueRange = 15f..240f, steps = 14,
        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant, activeTickColor = MaterialTheme.colorScheme.primary.copy(alpha = 0f),
            inactiveTickColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0f)),
        modifier = Modifier.padding(horizontal = ScreenMargin),
    )
    YogaNote("These count as distracting. Tap to change.")
    LazyColumn(Modifier.weight(1f)) {
        items(apps.sortedByDescending { it.appPackage in distracting }, key = { "d:" + it.appPackage }) { app ->
            val checked = app.appPackage in distracting
            YogaCheckRow(app.displayLabel, checked) {
                distracting = if (checked) distracting - app.appPackage else distracting + app.appPackage
                actions.setDistracting(app.appPackage, !checked)
            }
        }
    }
}

private fun formatAllowance(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}

@Composable
private fun ColumnScope.TimeValueStep(initial: OnboardingInitial, actions: OnboardingActions) {
    var on by remember { mutableStateOf(initial.timeValueOn) }
    var hourly by remember { mutableStateOf(initial.hourlyValue) }
    StepTitle("Optional", "What is an hour of your time worth?",
        "Give your hour a value and Yoga shows time in distracting apps as money burning, for example 20 minutes ≈ ₹167.")
    YogaSwitchRow("Show the value of my time", on, { on = it; actions.setTimeValue(it, hourly) })
    if (on) YogaTextField(
        hourly, { hourly = it.filter { c -> c.isDigit() || c == '.' }.take(8); actions.setTimeValue(true, hourly) },
        label = "Value of one hour", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
    YogaNote("Illustrative only. It's a way to picture your time, not money spent or income lost.")
}

@Composable
private fun ColumnScope.InterventionsStep(initial: OnboardingInitial, actions: OnboardingActions) {
    var reminders by remember { mutableStateOf(initial.reminders) }
    var pause by remember { mutableStateOf(initial.mindfulPause) }
    var grey by remember { mutableStateOf(initial.greyOverLimit) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        reminders = granted; actions.setReminders(granted)
    }
    StepTitle("Nudges", "Start gently", "Each of these can be changed later in Settings.")
    YogaSwitchRow("Reminders near my allowance", reminders, {
        reminders = it
        if (it && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS) else actions.setReminders(it)
    }, summary = "A quiet notification at 80% and when you go past")
    YogaSwitchRow("A short pause before distracting apps", pause, { pause = it; actions.setMindfulPause(it) },
        summary = "A few seconds to choose on purpose")
    YogaSwitchRow("Turn grey past my limit", grey, { grey = it; actions.setGreyOverLimit(it) },
        summary = "Colour returns tomorrow")
}

@Composable
private fun ColumnScope.LookStep(ui: MainUiState, actions: OnboardingActions) {
    var theme by remember { mutableStateOf(ui.preferences.themeMode) }
    var palette by remember { mutableStateOf(ui.preferences.palette) }
    StepTitle("Look", "Choose your look", "Three looks, each with its own colours and type. You can change it any time.")
    `in`.tuwi.yogaphone.ui.theme.PalettePicker(palette, theme, { palette = it; actions.setPalette(it) })
    YogaChipRow(Modifier.padding(top = 8.dp)) {
        ThemeModeChoices.forEach { (mode, label) -> YogaChip(label, theme == mode, { theme = mode; actions.setTheme(mode) }) }
    }
}

@Composable
private fun ColumnScope.DoneStep() {
    StepTitle("Ready", "That's everything.")
    YogaCard(Modifier.padding(top = 8.dp)) {
        listOf(
            YogaIcons.Search to "Swipe up, or tap Search, for every app.",
            YogaIcons.App to "Press and hold an app or a dock shortcut to change it.",
            YogaIcons.Timebox to "Timebox and Focus are the icons at the top.",
            YogaIcons.Settings to "Settings is top right. Help can replay this setup.",
        ).forEachIndexed { i, (icon, text) ->
            Row(Modifier.padding(top = if (i == 0) 0.dp else 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(16.dp))
                Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}
