package `in`.tuwi.yogaphone.usage

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Telephony
import android.telecom.TelecomManager
import `in`.tuwi.yogaphone.data.db.AppEntryEntity

data class AppClassification(val essential: Boolean, val distracting: Boolean)
class AppClassifier(private val context: Context) {
    fun mandatoryEssential(): Set<String> = setOfNotNull(context.packageName, "com.android.settings",
        runCatching { context.getSystemService(TelecomManager::class.java).defaultDialerPackage }.getOrNull(),
        runCatching { Telephony.Sms.getDefaultSmsPackage(context) }.getOrNull())
    private fun resolve(intent: Intent): String? = runCatching {
        context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
    }.getOrNull()?.takeUnless { it == "android" }
    fun classify(packageName: String, entry: AppEntryEntity?): AppClassification {
        if (packageName in mandatoryEssential()) return AppClassification(true, false)
        if (entry?.userClassified == true) return AppClassification(entry.isEssential, entry.isDistracting && !entry.isEssential)
        val essential = packageName in setOfNotNull(resolve(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)),
            resolve(Intent(AlarmClock.ACTION_SHOW_ALARMS)), "com.google.android.apps.maps", "com.google.android.apps.authenticator2",
            "com.azure.authenticator", "com.authy.authy", "org.fedorahosted.freeotp", "com.beemdevelopment.aegis", "com.twofasapp")
        val category = runCatching { context.packageManager.getApplicationInfo(packageName, 0).category }.getOrDefault(-1)
        val distracting = category in setOf(ApplicationInfo.CATEGORY_SOCIAL, ApplicationInfo.CATEGORY_VIDEO,
            ApplicationInfo.CATEGORY_GAME, ApplicationInfo.CATEGORY_NEWS) || packageName in distractingPackages
        return AppClassification(essential, distracting && !essential)
    }
    companion object {
        val distractingPackages = setOf("com.instagram.android", "com.facebook.katana", "com.facebook.lite", "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill", "com.google.android.youtube", "com.twitter.android", "com.reddit.frontpage",
            "com.snapchat.android", "com.netflix.mediaclient", "com.amazon.avod.thirdpartyclient", "com.pinterest",
            "com.instagram.barcelona", "com.linkedin.android", "in.mohalla.sharechat", "in.mohalla.video", "com.eterno.shortvideos")
    }
}
