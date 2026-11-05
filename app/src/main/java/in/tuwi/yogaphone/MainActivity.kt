package `in`.tuwi.yogaphone

import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import `in`.tuwi.yogaphone.receiver.AppChangeReceiver
import `in`.tuwi.yogaphone.ui.AppDrawerBottomSheet
import `in`.tuwi.yogaphone.ui.HomeScreen
import `in`.tuwi.yogaphone.ui.theme.YogaPhoneTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var appChangeReceiver: AppChangeReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Register broadcast receiver for app changes
        appChangeReceiver = AppChangeReceiver {
            viewModel.refreshApps()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                appChangeReceiver,
                AppChangeReceiver.getIntentFilter(),
                RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(appChangeReceiver, AppChangeReceiver.getIntentFilter())
        }

        setContent {
            YogaPhoneTheme {
                YogaLauncherApp(viewModel = viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(appChangeReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun YogaLauncherApp(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAppDrawer by remember { mutableStateOf(false) }
    var selectingFavoritePosition by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        // Ignore scaffold padding, handle insets manually for full-screen experience
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            HomeScreen(
                uiState = uiState,
                onAppClick = { app ->
                    viewModel.launchApp(app)
                },
                onAppLongClick = { app, position ->
                    // Long press on favorite app - show app drawer to replace it
                    selectingFavoritePosition = position
                    showAppDrawer = true
                },
                onOpenAppDrawer = {
                    // Only open if apps are loaded
                    if (!uiState.isLoading && uiState.allApps.isNotEmpty()) {
                        selectingFavoritePosition = null
                        showAppDrawer = true
                    }
                },
                modifier = Modifier.navigationBarsPadding()
            )

            // App drawer bottom sheet
            if (showAppDrawer && uiState.allApps.isNotEmpty()) {
                AppDrawerBottomSheet(
                    apps = uiState.allApps,
                    fontSize = getFontSize(uiState.fontSize),
                    onAppClick = { app ->
                        if (selectingFavoritePosition != null) {
                            // Setting a favorite
                            viewModel.setFavoriteApp(selectingFavoritePosition!!, app)
                            showAppDrawer = false
                            selectingFavoritePosition = null
                        } else {
                            // Launching app
                            viewModel.launchApp(app)
                            showAppDrawer = false
                        }
                    },
                    onDismiss = {
                        showAppDrawer = false
                        selectingFavoritePosition = null
                    },
                    selectingFavoritePosition = selectingFavoritePosition
                )
            }
        }
    }
}

@Composable
private fun getFontSize(fontSizeConstant: Int): Int {
    return when (fontSizeConstant) {
        0 -> 20 // Small
        1 -> 24 // Medium
        2 -> 28 // Large
        else -> 24
    }
}