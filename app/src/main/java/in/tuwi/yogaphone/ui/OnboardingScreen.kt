package `in`.tuwi.yogaphone.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import `in`.tuwi.yogaphone.BuildConfig
import `in`.tuwi.yogaphone.MainUiState
import `in`.tuwi.yogaphone.R
import `in`.tuwi.yogaphone.YogaApplication
import `in`.tuwi.yogaphone.data.AppModel
import `in`.tuwi.yogaphone.data.ThemeMode
import `in`.tuwi.yogaphone.guard.GuardPreferences
import `in`.tuwi.yogaphone.guard.GuardSurface
import `in`.tuwi.yogaphone.guard.YogaGuardService
import `in`.tuwi.yogaphone.review.FeedbackFeature
import `in`.tuwi.yogaphone.logic.SearchableApp
import `in`.tuwi.yogaphone.logic.rankAppSearch
import `in`.tuwi.yogaphone.pro.ProFeature
import `in`.tuwi.yogaphone.pro.ProGate
import `in`.tuwi.yogaphone.ui.theme.*
import `in`.tuwi.yogaphone.usage.UsageAccess
import `in`.tuwi.yogaphone.usage.UsageUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Bump to show the walkthrough again to everyone who finished an older one. */
const val ONBOARDING_VERSION = 3

/** What the walkthrough writes; every step goes straight to the existing settings. */
class OnboardingActions(
    val setHomeApps: (List<AppModel>) -> Unit,
    val setAllowance: (Int?) -> Unit,
    val setDistracting: (String, Boolean) -> Unit,
    val setTimeValue: (Boolean, String) -> Unit,
    val setHomeStyle: (`in`.tuwi.yogaphone.usage.HomeDisplayStyle) -> Unit,
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
    val previousVersion: Int = 0,
)

@Composable
fun OnboardingScreen(
    ui: MainUiState,
    usage: UsageUiState,
    isDistracting: (String) -> Boolean,
    isEssential: (String) -> Boolean,
    initial: OnboardingInitial,
    actions: OnboardingActions,
    replay: Boolean = false,
) {
    val context = LocalContext.current
    val app = context.applicationContext as YogaApplication
    val pro by app.proRepository.state.collectAsState()
    var resumed by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumed++
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val usageGranted = remember(resumed) { UsageAccess.granted(context) }
    val youtube = remember(resumed) { installed(context, YOUTUBE) }
    val instagram = remember(resumed) { installed(context, INSTAGRAM) }
    val steps by remember(initial.previousVersion, usageGranted, pro, youtube, instagram, replay) {
        derivedStateOf {
            onboardingSteps(initial.previousVersion, usageGranted,
                BuildConfig.GUARD && ProGate.allows(ProFeature.SHORTS_BLOCKING, pro), youtube, instagram, replay)
        }
    }
    var current by rememberSaveable { mutableStateOf(steps.firstOrNull() ?: Step.DONE) }
    val step = if (current in steps) current else steps.firstOrNull { it.ordinal > current.ordinal } ?: Step.DONE
    LaunchedEffect(step) { current = step }
    LaunchedEffect(steps.isEmpty()) { if (steps.isEmpty()) actions.finish() }
    if (steps.isEmpty()) return
    val index = steps.indexOf(step).coerceAtLeast(0)
    BackHandler(enabled = index > 0) { current = steps[index - 1] }
    val next: () -> Unit = { if (index < steps.lastIndex) current = steps[index + 1] else actions.finish() }

    Column(Modifier.fillMaxSize().padding(top = 8.dp, bottom = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = ScreenMargin, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            StepDots(index, steps.size, Modifier.weight(1f))
            if (step != Step.DONE) YogaTextButton("Skip", actions.finish, muted = true)
        }
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                (slideInHorizontally(tween(320)) { w -> if (forward) w / 8 else -w / 8 } + fadeIn(tween(400)))
                    .togetherWith(slideOutHorizontally(tween(220)) { w -> if (forward) -w / 8 else w / 8 } + fadeOut(tween(180)))
            },
            modifier = Modifier.weight(1f),
            label = "onboarding",
        ) { shown ->
            val contentModifier = if (shown == Step.APPS || shown == Step.ALLOWANCE) Modifier.fillMaxSize()
                    else Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            Column(contentModifier) {
                when (shown) {
                    Step.WELCOME -> WelcomeStep()
                    Step.HOME -> DefaultHomeStep()
                    Step.APPS -> HomeAppsStep(ui, usage, isDistracting, actions)
                    Step.USAGE -> UsageStep(usageGranted)
                    Step.WEEK -> WeekStep(ui, usage)
                    Step.BLOCKS -> BlocksStep(usage, youtube, instagram)
                    Step.ALLOWANCE -> AllowanceStep(ui, usage, isDistracting, isEssential, initial, actions)
                    Step.VALUE -> TimeValueStep(initial, actions)
                    Step.DISPLAY -> DisplayStep(usage, usageGranted, actions)
                    Step.NUDGES -> InterventionsStep(initial, actions)
                    Step.LOOK -> LookStep(ui, actions)
                    Step.DONE -> DoneStep(initial.previousVersion == 2 && !replay)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = ScreenMargin),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (index > 0) YogaTextButton("Back", { current = steps[index - 1] }, muted = true) else Spacer(Modifier.width(1.dp))
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
private fun OnboardingImage(@DrawableRes id: Int, compact: Boolean = false) {
    val height = if (compact) 120.dp else if (LocalConfiguration.current.screenHeightDp < 700) 160.dp else 200.dp
    Image(painterResource(id), contentDescription = null,
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary), contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenMargin).height(height))
}

