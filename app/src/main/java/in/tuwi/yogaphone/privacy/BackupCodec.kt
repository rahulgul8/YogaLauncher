package `in`.tuwi.yogaphone.privacy

import org.json.JSONArray
import org.json.JSONObject

/** One stored preference with its type, so it can be written back exactly. */
data class PrefEntry(val key: String, val type: String, val value: Any)

data class BackupPayload(
    val version: Int,
    val createdAt: Long,
    val includesHistory: Boolean,
    val prefs: List<PrefEntry>,
    /** Table name to rows; each row maps column to a Long, Double, String or null. */
    val tables: Map<String, List<Map<String, Any?>>>,
)

class BackupException(message: String) : Exception(message)

object BackupCodec {
    const val VERSION = 1
    private const val FORMAT = "yoga-launcher-backup"

    fun encode(payload: BackupPayload): String {
        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("version", payload.version)
        root.put("createdAt", payload.createdAt)
        root.put("includesHistory", payload.includesHistory)
        root.put("prefs", JSONArray().apply {
            payload.prefs.forEach { entry ->
                put(JSONObject().put("key", entry.key).put("type", entry.type).put("value",
                    if (entry.value is Set<*>) JSONArray(entry.value.map { it.toString() }) else entry.value))
            }
        })
        root.put("tables", JSONObject().apply {
            payload.tables.forEach { (table, rows) ->
                put(table, JSONArray().apply {
                    rows.forEach { row -> put(JSONObject().apply { row.forEach { (k, v) -> put(k, v ?: JSONObject.NULL) } }) }
                })
            }
        })
        return root.toString(2)
    }

    fun decode(text: String): BackupPayload {
        val root = runCatching { JSONObject(text) }.getOrElse { throw BackupException("This file isn't a Yoga Launcher backup.") }
        if (root.optString("format") != FORMAT) throw BackupException("This file isn't a Yoga Launcher backup.")
        val version = root.optInt("version", -1)
        if (version < 1 || version > VERSION) throw BackupException("This backup was made by a newer version of Yoga Launcher.")
        val prefs = root.optJSONArray("prefs")?.let { array ->
            (0 until array.length()).mapNotNull { i ->
                val o = array.getJSONObject(i)
                val key = o.getString("key")
                val value: Any = when (val type = o.getString("type")) {
                    "s" -> o.getString("value")
                    "b" -> o.getBoolean("value")
                    "i" -> o.getInt("value")
                    "l" -> o.getLong("value")
                    "f" -> o.getDouble("value").toFloat()
                    "d" -> o.getDouble("value")
                    "ss" -> o.getJSONArray("value").let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }
                    else -> return@mapNotNull null
                }
                PrefEntry(key, o.getString("type"), value)
            }
        }.orEmpty()
        val tablesJson = root.optJSONObject("tables") ?: JSONObject()
        val tables = tablesJson.keys().asSequence().associateWith { table ->
            val rows = tablesJson.getJSONArray(table)
            (0 until rows.length()).map { i ->
                val row = rows.getJSONObject(i)
                row.keys().asSequence().associateWith { column ->
                    when (val v = row.get(column)) {
                        JSONObject.NULL -> null
                        is Int -> v.toLong()
                        is Long -> v
                        is Double -> v
                        is Float -> v.toDouble()
                        is Boolean -> if (v) 1L else 0L
                        else -> v.toString()
                    }
                }
            }
        }
        return BackupPayload(version, root.optLong("createdAt"), root.optBoolean("includesHistory"), prefs, tables)
    }

    fun summary(payload: BackupPayload): String {
        val parts = payload.tables.filterValues { it.isNotEmpty() }.map { (table, rows) -> "${rows.size} ${label(table)}" }
        return "${payload.prefs.size} settings" + if (parts.isEmpty()) "" else ", " + parts.joinToString(", ")
    }

    private fun label(table: String) = when (table) {
        "app_entries" -> "app preferences"
        "usage_rules" -> "usage rules"
        "focus_schedules" -> "focus schedules"
        "categories" -> "categories"
        "profiles" -> "profiles"
        "notification_rules" -> "notification rules"
        "usage_aggregates" -> "usage records"
        "usage_hours" -> "hourly usage records"
        "usage_days" -> "recorded days"
        "focus_sessions" -> "focus sessions"
        "timebox_sessions" -> "timeboxes"
        else -> table
    }
}
