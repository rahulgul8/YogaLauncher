package `in`.tuwi.yogaphone.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

object TimeboxStatus {
    const val RUNNING = "RUNNING"
    const val PAUSED = "PAUSED"
    const val COMPLETED = "COMPLETED"
    const val CANCELLED = "CANCELLED"
}

@Entity(tableName = "timebox_sessions")
data class TimeboxSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purpose: String? = null,
    val boundPackage: String? = null,
    val plannedMinutes: Int,
    val startedAt: Long,
    /** When the countdown reaches zero while running. */
    val endAt: Long,
    /** Set while paused instead of [endAt]. */
    val pausedRemainingMs: Long? = null,
    val endedAt: Long? = null,
    val status: String = TimeboxStatus.RUNNING,
    val extendedMinutes: Int = 0,
    /** The completion prompt was answered (or dismissed) in the launcher. */
    val acknowledged: Boolean = false,
)

object FocusStatus {
    const val ACTIVE = "ACTIVE"
    const val COMPLETED = "COMPLETED"
    const val ENDED_EARLY = "ENDED_EARLY"
}

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startAt: Long,
    /** Null for "until I stop". */
    val plannedEndAt: Long? = null,
    val endedAt: Long? = null,
    val mode: String = "MANUAL",
    val scheduleId: Long? = null,
    val status: String = FocusStatus.ACTIVE,
    val overrideCount: Int = 0,
)

@Entity(tableName = "focus_schedules")
data class FocusScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** ISO day numbers 1 (Monday) .. 7 (Sunday), comma separated. */
    val days: String,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val enabled: Boolean = true,
)

@Entity(tableName = "intervention_events")
data class InterventionEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** DELAY, FOCUS_OVERRIDE or REMINDER. */
    val type: String,
    val packageName: String?,
    val shownAt: Long,
    /** CONTINUED, WENT_BACK or DISMISSED. */
    val actionTaken: String,
    val purpose: String? = null,
)

@Dao
interface FocusDao {
    @Insert suspend fun insertTimebox(session: TimeboxSessionEntity): Long
    @Upsert suspend fun saveTimebox(session: TimeboxSessionEntity)
    @Query("SELECT * FROM timebox_sessions ORDER BY id DESC LIMIT 1") fun observeLatestTimebox(): Flow<TimeboxSessionEntity?>
    @Query("SELECT * FROM timebox_sessions ORDER BY id DESC LIMIT 1") suspend fun latestTimebox(): TimeboxSessionEntity?
    @Query("SELECT * FROM timebox_sessions WHERE status = 'COMPLETED'") suspend fun completedTimeboxes(): List<TimeboxSessionEntity>

    @Insert suspend fun insertFocus(session: FocusSessionEntity): Long
    @Upsert suspend fun saveFocus(session: FocusSessionEntity)
    @Query("SELECT * FROM focus_sessions WHERE status = 'ACTIVE' ORDER BY id DESC LIMIT 1") fun observeActiveFocus(): Flow<FocusSessionEntity?>
    @Query("SELECT * FROM focus_sessions WHERE status = 'ACTIVE' ORDER BY id DESC LIMIT 1") suspend fun activeFocus(): FocusSessionEntity?
    @Query("SELECT * FROM focus_sessions WHERE status = 'COMPLETED'") suspend fun completedFocus(): List<FocusSessionEntity>

    @Upsert suspend fun saveSchedule(schedule: FocusScheduleEntity)
    @Query("DELETE FROM focus_schedules WHERE id = :id") suspend fun deleteSchedule(id: Long)
    @Query("SELECT * FROM focus_schedules ORDER BY startMinuteOfDay") fun observeSchedules(): Flow<List<FocusScheduleEntity>>
    @Query("SELECT * FROM focus_schedules") suspend fun schedules(): List<FocusScheduleEntity>

    @Insert suspend fun insertIntervention(event: InterventionEventEntity)
}
