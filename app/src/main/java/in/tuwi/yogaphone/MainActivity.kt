package `in`.tuwi.yogaphone

import `in`.tuwi.yogaphone.pro.*
import android.content.Context
import android.content.Intent
import android.widget.Toast
import `in`.tuwi.yogaphone.utils.requestWorkProfileOn
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import `in`.tuwi.yogaphone.usage.UsageViewModel
import `in`.tuwi.yogaphone.usage.UsageNotifications
import `in`.tuwi.yogaphone.ui.UsageDashboardScreen
import `in`.tuwi.yogaphone.ui.UsageSettingsScreen
import `in`.tuwi.yogaphone.ui.ValueOfTimeSettingsScreen
import `in`.tuwi.yogaphone.ui.GuardSettingsScreen
import `in`.tuwi.yogaphone.ui.rememberGuardDate
import `in`.tuwi.yogaphone.guard.GuardDaily
import `in`.tuwi.yogaphone.ui.HomeUsageIndicator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.rememberCoroutineScope
import `in`.tuwi.yogaphone.data.ThemeMode
import `in`.tuwi.yogaphone.data.db.TimeboxStatus
import `in`.tuwi.yogaphone.focus.FocusSystem
import `in`.tuwi.yogaphone.focus.FocusUiState
import `in`.tuwi.yogaphone.focus.FocusViewModel
import `in`.tuwi.yogaphone.focus.LaunchDecision
import `in`.tuwi.yogaphone.ui.FocusControlSheet
import `in`.tuwi.yogaphone.ui.FocusSettingsScreen
import `in`.tuwi.yogaphone.ui.FocusStartSheet
import `in`.tuwi.yogaphone.ui.HomeFocusStatus
import `in`.tuwi.yogaphone.ui.HomeQuickActions
import `in`.tuwi.yogaphone.ui.MindfulDelayScreen
import `in`.tuwi.yogaphone.ui.TimeboxCompletionSheet
import `in`.tuwi.yogaphone.ui.TimeboxControlSheet
import `in`.tuwi.yogaphone.ui.TimeboxStartSheet
import `in`.tuwi.yogaphone.usage.RuleState
import `in`.tuwi.yogaphone.ui.CategoriesScreen
import `in`.tuwi.yogaphone.ui.CategoryPickerDialog
import `in`.tuwi.yogaphone.ui.ProfileEditScreen
import `in`.tuwi.yogaphone.ui.ProfileSwitcherSheet
import `in`.tuwi.yogaphone.ui.ProfilesScreen
import `in`.tuwi.yogaphone.ui.HomeWidgets
import `in`.tuwi.yogaphone.ui.WidgetPickerScreen
import `in`.tuwi.yogaphone.ui.WidgetsSettingsScreen
import `in`.tuwi.yogaphone.widgets.WidgetRepository
import android.appwidget.AppWidgetManager
import android.app.Activity
import `in`.tuwi.yogaphone.ui.HelpScreen
import `in`.tuwi.yogaphone.ui.InsightsScreen
import `in`.tuwi.yogaphone.ui.OnboardingActions
import `in`.tuwi.yogaphone.ui.OnboardingScreen
import `in`.tuwi.yogaphone.ui.OnboardingInitial
import `in`.tuwi.yogaphone.ui.AdvancedSettingsScreen
import `in`.tuwi.yogaphone.ui.LeaveYogaScreen
import `in`.tuwi.yogaphone.ui.ONBOARDING_VERSION
import `in`.tuwi.yogaphone.ui.PrivacyScreen
import `in`.tuwi.yogaphone.ui.ReflectionSheet
import `in`.tuwi.yogaphone.insights.InsightsPreferences
import `in`.tuwi.yogaphone.insights.InsightsRepository
import `in`.tuwi.yogaphone.ui.DigestScreen
import `in`.tuwi.yogaphone.ui.HomeGreyIndicator
import `in`.tuwi.yogaphone.ui.QuietNotificationsScreen
import `in`.tuwi.yogaphone.ui.VisualInterventionsScreen
import `in`.tuwi.yogaphone.quiet.GreyReason
import `in`.tuwi.yogaphone.quiet.GreyTriggers
import `in`.tuwi.yogaphone.quiet.GreyscalePolicy
import `in`.tuwi.yogaphone.quiet.QuietPreferences
import `in`.tuwi.yogaphone.quiet.QuietRepository
import `in`.tuwi.yogaphone.focus.ScheduleEvaluator
import `in`.tuwi.yogaphone.focus.toWindow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import `in`.tuwi.yogaphone.data.AppModel
import `in`.tuwi.yogaphone.receiver.AppChangeReceiver
import `in`.tuwi.yogaphone.ui.AppActionSheet
import `in`.tuwi.yogaphone.ui.AppDrawerLayer
import `in`.tuwi.yogaphone.ui.rememberDrawerController
import `in`.tuwi.yogaphone.ui.DockRole
import `in`.tuwi.yogaphone.ui.DockSlot
import `in`.tuwi.yogaphone.ui.HomeDock
import `in`.tuwi.yogaphone.ui.resolveDock
import `in`.tuwi.yogaphone.ui.DefaultHomePrompt
import `in`.tuwi.yogaphone.ui.UpdatePrompt
import `in`.tuwi.yogaphone.ui.isDefaultHome
import `in`.tuwi.yogaphone.ui.defaultHomeIntent
import `in`.tuwi.yogaphone.data.DOCK_DEFAULT
import `in`.tuwi.yogaphone.data.DOCK_EMPTY
import androidx.compose.ui.graphics.graphicsLayer
import `in`.tuwi.yogaphone.ui.AppMenuAction
import `in`.tuwi.yogaphone.ui.AppearanceSettingsScreen
import `in`.tuwi.yogaphone.ui.HiddenAppsScreen
import `in`.tuwi.yogaphone.ui.HomeScreen
import `in`.tuwi.yogaphone.ui.HomeOptionsSheet
import `in`.tuwi.yogaphone.ui.RenameAppDialog
import `in`.tuwi.yogaphone.ui.SettingsScreen
import `in`.tuwi.yogaphone.ui.homeFontSize
import `in`.tuwi.yogaphone.ui.theme.YogaDialog
import androidx.compose.material3.Text
import `in`.tuwi.yogaphone.ui.theme.YogaPhoneTheme
import `in`.tuwi.yogaphone.ui.theme.themeUsesDarkIcons

