package `in`.tuwi.yogaphone.data

import android.os.UserHandle
import java.text.CollationKey

data class AppModel(
    val appLabel: String,
    val appPackage: String,
    val activityClassName: String?,
    val user: UserHandle,
    val key: CollationKey? = null,
    val customLabel: String? = null,
    val isFavorite: Boolean = false,
    val favoriteOrder: Int = Int.MAX_VALUE,
    val isHidden: Boolean = false,
    val isEssential: Boolean = false,
    val isDistracting: Boolean = false,
    /** An app in the work profile. Shown in the drawer and search only: home, hidden and categories are per package. */
    val isWork: Boolean = false,
    /** The work profile is paused, so the app can't open until it's turned back on. */
    val workPaused: Boolean = false,
) : Comparable<AppModel> {
    val displayLabel: String get() = customLabel?.takeIf { it.isNotBlank() } ?: appLabel
    override fun compareTo(other: AppModel): Int = displayLabel.compareTo(other.displayLabel, ignoreCase = true)
}
