package `in`.tuwi.yogaphone.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Only the title and text of a held notification are kept, and only for a day. */
@Entity(tableName = "held_notifications")
data class HeldNotificationEntity(
    @PrimaryKey val key: String,
    val packageName: String,
    val title: String,
    val text: String,
    val postedAt: Long,
)

@Entity(tableName = "notification_rules")
data class NotificationRuleEntity(
    @PrimaryKey val packageName: String,
    /** A NotificationPolicy name. */
    val policy: String,
)

@Dao
interface QuietDao {
    @Upsert suspend fun hold(item: HeldNotificationEntity)
    @Query("SELECT * FROM held_notifications ORDER BY postedAt DESC") fun observeHeld(): Flow<List<HeldNotificationEntity>>
    @Query("SELECT * FROM held_notifications") suspend fun held(): List<HeldNotificationEntity>
    @Query("DELETE FROM held_notifications WHERE postedAt < :before") suspend fun pruneHeld(before: Long)
    @Query("DELETE FROM held_notifications WHERE packageName = :pkg") suspend fun clearHeld(pkg: String)
    @Query("DELETE FROM held_notifications") suspend fun clearAllHeld()

    @Query("SELECT * FROM notification_rules") fun observeRules(): Flow<List<NotificationRuleEntity>>
    @Query("SELECT * FROM notification_rules WHERE packageName = :pkg") suspend fun rule(pkg: String): NotificationRuleEntity?
    @Upsert suspend fun saveRule(rule: NotificationRuleEntity)
}
