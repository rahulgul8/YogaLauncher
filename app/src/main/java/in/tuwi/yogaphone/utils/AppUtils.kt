package `in`.tuwi.yogaphone.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserHandle
import android.os.UserManager
import android.widget.Toast
import `in`.tuwi.yogaphone.data.AppModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator

/**
 * Get list of all installed apps
 */
suspend fun getAppsList(context: Context): List<AppModel> {
    return withContext(Dispatchers.IO) {
        val appList = mutableListOf<AppModel>()

        try {
            val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val collator = Collator.getInstance()

            for (profile in userManager.userProfiles) {
                for (app in launcherApps.getActivityList(null, profile)) {
                    // Skip our own app
                    if (app.applicationInfo.packageName == context.packageName) {
                        continue
                    }

                    val appModel = AppModel(
                        appLabel = app.label.toString(),
                        appPackage = app.applicationInfo.packageName,
                        activityClassName = app.componentName.className,
                        user = profile,
                        key = collator.getCollationKey(app.label.toString())
                    )
                    appList.add(appModel)
                }
            }

            appList.sort()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        appList
    }
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
 * Get UserHandle from string representation
 */
fun getUserHandleFromString(context: Context, userHandleString: String): UserHandle {
    val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    for (userHandle in userManager.userProfiles) {
        if (userHandle.toString() == userHandleString) {
            return userHandle
        }
    }
    return android.os.Process.myUserHandle()
}

/**
 * Show toast message
 */
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    if (message.isNotBlank()) {
        Toast.makeText(this, message, duration).show()
    }
}
