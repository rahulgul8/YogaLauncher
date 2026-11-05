package `in`.tuwi.yogaphone

import android.app.Application
import `in`.tuwi.yogaphone.data.AppRepository
import `in`.tuwi.yogaphone.data.Prefs
import `in`.tuwi.yogaphone.data.db.YogaDatabase

class YogaApplication : Application() {
    val prefs by lazy { Prefs(this) }
    val database by lazy { YogaDatabase.create(this) }
    val appRepository by lazy { AppRepository(this, database, prefs) }
}
