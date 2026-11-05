package `in`.tuwi.yogaphone.profiles

import java.util.Locale

data class BuiltInCategory(val id: String, val name: String, val distractingByDefault: Boolean)

object Categories {
    const val ESSENTIALS = "essentials"
    const val COMMUNICATION = "communication"
    const val SOCIAL = "social"
    const val VIDEO_MUSIC = "video_music"
    const val GAMES = "games"
    const val NEWS_READING = "news_reading"
    const val WORK = "work"
    const val SHOPPING = "shopping"
    const val TRAVEL_MAPS = "travel_maps"
    const val HEALTH = "health"
    const val FINANCE = "finance"
    const val TOOLS = "tools"
    const val OTHER = "other"

    val builtIns = listOf(
        BuiltInCategory(ESSENTIALS, "Essentials", false),
        BuiltInCategory(COMMUNICATION, "Communication", false),
        BuiltInCategory(SOCIAL, "Social", true),
        BuiltInCategory(VIDEO_MUSIC, "Video & Music", true),
        BuiltInCategory(GAMES, "Games", true),
        BuiltInCategory(NEWS_READING, "News & Reading", true),
        BuiltInCategory(WORK, "Work & Productivity", false),
        BuiltInCategory(SHOPPING, "Shopping", true),
        BuiltInCategory(TRAVEL_MAPS, "Travel & Maps", false),
        BuiltInCategory(HEALTH, "Health", false),
        BuiltInCategory(FINANCE, "Finance", false),
        BuiltInCategory(TOOLS, "Tools", false),
        BuiltInCategory(OTHER, "Other", false),
    )

    // android.content.pm.ApplicationInfo.CATEGORY_* values, kept here so this file stays Android-free.
    private const val APP_GAME = 0
    private const val APP_AUDIO = 1
    private const val APP_VIDEO = 2
    private const val APP_IMAGE = 3
    private const val APP_SOCIAL = 4
    private const val APP_NEWS = 5
    private const val APP_MAPS = 6
    private const val APP_PRODUCTIVITY = 7
    private const val APP_ACCESSIBILITY = 8

    private val knownPackages = mapOf(
        "com.whatsapp" to COMMUNICATION, "com.whatsapp.w4b" to COMMUNICATION, "org.telegram.messenger" to COMMUNICATION,
        "org.thoughtcrime.securesms" to COMMUNICATION, "com.google.android.gm" to COMMUNICATION,
        "com.google.android.apps.messaging" to COMMUNICATION, "com.google.android.dialer" to COMMUNICATION,
        "com.google.android.contacts" to COMMUNICATION, "com.microsoft.office.outlook" to COMMUNICATION,
        "com.instagram.android" to SOCIAL, "com.facebook.katana" to SOCIAL, "com.facebook.lite" to SOCIAL,
        "com.snapchat.android" to SOCIAL, "com.twitter.android" to SOCIAL, "com.reddit.frontpage" to SOCIAL,
        "com.linkedin.android" to SOCIAL, "com.pinterest" to SOCIAL, "com.instagram.barcelona" to SOCIAL,
        "in.mohalla.sharechat" to SOCIAL, "com.zhiliaoapp.musically" to SOCIAL,
        "com.google.android.youtube" to VIDEO_MUSIC, "com.netflix.mediaclient" to VIDEO_MUSIC,
        "com.spotify.music" to VIDEO_MUSIC, "com.amazon.avod.thirdpartyclient" to VIDEO_MUSIC,
        "in.startv.hotstar" to VIDEO_MUSIC, "com.google.android.apps.youtube.music" to VIDEO_MUSIC,
        "in.mohalla.video" to VIDEO_MUSIC, "com.eterno.shortvideos" to VIDEO_MUSIC,
        "com.amazon.mShop.android.shopping" to SHOPPING, "in.amazon.mShop.android.shopping" to SHOPPING,
        "com.flipkart.android" to SHOPPING, "com.myntra.android" to SHOPPING, "com.meesho.supply" to SHOPPING,
        "com.google.android.apps.maps" to TRAVEL_MAPS, "com.ubercab" to TRAVEL_MAPS, "com.olacabs.customer" to TRAVEL_MAPS,
        "net.one97.paytm" to FINANCE, "com.phonepe.app" to FINANCE, "com.google.android.apps.nbu.paisa.user" to FINANCE,
        "com.google.android.apps.docs" to WORK, "com.google.android.apps.docs.editors.docs" to WORK,
        "com.google.android.apps.docs.editors.sheets" to WORK, "com.Slack" to WORK, "com.microsoft.teams" to WORK,
        "us.zoom.videomeetings" to WORK, "com.google.android.calendar" to WORK, "notion.id" to WORK,
        "com.google.android.keep" to WORK, "com.amazon.kindle" to NEWS_READING,
        "com.google.android.apps.magazines" to NEWS_READING, "com.android.chrome" to TOOLS,
        "com.google.android.GoogleCamera" to TOOLS, "com.google.android.deskclock" to TOOLS,
        "com.google.android.calculator" to TOOLS, "com.android.settings" to ESSENTIALS,
    )

