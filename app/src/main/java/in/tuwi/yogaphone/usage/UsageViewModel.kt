package `in`.tuwi.yogaphone.usage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.tuwi.yogaphone.YogaApplication
import `in`.tuwi.yogaphone.data.AppModel
import `in`.tuwi.yogaphone.data.db.UsageRuleEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UsageUiState(val snapshot: UsageSnapshot = UsageSnapshot(), val preferences: UsagePreferences = UsagePreferences(),
    val rules: List<UsageRuleEntity> = emptyList(), val apps: List<AppModel> = emptyList(), val loading: Boolean = true)
class UsageViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as YogaApplication
    private val repository = app.usageRepository
    private val snapshot = MutableStateFlow(UsageSnapshot())
    private val loading = MutableStateFlow(true)
    val state = combine(snapshot, app.usagePrefs.preferences, repository.rules, app.appRepository.apps, loading) { s, p, r, a, l ->
        UsageUiState(s, p, r, a, l)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UsageUiState())
    private var refreshJob: Job? = null
    private var refreshAgain = false
    fun refresh() {
        if (refreshJob?.isActive == true) { refreshAgain = true; return }
        refreshJob = viewModelScope.launch {
            do {
                refreshAgain = false
            try { snapshot.value = repository.check(archiveYesterday = true) }
            catch (_: Exception) { snapshot.value = UsageSnapshot(access = UsageAccess.granted(app), error = "Usage could not be read. Please try again.") }
            finally { loading.value = false }
            } while (refreshAgain)
        }
    }
    fun preferences(transform: (UsagePreferences) -> UsagePreferences) = viewModelScope.launch {
        app.usagePrefs.update(transform); refresh()
    }
    fun saveRule(pkg: String?, minutes: Int?) = viewModelScope.launch { repository.saveRule(pkg, minutes); refresh() }
    fun saveCategoryRule(categoryId: String, minutes: Int?) = viewModelScope.launch { repository.saveCategoryRule(categoryId, minutes); refresh() }
    fun threshold(value: Int) = viewModelScope.launch { repository.setThreshold(value); refresh() }
    fun classify(pkg: String, essential: Boolean, distracting: Boolean) = viewModelScope.launch {
        repository.classify(pkg, essential, distracting); refresh()
    }
    fun snooze() = viewModelScope.launch { repository.snoozeToday() }
    fun clearHistory() = viewModelScope.launch { repository.clearHistory(); refresh() }
}

