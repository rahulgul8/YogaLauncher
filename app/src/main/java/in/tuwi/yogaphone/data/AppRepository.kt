package `in`.tuwi.yogaphone.data

import android.content.Context
import androidx.room.withTransaction
import `in`.tuwi.yogaphone.data.db.AppEntryEntity
import `in`.tuwi.yogaphone.data.db.YogaDatabase
import `in`.tuwi.yogaphone.logic.reorderFavoriteList
import `in`.tuwi.yogaphone.utils.getAppsList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class AppRepository(
    private val context: Context,
    private val database: YogaDatabase,
    private val prefs: Prefs,
) {
    private val dao = database.appEntryDao()
    private val installedApps = MutableStateFlow<List<AppModel>>(emptyList())

    val apps: Flow<List<AppModel>> = combine(installedApps, dao.observeAll()) { installed, entries ->
        val metadata = entries.associateBy(AppEntryEntity::packageName)
        installed.map { app ->
            val entry = metadata[app.appPackage]
            val classification = `in`.tuwi.yogaphone.usage.AppClassifier(context).classify(app.appPackage, entry)
            app.copy(
                customLabel = entry?.customLabel,
                isFavorite = entry?.isFavorite ?: false,
                favoriteOrder = entry?.favoriteOrder ?: Int.MAX_VALUE,
                isHidden = entry?.isHidden ?: false,
                isEssential = classification.essential,
                isDistracting = classification.distracting,
            )
        }.sorted()
    }

    suspend fun initialize() { migrateFavoritesOnce(); refreshApps() }

    suspend fun refreshApps() = withContext(Dispatchers.IO) {
        val refreshed = getAppsList(context).distinctBy { it.appPackage to it.activityClassName }
        installedApps.value = refreshed
        val packages = refreshed.map(AppModel::appPackage).distinct()
        // A temporarily locked profile or PackageManager failure must not wipe user metadata.
        if (packages.isNotEmpty()) dao.deleteUninstalled(packages)
    }

    suspend fun setFavorite(packageName: String, favorite: Boolean, replacePackage: String? = null) {
        val homeLimit = prefs.userPreferences.first().homeAppCount
        database.withTransaction {
            val before = dao.getFavorites()
            if (favorite && replacePackage == null && before.none { it.packageName == packageName } && before.size >= homeLimit) {
                return@withTransaction
            }
            if (replacePackage != null && replacePackage != packageName) {
                val replaced = dao.get(replacePackage) ?: AppEntryEntity(replacePackage)
                dao.upsert(replaced.copy(isFavorite = false, favoriteOrder = Int.MAX_VALUE))
            }
            val current = dao.get(packageName) ?: AppEntryEntity(packageName)
            val nextOrder = if (favorite) dao.getFavorites().count { it.packageName != packageName } else Int.MAX_VALUE
            dao.upsert(current.copy(isFavorite = favorite, favoriteOrder = nextOrder))
            normalizeFavoriteOrder()
        }
    }

    suspend fun moveFavorite(packageName: String, delta: Int) {
        database.withTransaction {
            val favorites = dao.getFavorites()
            val from = favorites.indexOfFirst { it.packageName == packageName }
            if (from == -1) return@withTransaction
            val reordered = reorderFavoriteList(favorites, from, (from + delta).coerceIn(favorites.indices))
            dao.upsertAll(reordered.mapIndexed { index, entry -> entry.copy(favoriteOrder = index) })
        }
    }

    suspend fun rename(packageName: String, label: String?) = update(packageName) {
        it.copy(customLabel = label?.trim()?.takeIf(String::isNotEmpty))
    }

    suspend fun setHidden(packageName: String, hidden: Boolean) = update(packageName) {
        it.copy(
            isHidden = hidden,
            isFavorite = if (hidden) false else it.isFavorite,
            favoriteOrder = if (hidden) Int.MAX_VALUE else it.favoriteOrder,
        )
    }

    suspend fun resetCustomLabels() = dao.resetCustomLabels()

    private suspend fun update(packageName: String, transform: (AppEntryEntity) -> AppEntryEntity) {
        dao.upsert(transform(dao.get(packageName) ?: AppEntryEntity(packageName)))
        normalizeFavoriteOrder()
    }

    private suspend fun normalizeFavoriteOrder() {
        dao.upsertAll(dao.getFavorites().mapIndexed { index, entry -> entry.copy(favoriteOrder = index) })
    }

    private suspend fun migrateFavoritesOnce() {
        if (prefs.userPreferences.first().favoritesMigrated) return
        database.withTransaction {
            prefs.legacyFavorites().forEach { legacy ->
                val entry = dao.get(legacy.packageName) ?: AppEntryEntity(legacy.packageName)
                dao.upsert(entry.copy(isFavorite = true, favoriteOrder = legacy.order))
            }
            normalizeFavoriteOrder()
        }
        prefs.markFavoritesMigrated()
    }
}