    private val keywordRules = listOf(
        listOf("bank", "upi", "wallet", "finance", "invest", "money", "pay") to FINANCE,
        listOf("mail", "messag", "messenger", "chat", "sms", "dialer", "phone", "contacts") to COMMUNICATION,
        listOf("maps", "travel", "ride", "cab", "metro", "rail", "flight", "transit") to TRAVEL_MAPS,
        listOf("music", "radio", "podcast", "video", "movie", "tv.", ".tv") to VIDEO_MUSIC,
        listOf("game", "games") to GAMES,
        listOf("shop", "cart", "mart", "store.") to SHOPPING,
        listOf("news", "reader", "books", "kindle", "magazine") to NEWS_READING,
        listOf("health", "fit", "medic", "doctor", "yoga", "meditat") to HEALTH,
        listOf("calc", "clock", "camera", "files", "scanner", "torch", "flashlight", "weather", "browser") to TOOLS,
        listOf("docs", "office", "sheets", "slides", "drive", "notes", "calendar", "task", "todo") to WORK,
    )

    /**
     * A local, best-effort category from the package name, the Android app category and the label.
     * [appCategory] is ApplicationInfo.category (or -1 when undefined).
     */
    fun autoCategory(packageName: String, appCategory: Int, label: String = "", essential: Boolean = false): String {
        if (essential) return ESSENTIALS
        knownPackages[packageName]?.let { return it }
        when (appCategory) {
            APP_GAME -> return GAMES
            APP_AUDIO, APP_VIDEO -> return VIDEO_MUSIC
            APP_SOCIAL -> return SOCIAL
            APP_NEWS -> return NEWS_READING
            APP_MAPS -> return TRAVEL_MAPS
            APP_PRODUCTIVITY -> return WORK
            APP_IMAGE, APP_ACCESSIBILITY -> return TOOLS
        }
        val haystack = "$packageName ${label.lowercase(Locale.ROOT)}".lowercase(Locale.ROOT)
        return keywordRules.firstOrNull { (words, _) -> words.any { it in haystack } }?.second ?: OTHER
    }

    fun slug(name: String): String = "custom_" + name.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "_").trim('_')
}

/** Ordered home-app list operations shared by profiles. */
object HomeList {
    fun add(list: List<String>, pkg: String, cap: Int): List<String> =
        if (pkg in list || list.size >= cap) list else list + pkg

    fun replace(list: List<String>, old: String, new: String): List<String> =
        if (new in list) list - old else list.map { if (it == old) new else it }

    fun move(list: List<String>, pkg: String, delta: Int): List<String> {
        val from = list.indexOf(pkg)
        if (from < 0) return list
        val to = (from + delta).coerceIn(list.indices)
        return list.toMutableList().apply { add(to, removeAt(from)) }
    }

    fun encode(list: List<String>) = list.joinToString(",")
    fun decode(raw: String) = raw.split(',').map(String::trim).filter(String::isNotEmpty).distinct()
}

/** Remembers an automatic, schedule-driven profile switch so it can be undone, and a manual override of it. */
data class AutoSwitchState(
    val spanKey: String? = null,
    val autoProfileId: Long? = null,
    val previousProfileId: Long? = null,
    val overriddenSpanKey: String? = null,
)

data class ProfileLink(val profileId: Long, val spanKey: String)

data class SwitchDecision(val activeProfileId: Long?, val state: AutoSwitchState)

object ProfileSwitchPlanner {
    /**
     * [link] is the profile whose schedule window is active now (if any), identified by a span key unique to that
     * occurrence. A manual switch during a span wins until the next transition. Null profile id = the default profile.
     */
    fun decide(active: Long?, link: ProfileLink?, state: AutoSwitchState): SwitchDecision {
        if (link != null) {
            if (state.overriddenSpanKey == link.spanKey || state.spanKey == link.spanKey) return SwitchDecision(active, state)
            val previous = if (state.spanKey != null && active == state.autoProfileId) state.previousProfileId else active
            return SwitchDecision(link.profileId, AutoSwitchState(link.spanKey, link.profileId, previous))
        }
        if (state.spanKey == null) return SwitchDecision(active, state.copy(overriddenSpanKey = null))
        // The window ended: restore what was active before, unless the user has since chosen something else.
        val restored = if (active == state.autoProfileId) state.previousProfileId else active
        return SwitchDecision(restored, AutoSwitchState())
    }

    /** The user picked a profile by hand. */
    fun manual(state: AutoSwitchState): AutoSwitchState =
        if (state.spanKey != null) AutoSwitchState(overriddenSpanKey = state.spanKey) else state
}
