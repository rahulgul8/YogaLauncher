package `in`.tuwi.yogaphone.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AppEntryEntity::class], version = 1, exportSchema = false)
abstract class YogaDatabase : RoomDatabase() {
    abstract fun appEntryDao(): AppEntryDao

    companion object {
        fun create(context: Context): YogaDatabase = Room.databaseBuilder(
            context.applicationContext,
            YogaDatabase::class.java,
            "yoga-launcher.db",
        ).fallbackToDestructiveMigration().build()
    }
}
