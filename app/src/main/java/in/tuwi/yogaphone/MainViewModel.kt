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
import `in`.tuwi.yogaphone.data.db.CategoryEntity
import `in`.tuwi.yogaphone.data.db.ProfileEntity
import `in`.tuwi.yogaphone.profiles.Categories
import `in`.tuwi.yogaphone.profiles.HomeList
import `in`.tuwi.yogaphone.profiles.OrganizePreferences

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

const val MAX_PROFILE_APPS = 12

data class MainUiState(
    val allApps: List<AppModel> = emptyList(),
    val drawerApps: List<AppModel> = emptyList(),
    val favoriteApps: List<AppModel> = emptyList(),
    val hiddenApps: List<AppModel> = emptyList(),
    val preferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = true,
    val profiles: List<ProfileEntity> = emptyList(),
    val activeProfile: ProfileEntity? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val categoryOf: Map<String, String> = emptyMap(),
    val organize: OrganizePreferences = OrganizePreferences(),
    /** Apps the active profile hides from the drawer (still reachable through "All apps"). */
    val otherApps: List<AppModel> = emptyList(),
) {
    val activeProfileName: String get() = activeProfile?.name ?: "Personal"
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as YogaApplication
    private val repository = app.appRepository
    private val prefs = app.prefs
    private val loading = MutableStateFlow(true)

    private val organizeRepo = app.organizeRepository
    private val organizeState = combine(organizeRepo.preferences, organizeRepo.profiles, organizeRepo.categories,
        organizeRepo.categoryMap(repository.apps)) { o, p, c, m -> OrganizeSnapshot(o, p, c, m) }

    val uiState: StateFlow<MainUiState> = combine(repository.apps, prefs.userPreferences, loading, organizeState) { apps, preferences, busy, org ->
        val visible = apps.filterNot(AppModel::isHidden).sorted()
        val active = org.profiles.firstOrNull { it.id == org.preferences.activeProfileId }
        val favorites = if (active == null) {
            apps.filter(AppModel::isFavorite).sortedBy(AppModel::favoriteOrder).take(preferences.homeAppCount)
        } else {
            val byPackage = visible.associateBy { it.appPackage }
            HomeList.decode(active.homeApps).mapNotNull { byPackage[it] }
        }
        val shown = if (active?.hideOtherApps == true) {
            val keep = HomeList.decode(active.homeApps).toSet()
            visible.filter { it.appPackage in keep || org.categoryOf[it.appPackage] == Categories.ESSENTIALS }
        } else visible
        MainUiState(
            allApps = apps,
            drawerApps = shown,
            favoriteApps = favorites,
            hiddenApps = apps.filter(AppModel::isHidden).sorted(),
            preferences = preferences,
            isLoading = busy,
            profiles = org.profiles,
            activeProfile = active,
            categories = org.categories,
            categoryOf = org.categoryOf,
            organize = org.preferences,
            otherApps = visible - shown.toSet(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    private data class OrganizeSnapshot(
        val preferences: OrganizePreferences,
        val profiles: List<ProfileEntity>,
        val categories: List<CategoryEntity>,
        val categoryOf: Map<String, String>,
    )

    init {
        viewModelScope.launch {
            runCatching { repository.initialize() }
            runCatching { organizeRepo.ensureBuiltIns() }
            loading.value = false
        }
    }

    fun refreshApps() = launch { repository.refreshApps() }
    fun launchApp(appModel: AppModel, searchTerm: String = "") {
        if (searchTerm.isNotBlank()) launch { prefs.recordRecentSearch(searchTerm) }
        launchApp(getApplication(), appModel)
    }

    /** Home edits go to the active profile's list, or to the default home when no profile is active. */
    fun setFavorite(app: AppModel, favorite: Boolean, replacePackage: String? = null) = launch {
        val profile = uiState.value.activeProfile
        if (profile == null) repository.setFavorite(app.appPackage, favorite, replacePackage)
        else organizeRepo.editHomeApps(profile.id) { list ->
            when {
                replacePackage != null -> HomeList.replace(list, replacePackage, app.appPackage)
                favorite -> HomeList.add(list, app.appPackage, MAX_PROFILE_APPS)
                else -> list - app.appPackage
            }
        }
    }
    fun moveFavorite(app: AppModel, delta: Int) = launch {
        val profile = uiState.value.activeProfile
        if (profile == null) repository.moveFavorite(app.appPackage, delta)
        else organizeRepo.editHomeApps(profile.id) { HomeList.move(it, app.appPackage, delta) }
    }
    /** Makes the home list exactly [apps], in order (used by the setup walkthrough). */
    fun setHomeApps(apps: List<AppModel>) = launch {
        val wanted = apps.map { it.appPackage }
        uiState.value.favoriteApps.filter { it.appPackage !in wanted }.forEach { repository.setFavorite(it.appPackage, false) }
        val current = uiState.value.favoriteApps.map { it.appPackage }.toSet()
        wanted.filter { it !in current }.forEach { repository.setFavorite(it, true) }
    }
    fun isOnHome(app: AppModel): Boolean = uiState.value.favoriteApps.any { it.appPackage == app.appPackage }

    fun switchProfile(id: Long?, onSwitched: (previous: ProfileEntity?, next: ProfileEntity?) -> Unit = { _, _ -> }) = launch {
        val previous = uiState.value.activeProfile
        val next = organizeRepo.setActive(id, manual = true)
        onSwitched(previous, next)
    }
    fun createProfile(name: String, fromCurrentHome: Boolean, essentialsOnly: Boolean, onCreated: (Long) -> Unit = {}) = launch {
        val state = uiState.value
        val apps = when {
            essentialsOnly -> state.drawerApps.filter { state.categoryOf[it.appPackage] == Categories.ESSENTIALS }.map { it.appPackage }
            fromCurrentHome -> state.favoriteApps.map { it.appPackage }
            else -> emptyList()
        }.take(MAX_PROFILE_APPS)
        val id = organizeRepo.createProfile(name, apps)
        if (essentialsOnly) organizeRepo.profile(id)?.let { organizeRepo.saveProfile(it.copy(hideOtherApps = true)) }
        onCreated(id)
    }
    fun saveProfile(profile: ProfileEntity) = launch { organizeRepo.saveProfile(profile) }
    fun deleteProfile(id: Long) = launch { organizeRepo.deleteProfile(id) }
    fun editProfileApps(id: Long, transform: (List<String>) -> List<String>) = launch { organizeRepo.editHomeApps(id, transform) }
    fun setAppCategory(app: AppModel, categoryId: String?) = launch { organizeRepo.setAppCategory(app.appPackage, categoryId) }
    fun addCategory(name: String, assignTo: AppModel? = null) = launch {
        val id = organizeRepo.addCategory(name)
        assignTo?.let { organizeRepo.setAppCategory(it.appPackage, id) }
    }
    fun renameCategory(category: CategoryEntity, name: String) = launch { organizeRepo.renameCategory(category, name) }
    fun deleteCategory(category: CategoryEntity) = launch { organizeRepo.deleteCategory(category) }
    fun setGroupDrawer(value: Boolean) = launch { organizeRepo.setGroupDrawer(value) }
    fun renameApp(app: AppModel, label: String?) = launch { repository.rename(app.appPackage, label) }
    fun setHidden(app: AppModel, hidden: Boolean) = launch { repository.setHidden(app.appPackage, hidden) }
    fun resetCustomLabels() = launch { repository.resetCustomLabels() }
    fun setTheme(value: ThemeMode) = launch { prefs.setThemeMode(value) }
    fun setFontSize(value: FontSizeOption) = launch { prefs.setFontSize(value) }
    fun setShowClock(value: Boolean) = launch { prefs.setShowClock(value) }
    fun setShowDate(value: Boolean) = launch { prefs.setShowDate(value) }
    fun setHomeAppCount(value: Int) = launch { prefs.setHomeAppCount(value) }
    fun setHomeTextAlignment(value: HomeTextAlignment) = launch { prefs.setHomeTextAlignment(value) }
    fun setDockSlot(index: Int, value: String) = launch { prefs.setDockSlot(index, value) }
    fun markHomeRoleAsked() = launch { prefs.markHomeRoleAsked() }

    private fun launch(block: suspend () -> Unit) { viewModelScope.launch { block() } }
}
