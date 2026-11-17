package `in`.tuwi.yogaphone.usage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import `in`.tuwi.yogaphone.data.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class UrgentAccessRepository(private val context: Context) {
    private object Keys {
        val counts = stringSetPreferencesKey(COUNTS_KEY_NAME)
        val windows = stringSetPreferencesKey(WINDOWS_KEY_NAME)
    }

    val state: Flow<UrgentAccessState> = context.dataStore.data.map { preferences ->
        UrgentAccessState(
            UrgentAccessCodec.decodeRecords(preferences[Keys.counts].orEmpty()),
            UrgentAccessCodec.decodeWindows(preferences[Keys.windows].orEmpty()),
        )
    }

    /** Atomically consumes the shared daily quota and opens a five-minute window. */
    suspend fun grant(date: String, packageName: String, now: Long = System.currentTimeMillis()): UrgentWindow? {
        var granted: UrgentWindow? = null
        context.dataStore.edit { preferences ->
            // Keep 30 days for stats; older counts would only grow the preference forever.
            val cutoff = java.time.LocalDate.parse(date).minusDays(30).toString()
            val records = UrgentAccessCodec.decodeRecords(preferences[Keys.counts].orEmpty()).filter { it.date >= cutoff }
            if (!UrgentAccessPolicy.canGrant(records, date)) return@edit
            val nextRecords = UrgentAccessPolicy.record(records, date, packageName)
            val nextWindow = UrgentWindow(packageName, now + UrgentAccessPolicy.WINDOW_MILLIS)
            val windows = UrgentAccessCodec.decodeWindows(preferences[Keys.windows].orEmpty()).toMutableMap()
            windows[packageName] = nextWindow
            preferences[Keys.counts] = UrgentAccessCodec.encodeRecords(nextRecords)
            preferences[Keys.windows] = UrgentAccessCodec.encodeWindows(windows.values)
            granted = nextWindow
        }
        return granted
    }

    suspend fun current(): UrgentAccessState = state.first()

    suspend fun clearWindows(packages: Set<String>) {
        if (packages.isEmpty()) return
        context.dataStore.edit { preferences ->
            val windows = UrgentAccessCodec.decodeWindows(preferences[Keys.windows].orEmpty())
                .filterKeys { it !in packages }
            preferences[Keys.windows] = UrgentAccessCodec.encodeWindows(windows.values)
        }
    }

    suspend fun clearAllWindows() {
        context.dataStore.edit { it.remove(Keys.windows) }
    }

    companion object {
        const val COUNTS_KEY_NAME = "urgent_open_counts"
        const val WINDOWS_KEY_NAME = "urgent_open_windows"
    }
}
