package `in`.tuwi.yogaphone

import android.app.Application
import `in`.tuwi.yogaphone.data.AppRepository
import `in`.tuwi.yogaphone.data.Prefs
import `in`.tuwi.yogaphone.data.db.YogaDatabase

import `in`.tuwi.yogaphone.usage.UsagePrefs
import `in`.tuwi.yogaphone.usage.UsageRepository
import `in`.tuwi.yogaphone.usage.UsageWorker
import `in`.tuwi.yogaphone.focus.FocusPrefs
import `in`.tuwi.yogaphone.focus.FocusRepository

class YogaApplication : Application() {
    val updateController by lazy { `in`.tuwi.yogaphone.update.UpdateController(this) }
    val proRepository by lazy { `in`.tuwi.yogaphone.pro.ProRepository(this) }
    val guardPrefs by lazy { `in`.tuwi.yogaphone.guard.GuardPrefs(this) }
    val usagePrefs by lazy { UsagePrefs(this) }
    val usageRepository by lazy { UsageRepository(this, database, usagePrefs) }
    override fun onCreate() { super.onCreate(); proRepository; UsageWorker.schedule(this) }
    val prefs by lazy { Prefs(this) }
    val database by lazy { YogaDatabase.create(this) }
    val appRepository by lazy { AppRepository(this, database, prefs) }
    val focusPrefs by lazy { FocusPrefs(this) }
    val focusRepository by lazy { FocusRepository(this, database, focusPrefs) }
    val insightsRepository by lazy { `in`.tuwi.yogaphone.insights.InsightsRepository(this, database) }
    val backupManager by lazy { `in`.tuwi.yogaphone.privacy.BackupManager(this, database) }
    val widgetRepository by lazy { `in`.tuwi.yogaphone.widgets.WidgetRepository(this, database) }
    val quietRepository by lazy { `in`.tuwi.yogaphone.quiet.QuietRepository(this, database) }
    val organizeRepository by lazy { `in`.tuwi.yogaphone.profiles.OrganizeRepository(this, database) }
}