@Composable
private fun ColumnScope.StepTitle(title: String, body: String? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = ScreenMargin).padding(top = 24.dp, bottom = 12.dp)) {
        Text(title, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
        if (body != null) Text(body, style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 14.dp))
    }
}

@Composable
private fun ColumnScope.WelcomeStep() {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1_000); now = System.currentTimeMillis() } }
    OnboardingImage(R.drawable.onboarding_welcome)
    Box(Modifier.padding(horizontal = ScreenMargin)) { BrandClock(now, onClick = {}, onLongClick = {}) }
    StepTitle("Your phone, on purpose.", "A calm home that shows where time goes.")
}

@Composable
private fun ColumnScope.DefaultHomeStep() {
    val context = LocalContext.current
    var isDefault by remember { mutableStateOf(isDefaultHome(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { isDefault = isDefaultHome(context) }
    StepTitle("Make Yoga your home.", "Switch back any time.")
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
    var query by rememberSaveable { mutableStateOf("") }
    StepTitle("Apps you reach for on purpose.", "Up to 8. The rest is one swipe away.")
    AppSearchField(query) { query = it }
    val visibleApps = if (query.isBlank()) apps.sortedByDescending { it.appPackage in chosen }
        else rankAppSearch(apps.map { SearchableApp(it.displayLabel, it) }, query)
    LazyColumn(Modifier.weight(1f)) {
        items(visibleApps, key = { it.appPackage }) { app ->
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
private fun ColumnScope.UsageStep(granted: Boolean) {
    OnboardingImage(R.drawable.onboarding_awareness)
    StepTitle("See where time goes.", "Stays on this phone. Never sent anywhere.")
    if (granted) DoneLine("Usage access is on")
    else UsageAccessCard()
}

@Composable
private fun ColumnScope.AllowanceStep(
    ui: MainUiState,
    usage: UsageUiState,
    isDistracting: (String) -> Boolean,
    isEssential: (String) -> Boolean,
    initial: OnboardingInitial,
    actions: OnboardingActions,
) {
    val app = LocalContext.current.applicationContext as YogaApplication
    val scope = rememberCoroutineScope()
    val startOn = initial.allowanceMinutes != null || !initial.returning
    var minutes by remember { mutableIntStateOf(initial.allowanceMinutes ?: allowancePrefill(usage)) }
    var enabled by remember { mutableStateOf(startOn) }
    var allowanceFeedback by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (startOn && initial.allowanceMinutes == null) app.reviewController.featureChanged(FeedbackFeature.ALLOWANCE, true)
    }
    val apps = ui.drawerApps.distinctBy { it.appPackage }.filterNot { isEssential(it.appPackage) }
    var distracting by remember { mutableStateOf(apps.filter { isDistracting(it.appPackage) }.map { it.appPackage }.toSet()) }
    // Write only what differs from today's setting, so replaying setup never overwrites a choice silently.
    LaunchedEffect(enabled, minutes) {
        val value = if (enabled) minutes else null
        if (value != initial.allowanceMinutes) actions.setAllowance(value)
    }
    var query by rememberSaveable { mutableStateOf("") }
    // Bound the scrolling header so search and the app list remain reachable on short screens.
    Column(Modifier.fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.4f).dp)
        .verticalScroll(rememberScrollState())) {
        OnboardingImage(R.drawable.onboarding_allowance, compact = true)
        StepTitle("How much is enough?")
        YogaSwitchRow(if (enabled) "${formatAllowance(minutes)} a day" else "No allowance for now", enabled, {
            enabled = it
            scope.launch { if (app.reviewController.featureChanged(FeedbackFeature.ALLOWANCE, it)) allowanceFeedback = true }
        })
        if (allowanceFeedback) FeedbackQuestion(FeedbackFeature.ALLOWANCE,
            listOf("Too strict", "Numbers looked wrong", "Didn't need it")) { allowanceFeedback = false }
        if (enabled) Slider(
            minutes.toFloat(), { minutes = (it / 15).toInt() * 15 }, valueRange = 15f..240f, steps = 14,
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant, activeTickColor = MaterialTheme.colorScheme.primary.copy(alpha = 0f),
                inactiveTickColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0f)),
            modifier = Modifier.padding(horizontal = ScreenMargin),
        )
    }
    AppSearchField(query) { query = it }
    val visibleApps = if (query.isBlank()) apps.sortedByDescending { it.appPackage in distracting }
        else rankAppSearch(apps.map { SearchableApp(it.displayLabel, it) }, query)
    LazyColumn(Modifier.weight(1f)) {
        items(visibleApps, key = { "d:" + it.appPackage }) { app ->
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
    StepTitle("What is your hour worth?", "See time in distracting apps as money.")
    YogaSwitchRow("Show the value of my time", on, { on = it; actions.setTimeValue(it, hourly) })
    if (on) YogaTextField(
        hourly, { hourly = it.filter { c -> c.isDigit() || c == '.' }.take(8); actions.setTimeValue(true, hourly) },
        label = "Value of one hour", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
    YogaNote("Illustrative only. Not money spent or income lost.")
}

@Composable
private fun ColumnScope.DisplayStep(usage: UsageUiState, usageGranted: Boolean, actions: OnboardingActions) {
    StepTitle("How should today look?", "Your usage, right on home.")
    // Real numbers once there are any; until then the picker labels its previews "Sample".
    val numbers = if (usageGranted && usage.snapshot.available)
        homeUsageNumbers(usage.snapshot, usage.rules, java.time.LocalDate.now()) else null
    HomeDisplayPicker(usage.preferences.homeStyle, numbers, actions.setHomeStyle)
}

@Composable
private fun ColumnScope.InterventionsStep(initial: OnboardingInitial, actions: OnboardingActions) {
    val app = LocalContext.current.applicationContext as YogaApplication
    val scope = rememberCoroutineScope()
    var reminders by remember { mutableStateOf(initial.reminders) }
    var pause by remember { mutableStateOf(initial.mindfulPause) }
    var pauseFeedback by remember { mutableStateOf(false) }
    var grey by remember { mutableStateOf(initial.greyOverLimit) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        reminders = granted; actions.setReminders(granted)
    }
    OnboardingImage(R.drawable.onboarding_nudges)
    StepTitle("Start gently.")
    YogaSwitchRow("Reminders near my allowance", reminders, {
        reminders = it
        if (it && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS) else actions.setReminders(it)
    }, summary = "At 80% and past your allowance")
    YogaSwitchRow("A short pause before distracting apps", pause, {
        pause = it
        actions.setMindfulPause(it)
        scope.launch { if (app.reviewController.featureChanged(FeedbackFeature.PAUSE, it)) pauseFeedback = true }
    },
        summary = "A few seconds to choose on purpose")
    if (pauseFeedback) FeedbackQuestion(FeedbackFeature.PAUSE, listOf("Too slow", "Annoying", "Didn't help")) { pauseFeedback = false }
    YogaSwitchRow("Turn grey past my limit", grey, { grey = it; actions.setGreyOverLimit(it) },
        summary = "Colour returns tomorrow")
}

@Composable
private fun ColumnScope.LookStep(ui: MainUiState, actions: OnboardingActions) {
    var theme by remember { mutableStateOf(ui.preferences.themeMode) }
    var palette by remember { mutableStateOf(ui.preferences.palette) }
    StepTitle("Choose your look.")
    `in`.tuwi.yogaphone.ui.theme.PalettePicker(palette, theme, { palette = it; actions.setPalette(it) })
    YogaChipRow(Modifier.padding(top = 8.dp)) {
        ThemeModeChoices.forEach { (mode, label) -> YogaChip(label, theme == mode, { theme = mode; actions.setTheme(mode) }) }
    }
}

@Composable
private fun ColumnScope.DoneStep(whatsNew: Boolean) {
    OnboardingImage(R.drawable.onboarding_done)
    StepTitle(if (whatsNew) "That's what's new." else "Ready.", "Everything is unlocked for 14 days.")
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

private const val YOUTUBE = "com.google.android.youtube"
private const val INSTAGRAM = "com.instagram.android"

private fun installed(context: Context, pkg: String): Boolean =
    runCatching { context.packageManager.getApplicationInfo(pkg, 0).enabled }.getOrDefault(false)

private fun duration(minutes: Long): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0L -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} m"
}

private fun allowancePrefill(usage: UsageUiState): Int {
    val snapshot = usage.snapshot
    val start = snapshot.date.minusDays(6).toString()
    val end = snapshot.date.toString()
    val days = snapshot.knownDays.filter { it >= start && it <= end }.toSet()
    val minutes = snapshot.week.filter { it.eligible && it.date in days }.sumOf { it.foregroundMinutes }
    return onboardingAllowancePrefill(if (days.isEmpty()) 0.0 else minutes.toDouble() / days.size, days.size)
}

@Composable
private fun AppSearchField(query: String, onQuery: (String) -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    YogaTextField(query, onQuery, label = "", placeholder = "Search apps",
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { keyboard?.hide(); focus.clearFocus() }),
        leadingIcon = { Icon(YogaIcons.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
        trailingIcon = {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                if (query.isNotBlank()) IconButton(onClick = { onQuery("") }) {
                    Icon(YogaIcons.Close, contentDescription = "Clear search")
                }
            }
        },
    )
}

@Composable
private fun ColumnScope.WeekStep(ui: MainUiState, usage: UsageUiState) {
    val context = LocalContext.current
    val home = context.packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
        ?.activityInfo?.packageName
    val totals = usage.snapshot.week.filterNot { it.packageName == context.packageName || it.packageName == home }
        .groupBy { it.packageName }.mapValues { (_, rows) -> rows.sumOf { it.foregroundMinutes } }
    StepTitle("Your last 7 days.")
    if (totals.isEmpty()) {
        YogaNote("Your numbers fill in as you use the phone.")
        return
    }
    Text(duration(totals.values.sum()), style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "lnum, tnum"),
        color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = ScreenMargin, vertical = 16.dp))
    // Include both video apps when present, even if neither made the top three.
    val packages = (totals.entries.sortedByDescending { it.value }.take(3).map { it.key } +
        listOf(YOUTUBE, INSTAGRAM).filter { it in totals }).distinct()
    packages.forEach { pkg ->
        val label = (ui.drawerApps + ui.otherApps).firstOrNull { it.appPackage == pkg }?.displayLabel
            ?: usage.snapshot.appLabels[pkg] ?: pkg
        Row(Modifier.fillMaxWidth().padding(horizontal = ScreenMargin, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(duration(totals.getValue(pkg)), style = MaterialTheme.typography.bodyLarge,
                color = if (pkg == YOUTUBE || pkg == INSTAGRAM) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
private fun ColumnScope.BlocksStep(usage: UsageUiState, youtube: Boolean, instagram: Boolean) {
    val context = LocalContext.current
    val app = context.applicationContext as YogaApplication
    val preferences by app.guardPrefs.preferences.collectAsState(initial = GuardPreferences())
    val scope = rememberCoroutineScope()
    val owner = LocalLifecycleOwner.current
    var guardEnabled by remember { mutableStateOf(YogaGuardService.isEnabled(context)) }
    var wanted by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var disclosure by rememberSaveable { mutableStateOf(false) }
    var awaitingSettings by rememberSaveable { mutableStateOf(false) }
    var returnedFromSettings by rememberSaveable { mutableStateOf(false) }
    var failed by rememberSaveable { mutableStateOf(false) }
    var done by rememberSaveable { mutableStateOf<String?>(null) }
    suspend fun enable(surfaces: List<GuardSurface>) {
        app.guardPrefs.update { p -> p.copy(surfaces = p.surfaces + surfaces.associateWith { surface ->
            p.surface(surface).copy(enabled = true, allowanceMinutes = 0)
        }) }
        done = when {
            GuardSurface.YOUTUBE in surfaces && GuardSurface.INSTAGRAM in surfaces -> "Shorts and Reels are off"
            GuardSurface.YOUTUBE in surfaces -> "Shorts are off"
            else -> "Reels are off"
        }
        // The global policy forbids asking while onboarding is showing; home evaluates this queued moment after setup.
        app.reviewController.queueOnboardingR1()
    }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                guardEnabled = YogaGuardService.isEnabled(context)
                if (awaitingSettings) {
                    awaitingSettings = false
                    returnedFromSettings = true
                    failed = !guardEnabled
                    if (guardEnabled) {
                        val surfaces = wanted.map(GuardSurface::valueOf)
                        wanted = arrayListOf()
                        if (surfaces.isNotEmpty()) scope.launch {
                            app.reviewController.featureChanged(FeedbackFeature.GUARD, true)
                            enable(surfaces)
                        }
                    }
                }
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    OnboardingImage(if (youtube) R.drawable.onboarding_shorts else R.drawable.onboarding_reels)
    StepTitle("Keep videos. Lose the scroll.", "Shorts and Reels off. Everything else works.")
    val surfaces = buildList {
        if (youtube) add(GuardSurface.YOUTUBE)
        if (instagram) add(GuardSurface.INSTAGRAM)
    }
    surfaces.forEach { surface ->
        val isYoutube = surface == GuardSurface.YOUTUBE
        val pkg = if (isYoutube) YOUTUBE else INSTAGRAM
        val rows = usage.snapshot.week.filter { it.packageName == pkg }
        val name = if (isYoutube) "YouTube" else "Instagram"
        val settings = preferences.surface(surface)
        YogaSwitchRow(if (isYoutube) "YouTube Shorts" else "Instagram Reels",
            (!returnedFromSettings || guardEnabled) && settings.enabled && settings.allowanceMinutes == 0,
            { checked ->
                done = null
                failed = false
                if (!checked) {
                    wanted = ArrayList(wanted.filterNot { it == surface.name })
                    scope.launch { app.guardPrefs.update { p -> p.copy(surfaces = p.surfaces +
                        (surface to p.surface(surface).copy(enabled = false))) } }
                } else if (YogaGuardService.isEnabled(context)) {
                    guardEnabled = true
                    scope.launch { enable(listOf(surface)) }
                } else {
                    wanted = ArrayList((wanted + surface.name).distinct())
                    disclosure = true
                }
            }, summary = if (rows.isNotEmpty()) "${duration(rows.sumOf { it.foregroundMinutes })} on $name this week"
                else if (isYoutube) "Regular videos still work" else "Posts and messages still work")
    }
    done?.let { DoneLine(it) }
    if (failed) {
        YogaNote("Yoga guard isn't on yet")
        Box(Modifier.padding(horizontal = ScreenMargin)) {
            YogaTextButton("Try again", { disclosure = true }, muted = true)
        }
    }
    if (disclosure) GuardDisclosureDialog(
        onConfirm = {
            disclosure = false
            awaitingSettings = true
            runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                .onFailure { awaitingSettings = false; failed = true }
        },
        onDismissRequest = { disclosure = false; wanted = arrayListOf(); failed = false },
    )
}
