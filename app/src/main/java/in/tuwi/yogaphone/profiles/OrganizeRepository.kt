package `in`.tuwi.yogaphone.profiles

import `in`.tuwi.yogaphone.pro.*
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import `in`.tuwi.yogaphone.data.AppModel
import `in`.tuwi.yogaphone.data.dataStore
import `in`.tuwi.yogaphone.data.db.AppEntryEntity
import `in`.tuwi.yogaphone.data.db.CategoryEntity
import `in`.tuwi.yogaphone.data.db.ProfileEntity
import `in`.tuwi.yogaphone.data.db.YogaDatabase
import `in`.tuwi.yogaphone.focus.ScheduleEvaluator
import `in`.tuwi.yogaphone.focus.toWindow
import `in`.tuwi.yogaphone.usage.AppClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

data class OrganizePreferences(
    val activeProfileId: Long? = null,
    val groupDrawerByCategory: Boolean = false,
    val autoSwitch: AutoSwitchState = AutoSwitchState(),
)

/** Categories, profiles and which profile is active. */
class OrganizeRepository(private val context: Context, private val database: YogaDatabase) {
    private val dao = database.organizeDao()
    private val classifier = AppClassifier(context)
    private val mutex = Mutex()

    private object Keys {
        val active = longPreferencesKey("profile_active")
        val group = booleanPreferencesKey("drawer_group_by_category")
        val autoSpan = stringPreferencesKey("profile_auto_span")
        val autoProfile = longPreferencesKey("profile_auto_id")
        val autoPrevious = longPreferencesKey("profile_auto_previous")
        val overridden = stringPreferencesKey("profile_overridden_span")
    }

    val preferences: Flow<OrganizePreferences> = context.dataStore.data.map { p ->
        OrganizePreferences(
            activeProfileId = p[Keys.active]?.takeIf { it > 0 },
            groupDrawerByCategory = p[Keys.group] ?: false,
            autoSwitch = AutoSwitchState(p[Keys.autoSpan], p[Keys.autoProfile]?.takeIf { it > 0 },
                p[Keys.autoPrevious]?.takeIf { it > 0 }, p[Keys.overridden]),
        )
    }

    val categories: Flow<List<CategoryEntity>> = dao.observeCategories()
    val profiles: Flow<List<ProfileEntity>> = dao.observeProfiles()

    /** Effective category for every installed app: the user's choice, else a local guess. */
    fun categoryMap(apps: Flow<List<AppModel>>): Flow<Map<String, String>> =
        combine(apps, database.appEntryDao().observeAll()) { installed, entries ->
            val byPackage = entries.associateBy { it.packageName }
            installed.map { it.appPackage to it.appLabel }.distinctBy { it.first }.associate { (pkg, label) ->
                pkg to categoryOf(pkg, label, byPackage[pkg])
            }
        }.flowOn(Dispatchers.IO)

    fun categoryOf(pkg: String, label: String, entry: AppEntryEntity?): String {
        entry?.categoryId?.let { return it }
        val appCategory = runCatching { context.packageManager.getApplicationInfo(pkg, 0).category }.getOrDefault(-1)
        return Categories.autoCategory(pkg, appCategory, label, essential = classifier.classify(pkg, entry).essential)
    }

    suspend fun ensureBuiltIns() = withContext(Dispatchers.IO) {
        val existing = dao.categories().map { it.id }.toSet()
        val missing = Categories.builtIns.mapIndexedNotNull { index, c ->
            if (c.id in existing) null else CategoryEntity(c.id, c.name, true, c.distractingByDefault, index)
        }
        if (missing.isNotEmpty()) dao.saveCategories(missing)
    }

    suspend fun setAppCategory(pkg: String, categoryId: String?) = withContext(Dispatchers.IO) {
        val entries = database.appEntryDao()
        entries.upsert((entries.get(pkg) ?: AppEntryEntity(pkg)).copy(categoryId = categoryId))
    }

    suspend fun addCategory(name: String): String = withContext(Dispatchers.IO) {
        val id = Categories.slug(name).let { base ->
            val taken = dao.categories().map { it.id }.toSet()
            generateSequence(1) { it + 1 }.map { if (it == 1) base else "${base}_$it" }.first { it !in taken }
        }
        dao.saveCategory(CategoryEntity(id, name.trim(), isBuiltIn = false, sortOrder = 100))
        id
    }

