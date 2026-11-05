package `in`.tuwi.yogaphone.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.os.Process
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import `in`.tuwi.yogaphone.data.db.YogaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Entity(tableName = "widgets")
data class WidgetEntity(
    @PrimaryKey val appWidgetId: Int,
    val providerComponent: String,
    val heightDp: Int,
    val sortOrder: Int,
)

@Dao
interface WidgetDao {
    @Query("SELECT * FROM widgets ORDER BY sortOrder, appWidgetId") fun observe(): Flow<List<WidgetEntity>>
    @Query("SELECT * FROM widgets ORDER BY sortOrder, appWidgetId") suspend fun all(): List<WidgetEntity>
    @Upsert suspend fun save(widget: WidgetEntity)
    @Upsert suspend fun saveAll(widgets: List<WidgetEntity>)
    @Query("DELETE FROM widgets WHERE appWidgetId = :id") suspend fun delete(id: Int)
}

/** Pure helpers, kept free of Android types for tests. */
object WidgetLayout {
    const val MIN_HEIGHT = 80
    const val MAX_HEIGHT = 400
    val presets = listOf("Small" to 120, "Medium" to 200, "Large" to 320)

    fun defaultHeight(providerMinHeightDp: Int): Int = providerMinHeightDp.coerceIn(MIN_HEIGHT, MAX_HEIGHT).coerceAtLeast(120)

    /** A preset never goes below what the provider needs. */
    fun resize(presetDp: Int, providerMinHeightDp: Int): Int = maxOf(presetDp, providerMinHeightDp).coerceIn(MIN_HEIGHT, MAX_HEIGHT)

    fun <T> move(list: List<T>, index: Int, delta: Int): List<T> {
        if (index !in list.indices) return list
        val to = (index + delta).coerceIn(list.indices)
        return list.toMutableList().apply { add(to, removeAt(index)) }
    }
}

/** Hosts other apps' widgets in a single calm column on home. */
class WidgetRepository(private val context: Context, private val database: YogaDatabase) {
    val manager: AppWidgetManager = AppWidgetManager.getInstance(context)
    val host = AppWidgetHost(context, HOST_ID)
    private val dao = database.widgetDao()
    val widgets: Flow<List<WidgetEntity>> = dao.observe()

    fun startListening() = runCatching { host.startListening() }
    fun stopListening() = runCatching { host.stopListening() }

    fun providers(): List<AppWidgetProviderInfo> = runCatching {
        manager.getInstalledProvidersForProfile(Process.myUserHandle())
    }.getOrDefault(emptyList())

    fun info(id: Int): AppWidgetProviderInfo? = runCatching { manager.getAppWidgetInfo(id) }.getOrNull()

    fun allocate(): Int = host.allocateAppWidgetId()

    fun bindIfAllowed(id: Int, provider: ComponentName): Boolean =
        runCatching { manager.bindAppWidgetIdIfAllowed(id, provider) }.getOrDefault(false)

    fun release(id: Int) = runCatching { host.deleteAppWidgetId(id) }

    suspend fun add(id: Int) = withContext(Dispatchers.IO) {
        val info = info(id) ?: run { release(id); return@withContext }
        val minHeight = (info.minHeight / context.resources.displayMetrics.density).toInt()
        dao.save(WidgetEntity(id, info.provider.flattenToString(), WidgetLayout.defaultHeight(minHeight), dao.all().size))
    }

    suspend fun remove(id: Int) = withContext(Dispatchers.IO) {
        dao.delete(id)
        release(id)
        normalize()
    }

    suspend fun resize(widget: WidgetEntity, presetDp: Int) = withContext(Dispatchers.IO) {
        val min = info(widget.appWidgetId)?.let { (it.minHeight / context.resources.displayMetrics.density).toInt() } ?: 0
        dao.save(widget.copy(heightDp = WidgetLayout.resize(presetDp, min)))
    }

    suspend fun move(widget: WidgetEntity, delta: Int) = withContext(Dispatchers.IO) {
        val list = dao.all()
        val moved = WidgetLayout.move(list, list.indexOfFirst { it.appWidgetId == widget.appWidgetId }, delta)
        dao.saveAll(moved.mapIndexed { i, w -> w.copy(sortOrder = i) })
    }

    /** Drops widgets whose provider app was uninstalled. */
    suspend fun cleanup() = withContext(Dispatchers.IO) {
        dao.all().filter { info(it.appWidgetId) == null }.forEach { dao.delete(it.appWidgetId); release(it.appWidgetId) }
        normalize()
    }

    private suspend fun normalize() = dao.saveAll(dao.all().mapIndexed { i, w -> w.copy(sortOrder = i) })

    companion object {
        const val HOST_ID = 4242
        const val REQUEST_CONFIGURE = 4243
    }
}
