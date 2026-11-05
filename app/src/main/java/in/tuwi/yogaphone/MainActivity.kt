package `in`.tuwi.yogaphone

import android.content.Context
import android.content.Intent
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
import `in`.tuwi.yogaphone.ui.AppDrawerBottomSheet
import `in`.tuwi.yogaphone.ui.AppMenuAction
import `in`.tuwi.yogaphone.ui.AppearanceSettingsScreen
import `in`.tuwi.yogaphone.ui.HiddenAppsScreen
import `in`.tuwi.yogaphone.ui.HomeScreen
import `in`.tuwi.yogaphone.ui.RenameAppDialog
import `in`.tuwi.yogaphone.ui.SettingsScreen
import `in`.tuwi.yogaphone.ui.homeFontSize
import `in`.tuwi.yogaphone.ui.theme.YogaPhoneTheme
import `in`.tuwi.yogaphone.ui.theme.themeUsesDarkIcons

private const val HOME = "home"
private const val SETTINGS = "settings"
private const val HIDDEN_APPS = "settings/hidden_apps"
private const val APPEARANCE = "settings/appearance"
private const val FOCUS_SETTINGS = "settings/focus"

private data class PendingPause(val app: AppModel, val query: String, val seconds: Int, val duringFocus: Boolean)

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val usageViewModel: UsageViewModel by viewModels()
    private val focusViewModel: FocusViewModel by viewModels()
    private var usageRequest by mutableIntStateOf(0)
    private lateinit var appChangeReceiver: AppChangeReceiver
    private var homeRequest by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appChangeReceiver = AppChangeReceiver(viewModel::refreshApps)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(appChangeReceiver, AppChangeReceiver.getIntentFilter(), RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION") registerReceiver(appChangeReceiver, AppChangeReceiver.getIntentFilter())
        }
        if (intent.action == UsageNotifications.ACTION_DETAILS) usageRequest++
        setContent { YogaLauncherApp(viewModel, usageViewModel, focusViewModel, homeRequest, usageRequest) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == UsageNotifications.ACTION_DETAILS) usageRequest++
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) homeRequest++
        if (intent.action == FocusSystem.ACTION_OPEN_TIMEBOX) homeRequest++
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(appChangeReceiver) }
        super.onDestroy()
    }
}