private const val HOME = "home"
private const val PRO = "settings/pro"
private const val SETTINGS = "settings"
private const val GUARD = "settings/guard"
private const val HIDDEN_APPS = "settings/hidden_apps"
private const val APPEARANCE = "settings/appearance"
private const val FOCUS_SETTINGS = "settings/focus"
private const val PROFILES = "settings/profiles"
private const val CATEGORIES = "settings/categories"
private const val VISUAL = "settings/visual"
private const val QUIET = "settings/quiet"
private const val DIGEST = "digest"
private const val INSIGHTS = "insights"
private const val PRIVACY = "settings/privacy"
private const val HELP = "settings/help"
private const val SUGGESTIONS = "settings/suggestions"
private const val ONBOARDING = "onboarding"
private const val WIDGETS = "settings/widgets"
private const val ADVANCED = "settings/advanced"
private const val LEAVE = "settings/advanced/leave"
private const val WIDGET_PICKER = "settings/widgets/add"

private data class PendingPause(val app: AppModel, val query: String, val seconds: Int, val duringFocus: Boolean)

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val usageViewModel: UsageViewModel by viewModels()
    private val focusViewModel: FocusViewModel by viewModels()
    private var usageRequest by mutableIntStateOf(0)
    private lateinit var appChangeReceiver: AppChangeReceiver
    /** A work profile turning on, off, or being added or removed. Only runtime-registered receivers get these. */
    private val workProfileReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) { viewModel.refreshWorkApps() }
    }
    private var homeRequest by mutableIntStateOf(0)
    /** Home pressed while Yoga was already in front: opens the drawer, like Nova's Home-button action. */
    private var homeAgainRequest by mutableIntStateOf(0)
    /** True from onStop until the next onResume: a Home intent then is a return from another app. */
    private var stoppedSinceResume = true
    private var digestRequest by mutableIntStateOf(0)
    private var insightsRequest by mutableIntStateOf(0)
    private val updateLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        (application as YogaApplication).updateController.onUpdateFlowResult(it.resultCode)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as YogaApplication).updateController.attachLauncher(updateLauncher)
        appChangeReceiver = AppChangeReceiver(viewModel::refreshApps)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(appChangeReceiver, AppChangeReceiver.getIntentFilter(), RECEIVER_NOT_EXPORTED)
            registerReceiver(workProfileReceiver, workProfileFilter(), RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION") registerReceiver(appChangeReceiver, AppChangeReceiver.getIntentFilter())
            @Suppress("DEPRECATION") registerReceiver(workProfileReceiver, workProfileFilter())
        }
        readIntroPreview(intent)
        if (intent.action == UsageNotifications.ACTION_DETAILS) usageRequest++
        if (intent.action == QuietRepository.ACTION_OPEN_DIGEST) digestRequest++
        if (intent.action == InsightsRepository.ACTION_OPEN_INSIGHTS) insightsRequest++
        setContent { YogaLauncherApp(viewModel, usageViewModel, focusViewModel, homeRequest, homeAgainRequest, usageRequest, digestRequest, insightsRequest) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readIntroPreview(intent)
        if (intent.action == UsageNotifications.ACTION_DETAILS) usageRequest++
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            if (stoppedSinceResume) homeRequest++ else homeAgainRequest++
        }
        if (intent.action == FocusSystem.ACTION_OPEN_TIMEBOX) homeRequest++
        if (intent.action == QuietRepository.ACTION_OPEN_DIGEST) digestRequest++
        if (intent.action == InsightsRepository.ACTION_OPEN_INSIGHTS) insightsRequest++
    }

    /** Debug and profile only: `adb shell am start -n <pkg>/.MainActivity --es intro_preview "youtube=400"`. */
    private fun readIntroPreview(intent: Intent) {
        if (!BuildConfig.INTRO_PREVIEW) return
        intent.getStringExtra("intro_preview")?.let { (application as YogaApplication).introSignals.setPreview(it) }
    }

    /** Widget configuration activities report back here (AppWidgetHost uses startActivityForResult). */
    var onWidgetConfigured: ((Int, Boolean) -> Unit)? = null
    var configuringWidgetId: Int = -1

    @Deprecated("AppWidgetHost.startAppWidgetConfigureActivityForResult delivers its result here")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION") super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == WidgetRepository.REQUEST_CONFIGURE && configuringWidgetId != -1) {
            onWidgetConfigured?.invoke(configuringWidgetId, resultCode == Activity.RESULT_OK)
            configuringWidgetId = -1
        }
    }

    override fun onResume() {
        super.onResume()
        stoppedSinceResume = false
        viewModel.refreshWorkApps()
        (application as YogaApplication).proRepository.onResume()
        (application as YogaApplication).updateController.onHomeResume(this)
    }

    override fun onStart() {
        super.onStart()
        (application as YogaApplication).widgetRepository.startListening()
    }

    override fun onStop() {
        stoppedSinceResume = true
        (application as YogaApplication).updateController.onHomeStop()
        (application as YogaApplication).widgetRepository.stopListening()
        super.onStop()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(appChangeReceiver) }
        runCatching { unregisterReceiver(workProfileReceiver) }
        super.onDestroy()
    }
}

