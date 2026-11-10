package `in`.tuwi.yogaphone.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.widget.Toast
import `in`.tuwi.yogaphone.data.AppModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator

/**
 * Get launcher activities for the main user. The Room schema is package-scoped,
 * so profile-specific favorites can be added later with an explicit schema migration.
 */
suspend fun getAppsList(context: Context): List<AppModel> {
    return withContext(Dispatchers.IO) {
        val appList = mutableListOf<AppModel>()

        try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val collator = Collator.getInstance()
            val profile = android.os.Process.myUserHandle()

            for (app in launcherApps.getActivityList(null, profile)) {
                if (app.applicationInfo.packageName == context.packageName) continue

                val appModel = AppModel(
                    appLabel = app.label.toString(),
                    appPackage = app.applicationInfo.packageName,
                    activityClassName = app.componentName.className,
                    user = profile,
                    key = collator.getCollationKey(app.label.toString()),
                )
                appList.add(appModel)
            }

            appList.sort()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        appList
    }
}

/** Launcher activities in the other profiles (work), each marked with whether its profile is paused. */
suspend fun getWorkApps(context: Context): List<AppModel> = withContext(Dispatchers.IO) {
    runCatching {
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        val users = context.getSystemService(UserManager::class.java)
        val collator = Collator.getInstance()
        launcherApps.profiles.filter { it != Process.myUserHandle() }.flatMap { profile ->
            val paused = runCatching { users.isQuietModeEnabled(profile) }.getOrDefault(false)
            launcherApps.getActivityList(null, profile).map { app ->
                AppModel(
                    appLabel = app.label.toString(),
                    appPackage = app.applicationInfo.packageName,
                    activityClassName = app.componentName.className,
                    user = profile,
                    key = collator.getCollationKey(app.label.toString()),
                    isWork = true,
                    workPaused = paused,
                )
            }
        }.sorted()
    }.getOrDefault(emptyList())
}

/**
 * Turns a paused work profile back on; Android may ask for the work PIN first. False when it can't be done
 * from here (Android 8, or Yoga isn't the default home app), so the caller can point to Settings instead.
 */
fun requestWorkProfileOn(context: Context, user: UserHandle): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
    return runCatching { context.getSystemService(UserManager::class.java).requestQuietModeEnabled(false, user); true }
        .getOrDefault(false)
}

/**
 * Launch an app
 */
fun launchApp(context: Context, appModel: AppModel) {
    val launcher = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val activityInfo = launcher.getActivityList(appModel.appPackage, appModel.user)

    val component = if (appModel.activityClassName.isNullOrBlank()) {
        when (activityInfo.size) {
            0 -> {
                context.showToast("App not found")
                return
            }
            1 -> ComponentName(appModel.appPackage, activityInfo[0].name)
            else -> ComponentName(appModel.appPackage, activityInfo[activityInfo.size - 1].name)
        }
    } else {
        ComponentName(appModel.appPackage, appModel.activityClassName)
    }

    try {
        launcher.startMainActivity(component, appModel.user, null, null)
    } catch (e: SecurityException) {
        try {
            launcher.startMainActivity(component, android.os.Process.myUserHandle(), null, null)
        } catch (e: Exception) {
            context.showToast("Unable to open app")
        }
    } catch (e: Exception) {
        context.showToast("Unable to open app")
    }
}

/**
 * Show toast message
 */
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    if (message.isNotBlank()) {
        Toast.makeText(this, message, duration).show()
    }
}
