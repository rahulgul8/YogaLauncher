package `in`.tuwi.yogaphone

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.tuwi.yogaphone.data.AppModel
import `in`.tuwi.yogaphone.data.FontSizeOption
import `in`.tuwi.yogaphone.data.HomeTextAlignment
import `in`.tuwi.yogaphone.data.ThemeMode
import `in`.tuwi.yogaphone.data.UserPreferences
import `in`.tuwi.yogaphone.utils.launchApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val allApps: List<AppModel> = emptyList(),
    val drawerApps: List<AppModel> = emptyList(),
    val favoriteApps: List<AppModel> = emptyList(),
    val hiddenApps: List<AppModel> = emptyList(),
    val preferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = true,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as YogaApplication
    private val repository = app.appRepository
    private val prefs = app.prefs
    private val loading = MutableStateFlow(true)

    val uiState: StateFlow<MainUiState> = combine(repository.apps, prefs.userPreferences, loading) { apps, preferences, busy ->
        MainUiState(
            allApps = apps,
            drawerApps = apps.filterNot(AppModel::isHidden).sorted(),
            favoriteApps = apps.filter(AppModel::isFavorite).sortedBy(AppModel::favoriteOrder).take(preferences.homeAppCount),
            hiddenApps = apps.filter(AppModel::isHidden).sorted(),
            preferences = preferences,
            isLoading = busy,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    init {
        viewModelScope.launch {
            runCatching { repository.initialize() }
            loading.value = false
        }
    }

    fun refreshApps() = launch { repository.refreshApps() }
    fun launchApp(appModel: AppModel, searchTerm: String = "") {
        if (searchTerm.isNotBlank()) launch { prefs.recordRecentSearch(searchTerm) }
        launchApp(getApplication(), appModel)
    }

    fun setFavorite(app: AppModel, favorite: Boolean, replacePackage: String? = null) =
        launch { repository.setFavorite(app.appPackage, favorite, replacePackage) }
    fun moveFavorite(app: AppModel, delta: Int) = launch { repository.moveFavorite(app.appPackage, delta) }
    fun renameApp(app: AppModel, label: String?) = launch { repository.rename(app.appPackage, label) }
    fun setHidden(app: AppModel, hidden: Boolean) = launch { repository.setHidden(app.appPackage, hidden) }
    fun resetCustomLabels() = launch { repository.resetCustomLabels() }
    fun setTheme(value: ThemeMode) = launch { prefs.setThemeMode(value) }
    fun setFontSize(value: FontSizeOption) = launch { prefs.setFontSize(value) }
    fun setShowClock(value: Boolean) = launch { prefs.setShowClock(value) }
    fun setShowDate(value: Boolean) = launch { prefs.setShowDate(value) }
    fun setHomeAppCount(value: Int) = launch { prefs.setHomeAppCount(value) }
    fun setHomeTextAlignment(value: HomeTextAlignment) = launch { prefs.setHomeTextAlignment(value) }

    private fun launch(block: suspend () -> Unit) { viewModelScope.launch { block() } }
}
