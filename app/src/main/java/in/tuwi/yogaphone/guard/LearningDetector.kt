package `in`.tuwi.yogaphone.guard

import java.util.Locale

data class WatchInfo(val channel: String? = null, val title: String? = null, val isWatchPage: Boolean = false)

object LearningDetector {
    const val YOUTUBE_PACKAGE = "com.google.android.youtube"
    private const val NODE_LIMIT = 400
    private const val DEPTH_LIMIT = 30

    private val channelIds = setOf("channel_name", "channel_title", "owner_name", "uploader_name", "video_owner_name")
    private val titleIds = setOf("video_title", "watch_title", "watch_metadata_title", "title")
    private val watchIds = setOf("watch_player", "watch_player_container", "watch_metadata", "video_metadata", "miniplayer", "mini_player")
    private val channelDescriptions = listOf(
        Regex("^go to channel[,:]?\\s*(.+)$", RegexOption.IGNORE_CASE),
        Regex("^(.+?)[, ]+channel avatar$", RegexOption.IGNORE_CASE),
        Regex("^channel avatar[,:]?\\s*(.+)$", RegexOption.IGNORE_CASE),
        Regex("^(.+?)[, ]+avatar$", RegexOption.IGNORE_CASE),
    )

    /** Owns and releases [root]. Values are returned only in memory to the caller. */
    fun read(packageName: String, root: NodeView): WatchInfo {
        if (packageName != YOUTUBE_PACKAGE) { root.recycle(); return WatchInfo() }
        var visited = 0
        var channelById: String? = null
        var channelByDescription: String? = null
        var title: String? = null
        var watchMarker = false

        fun clean(value: CharSequence?): String? = value?.toString()?.trim()?.takeIf(String::isNotEmpty)
        fun visit(node: NodeView, depth: Int) {
            try {
                if (visited >= NODE_LIMIT || depth > DEPTH_LIMIT) return
                visited++
                val localId = node.viewId.orEmpty().substringAfterLast('/')
                val text = clean(node.text)
                val description = clean(node.contentDescription)
                if (localId in watchIds) watchMarker = true
                if (localId in channelIds && channelById == null) channelById = text ?: description
                if (localId in titleIds && title == null) title = text ?: description
                if (channelByDescription == null && description != null) {
                    channelByDescription = channelDescriptions.firstNotNullOfOrNull { pattern ->
                        pattern.matchEntire(description)?.groupValues?.getOrNull(1)?.trim()?.takeIf(String::isNotEmpty)
                    }
                }
                if (depth < DEPTH_LIMIT) for (index in 0 until node.childCount.coerceAtMost(NODE_LIMIT)) {
                    if (visited >= NODE_LIMIT) break
                    node.child(index)?.let { visit(it, depth + 1) }
                }
            } finally { node.recycle() }
        }
        visit(root, 0)
        return if (watchMarker) WatchInfo(channelById ?: channelByDescription, title, true) else WatchInfo()
    }
}

object LearningChannels {
    val titleHintWords = setOf("lecture", "tutorial", "course", "chapter", "class", "lesson", "explained", "how to", "interview prep")

    fun normalize(raw: String): String {
        val withoutEmoji = buildString {
            var index = 0
            val input = raw.trim()
            while (index < input.length) {
                val codePoint = input.codePointAt(index)
                val type = Character.getType(codePoint)
                val strip = type == Character.OTHER_SYMBOL.toInt() || type == Character.MODIFIER_SYMBOL.toInt() ||
                    type == Character.SURROGATE.toInt() || codePoint == 0xFE0F || codePoint == 0x200D
                if (!strip) appendCodePoint(codePoint)
                index += Character.charCount(codePoint)
            }
        }
        return withoutEmoji.replace(Regex("\\s+-\\s+topic\\s*$", RegexOption.IGNORE_CASE), "")
            .lowercase(Locale.ROOT).trim().replace(Regex("\\s+"), " ")
    }

    fun suggestsLearning(title: String?): Boolean {
        val value = title?.lowercase(Locale.ROOT)?.replace(Regex("[^\\p{L}\\p{N}]+"), " ")?.trim() ?: return false
        return titleHintWords.any { word -> Regex("(^|\\s)${Regex.escape(word)}(\\s|$)").containsMatchIn(value) }
    }
}

object LearningAccounting {
    fun effectiveSeconds(measuredSeconds: Long, learningSeconds: Long): Long =
        (measuredSeconds - learningSeconds).coerceAtLeast(0)

    fun effectiveMinutes(measuredMinutes: Long, learningSeconds: Long): Long =
        effectiveSeconds(measuredMinutes.coerceAtLeast(0) * 60, learningSeconds.coerceAtLeast(0)) / 60

    fun totalFor(totals: Map<String, Long>, date: String, packageName: String): Long =
        totals["$date|$packageName"]?.coerceAtLeast(0) ?: 0
}

object LearningPolicy {
    fun isLearning(watch: WatchInfo, learningKeys: Set<String>): Boolean {
        val key = watch.channel?.let(LearningChannels::normalize)?.takeIf(String::isNotEmpty)
        return watch.isWatchPage && key != null && key in learningKeys
    }
}

enum class YouTubeFocusAction { ALLOW, BLOCK_WATCH, BLOCK_HOME }

object LearningFocusPolicy {
    fun decide(focusActive: Boolean, shorts: Boolean, watch: WatchInfo, channelKey: String?, learningKeys: Set<String>,
        allowHomeDuringFocus: Boolean): YouTubeFocusAction {
        if (!focusActive || shorts) return YouTubeFocusAction.ALLOW
        if (!watch.isWatchPage) return if (allowHomeDuringFocus) YouTubeFocusAction.ALLOW else YouTubeFocusAction.BLOCK_HOME
        return if (channelKey != null && channelKey in learningKeys) YouTubeFocusAction.ALLOW else YouTubeFocusAction.BLOCK_WATCH
    }
}

/** Debounced Back -> Back -> Home escalation for a focus-only YouTube block. */
class LearningFocusGate {
    private var blocked = false
    private var lastActionAt: Long? = null
    private val backs = mutableListOf<Long>()
    private var escalated = false

    fun evaluate(shouldBlock: Boolean, nowMillis: Long): GateAction {
        if (!shouldBlock) { blocked = false; lastActionAt = null; backs.clear(); escalated = false; return GateAction.NONE }
        if (!blocked) { blocked = true; lastActionAt = null; backs.clear(); escalated = false }
        if (escalated || lastActionAt?.let { nowMillis - it < 1_000 } == true) return GateAction.NONE
        backs.removeAll { nowMillis - it > 3_000 }
        val action = if (backs.size >= 2) GateAction.HOME else GateAction.BACK
        if (action == GateAction.HOME) { backs.clear(); escalated = true } else backs += nowMillis
        lastActionAt = nowMillis
        return action
    }
}
