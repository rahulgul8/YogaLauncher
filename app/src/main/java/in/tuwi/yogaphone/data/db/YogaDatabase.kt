package `in`.tuwi.yogaphone.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AppEntryEntity::class, UsageAggregateEntity::class, UsageRuleEntity::class, UsageDayEntity::class,
    TimeboxSessionEntity::class, FocusSessionEntity::class, FocusScheduleEntity::class, InterventionEventEntity::class,
    CategoryEntity::class, ProfileEntity::class,
    HeldNotificationEntity::class, NotificationRuleEntity::class, UsageHourEntity::class, `in`.tuwi.yogaphone.widgets.WidgetEntity::class], version = 8, exportSchema = true)
abstract class YogaDatabase : RoomDatabase() {
    abstract fun appEntryDao(): AppEntryDao
    abstract fun usageDao(): UsageDao
    abstract fun focusDao(): FocusDao
    abstract fun organizeDao(): OrganizeDao
    abstract fun quietDao(): QuietDao
    abstract fun widgetDao(): `in`.tuwi.yogaphone.widgets.WidgetDao

    companion object {
        // Version 8 is the first released schema (app/schemas). From here on every schema change needs a
        // Migration added below; only a downgrade (installing an older build over a newer one) wipes data.
        fun create(context: Context): YogaDatabase = Room.databaseBuilder(
            context.applicationContext,
            YogaDatabase::class.java,
            "yoga-launcher.db",
        ).fallbackToDestructiveMigrationOnDowngrade().build()
    }
}

