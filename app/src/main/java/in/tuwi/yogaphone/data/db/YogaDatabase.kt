package `in`.tuwi.yogaphone.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AppEntryEntity::class, UsageAggregateEntity::class, UsageRuleEntity::class, UsageDayEntity::class], version = 2, exportSchema = false)
abstract class YogaDatabase : RoomDatabase() {
    abstract fun appEntryDao(): AppEntryDao
    abstract fun usageDao(): UsageDao

    companion object {
        fun create(context: Context): YogaDatabase = Room.databaseBuilder(
            context.applicationContext,
            YogaDatabase::class.java,
            "yoga-launcher.db",
        ).fallbackToDestructiveMigration().build()
    }
}

