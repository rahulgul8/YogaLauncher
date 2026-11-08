package `in`.tuwi.yogaphone.intro

import android.content.Context
import androidx.datastore.preferences.core.*
import `in`.tuwi.yogaphone.data.dataStore
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Selection and impression are one DataStore transaction, shared by all three surfaces. */
class IntroStore(private val context: Context) {
    companion object { val onboardingFinished = longPreferencesKey("intro_onboarding_finished") }
    private val lastNew = stringPreferencesKey("intro_last_new")
    private val home = stringPreferencesKey("intro_home_shown")
    private val homeId = stringPreferencesKey("intro_home_id")
    private val pending = stringPreferencesKey("intro_pending_guard")
    private fun key(id: IntroId) = stringPreferencesKey("intro_record_${id.name}")
    private fun date(value: String?) = value?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    private fun read(p: Preferences) = IntroState(IntroId.entries.associateWith { id ->
        val fields = p[key(id)].orEmpty().split('|')
        IntroRecord(fields.getOrNull(0)?.toIntOrNull() ?: 0, date(fields.getOrNull(1)),
            date(fields.getOrNull(2)), fields.getOrNull(3)?.toIntOrNull() ?: 0,
            fields.getOrNull(4) == "true", fields.getOrNull(5) == "true")
    }, date(p[lastNew]), p[onboardingFinished] ?: 0, date(p[home]),
        p[homeId]?.let { runCatching { IntroId.valueOf(it) }.getOrNull() })
    private fun write(p: MutablePreferences, state: IntroState) {
        state.records.forEach { (id, r) -> p[key(id)] = listOf(r.shownCount, r.firstShownDate ?: "",
            r.snoozedUntil ?: "", r.dismissals, r.retired, r.acted).joinToString("|") }
        state.lastNewIntroDate?.let { p[lastNew] = it.toString() }
        state.homeShownDate?.let { p[home] = it.toString() }
        state.homeShownId?.let { p[homeId] = it.name }
    }
    val state = context.dataStore.data.map(::read)
    val pendingAction = context.dataStore.data.map { p -> p[pending]?.let { runCatching { IntroId.valueOf(it) }.getOrNull() } }
    suspend fun setPending(id: IntroId?) { context.dataStore.edit { if (id == null) it.remove(pending) else it[pending] = id.name } }
    suspend fun claim(surface: IntroSurface, signals: IntroSignals, today: LocalDate = LocalDate.now(),
        nowMillis: Long = System.currentTimeMillis()): Intro? {
        var result: Intro? = null
        context.dataStore.edit { p ->
            val state = read(p)
            result = IntroEngine.pick(surface, signals, state, today, nowMillis)
            result?.let { write(p, IntroEngine.shown(state, it, today)) }
        }
        return result
    }
    suspend fun markShown(intro: Intro, today: LocalDate = LocalDate.now()) {
        context.dataStore.edit { write(it, IntroEngine.shown(read(it), intro, today)) }
    }
    suspend fun dismiss(id: IntroId, today: LocalDate = LocalDate.now()) {
        context.dataStore.edit { p -> val state = read(p)
            write(p, state.copy(records = state.records + (id to IntroEngine.dismissed(state.records[id] ?: IntroRecord(), today)))) }
    }
    suspend fun acted(id: IntroId) {
        context.dataStore.edit { p -> val state = read(p)
            write(p, state.copy(records = state.records + (id to (state.records[id] ?: IntroRecord()).copy(acted = true)))) }
    }
    suspend fun resetForSuggestion(id: IntroId) {
        context.dataStore.edit { p -> val state = read(p)
            write(p, state.copy(records = state.records + (id to (state.records[id] ?: IntroRecord()).copy(
                snoozedUntil = null, retired = false, dismissals = 0, acted = false)))) }
    }
}
