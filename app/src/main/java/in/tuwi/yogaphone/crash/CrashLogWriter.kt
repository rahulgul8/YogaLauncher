package `in`.tuwi.yogaphone.crash

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes the last few crash stack traces to a capped file in [directory].
 * Each entry records the time, app version, Android version, thread and stack trace.
 * No personal data is stored.
 *
 * The file is rotated: once it exceeds [maxEntries] entries, the oldest are dropped.
 */
class CrashLogWriter(
    private val directory: File,
    private val maxEntries: Int = MAX_ENTRIES,
    private val maxFileSizeBytes: Long = MAX_FILE_SIZE,
) {
    private val file get() = File(directory, FILE_NAME)

    /**
     * Appends a crash entry. Thread-safe: called from the uncaught exception handler,
     * which runs on the crashing thread and cannot contend with another crash handler.
     */
    fun write(
        throwable: Throwable,
        threadName: String,
        appVersionName: String,
        appVersionCode: Int,
        androidVersion: String,
        timestamp: Long = System.currentTimeMillis(),
    ) {
        try {
            directory.mkdirs()
            val entry = formatEntry(throwable, threadName, appVersionName, appVersionCode, androidVersion, timestamp)
            val existing = readEntries()
            // Keep the last (maxEntries - 1) entries plus the new one.
            val kept = if (existing.size >= maxEntries) existing.takeLast(maxEntries - 1) else existing
            val all = kept + entry
            file.writeText(all.joinToString(SEPARATOR))
            // If somehow the file is way too large, truncate from the front.
            if (file.length() > maxFileSizeBytes) {
                val trimmed = readEntries().takeLast(maxEntries / 2)
                file.writeText(trimmed.joinToString(SEPARATOR))
            }
        } catch (_: Exception) {
            // The crash handler must never throw.
        }
    }

    /** Returns the latest crash entry, or null if no crashes are stored. */
    fun latest(): String? = readEntries().lastOrNull()

    /** Returns all stored crash entries, oldest first. */
    fun all(): List<String> = readEntries()

    /** Deletes the crash log file. */
    fun clear() {
        try { file.delete() } catch (_: Exception) {}
    }

    /** True when there is at least one stored crash entry. */
    fun hasEntries(): Boolean = try { file.exists() && file.length() > 0 } catch (_: Exception) { false }

    private fun readEntries(): List<String> = try {
        if (!file.exists()) emptyList()
        else file.readText().split(SEPARATOR).filter { it.isNotBlank() }
    } catch (_: Exception) { emptyList() }

    companion object {
        const val FILE_NAME = "crash_log.txt"
        const val MAX_ENTRIES = 5
        const val MAX_FILE_SIZE = 64L * 1024 // 64 KiB
        /** Separator between entries in the file. Chosen to be unlikely in a stack trace. */
        internal const val SEPARATOR = "\n--- CRASH ---\n"

        fun formatEntry(
            throwable: Throwable,
            threadName: String,
            appVersionName: String,
            appVersionCode: Int,
            androidVersion: String,
            timestamp: Long,
        ): String {
            val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US).format(Date(timestamp))
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val trace = sw.toString().let { if (it.length > 8_000) it.take(8_000) + "\n... (truncated)" else it }
            return buildString {
                appendLine("Time: $time")
                appendLine("Version: $appVersionName ($appVersionCode)")
                appendLine("Android: $androidVersion")
                appendLine("Thread: $threadName")
                appendLine("Trace:")
                append(trace)
            }
        }
    }
}
