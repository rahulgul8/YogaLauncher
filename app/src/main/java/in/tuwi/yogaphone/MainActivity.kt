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

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
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
        setContent { YogaLauncherApp(viewModel, homeRequest) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) homeRequest++
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(appChangeReceiver) }
        super.onDestroy()
    }
}

@Composable
private fun YogaLauncherApp(viewModel: MainViewModel, homeRequest: Int) {
    val uiState by viewModel.uiState.collectAsState()
    val activity = androidx.compose.ui.platform.LocalContext.current as ComponentActivity
    val darkIcons = themeUsesDarkIcons(uiState.preferences.themeMode)
    SideEffect {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { !darkIcons },
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { !darkIcons },
        )
    }
    YogaPhoneTheme(uiState.preferences.themeMode) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            LauncherNavigation(viewModel, uiState, homeRequest)
        }
    }
}

@Composable
private fun LauncherNavigation(viewModel: MainViewModel, uiState: MainUiState, homeRequest: Int) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val navController = rememberNavController()
    val route = navController.currentBackStackEntryAsState().value?.destination?.route
    var showDrawer by remember { mutableStateOf(false) }
    var replacingApp by remember { mutableStateOf<AppModel?>(null) }
    var favoriteMenuApp by remember { mutableStateOf<AppModel?>(null) }
    var drawerMenuApp by remember { mutableStateOf<AppModel?>(null) }
    var renameApp by remember { mutableStateOf<AppModel?>(null) }

    LaunchedEffect(homeRequest) {
        if (homeRequest > 0) {
            showDrawer = false
            replacingApp = null
            favoriteMenuApp = null
            drawerMenuApp = null
            renameApp = null
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
            HomeScreen(
                uiState = uiState,
                onAppClick = { viewModel.launchApp(it) },
                onAppLongClick = { favoriteMenuApp = it },
                onOpenAppDrawer = { showDrawer = true; replacingApp = null },
                onOpenSettings = { navController.navigate(SETTINGS) },
                onOpenClock = { context.openClock() },
                onOpenCalendar = { context.openCalendar() },
                onExpandNotifications = { context.expandNotifications() },
                modifier = Modifier,
            )
        }
        composable(SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onAppearance = { navController.navigate(APPEARANCE) },
                onHiddenApps = { navController.navigate(HIDDEN_APPS) },
                onResetNames = viewModel::resetCustomLabels,
            )
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
                else viewModel.launchApp(selected, query)
                showDrawer = false
                replacingApp = null
            },
            onAppLongClick = { drawerMenuApp = it; showDrawer = false },
            onDismiss = { showDrawer = false; replacingApp = null },
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
                AppMenuAction("Hide") { viewModel.setHidden(app, true) },
                AppMenuAction("App info") { context.openAppInfo(app.appPackage) },
                AppMenuAction("Uninstall") { context.uninstall(app.appPackage) },
            ),
        ) { drawerMenuApp = null }
    }
    renameApp?.let { app -> RenameAppDialog(app, { renameApp = null }) { viewModel.renameApp(app, it) } }
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