@Composable
private fun YogaLauncherApp(viewModel: MainViewModel, usageViewModel: UsageViewModel, focusViewModel: FocusViewModel, homeRequest: Int, homeAgainRequest: Int, usageRequest: Int, digestRequest: Int, insightsRequest: Int) {
    val uiState by viewModel.uiState.collectAsState()
    val pro by (androidx.compose.ui.platform.LocalContext.current.applicationContext as YogaApplication).proRepository.state.collectAsState()
    val focusState by focusViewModel.state.collectAsState()
    // Greyscale during focus is an override; the chosen theme is never rewritten.
    val profileTheme = uiState.activeProfile?.themeOverride?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
    val quiet = (androidx.compose.ui.platform.LocalContext.current.applicationContext as YogaApplication).quietRepository
    val quietPrefs by quiet.preferences.collectAsState(initial = QuietPreferences())
    val usageForGrey by usageViewModel.state.collectAsState()
    val greyReason = run {
        val bedtime = quietPrefs.greyBedtimeScheduleId?.takeIf { ProGate.allows(ProFeature.FOCUS, pro) }?.let { id ->
            focusState.schedules.firstOrNull { it.id == id }?.let {
                ScheduleEvaluator.activeAt(listOf(it.toWindow()), java.time.Instant.ofEpochMilli(focusState.now), java.time.ZoneId.systemDefault()) != null
            }
        } ?: false
        val over = usageForGrey.snapshot.evaluation.overall?.state == RuleState.OVER
        GreyscalePolicy.reason(GreyTriggers(quietPrefs.greyOverLimit, focusState.preferences.focusGreyscale, quietPrefs.greyBedtimeScheduleId != null && ProGate.allows(ProFeature.FOCUS, pro)),
            focusState.focusActive, bedtime, over, quietPrefs.greySnoozedUntil, focusState.now)
    }
    val themeMode = if (greyReason != null) ThemeMode.GREYSCALE else profileTheme ?: uiState.preferences.themeMode
    val activity = androidx.compose.ui.platform.LocalContext.current as ComponentActivity
    val darkIcons = themeUsesDarkIcons(themeMode)
    SideEffect {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { !darkIcons },
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { !darkIcons },
        )
    }
    YogaPhoneTheme(themeMode, if (ProGate.allows(ProFeature.LOOKS, pro)) uiState.preferences.palette else `in`.tuwi.yogaphone.data.Palette.OBSIDIAN_BRASS) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            `in`.tuwi.yogaphone.ui.IntroActionHost {
                LauncherNavigation(viewModel, usageViewModel, focusViewModel, focusState, uiState, homeRequest, homeAgainRequest, usageRequest, digestRequest, greyReason, quietPrefs, quiet, insightsRequest)
            }
        }
    }
}