    suspend fun renameCategory(category: CategoryEntity, name: String) = withContext(Dispatchers.IO) {
        dao.saveCategory(category.copy(name = name.trim()))
    }

    suspend fun deleteCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        if (category.isBuiltIn) return@withContext
        dao.clearCategory(category.id)
        dao.deleteCustomCategory(category.id)
    }

    suspend fun setGroupDrawer(value: Boolean) { context.dataStore.edit { it[Keys.group] = value } }

    // ---- Profiles ----

    suspend fun createProfile(name: String, homeApps: List<String>): Long = withContext(Dispatchers.IO) {
        dao.insertProfile(ProfileEntity(name = name.trim(), homeApps = HomeList.encode(homeApps),
            sortOrder = dao.profiles().size + 1))
    }

    suspend fun saveProfile(profile: ProfileEntity) = withContext(Dispatchers.IO) { dao.saveProfile(profile) }

    suspend fun deleteProfile(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteProfile(id)
        if (preferences.first().activeProfileId == id) setActive(null, manual = true)
    }

    suspend fun profile(id: Long) = withContext(Dispatchers.IO) { dao.profile(id) }

    suspend fun editHomeApps(profileId: Long, transform: (List<String>) -> List<String>) = withContext(Dispatchers.IO) {
        val profile = dao.profile(profileId) ?: return@withContext
        dao.saveProfile(profile.copy(homeApps = HomeList.encode(transform(HomeList.decode(profile.homeApps)))))
    }

    /** Switches profile. Returns the profile that is now active (null = the default home). */
    suspend fun setActive(id: Long?, manual: Boolean): ProfileEntity? = mutex.withLock {
        context.dataStore.edit { p ->
            if (id == null) p.remove(Keys.active) else p[Keys.active] = id
            if (manual) writeAuto(p, ProfileSwitchPlanner.manual(readAuto(p)))
        }
        id?.let { dao.profile(it) }
    }

    /**
     * Applies schedule-linked profiles. Returns the profile that became active when a switch happened,
     * wrapped so callers can tell "switched to default" (Switched(null)) from "no change" (null).
     */
    suspend fun reconcileSchedules(): Switched? = withContext(Dispatchers.IO) {
        if (!(context.applicationContext as `in`.tuwi.yogaphone.YogaApplication).proRepository.allows(ProFeature.PROFILES)) return@withContext null
        mutex.withLock {
            val profiles = dao.profiles().filter { it.scheduleId != null }
            val windows = database.focusDao().schedules().map { it.toWindow() }
            val now = Instant.now()
            val zone = ZoneId.systemDefault()
            val link = profiles.firstNotNullOfOrNull { profile ->
                val window = windows.firstOrNull { it.id == profile.scheduleId } ?: return@firstNotNullOfOrNull null
                ScheduleEvaluator.activeAt(listOf(window), now, zone)?.let { span ->
                    ProfileLink(profile.id, "${window.id}|${span.start.toEpochMilli()}")
                }
            }
            val current = preferences.first()
            val decision = ProfileSwitchPlanner.decide(current.activeProfileId, link, current.autoSwitch)
            context.dataStore.edit { p ->
                if (decision.activeProfileId == null) p.remove(Keys.active) else p[Keys.active] = decision.activeProfileId
                writeAuto(p, decision.state)
            }
            if (decision.activeProfileId != current.activeProfileId) Switched(decision.activeProfileId?.let { dao.profile(it) }) else null
        }
    }

    data class Switched(val profile: ProfileEntity?)

    private fun readAuto(p: androidx.datastore.preferences.core.Preferences) = AutoSwitchState(
        p[Keys.autoSpan], p[Keys.autoProfile]?.takeIf { it > 0 }, p[Keys.autoPrevious]?.takeIf { it > 0 }, p[Keys.overridden])

    private fun writeAuto(p: androidx.datastore.preferences.core.MutablePreferences, state: AutoSwitchState) {
        state.spanKey?.let { p[Keys.autoSpan] = it } ?: p.remove(Keys.autoSpan)
        state.autoProfileId?.let { p[Keys.autoProfile] = it } ?: p.remove(Keys.autoProfile)
        state.previousProfileId?.let { p[Keys.autoPrevious] = it } ?: p.remove(Keys.autoPrevious)
        state.overriddenSpanKey?.let { p[Keys.overridden] = it } ?: p.remove(Keys.overridden)
    }
}
