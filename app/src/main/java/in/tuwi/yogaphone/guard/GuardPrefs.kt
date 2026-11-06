package `in`.tuwi.yogaphone.guard

import android.content.Context
import androidx.datastore.preferences.core.*
import `in`.tuwi.yogaphone.data.dataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

/** Only preferences and aggregate daily counters; no app/window content is persisted. */
class GuardPrefs(private val context: Context) {
    private val live = booleanPreferencesKey("guard_live")
    private val style = stringPreferencesKey("guard_style")
    private val start = stringPreferencesKey("guard_start")
    private val y = intPreferencesKey("guard_y")
    private val day = stringPreferencesKey("guard_day")
    private fun enabled(s: GuardSurface) = booleanPreferencesKey("guard_${s.name}_enabled")
    private fun allowance(s: GuardSurface) = intPreferencesKey("guard_${s.name}_allowance")
    private fun last(s: GuardSurface) = intPreferencesKey("guard_${s.name}_last")
    private fun blocks(s: GuardSurface) = intPreferencesKey("guard_${s.name}_blocks")
    private fun used(s: GuardSurface) = longPreferencesKey("guard_${s.name}_used")
    private fun read(p: Preferences) = GuardPreferences(p[live] ?: false,
        runCatching { GuardStyle.valueOf(p[style].orEmpty()) }.getOrDefault(GuardStyle.BURNING),
        runCatching { StartCounting.valueOf(p[start].orEmpty()) }.getOrDefault(StartCounting.FROM_FIRST_MINUTE),
        p[y] ?: 80, GuardSurface.entries.associateWith { s -> SurfaceSettings(p[enabled(s)] ?: false,
            p[allowance(s)] ?: 0, p[last(s)] ?: 10) })
    val preferences = context.dataStore.data.map(::read).distinctUntilChanged()
    val daily = context.dataStore.data.map { p -> GuardDaily(p[day].orEmpty(),
        GuardSurface.entries.associateWith { p[blocks(it)] ?: 0 },
        GuardSurface.entries.associateWith { p[used(it)] ?: 0L }) }
    suspend fun update(transform: (GuardPreferences) -> GuardPreferences) {
        context.dataStore.edit { p ->
            val next = transform(read(p))
            p[live] = next.liveIndicator; p[style] = next.style.name; p[start] = next.startCounting.name; p[y] = next.positionY
            GuardSurface.entries.forEach { s ->
                val config = next.surface(s)
                p[enabled(s)] = config.enabled
                p[allowance(s)] = config.allowanceMinutes.takeIf { it in setOf(5, 10, 15, 30) } ?: 0
                p[last(s)] = config.lastAllowanceMinutes.takeIf { it in setOf(5, 10, 15, 30) } ?: 10
            }
        }
    }
    suspend fun saveDaily(value: GuardDaily) {
        context.dataStore.edit { p ->
            p[day] = value.date
            GuardSurface.entries.forEach { s -> p[blocks(s)] = value.blocks[s] ?: 0; p[used(s)] = value.usedMillis[s] ?: 0L }
        }
    }
    suspend fun clearHistory() = saveDaily(GuardDaily())
}
