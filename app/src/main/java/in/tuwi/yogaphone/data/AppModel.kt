package `in`.tuwi.yogaphone.data

import android.os.UserHandle
import java.text.CollationKey

data class AppModel(
    val appLabel: String,
    val appPackage: String,
    val activityClassName: String?,
    val user: UserHandle,
    val key: CollationKey? = null,
    val isFavorite: Boolean = false
) : Comparable<AppModel> {
    override fun compareTo(other: AppModel): Int = when {
        key != null && other.key != null -> key.compareTo(other.key)
        else -> appLabel.compareTo(other.appLabel, ignoreCase = true)
    }
}
