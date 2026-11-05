package `in`.tuwi.yogaphone.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isBuiltIn: Boolean,
    val isDistractingByDefault: Boolean = false,
    val sortOrder: Int = 0,
)

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Ordered package names, comma separated. */
    val homeApps: String = "",
    val hideOtherApps: Boolean = false,
    val focusOnActivate: Boolean = false,
    /** A ThemeMode name, or null to keep the chosen theme. */
    val themeOverride: String? = null,
    /** A focus schedule whose windows switch to this profile automatically. */
    val scheduleId: Long? = null,
    val sortOrder: Int = 0,
)

@Dao
interface OrganizeDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder, name") fun observeCategories(): Flow<List<CategoryEntity>>
    @Query("SELECT * FROM categories") suspend fun categories(): List<CategoryEntity>
    @Upsert suspend fun saveCategory(category: CategoryEntity)
    @Upsert suspend fun saveCategories(categories: List<CategoryEntity>)
    @Query("DELETE FROM categories WHERE id = :id AND isBuiltIn = 0") suspend fun deleteCustomCategory(id: String)
    @Query("UPDATE app_entries SET categoryId = NULL WHERE categoryId = :id") suspend fun clearCategory(id: String)

    @Query("SELECT * FROM profiles ORDER BY sortOrder, id") fun observeProfiles(): Flow<List<ProfileEntity>>
    @Query("SELECT * FROM profiles ORDER BY sortOrder, id") suspend fun profiles(): List<ProfileEntity>
    @Query("SELECT * FROM profiles WHERE id = :id") suspend fun profile(id: Long): ProfileEntity?
    @Insert suspend fun insertProfile(profile: ProfileEntity): Long
    @Upsert suspend fun saveProfile(profile: ProfileEntity)
    @Query("DELETE FROM profiles WHERE id = :id") suspend fun deleteProfile(id: Long)
}
