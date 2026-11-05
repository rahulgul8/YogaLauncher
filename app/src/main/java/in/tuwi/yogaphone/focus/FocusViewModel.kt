package `in`.tuwi.yogaphone.focus

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.tuwi.yogaphone.YogaApplication
import `in`.tuwi.yogaphone.data.AppModel
import `in`.tuwi.yogaphone.data.db.FocusScheduleEntity
import `in`.tuwi.yogaphone.data.db.FocusSessionEntity
import `in`.tuwi.yogaphone.data.db.TimeboxSessionEntity
import `in`.tuwi.yogaphone.usage.AppClassification
import `in`.tuwi.yogaphone.usage.AppClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FocusUiState(
    val timebox: TimeboxSessionEntity? = null,
    val focus: FocusSessionEntity? = null,
    val preferences: FocusPreferences = FocusPreferences(),
    val schedules: List<FocusScheduleEntity> = emptyList(),
    val classifications: Map<String, AppClassification> = emptyMap(),
    val now: Long = System.currentTimeMillis(),
    val streak: Int = 0,
) {
    val focusActive: Boolean get() = focus != null

    fun isDistracting(pkg: String) = classifications[pkg]?.distracting == true

    /** Distracting apps not explicitly allowed are paused while focus is on. Essential apps never are. */
    fun pausedByFocus(pkg: String) = focusActive && isDistracting(pkg) && pkg !in preferences.focusAllowed

    val timeboxRemainingMs: Long? get() = timebox?.takeIf { it.isLive }?.clock?.remainingMs(now)
}

/** What the launcher should do when the user taps an app. */
sealed interface LaunchDecision {
    data object Open : LaunchDecision
    data class Pause(val seconds: Int, val duringFocus: Boolean) : LaunchDecision
}

class FocusViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as YogaApplication
    private val repository = app.focusRepository
    private val classifier = AppClassifier(application)
    private val ticker = MutableStateFlow(System.currentTimeMillis())
    private val streak = MutableStateFlow(0)

    private val classifications = combine(app.appRepository.apps, app.database.appEntryDao().observeAll()) { apps, entries ->
        val byPackage = entries.associateBy { it.packageName }
        apps.map { it.appPackage }.distinct().associateWith { classifier.classify(it, byPackage[it]) }
    }.flowOn(Dispatchers.IO)

    private val sessions = combine(repository.latestTimebox, repository.activeFocus, repository.schedules) { t, f, s -> Triple(t, f, s) }

    val state: StateFlow<FocusUiState> = combine(sessions, repository.prefs.preferences, classifications, ticker, streak) { s, p, c, now, k ->
        FocusUiState(s.first, s.second, p, s.third, c, now, k)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusUiState())

    init {
        viewModelScope.launch {
            while (true) { ticker.value = System.currentTimeMillis(); delay(15_000) }
        }
    }

    /** Called when the launcher comes to the foreground. */
    fun refresh() = viewModelScope.launch {
        ticker.value = System.currentTimeMillis()
        runCatching { repository.reconcile() }
        runCatching {
            app.organizeRepository.reconcileSchedules()?.profile?.let { if (it.focusOnActivate) repository.startFocus(null) }
        }
        runCatching { app.quietRepository.reconcileGreyscale() }
        streak.value = runCatching { repository.streak() }.getOrDefault(0)
    }

    suspend fun decide(app: AppModel): LaunchDecision {
        val current = state.value
        val pkg = app.appPackage
        val prefs = repository.prefs.preferences.first()
        val focusPause = current.pausedByFocus(pkg)
        val delayPause = prefs.delayEnabled && current.isDistracting(pkg) && pkg !in prefs.delayOptOut
        if (!focusPause && !delayPause) return LaunchDecision.Open
        if (!DelayGate.shouldPause(repository.prefs.lastContinuedAt(pkg), System.currentTimeMillis(), prefs.dontAskMinutes)) {
            return LaunchDecision.Open
        }
        return LaunchDecision.Pause(prefs.delaySeconds + if (focusPause) 5 else 0, focusPause)
    }

    fun pauseOutcome(app: AppModel, continued: Boolean, duringFocus: Boolean, purpose: String?) = viewModelScope.launch {
        repository.recordIntervention(if (duringFocus) "FOCUS_OVERRIDE" else "DELAY", app.appPackage,
            if (continued) "CONTINUED" else "WENT_BACK", purpose)
        if (continued) {
            repository.prefs.recordDelayContinue(app.appPackage, purpose, System.currentTimeMillis())
            if (duringFocus) repository.recordFocusOverride()
        }
    }

    fun startTimebox(minutes: Int, purpose: String?, boundPackage: String?) = launch { repository.startTimebox(minutes, purpose, boundPackage) }
    fun pauseTimebox() = launch { repository.pauseTimebox() }
    fun resumeTimebox() = launch { repository.resumeTimebox() }
    fun stopTimebox() = launch { repository.stopTimebox() }
    fun extendTimebox(minutes: Int) = launch { repository.extendTimebox(minutes) }
    fun acknowledgeTimebox() = launch { repository.acknowledgeTimebox() }

    fun startFocus(minutes: Int?) = launch { repository.startFocus(minutes); app.quietRepository.reconcileGreyscale() }
    fun endFocus() = launch { repository.endFocus(); app.quietRepository.reconcileGreyscale() }

    fun saveSchedule(schedule: FocusScheduleEntity) = launch { repository.saveSchedule(schedule) }
    fun deleteSchedule(id: Long) = launch { repository.deleteSchedule(id) }

    fun setDelayEnabled(value: Boolean) = launch { repository.prefs.setDelayEnabled(value) }
    fun setDelaySeconds(value: Int) = launch { repository.prefs.setDelaySeconds(value) }
    fun setDontAskMinutes(value: Int) = launch { repository.prefs.setDontAskMinutes(value) }
    fun setDelayOptOut(pkg: String, value: Boolean) = launch { repository.prefs.setDelayOptOut(pkg, value) }
    fun setFocusAllowed(pkg: String, value: Boolean) = launch { repository.prefs.setFocusAllowed(pkg, value) }
    fun setFocusGreyscale(value: Boolean) = launch { repository.prefs.setFocusGreyscale(value); app.quietRepository.reconcileGreyscale() }
    fun setFocusDnd(value: Boolean) = launch { repository.prefs.setFocusDnd(value) }

    fun system() = repository.system()

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
            // Tick after the change so "time left" is measured from the new state, not a stale clock.
            ticker.value = System.currentTimeMillis()
        }
    }
}
