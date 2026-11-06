package `in`.tuwi.yogaphone.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import `in`.tuwi.yogaphone.logic.updateRecentSearches

private const val RECENT_PREFS = "yoga_recent_entries"
private const val SEPARATOR = "\u001F"

internal const val RECENT_SCHEDULE_NAMES = "schedule_names"
internal const val RECENT_PROFILE_NAMES = "profile_names"
internal const val RECENT_CATEGORY_NAMES = "category_names"

@Composable
internal fun rememberRecentEntries(key: String): MutableState<List<String>> {
    val context = LocalContext.current
    return remember(key) {
        mutableStateOf(context.getSharedPreferences(RECENT_PREFS, Context.MODE_PRIVATE).getString(key, null)
            ?.split(SEPARATOR)?.filter(String::isNotBlank).orEmpty().take(3))
    }
}

internal fun recordRecentEntry(context: Context, key: String, state: MutableState<List<String>>, value: String) {
    val updated = updateRecentSearches(state.value, value)
    context.getSharedPreferences(RECENT_PREFS, Context.MODE_PRIVATE).edit().putString(key, updated.joinToString(SEPARATOR)).apply()
    state.value = updated
}
