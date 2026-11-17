package `in`.tuwi.yogaphone.crash

import android.os.Build
import `in`.tuwi.yogaphone.BuildConfig
import java.io.File

/**
 * Installs a [Thread.UncaughtExceptionHandler] that persists crash information
 * to a capped file before chaining to the previous handler (which typically
 * terminates the process).
 *
 * Call [install] once from [android.app.Application.onCreate].
 */
object YogaCrashHandler {

    @Volatile
    private var installed = false

    fun install(filesDir: File) {
        if (installed) return
        installed = true

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        val writer = CrashLogWriter(filesDir)

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writer.write(
                    throwable = throwable,
                    threadName = thread.name,
                    appVersionName = BuildConfig.VERSION_NAME,
                    appVersionCode = BuildConfig.VERSION_CODE,
                    androidVersion = Build.VERSION.RELEASE,
                )
            } catch (_: Exception) {
                // Must never prevent the previous handler from running.
            }
            // Chain to the previous handler (Android's default kills the process).
            previous?.uncaughtException(thread, throwable)
        }
    }

    /** Convenience: a [CrashLogWriter] pointing at the same directory. */
    fun writer(filesDir: File) = CrashLogWriter(filesDir)
}
