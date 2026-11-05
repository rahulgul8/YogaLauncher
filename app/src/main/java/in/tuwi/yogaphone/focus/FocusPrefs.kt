package `in`.tuwi.yogaphone.focus

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import `in`.tuwi.yogaphone.data.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class FocusPreferences(
    val delayEnabled: Boolean = true,
    val delaySeconds: Int = 5,
    val dontAskMinutes: Int = 5,
    val delayOptOut: Set<String> = emptySet(),
    val focusAllowed: Set<String> = emptySet(),
    val focusGreyscale: Boolean = false,
    val focusDnd: Boolean = false,
    val lastTimeboxMinutes: Int = 25,
    /** 0 means "until I stop". */
    val lastFocusMinutes: Int = 45,
    val timeboxPurposes: List<String> = emptyList(),
    val delayPurposes: List<String> = emptyList(),
)

private const val SEPARATOR = "\u001F"

class FocusPrefs(private val context: Context) {
    private object Keys {
        val delayEnabled = booleanPreferencesKey("delay_enabled")
        val delaySeconds = intPreferencesKey("delay_seconds")
        val dontAsk = intPreferencesKey("delay_dont_ask_minutes")
        val optOut = stringSetPreferencesKey("delay_opt_out")
        val allowed = stringSetPreferencesKey("focus_allowed")
        val greyscale = booleanPreferencesKey("focus_greyscale")
        val dnd = booleanPreferencesKey("focus_dnd")
        val lastTimebox = intPreferencesKey("timebox_last_minutes")
        val lastFocus = intPreferencesKey("focus_last_minutes")
        val timeboxPurposes = stringPreferencesKey("timebox_purposes")
        val delayPurposes = stringPreferencesKey("delay_purposes")
        val continued = stringSetPreferencesKey("delay_continued_at")
        val dndChangedByYoga = booleanPreferencesKey("focus_dnd_changed")
        val previousFilter = intPreferencesKey("focus_previous_filter")
        val suppressedSpan = stringPreferencesKey("focus_suppressed_span")
    }

    val preferences: Flow<FocusPreferences> = context.dataStore.data.map(::read)

    private fun read(p: Preferences) = FocusPreferences(
        delayEnabled = p[Keys.delayEnabled] ?: true,
        delaySeconds = (p[Keys.delaySeconds] ?: 5).coerceIn(2, 10),
        dontAskMinutes = p[Keys.dontAsk] ?: 5,
        delayOptOut = p[Keys.optOut].orEmpty(),
        focusAllowed = p[Keys.allowed].orEmpty(),
        focusGreyscale = p[Keys.greyscale] ?: false,
        focusDnd = p[Keys.dnd] ?: false,
        lastTimeboxMinutes = p[Keys.lastTimebox] ?: 25,
        lastFocusMinutes = p[Keys.lastFocus] ?: 45,
        timeboxPurposes = p[Keys.timeboxPurposes].split(),
        delayPurposes = p[Keys.delayPurposes].split(),
    )

    private fun String?.split() = this?.split(SEPARATOR)?.filter(String::isNotBlank).orEmpty()

    suspend fun setDelayEnabled(value: Boolean) = edit { it[Keys.delayEnabled] = value }
    suspend fun setDelaySeconds(value: Int) = edit { it[Keys.delaySeconds] = value.coerceIn(2, 10) }
    suspend fun setDontAskMinutes(value: Int) = edit { it[Keys.dontAsk] = value }
    suspend fun setDelayOptOut(pkg: String, optOut: Boolean) = edit {
        it[Keys.optOut] = it[Keys.optOut].orEmpty().let { set -> if (optOut) set + pkg else set - pkg }
    }
    suspend fun setFocusAllowed(pkg: String, allowed: Boolean) = edit {
        it[Keys.allowed] = it[Keys.allowed].orEmpty().let { set -> if (allowed) set + pkg else set - pkg }
    }
    suspend fun setFocusGreyscale(value: Boolean) = edit { it[Keys.greyscale] = value }
    suspend fun setFocusDnd(value: Boolean) = edit { it[Keys.dnd] = value }
    suspend fun setLastTimebox(minutes: Int, purpose: String?) = edit {
        it[Keys.lastTimebox] = minutes
        if (!purpose.isNullOrBlank()) it[Keys.timeboxPurposes] =
            updateRecentList(it[Keys.timeboxPurposes].split(), purpose).joinToString(SEPARATOR)
    }
    suspend fun setLastFocus(minutes: Int) = edit { it[Keys.lastFocus] = minutes }
    suspend fun recordDelayContinue(pkg: String, purpose: String?, now: Long) = edit {
        it[Keys.continued] = DelayGate.encode(DelayGate.record(DelayGate.decode(it[Keys.continued].orEmpty()), pkg, now))
        if (!purpose.isNullOrBlank()) it[Keys.delayPurposes] =
            updateRecentList(it[Keys.delayPurposes].split(), purpose).joinToString(SEPARATOR)
    }
    suspend fun lastContinuedAt(pkg: String): Long? = DelayGate.decode(context.dataStore.data.first()[Keys.continued].orEmpty())[pkg]

    suspend fun dndState(): Pair<Boolean, Int?> = context.dataStore.data.first().let { (it[Keys.dndChangedByYoga] == true) to it[Keys.previousFilter] }
    suspend fun setDndState(changed: Boolean, previous: Int?) = edit {
        it[Keys.dndChangedByYoga] = changed
        if (previous == null) it.remove(Keys.previousFilter) else it[Keys.previousFilter] = previous
    }

    /** A scheduled span the user ended early, so it is not restarted until it is over ("scheduleId|startMillis"). */
    suspend fun suppressedSpan(): String? = context.dataStore.data.first()[Keys.suppressedSpan]
    suspend fun setSuppressedSpan(value: String?) = edit {
        if (value == null) it.remove(Keys.suppressedSpan) else it[Keys.suppressedSpan] = value
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
