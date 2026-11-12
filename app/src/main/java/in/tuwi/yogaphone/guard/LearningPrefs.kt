package `in`.tuwi.yogaphone.guard

import android.content.Context
import androidx.datastore.preferences.core.*
import `in`.tuwi.yogaphone.data.dataStore
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

data class LearningChannel(val displayName: String, val key: String = LearningChannels.normalize(displayName))
data class LearningPreferences(
    val channels: List<LearningChannel> = emptyList(),
    val recent: List<String> = emptyList(),
    val allowYouTubeHomeDuringFocus: Boolean = false,
    val titleHints: Boolean = true,
)

class LearningPrefs(private val context: Context) {
    companion object {
        const val CHANNELS_KEY = "learning_channels"
        const val RECENT_KEY = "learning_recent"
        const val TOTALS_KEY = "learning_totals"
        val starterChannels = listOf("Khan Academy", "NPTEL", "MIT OpenCourseWare", "CrashCourse", "3Blue1Brown",
            "Physics Wallah", "TED-Ed", "freeCodeCamp", "Google for Developers", "Harvard Business Review", "TED",
            "Ali Abdaal", "Y Combinator")
    }
    private val channels = stringPreferencesKey(CHANNELS_KEY)
    private val recent = stringPreferencesKey(RECENT_KEY)
    private val totals = stringPreferencesKey(TOTALS_KEY)
    private val allowHome = booleanPreferencesKey("learning_allow_youtube_home_focus")
    private val hints = booleanPreferencesKey("learning_title_hints")

    private fun names(raw: String?): List<String> = runCatching {
        val array = JSONArray(raw ?: "[]")
        (0 until array.length()).map { array.getString(it) }
    }.getOrDefault(emptyList())
    private fun encode(values: List<String>) = JSONArray(values).toString()
    private fun readChannels(raw: String?): List<LearningChannel> = runCatching {
        val array = JSONArray(raw ?: "[]")
        (0 until array.length()).mapNotNull { index ->
            when (val value = array.get(index)) {
                is JSONObject -> value.optString("displayName").takeIf(String::isNotBlank)?.let { name ->
                    LearningChannel(name, value.optString("key").takeIf(String::isNotBlank) ?: LearningChannels.normalize(name))
                }
                is String -> value.takeIf(String::isNotBlank)?.let(::LearningChannel)
                else -> null
            }
        }
    }.getOrDefault(emptyList())
    private fun encodeChannels(values: List<LearningChannel>) = JSONArray().apply { values.forEach { channel ->
        put(JSONObject().put("displayName", channel.displayName).put("key", channel.key))
    } }.toString()
    private fun read(p: Preferences) = LearningPreferences(
        channels = readChannels(p[channels]).filter { it.key.isNotEmpty() }.distinctBy(LearningChannel::key),
        recent = names(p[recent]).distinctBy(LearningChannels::normalize).take(3),
        allowYouTubeHomeDuringFocus = p[allowHome] ?: false,
        titleHints = p[hints] ?: true,
    )
    val preferences = context.dataStore.data.map(::read).distinctUntilChanged()
    val dailyTotals = context.dataStore.data.map { p -> decodeTotals(p[totals]) }.distinctUntilChanged()

    suspend fun add(displayName: String) {
        val name = displayName.trim().replace(Regex("\\s+"), " ")
        val key = LearningChannels.normalize(name)
        if (key.isEmpty()) return
        context.dataStore.edit { p ->
            val state = read(p)
            p[channels] = encodeChannels(state.channels.filterNot { it.key == key } + LearningChannel(name, key))
            p[recent] = encode((listOf(name) + state.recent.filterNot { LearningChannels.normalize(it) == key }).take(3))
        }
    }
    suspend fun remove(key: String) = context.dataStore.edit { p ->
        p[channels] = encodeChannels(read(p).channels.filterNot { it.key == key })
    }
    suspend fun setAllowHome(value: Boolean) = context.dataStore.edit { it[allowHome] = value }
    suspend fun setTitleHints(value: Boolean) = context.dataStore.edit { it[hints] = value }

    suspend fun addSeconds(date: String, packageName: String, seconds: Long) {
        if (seconds <= 0) return
        context.dataStore.edit { p ->
            // Keep about a year of daily totals; ISO dates compare correctly as strings.
            val oldest = runCatching { java.time.LocalDate.parse(date).minusDays(400).toString() }.getOrNull()
            val current = decodeTotals(p[totals]).filterKeys { oldest == null || it.substringBefore('|') >= oldest }.toMutableMap()
            val key = "$date|$packageName"
            current[key] = (current[key] ?: 0L) + seconds
            p[totals] = encodeTotals(current)
        }
    }
    suspend fun seconds(date: String, packageName: String): Long =
        LearningAccounting.totalFor(dailyTotals.first(), date, packageName)
    suspend fun clearHistory() = context.dataStore.edit { it.remove(totals) }

    private fun decodeTotals(raw: String?): Map<String, Long> = runCatching {
        val json = JSONObject(raw ?: "{}")
        json.keys().asSequence().associateWith { json.optLong(it).coerceAtLeast(0) }
    }.getOrDefault(emptyMap())
    private fun encodeTotals(values: Map<String, Long>) = JSONObject().apply { values.forEach { (key, value) -> put(key, value) } }.toString()
}