@Composable
private fun LauncherNavigation(
    viewModel: MainViewModel,
    usageViewModel: UsageViewModel,
    focusViewModel: FocusViewModel,
    focusState: FocusUiState,
    uiState: MainUiState,
    homeRequest: Int,
    homeAgainRequest: Int,
    usageRequest: Int,
    digestRequest: Int,
    greyReason: GreyReason?,
    quietPrefs: QuietPreferences,
    quiet: QuietRepository,
    insightsRequest: Int,
) {
    val scope = rememberCoroutineScope()
    val yoga = androidx.compose.ui.platform.LocalContext.current.applicationContext as YogaApplication
    val insights = yoga.insightsRepository
    val insightsPrefs by insights.preferences.collectAsState(initial = null)
    var reflectionFor by remember { mutableStateOf<Long?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val navController = rememberNavController()
    val pro by yoga.proRepository.state.collectAsState()
    val access = ProAccess(pro) { navController.navigate(PRO) { launchSingleTop = true } }
    fun openProFeature(feature: ProFeature, route: String) = access.run(feature) { navController.navigate(route) }
    LaunchedEffect(pro.entitlement) { usageViewModel.refresh(); focusViewModel.refresh() }
    val widgetRepo = yoga.widgetRepository
    val widgets by widgetRepo.widgets.collectAsState(initial = emptyList())
    val activity = androidx.compose.ui.platform.LocalContext.current as MainActivity
    fun finishWidget(id: Int, ok: Boolean) {
        if (ok) scope.launch { widgetRepo.add(id); navController.popBackStack(WIDGETS, false) } else widgetRepo.release(id)
    }
    fun configureOrAdd(id: Int) {
        val info = widgetRepo.info(id)
        if (info?.configure != null) {
            activity.configuringWidgetId = id
            activity.onWidgetConfigured = ::finishWidget
            runCatching { widgetRepo.host.startAppWidgetConfigureActivityForResult(activity, id, 0, WidgetRepository.REQUEST_CONFIGURE, null) }
                .onFailure { finishWidget(id, false) }
        } else finishWidget(id, info != null)
    }
    var bindingWidgetId by remember { mutableStateOf(-1) }
    val bindLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val id = bindingWidgetId
        bindingWidgetId = -1
        if (id != -1) { if (result.resultCode == Activity.RESULT_OK) configureOrAdd(id) else widgetRepo.release(id) }
    }
    fun pickWidget(info: android.appwidget.AppWidgetProviderInfo) {
        val id = widgetRepo.allocate()
        if (widgetRepo.bindIfAllowed(id, info.provider)) configureOrAdd(id)
        else {
            bindingWidgetId = id
            bindLauncher.launch(Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider))
        }
    }
    val usageState by usageViewModel.state.collectAsState()
    val owner = LocalLifecycleOwner.current
    var resumed by remember { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) { resumed = true; usageViewModel.refresh(); focusViewModel.refresh() }
            if (event == Lifecycle.Event.ON_PAUSE) resumed = false
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(resumed) {
        if (resumed) while (true) { usageViewModel.refresh(); delay(30_000) }
    }
    LaunchedEffect(insightsRequest) {
        if (insightsRequest > 0) openProFeature(ProFeature.INSIGHTS, INSIGHTS)
    }
    LaunchedEffect(Unit) {
        runCatching { insights.scheduleWeekly(); quiet.scheduleDigest() }
    }
    // First run: show setup, unless this is a returning user who already set things up.
    val onboardingVersion = insightsPrefs?.onboardingVersion
    LaunchedEffect(onboardingVersion, uiState.isLoading) {
        if (onboardingVersion == null || onboardingVersion >= ONBOARDING_VERSION || uiState.isLoading) return@LaunchedEffect
        // Everyone sees the current walkthrough once; it starts from their existing settings.
        navController.navigate(ONBOARDING) { launchSingleTop = true }
    }
    LaunchedEffect(resumed) {
        if (resumed) reflectionFor = runCatching { insights.pendingReflection() }.getOrNull()
        if (resumed) runCatching { widgetRepo.cleanup() }
    }
    LaunchedEffect(digestRequest) {
        if (digestRequest > 0) openProFeature(ProFeature.QUIET_NOTIFICATIONS, DIGEST)
    }
    LaunchedEffect(usageRequest) {
        if (usageRequest > 0) navController.navigate("usage?details=true") { launchSingleTop = true }
    }
    val route = navController.currentBackStackEntryAsState().value?.destination?.route
    val drawer = rememberDrawerController()
    // Read the drawer position only through this, so a slide doesn't recompose the whole home screen.
    val drawerClosed by remember { androidx.compose.runtime.derivedStateOf { drawer.isClosed } }
    var pickingDockSlot by remember { mutableStateOf<Int?>(null) }
    var dockMenuSlot by remember { mutableStateOf<DockSlot?>(null) }
    var isDefault by remember { mutableStateOf(isDefaultHome(context)) }
    var homeAskedAt by remember { mutableStateOf(0L) }
    var homeAskFromButton by remember { mutableStateOf(false) }
    val homeRoleLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
    ) { _ ->
        isDefault = isDefaultHome(context)
        // Android stops showing the role dialog after it was dismissed a few times and answers at once.
        // Only then, and only when the user tapped "Make it home", open the system's home-app picker.
        val instant = android.os.SystemClock.elapsedRealtime() - homeAskedAt < 600
        if (!isDefault && instant && homeAskFromButton) runCatching { context.startActivity(Intent(android.provider.Settings.ACTION_HOME_SETTINGS)) }
    }
    fun askToBeHome(fromButton: Boolean = true) {
        homeAskedAt = android.os.SystemClock.elapsedRealtime(); homeAskFromButton = fromButton
        runCatching { homeRoleLauncher.launch(defaultHomeIntent(context)) }
    }
    LaunchedEffect(resumed) { if (resumed) isDefault = isDefaultHome(context) }
    // Ask once, after setup, if Yoga isn't the home app yet. The home banner covers later visits.
    LaunchedEffect(resumed, uiState.isLoading, insightsPrefs?.onboardingVersion) {
        val onboarded = (insightsPrefs?.onboardingVersion ?: 0) >= ONBOARDING_VERSION
        if (resumed && onboarded && !uiState.isLoading && !uiState.preferences.homeRoleAsked && !isDefaultHome(context)) {
            viewModel.markHomeRoleAsked(); askToBeHome(fromButton = false)
        }
    }
    DisposableEffect(owner) {
        // Leaving for an app closes the drawer without an animation, so coming back lands on home.
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) drawer.snapClosed() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    var replacingApp by remember { mutableStateOf<AppModel?>(null) }
    var homePicking by remember { mutableStateOf(false) }
    val workApps by viewModel.workApps.collectAsState()
    var workPrompt by remember { mutableStateOf<AppModel?>(null) }
    var pendingWork by remember { mutableStateOf<AppModel?>(null) }
    var showHomeOptions by remember { mutableStateOf(false) }
    var favoriteMenuApp by remember { mutableStateOf<AppModel?>(null) }
    var drawerMenuApp by remember { mutableStateOf<AppModel?>(null) }
    var renameApp by remember { mutableStateOf<AppModel?>(null) }
    var pendingPause by remember { mutableStateOf<PendingPause?>(null) }
    var timeboxStartFor by remember { mutableStateOf<AppModel?>(null) }
    var showTimeboxStart by remember { mutableStateOf(false) }
    var showTimeboxControl by remember { mutableStateOf(false) }
    var showFocusStart by remember { mutableStateOf(false) }
    var showFocusControl by remember { mutableStateOf(false) }
    var showProfiles by remember { mutableStateOf(false) }
    var categoryFor by remember { mutableStateOf<AppModel?>(null) }

    fun switchProfile(id: Long?) = viewModel.switchProfile(id) { previous, next ->
        if (previous?.focusOnActivate == true && focusState.focus?.plannedEndAt == null) focusViewModel.endFocus()
        if (next?.focusOnActivate == true) focusViewModel.startFocus(null)
    }

    // Every launch from Yoga goes through the mindful-pause gate.
    fun requestLaunch(app: AppModel, query: String = "") {
        scope.launch {
            when (val decision = focusViewModel.decide(app)) {
                LaunchDecision.Open -> viewModel.launchApp(app, query)
                is LaunchDecision.Pause -> pendingPause = PendingPause(app, query, decision.seconds, decision.duringFocus)
            }
        }
    }

    fun resetToHome() {
        drawer.close()
        replacingApp = null
        homePicking = false
        showHomeOptions = false
        pickingDockSlot = null
        favoriteMenuApp = null
        drawerMenuApp = null
        renameApp = null
        showTimeboxStart = false
        showTimeboxControl = false
        showFocusStart = false
        showFocusControl = false
        dockMenuSlot = null
        showProfiles = false
        categoryFor = null
        navController.returnHome()
    }
    LaunchedEffect(homeRequest) { if (homeRequest > 0) resetToHome() }
    // Home pressed on a calm home screen opens the drawer with the keyboard; anywhere else it just goes home.
    LaunchedEffect(homeAgainRequest) {
        if (homeAgainRequest == 0) return@LaunchedEffect
        val calm = route == HOME && drawer.isClosed && replacingApp == null && pickingDockSlot == null && !homePicking &&
            !showHomeOptions && favoriteMenuApp == null && drawerMenuApp == null && renameApp == null && dockMenuSlot == null &&
            !showTimeboxStart && !showTimeboxControl && !showFocusStart && !showFocusControl && !showProfiles &&
            pendingPause == null && categoryFor == null
        if (calm) drawer.open() else resetToHome()
    }
    BackHandler(enabled = route == HOME && drawerClosed) { /* A HOME activity stays home. */ }

    androidx.compose.runtime.CompositionLocalProvider(LocalProAccess provides access) {
    NavHost(
        navController = navController,
        startDestination = HOME,
        modifier = Modifier.statusBarsPadding().navigationBarsPadding(),
    ) {
        composable(PRO) { ProScreen(yoga.proRepository) { navController.popBackStack() } }
        composable(HOME) {
            val homeState = if (focusState.focusActive)
                uiState.copy(favoriteApps = uiState.favoriteApps.filterNot { focusState.pausedByFocus(it.appPackage) })
            else uiState
            HomeScreen(
                uiState = homeState,
                onAppClick = { requestLaunch(it) },
                onAppLongClick = { favoriteMenuApp = it },
                onDrawerDrag = drawer::dragBy,
                onDrawerRelease = drawer::release,
                onOpenSettings = { navController.navigate(SETTINGS) },
                onOpenClock = { context.openClock() },
                onOpenCalendar = { context.openCalendar() },
                onExpandNotifications = { context.expandNotifications() },
                onEmptyLongPress = { showHomeOptions = true },
                statusContent = {
                    Column {
                        if (!isDefault) DefaultHomePrompt { askToBeHome() }
                        UpdatePrompt(activity, yoga.updateController)
                        `in`.tuwi.yogaphone.ui.HomeIntroduction()
                        val guardDay by yoga.guardPrefs.daily.collectAsState(initial = GuardDaily())
                        TrialEndingCard(if (guardDay.date == rememberGuardDate()) guardDay.blocks.values.sum() else 0,
                            if (usageState.preferences.timeValueEnabled) `in`.tuwi.yogaphone.usage.TimeValueCalculator().format(60, usageState.preferences.hourlyAmount(), usageState.preferences.currency) else null)
                        HomeGreyIndicator(greyReason) { scope.launch { quiet.snoozeGrey(30) } }
                        val prefsNow = insightsPrefs
                        if (prefsNow != null && prefsNow.onboardingVersion >= 1 && "swipe_up" !in prefsNow.tipsSeen) {
                            androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                androidx.compose.material3.Text("Tip: swipe up anywhere to see all your apps.",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f))
                                androidx.compose.material3.TextButton(onClick = { scope.launch { insights.markTip("swipe_up") } }) {
                                    androidx.compose.material3.Text("Got it")
                                }
                            }
                        }
                        HomeFocusStatus(focusState, onTimebox = { showTimeboxControl = true }, onFocus = { showFocusControl = true })
                        HomeUsageIndicator(usageState, { navController.navigate("usage") }, { navController.navigate("usage?details=true") })
                    }
                },
                topActions = {
                    HomeQuickActions(
                        onTimebox = { access.run(ProFeature.TIMEBOX) {
                            if (focusState.timeboxRemainingMs != null) showTimeboxControl = true
                            else { timeboxStartFor = null; showTimeboxStart = true }
                        } },
                        onFocus = { access.run(ProFeature.FOCUS) { if (focusState.focusActive) showFocusControl = true else showFocusStart = true } },
                        focusActive = focusState.focusActive,
                        timeboxActive = focusState.timeboxRemainingMs != null,
                    )
                },
                dock = {
                    val slots = remember(uiState.preferences.dockSlots, uiState.allApps) {
                        resolveDock(context, uiState.preferences.dockSlots, uiState.allApps.filterNot(AppModel::isHidden))
                    }
                    HomeDock(
                        slots = slots,
                        onSearch = { replacingApp = null; pickingDockSlot = null; drawer.open() },
                        onSlotClick = { slot ->
                            val app = slot.app
                            if (app != null) requestLaunch(app) else { pickingDockSlot = slot.index; drawer.open() }
                        },
                        onSlotLongClick = { slot ->
                            if (slot.isEmpty) { pickingDockSlot = slot.index; drawer.open() } else dockMenuSlot = slot
                        },
                    )
                },
                profileLabel = if (uiState.profiles.isNotEmpty()) uiState.activeProfileName + if (access.allows(ProFeature.PROFILES)) "" else " \u00b7 Pro" else null,
                widgets = {
                    if (access.allows(ProFeature.WIDGETS)) HomeWidgets(widgets, widgetRepo, when {
                        focusState.focusActive -> "during focus"
                        uiState.activeProfile?.hideOtherApps == true -> "in this profile"
                        else -> null
                    })
                },
                onProfileClick = { access.run(ProFeature.PROFILES) { showProfiles = true } },
                modifier = Modifier,
            )
        }
        composable(SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onAppearance = { navController.navigate(APPEARANCE) },
                onHiddenApps = { navController.navigate(HIDDEN_APPS) },
                onResetNames = viewModel::resetCustomLabels,
                onUsage = { navController.navigate("usage") },
                onUsageRules = { navController.navigate("settings/usage") },
                onValueOfTime = { navController.navigate("settings/value") },
                onGuard = { navController.navigate(GUARD) },
                onFocus = { navController.navigate(FOCUS_SETTINGS) },
                onProfiles = { navController.navigate(PROFILES) },
                onCategories = { navController.navigate(CATEGORIES) },
                onVisual = { navController.navigate(VISUAL) },
                onQuiet = { navController.navigate(QUIET) },
                onInsights = { navController.navigate(INSIGHTS) },
                onPrivacy = { navController.navigate(PRIVACY) },
                onHelp = { navController.navigate(HELP) },
                onWidgets = { navController.navigate(WIDGETS) },
                onAdvanced = { navController.navigate(ADVANCED) },
            )
        }
        composable(ADVANCED) {
            AdvancedSettingsScreen({ navController.popBackStack() }, viewModel::resetCustomLabels) { navController.navigate(LEAVE) }
        }
        composable(LEAVE) { LeaveYogaScreen { navController.popBackStack() } }
        composable("usage?details={details}", arguments = listOf(androidx.navigation.navArgument("details") { defaultValue = "false" })) { entry ->
            val guardDaily by yoga.guardPrefs.daily.collectAsState(initial = GuardDaily())
            val guardDate = rememberGuardDate()
            val blockedToday = if (guardDaily.date == guardDate) guardDaily.blocks.values.sum() else 0
            UsageDashboardScreen(usageState, usageViewModel, { navController.popBackStack() },
                { navController.navigate("settings/usage") }, entry.arguments?.getString("details") == "true",
                uiState.categories.associate { it.id to it.name }, blockedToday)
        }
        composable("settings/usage") {
            UsageSettingsScreen(usageState, usageViewModel, { navController.popBackStack() },
                { navController.navigate("usage") }, { navController.navigate("settings/value") }, uiState.categories)
        }
        composable("settings/value") {
            if (!access.allows(ProFeature.VALUE_OF_TIME)) { ProScreen(yoga.proRepository) { navController.popBackStack() }; return@composable }
            ValueOfTimeSettingsScreen(usageState, usageViewModel, yoga.guardPrefs,
                { navController.navigate(GUARD) }, { navController.popBackStack() })
        }
        composable(GUARD) {
            GuardSettingsScreen(yoga.guardPrefs, usageState.preferences.timeValueEnabled,
                { navController.navigate("settings/value") }, { navController.popBackStack() })
        }
        composable(APPEARANCE) {
            AppearanceSettingsScreen(
                preferences = uiState.preferences,
                onBack = { navController.popBackStack() },
                onTheme = viewModel::setTheme,
                onPalette = viewModel::setPalette,
                onFontSize = viewModel::setFontSize,
                onShowClock = viewModel::setShowClock,
                onShowDate = viewModel::setShowDate,
                onHomeAppCount = viewModel::setHomeAppCount,
                onAlignment = viewModel::setHomeTextAlignment,
                onAutoOpenSingleMatch = viewModel::setAutoOpenSingleMatch,
            )
        }
        composable(PROFILES) {
            if (!access.allows(ProFeature.PROFILES)) { ProScreen(yoga.proRepository) { navController.popBackStack() }; return@composable }
            ProfilesScreen(uiState, viewModel, { navController.popBackStack() }) { navController.navigate("$PROFILES/$it") }
        }
        composable("$PROFILES/{id}") { entry ->
            if (!access.allows(ProFeature.PROFILES)) { ProScreen(yoga.proRepository) { navController.popBackStack() }; return@composable }
            val id = entry.arguments?.getString("id")?.toLongOrNull()
            val profile = uiState.profiles.firstOrNull { it.id == id }
            if (profile != null) ProfileEditScreen(profile, uiState, focusState.schedules, viewModel) { navController.popBackStack() }
        }
        composable(WIDGETS) {
            if (!access.allows(ProFeature.WIDGETS)) { ProScreen(yoga.proRepository) { navController.popBackStack() }; return@composable }
            WidgetsSettingsScreen(widgets, widgetRepo, onAdd = { navController.navigate(WIDGET_PICKER) }) { navController.popBackStack() }
        }
        composable(WIDGET_PICKER) {
            if (!access.allows(ProFeature.WIDGETS)) { ProScreen(yoga.proRepository) { navController.popBackStack() }; return@composable }
            WidgetPickerScreen(widgetRepo, onPick = ::pickWidget) { navController.popBackStack() }
        }
        composable(INSIGHTS) {
            if (!access.allows(ProFeature.INSIGHTS)) { ProScreen(yoga.proRepository) { navController.popBackStack() }; return@composable }
            insightsPrefs?.let { prefs ->
                InsightsScreen(insights, prefs, onAcceptSuggestion = { usageViewModel.saveRule(null, it) }) { navController.popBackStack() }
            }
        }
        composable(PRIVACY) {
            insightsPrefs?.let { prefs ->
                PrivacyScreen(prefs, insights, yoga.backupManager, onReset = {
                    scope.launch { navController.returnHome(); navController.navigate(ONBOARDING) }
                }, onGuard = { navController.navigate(GUARD) }) { navController.popBackStack() }
            }
        }
        composable(SUGGESTIONS) { `in`.tuwi.yogaphone.ui.SuggestionsScreen { navController.popBackStack() } }
        composable(HELP) {
            HelpScreen(onReplaySetup = { navController.navigate(ONBOARDING) }, onSuggestions = { navController.navigate(SUGGESTIONS) }) { navController.popBackStack() }
        }
        composable(ONBOARDING) {
            OnboardingScreen(
                ui = uiState,
                usage = usageState,
                isDistracting = focusState::isDistracting,
                isEssential = { focusState.classifications[it]?.essential == true },
                initial = OnboardingInitial(
                    allowanceMinutes = usageState.rules.firstOrNull { it.scope == "TOTAL" && it.enabled }?.dailyAllowanceMinutes,
                    returning = uiState.favoriteApps.isNotEmpty() || usageState.rules.isNotEmpty(),
                    timeValueOn = usageState.preferences.timeValueEnabled,
                    hourlyValue = usageState.preferences.hourlyValue,
                    reminders = usageState.preferences.reminders,
                    mindfulPause = focusState.preferences.delayEnabled,
                    greyOverLimit = quietPrefs.greyOverLimit,
                ),
                actions = OnboardingActions(
                    setHomeApps = viewModel::setHomeApps,
                    setAllowance = { usageViewModel.saveRule(null, it) },
                    setDistracting = { pkg, value -> usageViewModel.classify(pkg, essential = false, distracting = value) },
                    setTimeValue = { on, hourly -> usageViewModel.preferences { it.copy(timeValueEnabled = on, hourlyValue = hourly.ifBlank { it.hourlyValue }) } },
                    setReminders = { on -> usageViewModel.preferences { it.copy(reminders = on) } },
                    setMindfulPause = focusViewModel::setDelayEnabled,
                    setGreyOverLimit = { on -> scope.launch { quiet.setGreyOverLimit(on) } },
                    setTheme = viewModel::setTheme,
                    setPalette = viewModel::setPalette,
                    finish = { scope.launch { insights.setOnboardingVersion(ONBOARDING_VERSION) }; navController.returnHome() },
                ),
            )
        }
        composable(VISUAL) {
            VisualInterventionsScreen(quietPrefs, focusState.preferences.focusGreyscale, focusState.schedules, quiet,
                focusViewModel::setFocusGreyscale) { navController.popBackStack() }
        }
        composable(QUIET) {
            if (!access.allows(ProFeature.QUIET_NOTIFICATIONS)) { ProScreen(yoga.proRepository) { navController.popBackStack() }; return@composable }
            val heldRules by quiet.rules.collectAsState(initial = emptyList())
            QuietNotificationsScreen(quietPrefs, uiState.drawerApps + uiState.otherApps, heldRules,
                isDistracting = focusState::isDistracting,
                isEssential = { focusState.classifications[it]?.essential == true },
                repository = quiet, onOpenDigest = { navController.navigate(DIGEST) }) { navController.popBackStack() }
        }
        composable(DIGEST) {
            if (!access.allows(ProFeature.QUIET_NOTIFICATIONS)) { ProScreen(yoga.proRepository) { navController.popBackStack() }; return@composable }
            val held by quiet.held.collectAsState(initial = emptyList())
            val labels = uiState.allApps.associate { it.appPackage to it.displayLabel }
            DigestScreen(held, { labels[it] ?: it }, onOpenApp = { pkg ->
                uiState.allApps.firstOrNull { it.appPackage == pkg }?.let { requestLaunch(it) }
            }, repository = quiet) { navController.popBackStack() }
        }
        composable(CATEGORIES) {
            CategoriesScreen(uiState, viewModel) { navController.popBackStack() }
        }
        composable(FOCUS_SETTINGS) {
            FocusSettingsScreen(focusState, uiState.drawerApps, focusViewModel) { navController.popBackStack() }
        }
        composable(HIDDEN_APPS) {
            HiddenAppsScreen(uiState.hiddenApps, { navController.popBackStack() }) { viewModel.setHidden(it, false) }
        }
    }

    if (route == HOME) {
        val categoryNames = uiState.categories.associate { it.id to it.name }
        AppDrawerLayer(
            controller = drawer,
            apps = uiState.drawerApps,
            recentSearches = uiState.preferences.recentSearches,
            fontSize = homeFontSize(uiState.preferences.fontSize),
            pickingTitle = replacingApp?.let { "Replace ${it.displayLabel}" }
                ?: pickingDockSlot?.let { "Choose an app for the dock" }
                ?: "Choose apps for home".takeIf { homePicking },
            autoOpenSingleMatch = uiState.preferences.autoOpenSingleMatch,
            isSelected = if (homePicking) viewModel::isOnHome else null,
            onAppClick = { selected, query ->
                val replaced = replacingApp
                val slot = pickingDockSlot
                when {
                    replaced != null -> { viewModel.setFavorite(selected, true, replaced.appPackage); drawer.close() }
                    slot != null -> { viewModel.setDockSlot(slot, selected.appPackage); drawer.close() }
                    homePicking -> {
                        val onHome = viewModel.isOnHome(selected)
                        val limit = if (uiState.activeProfile == null) uiState.preferences.homeAppCount else MAX_PROFILE_APPS
                        if (onHome || uiState.favoriteApps.size < limit) viewModel.setFavorite(selected, !onHome)
                        else Toast.makeText(context, "Home holds $limit apps. Remove one first.", Toast.LENGTH_SHORT).show()
                    }
                    selected.isWork && selected.workPaused -> workPrompt = selected
                    else -> requestLaunch(selected, query)
                }
            },
            onAppLongClick = { drawerMenuApp = it },
            onClosed = { replacingApp = null; pickingDockSlot = null; homePicking = false },
            isPaused = { focusState.pausedByFocus(it.appPackage) },
            groupOf = categoryNames.takeIf { uiState.organize.groupDrawerByCategory }?.let { names ->
                { app: AppModel -> names[uiState.categoryOf[app.appPackage]] ?: "Other" }
            },
            otherApps = uiState.otherApps,
            workApps = workApps,
        )
    }
    LaunchedEffect(route) { if (route != HOME) drawer.snapClosed() }

    workPrompt?.let { app ->
        YogaDialog(
            title = "Turn on work apps?",
            confirmText = "Turn on",
            onConfirm = {
                workPrompt = null
                if (requestWorkProfileOn(context, app.user)) pendingWork = app
                else {
                    Toast.makeText(context, "Turn on work apps in Settings", Toast.LENGTH_SHORT).show()
                    runCatching { context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)) }
                }
            },
            onDismissRequest = { workPrompt = null },
            text = { Text("Your work profile is paused. Turn it on to open ${app.displayLabel}.") },
        )
    }
    // Open the app that was tapped once its profile is on. Give up after a while (the PIN may have been cancelled).
    LaunchedEffect(workApps, pendingWork) {
        val wanted = pendingWork ?: return@LaunchedEffect
        workApps.firstOrNull { it.user == wanted.user && it.appPackage == wanted.appPackage &&
            it.activityClassName == wanted.activityClassName && !it.workPaused }?.let { pendingWork = null; requestLaunch(it) }
    }
    LaunchedEffect(pendingWork) { if (pendingWork != null) { kotlinx.coroutines.delay(30_000); pendingWork = null } }

    if (showHomeOptions) HomeOptionsSheet(
        onAddApps = { homePicking = true; drawer.open() },
        onWidgets = { navController.navigate(WIDGETS) },
        onAppearance = { navController.navigate(APPEARANCE) },
        onSettings = { navController.navigate(SETTINGS) },
        onDismiss = { showHomeOptions = false },
    )

    dockMenuSlot?.let { slot ->
        val app = slot.app
        if (app != null) AppActionSheet(
            app,
            listOf(
                AppMenuAction("Change app") { pickingDockSlot = slot.index; drawer.open() },
                AppMenuAction("Use the phone's ${DockRole.entries.getOrNull(slot.index)?.label?.lowercase() ?: "default"} app",
                    slot.role == null && slot.index < DockRole.entries.size) { viewModel.setDockSlot(slot.index, DOCK_DEFAULT) },
                AppMenuAction("Remove from dock") { viewModel.setDockSlot(slot.index, DOCK_EMPTY) },
            ),
        ) { dockMenuSlot = null }
    }

    favoriteMenuApp?.let { app ->
        val index = uiState.favoriteApps.indexOfFirst { it.appPackage == app.appPackage }
        AppActionSheet(
            app,
            listOf(
                AppMenuAction("Replace") { replacingApp = app; pickingDockSlot = null; drawer.open() },
                AppMenuAction("Remove from home") { viewModel.setFavorite(app, false) },
                AppMenuAction("Rename") { renameApp = app },
                AppMenuAction("Move up", index > 0) { viewModel.moveFavorite(app, -1) },
                AppMenuAction("Move down", index in 0 until uiState.favoriteApps.lastIndex) { viewModel.moveFavorite(app, 1) },
                AppMenuAction("App info") { context.openAppInfo(app.appPackage) },
            ),
        ) { favoriteMenuApp = null }
    }
    drawerMenuApp?.let { app ->
        AppActionSheet(
            app,
            listOf(
                AppMenuAction(if (viewModel.isOnHome(app)) "Remove from home" else "Add to home") {
                    viewModel.setFavorite(app, !viewModel.isOnHome(app))
                },
                AppMenuAction("Category…") { categoryFor = app },
                AppMenuAction("Rename") { renameApp = app },
                AppMenuAction(if (access.allows(ProFeature.TIMEBOX)) "Timebox this app" else "Timebox this app \u00b7 Pro") { access.run(ProFeature.TIMEBOX) { timeboxStartFor = app; showTimeboxStart = true } },
                AppMenuAction("Hide") { viewModel.setHidden(app, true) },
                AppMenuAction("App info") { context.openAppInfo(app.appPackage) },
                AppMenuAction("Uninstall") { context.uninstall(app.appPackage) },
            ),
        ) { drawerMenuApp = null }
    }
    renameApp?.let { app -> RenameAppDialog(app, { renameApp = null }) { viewModel.renameApp(app, it) } }

    if (showProfiles && access.allows(ProFeature.PROFILES)) {
        ProfileSwitcherSheet(uiState, ::switchProfile, onManage = { navController.navigate(PROFILES) }) { showProfiles = false }
    }
    categoryFor?.let { app ->
        CategoryPickerDialog(app, uiState.categories, uiState.categoryOf[app.appPackage],
            onPick = { viewModel.setAppCategory(app, it) }, onAdd = { viewModel.addCategory(it, app) }, onDismiss = { categoryFor = null })
    }

    if (showTimeboxStart && access.allows(ProFeature.TIMEBOX)) {
        val bound = timeboxStartFor
        TimeboxStartSheet(
            lastMinutes = focusState.preferences.lastTimeboxMinutes,
            recentPurposes = focusState.preferences.timeboxPurposes,
            boundApp = bound,
            onStart = { minutes, purpose ->
                focusViewModel.startTimebox(minutes, purpose, bound?.appPackage)
                // A timeboxed app is a deliberate choice, so it opens without the pause.
                bound?.let { viewModel.launchApp(it) }
            },
            onDismiss = { showTimeboxStart = false; timeboxStartFor = null },
        )
    }
    val timebox = focusState.timebox
    val timeboxRemaining = focusState.timeboxRemainingMs
    if (showTimeboxControl && timebox != null && timeboxRemaining != null) {
        TimeboxControlSheet(
            session = timebox,
            remainingMs = timeboxRemaining,
            onPause = focusViewModel::pauseTimebox,
            onResume = focusViewModel::resumeTimebox,
            onStop = focusViewModel::stopTimebox,
            onExtend = focusViewModel::extendTimebox,
            onDismiss = { showTimeboxControl = false },
        )
    }
    if (timebox != null && timebox.status == TimeboxStatus.COMPLETED && !timebox.acknowledged && route == HOME) {
        TimeboxCompletionSheet(
            session = timebox,
            streak = focusState.streak,
            onDone = focusViewModel::acknowledgeTimebox,
            onExtend = focusViewModel::extendTimebox,
            onStartFocus = { showFocusStart = true },
        )
    }
    if (showFocusStart && access.allows(ProFeature.FOCUS)) {
        FocusStartSheet(focusState.preferences.lastFocusMinutes, focusViewModel::startFocus) { showFocusStart = false }
    }
    if (showFocusControl && focusState.focusActive) {
        FocusControlSheet(focusState, focusViewModel::endFocus) { showFocusControl = false }
    }

    reflectionFor?.let { focusId ->
        if (route == HOME && access.allows(ProFeature.INSIGHTS)) ReflectionSheet { feeling ->
            scope.launch { insights.recordReflection(focusId, feeling) }
            reflectionFor = null
        }
    }

    } // LocalProAccess

    pendingPause?.let { pause ->
        val overall = usageState.snapshot.evaluation.overall
        val usageLine = overall?.takeIf { usageState.snapshot.available }?.let {
            if (it.state == RuleState.OVER) "You're ${it.excessMinutes} minutes past today's limit."
            else "You've used ${it.used} of ${it.allowance} minutes today."
        }
        MindfulDelayScreen(
            app = pause.app,
            seconds = pause.seconds,
            duringFocus = pause.duringFocus,
            usageLine = usageLine,
            recentPurposes = focusState.preferences.delayPurposes,
            onContinue = { purpose ->
                focusViewModel.pauseOutcome(pause.app, continued = true, duringFocus = pause.duringFocus, purpose = purpose)
                viewModel.launchApp(pause.app, pause.query)
                pendingPause = null
            },
            onGoBack = {
                focusViewModel.pauseOutcome(pause.app, continued = false, duringFocus = pause.duringFocus, purpose = null)
                pendingPause = null
            },
        )
    }
}

private fun NavHostController.returnHome() {
    if (!popBackStack(HOME, false)) navigate(HOME) { launchSingleTop = true }
}

private fun Context.safeStart(vararg intents: Intent) {
    intents.firstOrNull { it.resolveActivity(packageManager) != null }?.let {
        runCatching { startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}

private fun Context.openClock() = safeStart(
    Intent(AlarmClock.ACTION_SHOW_ALARMS),
)

private fun Context.openCalendar() = safeStart(
    Intent(Intent.ACTION_VIEW, CalendarContract.CONTENT_URI),
    Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR),
)

private fun Context.openAppInfo(packageName: String) = safeStart(
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")),
)

private fun Context.uninstall(packageName: String) = safeStart(
    Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName")),
)

private fun Context.expandNotifications() {
    runCatching {
        val service = getSystemService("statusbar") ?: return
        service.javaClass.getMethod("expandNotificationsPanel").invoke(service)
    }
}

private fun workProfileFilter() = android.content.IntentFilter().apply {
    addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
    addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
    addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED)
    addAction(Intent.ACTION_MANAGED_PROFILE_ADDED)
    addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED)
}
