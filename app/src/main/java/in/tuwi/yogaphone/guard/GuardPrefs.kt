package `in`.tuwi.yogaphone.guard

import android.content.Context
import androidx.datastore.preferences.core.*
import `in`.tuwi.yogaphone.data.dataStore
import java.time.LocalDate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

/** Only preferences and aggregate daily counters; no app/window content is persisted. */
class GuardPrefs(private val context: Context) {
    private val live = booleanPreferencesKey("guard_live")
    private val style = stringPreferencesKey("guard_style")
    private val start = stringPreferencesKey("guard_start")
    private val y = intPreferencesKey("guard_y")
    private val history = stringSetPreferencesKey("guard_blocks_7d")
    private val strict = booleanPreferencesKey("guard_strict")
    private val strictOff = stringPreferencesKey("guard_strict_off_from")
    private val lastPass = intPreferencesKey("guard_last_pass_minutes")
    private val passHistory = stringSetPreferencesKey("guard_passes_7d")
    private val resistHistory = stringSetPreferencesKey("guard_resisted_7d")
    private fun passUntil(s: GuardSurface) = longPreferencesKey("guard_${s.name}_pass_until")
    private fun passMinutes(s: GuardSurface) = intPreferencesKey("guard_${s.name}_pass_minutes")
    private fun passes(s: GuardSurface) = intPreferencesKey("guard_${s.name}_passes")
    private fun resisted(s: GuardSurface) = intPreferencesKey("guard_${s.name}_resisted")
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
            p[allowance(s)] ?: 0, p[last(s)] ?: 10) },
        strict = p[strict] ?: false, strictOffFrom = p[strictOff],
        passUntil = GuardSurface.entries.associateWith { p[passUntil(it)] ?: 0L },
        lastPassMinutes = p[lastPass], passMinutes = GuardSurface.entries.associateWith { p[passMinutes(it)] ?: 0 })
    val preferences = context.dataStore.data.map(::read).distinctUntilChanged()
    private fun readDaily(p: Preferences) = GuardDaily(p[day].orEmpty(),
        GuardSurface.entries.associateWith { p[blocks(it)] ?: 0 },
        GuardSurface.entries.associateWith { p[used(it)] ?: 0L },
        GuardSurface.entries.associateWith { p[passes(it)] ?: 0 },
        GuardSurface.entries.associateWith { p[resisted(it)] ?: 0 })
    val daily = context.dataStore.data.map(::readDaily)
    suspend fun update(transform: (GuardPreferences) -> GuardPreferences) {
        context.dataStore.edit { p ->
            val next = transform(read(p))
            p[live] = next.liveIndicator; p[style] = next.style.name; p[start] = next.startCounting.name; p[y] = next.positionY
            p[strict] = next.strict
            next.strictOffFrom?.let { p[strictOff] = it } ?: p.remove(strictOff)
            next.lastPassMinutes?.let { p[lastPass] = it } ?: p.remove(lastPass)
            GuardSurface.entries.forEach { s ->
                p[passUntil(s)] = next.passUntil[s] ?: 0L
                p[passMinutes(s)] = next.passMinutes[s] ?: 0
                val config = next.surface(s)
                p[enabled(s)] = config.enabled
                p[allowance(s)] = config.allowanceMinutes.takeIf { it in setOf(5, 10, 15, 30) } ?: 0
                p[last(s)] = config.lastAllowanceMinutes.takeIf { it in setOf(5, 10, 15, 30) } ?: 10
            }
        }
    }
    /** Date-keyed ring; seven daily aggregate rows, never screen content. */
    val weeklyBlocks = context.dataStore.data.map { p ->
        p[history].orEmpty().mapNotNull { row ->
            val parts = row.split('|')
            val date = runCatching { LocalDate.parse(parts[0]) }.getOrNull() ?: return@mapNotNull null
            date to GuardSurface.entries.associateWith { parts.getOrNull(it.ordinal + 1)?.toIntOrNull() ?: 0 }
        }.toMap()
    }
    private fun writeDaily(p: MutablePreferences, value: GuardDaily, today: LocalDate) {
        val previous = readDaily(p)
        p[history] = guardBlockHistory(p[history].orEmpty(), previous, value, today)
        p[passHistory] = guardBlockHistory(p[passHistory].orEmpty(), previous.copy(blocks = previous.passes), value.copy(blocks = value.passes), today)
        p[resistHistory] = guardBlockHistory(p[resistHistory].orEmpty(), previous.copy(blocks = previous.resisted), value.copy(blocks = value.resisted), today)
        p[day] = value.date
        GuardSurface.entries.forEach { s ->
            p[blocks(s)] = value.blocks[s] ?: 0; p[used(s)] = value.usedMillis[s] ?: 0L
            p[passes(s)] = value.passes[s] ?: 0; p[resisted(s)] = value.resisted[s] ?: 0
        }
    }
    suspend fun saveDaily(value: GuardDaily) {
        context.dataStore.edit { p -> writeDaily(p, value, LocalDate.now()) }
    }
    /** Merge inside the edit, not before it: a choice may race the service's save. */
    suspend fun saveGateDaily(value: GuardDaily) {
        context.dataStore.edit { p ->
            val today = LocalDate.now()
            val current = readDaily(p)
            val next = mergeGuardDaily(current, value)
            writeDaily(p, if (next.date < today.toString()) GuardDaily(today.toString()) else next, today)
        }
    }
    suspend fun startPass(surface: GuardSurface, minutes: Int, now: Long) {
        require(minutes in `in`.tuwi.yogaphone.resist.ResistanceLadder.PASS_MINUTES)
        context.dataStore.edit { p ->
            val today = LocalDate.now()
            if (isStrict(read(p), today)) return@edit
            val current = readDaily(p).takeIf { it.date == today.toString() } ?: GuardDaily(today.toString())
            surface.family().forEach { s -> p[passUntil(s)] = now + minutes * 60_000L; p[passMinutes(s)] = minutes }
            p[lastPass] = minutes
            writeDaily(p, current.copy(passes = current.passes + (surface to ((current.passes[surface] ?: 0) + 1))), today)
        }
    }
    suspend fun recordResisted(surface: GuardSurface) {
        context.dataStore.edit { p ->
            val today = LocalDate.now()
            val current = readDaily(p).takeIf { it.date == today.toString() } ?: GuardDaily(today.toString())
            writeDaily(p, current.copy(resisted = current.resisted + (surface to ((current.resisted[surface] ?: 0) + 1))), today)
        }
    }
    /** Consume an expired pass once for its whole family, keeping the correct duration. */
    suspend fun consumeExpiredPass(surface: GuardSurface, now: Long): Int? {
        var minutes: Int? = null
        context.dataStore.edit { p ->
            val prefs = read(p)
            val expiry = prefs.passExpiry(surface)
            if (expiry > 0 && expiry <= now) {
                minutes = surface.family().firstNotNullOfOrNull { prefs.passMinutes[it]?.takeIf { n -> n > 0 } }
                surface.family().forEach { p.remove(passUntil(it)); p.remove(passMinutes(it)) }
            }
        }
        return minutes
    }
    suspend fun pruneBlockHistory(today: LocalDate = LocalDate.now()) {
        context.dataStore.edit { p ->
            val current = readDaily(p)
            writeDaily(p, current.takeIf { it.date == today.toString() } ?: GuardDaily(today.toString()), today)
        }
    }
    suspend fun clearHistory() {
        context.dataStore.edit { p ->
            p.remove(history); p.remove(passHistory); p.remove(resistHistory); p[day] = ""
            p.remove(lastPass)
            GuardSurface.entries.forEach {
                p[blocks(it)] = 0; p[used(it)] = 0L; p[passes(it)] = 0; p[resisted(it)] = 0
                p.remove(passUntil(it)); p.remove(passMinutes(it))
            }
        }
    }
}
