package `in`.tuwi.yogaphone.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import `in`.tuwi.yogaphone.logic.updateRecentSearches
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.PREFS_NAME)

enum class ThemeMode { FOLLOW_SYSTEM, LIGHT, DARK, GREYSCALE }
enum class FontSizeOption { SMALL, MEDIUM, LARGE, EXTRA_LARGE }
enum class HomeTextAlignment { LEFT, CENTER, RIGHT }

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    val fontSize: FontSizeOption = FontSizeOption.MEDIUM,
    val showClock: Boolean = true,
    val showDate: Boolean = true,
    val homeAppCount: Int = Constants.DEFAULT_HOME_APP_COUNT,
    val homeTextAlignment: HomeTextAlignment = HomeTextAlignment.LEFT,
    val recentSearches: List<String> = emptyList(),
    val favoritesMigrated: Boolean = false,
    /** Quick-launch dock, four slots: [DOCK_DEFAULT] (the phone's own app for that slot), [DOCK_EMPTY], or a package. */
    val dockSlots: List<String> = List(DOCK_SIZE) { DOCK_DEFAULT },
    val homeRoleAsked: Boolean = false,
)

const val DOCK_SIZE = 4
const val DOCK_DEFAULT = "@default"
const val DOCK_EMPTY = "@empty"


class Prefs(private val context: Context) {
    private object Keys {
        val theme = stringPreferencesKey("theme_mode_v2")
        val legacyTheme = intPreferencesKey("theme")
        val fontSize = stringPreferencesKey("font_size_v2")
        val legacyFontSize = intPreferencesKey("font_size")
        val showClock = booleanPreferencesKey("show_clock")
        val showDate = booleanPreferencesKey("show_date")
        val homeAppCount = intPreferencesKey("home_app_count")
        val alignment = stringPreferencesKey("home_text_alignment")
        val recentSearches = stringPreferencesKey("recent_searches")
        val favoritesMigrated = booleanPreferencesKey("favorites_migrated_to_room")
        val dockSlots = stringPreferencesKey("dock_slots")
        val homeRoleAsked = booleanPreferencesKey("home_role_asked")
    }

    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { values ->
        UserPreferences(
            themeMode = values[Keys.theme].enumOrNull<ThemeMode>() ?: when (values[Keys.legacyTheme]) {
                0 -> ThemeMode.LIGHT
                1 -> ThemeMode.DARK
                2 -> ThemeMode.GREYSCALE
                else -> ThemeMode.FOLLOW_SYSTEM
            },
            fontSize = values[Keys.fontSize].enumOrNull<FontSizeOption>() ?: when (values[Keys.legacyFontSize]) {
                0 -> FontSizeOption.SMALL
                2 -> FontSizeOption.LARGE
                else -> FontSizeOption.MEDIUM
            },
            showClock = values[Keys.showClock] ?: true,
            showDate = values[Keys.showDate] ?: true,
            homeAppCount = (values[Keys.homeAppCount] ?: Constants.DEFAULT_HOME_APP_COUNT)
                .coerceIn(Constants.MIN_HOME_APP_COUNT, Constants.MAX_HOME_APP_COUNT),
            homeTextAlignment = values[Keys.alignment].enumOrNull<HomeTextAlignment>() ?: HomeTextAlignment.LEFT,
            recentSearches = values[Keys.recentSearches]?.split(RECENT_SEPARATOR)?.filter(String::isNotBlank).orEmpty(),
            favoritesMigrated = values[Keys.favoritesMigrated] ?: false,
            dockSlots = values[Keys.dockSlots]?.split(RECENT_SEPARATOR)?.takeIf { it.size == DOCK_SIZE }
                ?: List(DOCK_SIZE) { DOCK_DEFAULT },
            homeRoleAsked = values[Keys.homeRoleAsked] ?: false,
        )
    }

    suspend fun setThemeMode(value: ThemeMode) = setString(Keys.theme, value.name)
    suspend fun setFontSize(value: FontSizeOption) = setString(Keys.fontSize, value.name)
    suspend fun setShowClock(value: Boolean) = setBoolean(Keys.showClock, value)
    suspend fun setShowDate(value: Boolean) = setBoolean(Keys.showDate, value)
    suspend fun setHomeAppCount(value: Int) = setInt(Keys.homeAppCount, value.coerceIn(4, 12))
    suspend fun setHomeTextAlignment(value: HomeTextAlignment) = setString(Keys.alignment, value.name)

    suspend fun setDockSlot(index: Int, value: String) {
        context.dataStore.edit { values ->
            val current = values[Keys.dockSlots]?.split(RECENT_SEPARATOR)?.takeIf { it.size == DOCK_SIZE }
                ?: List(DOCK_SIZE) { DOCK_DEFAULT }
            if (index in current.indices) values[Keys.dockSlots] = current.toMutableList().also { it[index] = value }
                .joinToString(RECENT_SEPARATOR)
        }
    }

    suspend fun markHomeRoleAsked() = setBoolean(Keys.homeRoleAsked, true)

    suspend fun recordRecentSearch(term: String) {
        context.dataStore.edit { values ->
            val current = values[Keys.recentSearches]?.split(RECENT_SEPARATOR)?.filter(String::isNotBlank).orEmpty()
            values[Keys.recentSearches] = updateRecentSearches(current, term).joinToString(RECENT_SEPARATOR)
        }
    }

    suspend fun legacyFavorites(): List<LegacyFavorite> {
        val values = context.dataStore.data.first()
        return (1..8).mapNotNull { position ->
            val packageName = values[stringPreferencesKey("${Constants.KEY_APP_PACKAGE_PREFIX}$position")]
                ?: return@mapNotNull null
            LegacyFavorite(packageName, position - 1)
        }
    }

    suspend fun markFavoritesMigrated() = setBoolean(Keys.favoritesMigrated, true)
    private suspend fun setString(key: Preferences.Key<String>, value: String) = context.dataStore.edit { it[key] = value }
    private suspend fun setBoolean(key: Preferences.Key<Boolean>, value: Boolean) = context.dataStore.edit { it[key] = value }
    private suspend fun setInt(key: Preferences.Key<Int>, value: Int) = context.dataStore.edit { it[key] = value }

    data class LegacyFavorite(val packageName: String, val order: Int)
    private inline fun <reified T : Enum<T>> String?.enumOrNull(): T? = this?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }
    private companion object { const val RECENT_SEPARATOR = "\u001F" }
}
