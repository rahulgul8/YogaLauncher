package `in`.tuwi.yogaphone.insights

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import `in`.tuwi.yogaphone.YogaApplication
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.morningNoteStore by preferencesDataStore("morning_note")

/** Builds yesterday's achievements and decides whether the home note should show. */
class MorningNoteController(private val app: YogaApplication) {

    private val _note = MutableStateFlow<DayAchievements?>(null)

    /** Yesterday's achievements when the note should show now, else null. */
    val note: StateFlow<DayAchievements?> = _note

    /** Re-evaluate; call when home resumes. */
    suspend fun refresh() {
        runCatching {
            withContext(Dispatchers.IO) {
                val zone = ZoneId.systemDefault()
                val yesterday = LocalDate.now(zone).minusDays(1)
                val usage = app.database.usageDao()
                val focus = app.database.focusDao()
                val aggregates = usage.aggregates(yesterday.minusDays(1).toString(), yesterday.toString())
                val allowance = usage.rules().firstOrNull { it.id == "total" && it.enabled }?.dailyAllowanceMinutes
                val since = yesterday.atStartOfDay(zone).toInstant().toEpochMilli()
                val until = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
                val timeboxes = focus.timeboxesSince(since)
                val sessions = focus.focusSince(since)
                val wentBack = focus.countInterventionsSince("DELAY", "WENT_BACK", null, since) -
                    focus.countInterventionsSince("DELAY", "WENT_BACK", null, until)
                val achievements = DayAchievementsBuilder.build(yesterday, zone, aggregates, allowance, timeboxes, sessions, wentBack)
                if (MorningNotePolicy.shouldShow(LocalDateTime.now(zone), closedOn(), achievements.hasContent)) achievements else null
            }
        }.onSuccess { _note.value = it }
    }

    /** The user opened or dismissed the note today. */
    suspend fun close() {
        runCatching {
            app.morningNoteStore.edit { it[closedOn] = LocalDate.now().toString() }
        }
        _note.value = null
    }

    private suspend fun closedOn(): LocalDate? =
        app.morningNoteStore.data.map { p -> p[closedOn]?.let { runCatching { LocalDate.parse(it) }.getOrNull() } }.first()

    private companion object { val closedOn = stringPreferencesKey("closed_on") }
}
