package `in`.tuwi.yogaphone

import android.app.Application
import `in`.tuwi.yogaphone.data.AppRepository
import `in`.tuwi.yogaphone.data.Prefs
import `in`.tuwi.yogaphone.data.db.YogaDatabase

import `in`.tuwi.yogaphone.usage.UsagePrefs
import `in`.tuwi.yogaphone.usage.UsageRepository
import `in`.tuwi.yogaphone.usage.UsageWorker

class YogaApplication : Application() {
    val usagePrefs by lazy { UsagePrefs(this) }
    val usageRepository by lazy { UsageRepository(this, database, usagePrefs) }
    override fun onCreate() { super.onCreate(); UsageWorker.schedule(this) }
    val prefs by lazy { Prefs(this) }
    val database by lazy { YogaDatabase.create(this) }
    val appRepository by lazy { AppRepository(this, database, prefs) }
}