@Composable
private fun YogaLauncherApp(viewModel: MainViewModel, usageViewModel: UsageViewModel, focusViewModel: FocusViewModel, homeRequest: Int, usageRequest: Int) {
    val uiState by viewModel.uiState.collectAsState()
    val focusState by focusViewModel.state.collectAsState()
    // Greyscale during focus is an override; the chosen theme is never rewritten.
    val themeMode = if (focusState.focusActive && focusState.preferences.focusGreyscale) ThemeMode.GREYSCALE else uiState.preferences.themeMode
    val activity = androidx.compose.ui.platform.LocalContext.current as ComponentActivity
    val darkIcons = themeUsesDarkIcons(themeMode)
    SideEffect {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { !darkIcons },
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { !darkIcons },
        )
    }
    YogaPhoneTheme(themeMode) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            LauncherNavigation(viewModel, usageViewModel, focusViewModel, focusState, uiState, homeRequest, usageRequest)
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
    usageRequest: Int,
) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val navController = rememberNavController()
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
    LaunchedEffect(usageRequest) {
        if (usageRequest > 0) navController.navigate("usage?details=true") { launchSingleTop = true }
    }
    val route = navController.currentBackStackEntryAsState().value?.destination?.route
    var showDrawer by remember { mutableStateOf(false) }
    var replacingApp by remember { mutableStateOf<AppModel?>(null) }
    var favoriteMenuApp by remember { mutableStateOf<AppModel?>(null) }
    var drawerMenuApp by remember { mutableStateOf<AppModel?>(null) }
    var renameApp by remember { mutableStateOf<AppModel?>(null) }
    var pendingPause by remember { mutableStateOf<PendingPause?>(null) }
    var timeboxStartFor by remember { mutableStateOf<AppModel?>(null) }
    var showTimeboxStart by remember { mutableStateOf(false) }
    var showTimeboxControl by remember { mutableStateOf(false) }
    var showFocusStart by remember { mutableStateOf(false) }
    var showFocusControl by remember { mutableStateOf(false) }

    // Every launch from Yoga goes through the mindful-pause gate.
    fun requestLaunch(app: AppModel, query: String = "") {
        scope.launch {
            when (val decision = focusViewModel.decide(app)) {
                LaunchDecision.Open -> viewModel.launchApp(app, query)
                is LaunchDecision.Pause -> pendingPause = PendingPause(app, query, decision.seconds, decision.duringFocus)
            }
        }
    }

    LaunchedEffect(homeRequest) {
        if (homeRequest > 0) {
            showDrawer = false
            replacingApp = null
            favoriteMenuApp = null
            drawerMenuApp = null
            renameApp = null
            showTimeboxStart = false
            showTimeboxControl = false
            showFocusStart = false
            showFocusControl = false
            navController.returnHome()
        }
    }
    BackHandler(enabled = route == HOME && !showDrawer) { /* A HOME activity stays home. */ }

    NavHost(
        navController = navController,
        startDestination = HOME,
        modifier = Modifier.statusBarsPadding().navigationBarsPadding(),
    ) {
        composable(HOME) {
            val homeState = if (focusState.focusActive)
                uiState.copy(favoriteApps = uiState.favoriteApps.filterNot { focusState.pausedByFocus(it.appPackage) })
            else uiState
            HomeScreen(
                uiState = homeState,
                onAppClick = { requestLaunch(it) },
                onAppLongClick = { favoriteMenuApp = it },
                onOpenAppDrawer = { showDrawer = true; replacingApp = null },
                onOpenSettings = { navController.navigate(SETTINGS) },
                onOpenClock = { context.openClock() },
                onOpenCalendar = { context.openCalendar() },
                onExpandNotifications = { context.expandNotifications() },
                statusContent = {
                    Column {
                        HomeFocusStatus(focusState, onTimebox = { showTimeboxControl = true }, onFocus = { showFocusControl = true })
                        HomeUsageIndicator(usageState, { navController.navigate("usage") }, { navController.navigate("usage?details=true") })
                    }
                },
                quickActions = {
                    HomeQuickActions(
                        onTimebox = {
                            if (focusState.timeboxRemainingMs != null) showTimeboxControl = true
                            else { timeboxStartFor = null; showTimeboxStart = true }
                        },
                        onFocus = { showFocusStart = true },
                        focusActive = focusState.focusActive,
                    )
                },
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
                onFocus = { navController.navigate(FOCUS_SETTINGS) },
            )
        }
        composable("usage?details={details}", arguments = listOf(androidx.navigation.navArgument("details") { defaultValue = "false" })) { entry ->
            UsageDashboardScreen(usageState, usageViewModel, { navController.popBackStack() },
                { navController.navigate("settings/usage") }, entry.arguments?.getString("details") == "true")
        }
        composable("settings/usage") {
            UsageSettingsScreen(usageState, usageViewModel, { navController.popBackStack() },
                { navController.navigate("usage") }, { navController.navigate("settings/value") })
        }
        composable("settings/value") {
            ValueOfTimeSettingsScreen(usageState, usageViewModel, { navController.popBackStack() })
        }
        composable(APPEARANCE) {
            AppearanceSettingsScreen(
                preferences = uiState.preferences,
                onBack = { navController.popBackStack() },
                onTheme = viewModel::setTheme,
                onFontSize = viewModel::setFontSize,
                onShowClock = viewModel::setShowClock,
                onShowDate = viewModel::setShowDate,
                onHomeAppCount = viewModel::setHomeAppCount,
                onAlignment = viewModel::setHomeTextAlignment,
            )
        }
        composable(FOCUS_SETTINGS) {
            FocusSettingsScreen(focusState, uiState.drawerApps, focusViewModel) { navController.popBackStack() }
        }
        composable(HIDDEN_APPS) {
            HiddenAppsScreen(uiState.hiddenApps, { navController.popBackStack() }) { viewModel.setHidden(it, false) }
        }
    }

    if (showDrawer) {
        AppDrawerBottomSheet(
            apps = uiState.drawerApps,
            recentSearches = uiState.preferences.recentSearches,
            fontSize = homeFontSize(uiState.preferences.fontSize),
            replacingApp = replacingApp,
            onAppClick = { selected, query ->
                val replaced = replacingApp
                if (replaced != null) viewModel.setFavorite(selected, true, replaced.appPackage)
                else requestLaunch(selected, query)
                showDrawer = false
                replacingApp = null
            },
            onAppLongClick = { drawerMenuApp = it; showDrawer = false },
            onDismiss = { showDrawer = false; replacingApp = null },
            isPaused = { focusState.pausedByFocus(it.appPackage) },
        )
    }

    favoriteMenuApp?.let { app ->
        val index = uiState.favoriteApps.indexOfFirst { it.appPackage == app.appPackage }
        AppActionSheet(
            app,
            listOf(
                AppMenuAction("Replace") { replacingApp = app; showDrawer = true },
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
                AppMenuAction(if (app.isFavorite) "Remove from home" else "Add to home") {
                    viewModel.setFavorite(app, !app.isFavorite)
                },
                AppMenuAction("Rename") { renameApp = app },
                AppMenuAction("Timebox this app") { timeboxStartFor = app; showTimeboxStart = true },
                AppMenuAction("Hide") { viewModel.setHidden(app, true) },
                AppMenuAction("App info") { context.openAppInfo(app.appPackage) },
                AppMenuAction("Uninstall") { context.uninstall(app.appPackage) },
            ),
        ) { drawerMenuApp = null }
    }
    renameApp?.let { app -> RenameAppDialog(app, { renameApp = null }) { viewModel.renameApp(app, it) } }

    if (showTimeboxStart) {
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
    if (showFocusStart) {
        FocusStartSheet(focusState.preferences.lastFocusMinutes, focusViewModel::startFocus) { showFocusStart = false }
    }
    if (showFocusControl && focusState.focusActive) {
        FocusControlSheet(focusState, focusViewModel::endFocus) { showFocusControl = false }
    }

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

