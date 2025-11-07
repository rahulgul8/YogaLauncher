package `in`.tuwi.yogaphone.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.PREFS_NAME)

class Prefs(private val context: Context) {

    private val firstLaunchKey = booleanPreferencesKey(Constants.KEY_FIRST_LAUNCH)
    private val themeKey = intPreferencesKey(Constants.KEY_THEME)
    private val fontSizeKey = intPreferencesKey(Constants.KEY_FONT_SIZE)

    // First launch
    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[firstLaunchKey] ?: true
    }

    suspend fun setFirstLaunchComplete() {
        context.dataStore.edit { prefs ->
            prefs[firstLaunchKey] = false
        }
    }

    // Theme
    val theme: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[themeKey] ?: Constants.THEME_DARK
    }

    suspend fun setTheme(theme: Int) {
        context.dataStore.edit { prefs ->
            prefs[themeKey] = theme
        }
    }

    // Font size
    val fontSize: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[fontSizeKey] ?: Constants.FONT_SIZE_MEDIUM
    }

    suspend fun setFontSize(size: Int) {
        context.dataStore.edit { prefs ->
            prefs[fontSizeKey] = size
        }
    }

    // Favorite apps
    suspend fun getFavoriteApp(position: Int): FavoriteApp? {
        val prefs = context.dataStore.data.first()
        val name = prefs[stringPreferencesKey("${Constants.KEY_APP_NAME_PREFIX}$position")] ?: return null
        val packageName = prefs[stringPreferencesKey("${Constants.KEY_APP_PACKAGE_PREFIX}$position")] ?: return null
        val className = prefs[stringPreferencesKey("${Constants.KEY_APP_CLASS_PREFIX}$position")]
        val userString = prefs[stringPreferencesKey("${Constants.KEY_APP_USER_PREFIX}$position")] ?: ""

        return FavoriteApp(name, packageName, className, userString)
    }

    suspend fun setFavoriteApp(position: Int, app: AppModel) {
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey("${Constants.KEY_APP_NAME_PREFIX}$position")] = app.appLabel
            prefs[stringPreferencesKey("${Constants.KEY_APP_PACKAGE_PREFIX}$position")] = app.appPackage
            prefs[stringPreferencesKey("${Constants.KEY_APP_CLASS_PREFIX}$position")] = app.activityClassName ?: ""
            prefs[stringPreferencesKey("${Constants.KEY_APP_USER_PREFIX}$position")] = app.user.toString()
        }
    }

    suspend fun removeFavoriteApp(position: Int) {
        context.dataStore.edit { prefs ->
            prefs.remove(stringPreferencesKey("${Constants.KEY_APP_NAME_PREFIX}$position"))
            prefs.remove(stringPreferencesKey("${Constants.KEY_APP_PACKAGE_PREFIX}$position"))
            prefs.remove(stringPreferencesKey("${Constants.KEY_APP_CLASS_PREFIX}$position"))
            prefs.remove(stringPreferencesKey("${Constants.KEY_APP_USER_PREFIX}$position"))
        }
    }

    suspend fun getAllFavoriteApps(): List<FavoriteApp> {
        val favorites = mutableListOf<FavoriteApp>()
        for (i in 1..Constants.MAX_FAVORITE_APPS) {
            getFavoriteApp(i)?.let { favorites.add(it) }
        }
        return favorites
    }

    data class FavoriteApp(
        val name: String,
        val packageName: String,
        val className: String?,
        val userString: String
    )
}
