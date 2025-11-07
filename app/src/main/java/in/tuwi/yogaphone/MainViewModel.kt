package `in`.tuwi.yogaphone

import android.app.Application
import android.content.Context
import android.os.UserHandle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.tuwi.yogaphone.data.AppModel
import `in`.tuwi.yogaphone.data.Constants
import `in`.tuwi.yogaphone.data.Prefs
import `in`.tuwi.yogaphone.utils.getAppsList
import `in`.tuwi.yogaphone.utils.getUserHandleFromString
import `in`.tuwi.yogaphone.utils.launchApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class MainUiState(
    val allApps: List<AppModel> = emptyList(),
    val favoriteApps: List<AppModel> = emptyList(),
    val isLoading: Boolean = true,
    val theme: Int = Constants.THEME_DARK,
    val fontSize: Int = Constants.FONT_SIZE_MEDIUM,
    val isFirstLaunch: Boolean = true
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context = application.applicationContext
    private val prefs = Prefs(context)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Load preferences
            val theme = prefs.theme.first()
            val fontSize = prefs.fontSize.first()
            val isFirstLaunch = prefs.isFirstLaunch.first()

            _uiState.value = _uiState.value.copy(
                theme = theme,
                fontSize = fontSize,
                isFirstLaunch = isFirstLaunch
            )

            // Load apps immediately on startup
            loadApps()
        }
    }

    private suspend fun loadApps() {
        try {
            val allApps = getAppsList(context)
            val favoriteAppsData = prefs.getAllFavoriteApps()

            // Match favorites with installed apps
            val favoriteApps = favoriteAppsData.mapNotNull { fav ->
                allApps.find { it.appPackage == fav.packageName }?.copy(isFavorite = true)
            }

            _uiState.value = _uiState.value.copy(
                allApps = allApps,
                favoriteApps = favoriteApps,
                isLoading = false
            )
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun refreshApps() {
        viewModelScope.launch {
            // Don't show loading spinner when refreshing in background
            loadApps()
        }
    }

    private suspend fun updateFavoritesOnly() {
        // Update favorites without reloading all apps (faster)
        val favoriteAppsData = prefs.getAllFavoriteApps()
        val currentAllApps = _uiState.value.allApps

        val favoriteApps = favoriteAppsData.mapNotNull { fav ->
            currentAllApps.find { it.appPackage == fav.packageName }?.copy(isFavorite = true)
        }

        _uiState.value = _uiState.value.copy(favoriteApps = favoriteApps)
    }

    fun launchApp(appModel: AppModel) {
        launchApp(context, appModel)
    }

    fun setFavoriteApp(position: Int, appModel: AppModel) {
        viewModelScope.launch {
            prefs.setFavoriteApp(position, appModel)
            // Only update favorites, don't reload all apps
            updateFavoritesOnly()
        }
    }

    fun removeFavoriteApp(position: Int) {
        viewModelScope.launch {
            prefs.removeFavoriteApp(position)
            // Only update favorites, don't reload all apps
            updateFavoritesOnly()
        }
    }

    fun setTheme(theme: Int) {
        viewModelScope.launch {
            prefs.setTheme(theme)
            _uiState.value = _uiState.value.copy(theme = theme)
        }
    }

    fun setFontSize(fontSize: Int) {
        viewModelScope.launch {
            prefs.setFontSize(fontSize)
            _uiState.value = _uiState.value.copy(fontSize = fontSize)
        }
    }

    fun setFirstLaunchComplete() {
        viewModelScope.launch {
            prefs.setFirstLaunchComplete()
            _uiState.value = _uiState.value.copy(isFirstLaunch = false)
        }
    }
}
