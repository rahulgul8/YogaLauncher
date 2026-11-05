package `in`.tuwi.yogaphone.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import `in`.tuwi.yogaphone.usage.RuleScope
import `in`.tuwi.yogaphone.usage.UsageRule

@Entity(tableName = "usage_aggregates", primaryKeys = ["date", "packageName"])
data class UsageAggregateEntity(val date: String, val packageName: String, val foregroundMinutes: Long, val eligible: Boolean)

@Entity(tableName = "usage_rules")
data class UsageRuleEntity(@PrimaryKey val id: String, val scope: String = "TOTAL", val targetPackage: String? = null,
    val dailyAllowanceMinutes: Int, val warnAtPercent: Int = 80, val enabled: Boolean = true) {
    fun toRule() = UsageRule(id, RuleScope.valueOf(scope), targetPackage, dailyAllowanceMinutes, warnAtPercent, enabled)
}

/** Eligible minutes per local hour, for late-night reporting and the heatmap. */
@Entity(tableName = "usage_hours", primaryKeys = ["date", "hour"])
data class UsageHourEntity(val date: String, val hour: Int, val eligibleMinutes: Long)

/** Records which dates were actually sampled, including genuine empty days. */
@Entity(tableName = "usage_days")
data class UsageDayEntity(@PrimaryKey val date: String, val sampledAt: Long)

@Dao
interface UsageDao {
    @Query("SELECT * FROM usage_rules") fun observeRules(): Flow<List<UsageRuleEntity>>
    @Query("SELECT * FROM usage_rules") suspend fun rules(): List<UsageRuleEntity>
    @Upsert suspend fun saveRule(rule: UsageRuleEntity)
    @Query("DELETE FROM usage_rules WHERE id = :id") suspend fun removeRule(id: String)
    @Query("SELECT * FROM usage_aggregates WHERE date >= :start AND date <= :end")
    suspend fun aggregates(start: String, end: String): List<UsageAggregateEntity>
    @Upsert suspend fun saveAggregates(rows: List<UsageAggregateEntity>)
    @Query("DELETE FROM usage_aggregates WHERE date = :date") suspend fun clearDate(date: String)
    @Upsert suspend fun saveDay(day: UsageDayEntity)
    @Query("SELECT * FROM usage_days WHERE date >= :start AND date <= :end")
    suspend fun days(start: String, end: String): List<UsageDayEntity>
    @Query("DELETE FROM usage_aggregates") suspend fun clearHistory()
    @Query("DELETE FROM usage_days") suspend fun clearDays()
    @Upsert suspend fun saveHours(rows: List<UsageHourEntity>)
    @Query("DELETE FROM usage_hours WHERE date = :date") suspend fun clearHours(date: String)
    @Query("SELECT * FROM usage_hours WHERE date >= :start AND date <= :end") suspend fun hours(start: String, end: String): List<UsageHourEntity>
    @Query("DELETE FROM usage_hours") suspend fun clearAllHours()
    @Query("DELETE FROM usage_aggregates WHERE date < :before") suspend fun pruneAggregates(before: String)
    @Query("DELETE FROM usage_hours WHERE date < :before") suspend fun pruneHours(before: String)
    @Query("DELETE FROM usage_days WHERE date < :before") suspend fun pruneDays(before: String)
    @Query("SELECT COUNT(*) FROM usage_days") suspend fun dayCount(): Int
}
