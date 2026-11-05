package `in`.tuwi.yogaphone.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AppEntryEntity::class, UsageAggregateEntity::class, UsageRuleEntity::class, UsageDayEntity::class,
    TimeboxSessionEntity::class, FocusSessionEntity::class, FocusScheduleEntity::class, InterventionEventEntity::class], version = 3, exportSchema = false)
abstract class YogaDatabase : RoomDatabase() {
    abstract fun appEntryDao(): AppEntryDao
    abstract fun usageDao(): UsageDao
    abstract fun focusDao(): FocusDao

    companion object {
        fun create(context: Context): YogaDatabase = Room.databaseBuilder(
            context.applicationContext,
            YogaDatabase::class.java,
            "yoga-launcher.db",
        ).fallbackToDestructiveMigration().build()
    }
}

