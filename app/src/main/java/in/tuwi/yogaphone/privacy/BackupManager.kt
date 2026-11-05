package `in`.tuwi.yogaphone.privacy

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import `in`.tuwi.yogaphone.data.dataStore
import `in`.tuwi.yogaphone.data.db.YogaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Exports and imports settings (and optionally history) as a JSON file the user keeps. */
class BackupManager(private val context: Context, private val database: YogaDatabase) {

    private val configTables = listOf("app_entries", "usage_rules", "focus_schedules", "categories", "profiles", "notification_rules")
    private val historyTables = listOf("usage_aggregates", "usage_days", "usage_hours", "focus_sessions", "timebox_sessions")

    /** Device- or moment-specific state that must not travel to another phone or another day. */
    private val skippedPrefs = setOf(
        "usage_sent_by_date", "usage_snoozed_date", "delay_continued_at", "focus_dnd_changed", "focus_previous_filter",
        "focus_suppressed_span", "grey_applied_by_yoga", "grey_saved_daltonizer_enabled", "grey_saved_daltonizer_mode",
        "grey_snoozed_until", "grey_over_limit_date", "profile_auto_span", "profile_auto_id", "profile_auto_previous",
        "profile_overridden_span", "insights_last_reflected_focus",
    )

    suspend fun export(uri: Uri, includeHistory: Boolean): String = withContext(Dispatchers.IO) {
        val prefs = context.dataStore.data.first().asMap().mapNotNull { (key, value) ->
            if (key.name in skippedPrefs) return@mapNotNull null
            val type = when (value) {
                is String -> "s"; is Boolean -> "b"; is Int -> "i"; is Long -> "l"; is Float -> "f"; is Double -> "d"
                is Set<*> -> "ss"; else -> return@mapNotNull null
            }
            PrefEntry(key.name, type, value)
        }
        val tables = (configTables + if (includeHistory) historyTables else emptyList()).associateWith { readTable(it) }
        val payload = BackupPayload(BackupCodec.VERSION, System.currentTimeMillis(), includeHistory, prefs, tables)
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(BackupCodec.encode(payload).toByteArray()) }
            ?: throw BackupException("Couldn't write the file.")
        BackupCodec.summary(payload)
    }

    suspend fun read(uri: Uri): BackupPayload = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            ?: throw BackupException("Couldn't open the file.")
        BackupCodec.decode(text)
    }

    /** Replaces the tables present in the backup and writes its settings over the current ones. */
    suspend fun apply(payload: BackupPayload) = withContext(Dispatchers.IO) {
        val known = (configTables + historyTables).toSet()
        val db = database.openHelper.writableDatabase
        db.beginTransaction()
        try {
            payload.tables.filterKeys { it in known }.forEach { (table, rows) ->
                db.delete(table, null, null)
                rows.forEach { row ->
                    val values = ContentValues().apply {
                        row.forEach { (column, value) ->
                            when (value) {
                                null -> putNull(column)
                                is Long -> put(column, value)
                                is Double -> put(column, value)
                                else -> put(column, value.toString())
                            }
                        }
                    }
                    db.insert(table, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, values)
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        database.invalidationTracker.refreshVersionsAsync()
        context.dataStore.edit { prefs ->
            payload.prefs.filter { it.key !in skippedPrefs }.forEach { entry -> write(prefs, entry) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun write(prefs: androidx.datastore.preferences.core.MutablePreferences, entry: PrefEntry) {
        when (entry.type) {
            "s" -> prefs[stringPreferencesKey(entry.key)] = entry.value as String
            "b" -> prefs[booleanPreferencesKey(entry.key)] = entry.value as Boolean
            "i" -> prefs[intPreferencesKey(entry.key)] = entry.value as Int
            "l" -> prefs[longPreferencesKey(entry.key)] = entry.value as Long
            "f" -> prefs[floatPreferencesKey(entry.key)] = entry.value as Float
            "d" -> prefs[doublePreferencesKey(entry.key)] = entry.value as Double
            "ss" -> prefs[stringSetPreferencesKey(entry.key)] = entry.value as Set<String>
        }
    }

    private fun readTable(table: String): List<Map<String, Any?>> {
        val rows = mutableListOf<Map<String, Any?>>()
        database.openHelper.readableDatabase.query("SELECT * FROM $table").use { cursor ->
            while (cursor.moveToNext()) {
                rows += (0 until cursor.columnCount).associate { i ->
                    cursor.getColumnName(i) to when (cursor.getType(i)) {
                        Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(i)
                        Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(i)
                        Cursor.FIELD_TYPE_STRING -> cursor.getString(i)
                        else -> null
                    }
                }
            }
        }
        return rows
    }

    /** Counts for the privacy dashboard. */
    suspend fun counts(): Map<String, Int> = withContext(Dispatchers.IO) {
        mapOf(
            "Days of usage history" to database.usageDao().dayCount(),
            "Focus sessions" to database.focusDao().focusCount(),
            "Timeboxes" to database.focusDao().timeboxCount(),
            "Pauses and reflections" to database.focusDao().interventionCount(),
            "Held notifications" to database.quietDao().held().size,
        )
    }

    suspend fun deleteUsageHistory() = withContext(Dispatchers.IO) {
        val usage = database.usageDao()
        usage.clearHistory(); usage.clearDays(); usage.clearAllHours()
    }

    /** Deletes everything Yoga stores. The app returns to first-run state. */
    suspend fun resetEverything() = withContext(Dispatchers.IO) {
        database.clearAllTables()
        context.dataStore.edit { it.clear() }
    }

    @Suppress("unused")
    private fun Preferences.Key<*>.isSkipped() = name in skippedPrefs
}
