package `in`.tuwi.yogaphone.guard

import java.net.URI
import java.util.Locale

/** Children are acquired lazily. detect owns and releases every acquired node, including root. */
interface NodeView {
    val viewId: String?
    val className: String?
    val text: CharSequence?
    val contentDescription: CharSequence?
    val childCount: Int
    fun child(index: Int): NodeView?
    fun recycle() {}
}

object ShortsDetector {
    val youtubeIds = setOf("reel_recycler", "reel_watch_player", "reel_player_page_container", "shorts_container")
    val instagramIds = setOf("clips_viewer_view_pager", "clips_viewer_pager", "clips_video_container", "root_clips_layout")
    val browserAddressIds = mapOf(
        "com.android.chrome" to setOf("url_bar"),
        "com.sec.android.app.sbrowser" to setOf("location_bar_edit_text", "location_bar_text"),
        "org.mozilla.firefox" to setOf("mozac_browser_toolbar_url_view", "mozac_browser_toolbar_edit_url_view"),
    )
    private val nativeSurfaces = mapOf(
        "com.google.android.youtube" to (GuardSurface.YOUTUBE to youtubeIds),
        "com.instagram.android" to (GuardSurface.INSTAGRAM to instagramIds),
    )

    fun supportedPackages(enabled: Set<GuardSurface>): Set<String> =
        nativeSurfaces.filterValues { it.first in enabled }.keys +
            if (GuardSurface.BROWSER_YOUTUBE in enabled || GuardSurface.BROWSER_INSTAGRAM in enabled)
                browserAddressIds.keys else emptySet()

    fun detect(packageName: String, root: NodeView, enabled: Set<GuardSurface>): GuardSurface? {
        var visited = 0
        fun visit(node: NodeView, depth: Int): GuardSurface? {
            try {
                if (visited >= 400 || depth > 30) return null
                visited++
                val id = node.viewId.orEmpty()
                val localId = id.substringAfterLast('/')
                val native = nativeSurfaces[packageName]
                if (native != null && native.first in enabled && localId in native.second &&
                    (id == localId || id.startsWith("$packageName:id/"))) return native.first
                if (localId in browserAddressIds[packageName].orEmpty()) {
                    val surface = browserSurface(node.text?.toString().orEmpty())
                        ?: browserSurface(node.contentDescription?.toString().orEmpty())
                    if (surface in enabled) return surface
                }
                if (depth < 30) for (index in 0 until node.childCount.coerceAtMost(400)) {
                    if (visited >= 400) break
                    val child = node.child(index) ?: continue
                    visit(child, depth + 1)?.let { return it }
                }
                return null
            } finally { node.recycle() }
        }
        return visit(root, 0)
    }

    private fun browserSurface(raw: String): GuardSurface? {
        val value = raw.trim()
        val uri = runCatching { URI(if ("://" in value) value else "https://$value") }.getOrNull() ?: return null
        if (uri.scheme !in setOf("https", "http")) return null
        val host = uri.host?.lowercase(Locale.ROOT) ?: return null
        val path = uri.path.orEmpty()
        return when {
            (host == "youtube.com" || host.endsWith(".youtube.com")) &&
                (path == "/shorts" || path.startsWith("/shorts/")) -> GuardSurface.BROWSER_YOUTUBE
            (host == "instagram.com" || host.endsWith(".instagram.com")) &&
                (path == "/reels" || path.startsWith("/reels/") || path == "/reel" || path.startsWith("/reel/")) -> GuardSurface.BROWSER_INSTAGRAM
            else -> null
        }
    }
}
