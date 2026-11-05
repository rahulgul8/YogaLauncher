package `in`.tuwi.yogaphone.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AppEntryDao {
    @Query("SELECT * FROM app_entries") fun observeAll(): Flow<List<AppEntryEntity>>
    @Query("SELECT * FROM app_entries WHERE isFavorite = 1 ORDER BY favoriteOrder")
    fun observeFavorites(): Flow<List<AppEntryEntity>>
    @Query("SELECT * FROM app_entries WHERE isHidden = 1 ORDER BY COALESCE(customLabel, packageName)")
    fun observeHidden(): Flow<List<AppEntryEntity>>
    @Query("SELECT * FROM app_entries WHERE packageName = :packageName")
    suspend fun get(packageName: String): AppEntryEntity?
    @Query("SELECT * FROM app_entries WHERE isFavorite = 1 ORDER BY favoriteOrder")
    suspend fun getFavorites(): List<AppEntryEntity>
    @Upsert suspend fun upsert(entry: AppEntryEntity)
    @Upsert suspend fun upsertAll(entries: List<AppEntryEntity>)
    @Query("DELETE FROM app_entries WHERE packageName NOT IN (:installedPackages)")
    suspend fun deleteUninstalled(installedPackages: List<String>)
    @Query("DELETE FROM app_entries") suspend fun deleteAll()
    @Query("UPDATE app_entries SET customLabel = NULL") suspend fun resetCustomLabels()
}
