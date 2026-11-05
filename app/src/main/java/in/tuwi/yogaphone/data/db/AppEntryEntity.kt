package `in`.tuwi.yogaphone.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_entries")
data class AppEntryEntity(
    @PrimaryKey val packageName: String,
    val customLabel: String? = null,
    val categoryId: String? = null,
    val isFavorite: Boolean = false,
    val favoriteOrder: Int = Int.MAX_VALUE,
    val isHidden: Boolean = false,
    val isEssential: Boolean = false,
    val isDistracting: Boolean = false,
    val userClassified: Boolean = false,
)

