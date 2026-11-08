package `in`.tuwi.yogaphone.update

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import `in`.tuwi.yogaphone.data.dataStore
import kotlinx.coroutines.flow.first

data class UpdateStoredState(
    val lastCheckMillis: Long? = null,
    val downloadedAtMillis: Long? = null,
    val downloadPending: Boolean = false,
    val dismissedVersionCode: Int? = null,
    val dismissedAtMillis: Long? = null,
    val lastRunVersionCode: Int? = null,
) {
    fun isDismissed(versionCode: Int, nowMillis: Long): Boolean =
        dismissedVersionCode == versionCode && dismissedAtMillis != null &&
            nowMillis - dismissedAtMillis < UpdateStore.DISMISS_MILLIS
}

interface UpdatePersistence {
    suspend fun read(): UpdateStoredState
    suspend fun recordCheck(atMillis: Long)
    suspend fun setDownloadPending(pending: Boolean)
    suspend fun recordDownloaded(atMillis: Long)
    suspend fun clearDownload()
    suspend fun dismiss(versionCode: Int, atMillis: Long)
    suspend fun setLastRunVersion(versionCode: Int)
}

class UpdateStore(private val context: Context) : UpdatePersistence {
    override suspend fun read(): UpdateStoredState {
        val p = context.dataStore.data.first()
        return UpdateStoredState(
            p[LAST_CHECK], p[DOWNLOADED_AT], p[DOWNLOAD_PENDING] ?: false,
            p[DISMISSED_VERSION], p[DISMISSED_AT], p[LAST_RUN_VERSION],
        )
    }

    override suspend fun recordCheck(atMillis: Long) {
        context.dataStore.edit { it[LAST_CHECK] = atMillis }
    }

    override suspend fun setDownloadPending(pending: Boolean) {
        context.dataStore.edit { it[DOWNLOAD_PENDING] = pending }
    }

    override suspend fun recordDownloaded(atMillis: Long) {
        context.dataStore.edit { p -> p[DOWNLOADED_AT] = atMillis; p[DOWNLOAD_PENDING] = false }
    }

    override suspend fun clearDownload() {
        context.dataStore.edit { p -> p.remove(DOWNLOADED_AT); p[DOWNLOAD_PENDING] = false }
    }

    override suspend fun dismiss(versionCode: Int, atMillis: Long) {
        context.dataStore.edit { p -> p[DISMISSED_VERSION] = versionCode; p[DISMISSED_AT] = atMillis }
    }

    override suspend fun setLastRunVersion(versionCode: Int) {
        context.dataStore.edit { it[LAST_RUN_VERSION] = versionCode }
    }

    companion object {
        const val DISMISS_MILLIS = 3L * UpdatePolicy.DAY_MILLIS
        private val LAST_CHECK = longPreferencesKey("update_last_check")
        private val DOWNLOADED_AT = longPreferencesKey("update_downloaded_at")
        private val DOWNLOAD_PENDING = booleanPreferencesKey("update_download_pending")
        private val DISMISSED_VERSION = intPreferencesKey("update_dismissed_version")
        private val DISMISSED_AT = longPreferencesKey("update_dismissed_at")
        private val LAST_RUN_VERSION = intPreferencesKey("update_last_run_version")
    }
}
