package `in`.tuwi.yogaphone.guard

import org.junit.Assert.*
import org.junit.Test

class ShortsDetectorTest {
    private class Node(override val viewId: String? = null, override val text: CharSequence? = null,
        val children: List<Node> = emptyList()) : NodeView {
        override val className: String? = null
        override val contentDescription: CharSequence? = null
        override val childCount get() = children.size
        var recycled = false
        override fun child(index: Int) = children[index]
        override fun recycle() { recycled = true }
    }
    private val enabled = GuardSurface.entries.toSet()
    @Test fun nativeIdsMatchTheirPackageOnly() {
        ShortsDetector.youtubeIds.forEach { id ->
            assertEquals(GuardSurface.YOUTUBE, ShortsDetector.detect("com.google.android.youtube", Node(children = listOf(Node("com.google.android.youtube:id/$id"))), enabled))
            assertNull(ShortsDetector.detect("com.instagram.android", Node(id), enabled))
        }
        ShortsDetector.instagramIds.forEach { id ->
            assertEquals(GuardSurface.INSTAGRAM, ShortsDetector.detect("com.instagram.android", Node("com.instagram.android:id/$id"), enabled))
            assertNull(ShortsDetector.detect("com.google.android.youtube", Node(id), enabled))
        }
        assertNull(ShortsDetector.detect("com.google.android.youtube", Node("video_player", "Shorts"), enabled))
        assertNull(ShortsDetector.detect("com.instagram.android", Node("feed", "Reels"), enabled))
        assertNull(ShortsDetector.detect("com.google.android.youtube", Node("shorts_container"), emptySet()))
    }
    @Test fun browsersOnlyMatchAddressBarAndExactHostsAndPaths() {
        ShortsDetector.browserAddressIds.forEach { (pkg, ids) -> ids.forEach { id ->
            assertEquals(GuardSurface.BROWSER_YOUTUBE, ShortsDetector.detect(pkg, Node(id, "https://www.youtube.com/shorts/123"), enabled))
            assertEquals(GuardSurface.BROWSER_INSTAGRAM, ShortsDetector.detect(pkg, Node(id, "instagram.com/reels/123"), enabled))
            listOf("https://youtube.com/watch?v=123", "https://instagram.com/profile", "https://youtube.com.evil.test/shorts/123", "https://evil.test/youtube.com/shorts", "https://youtube.com/shortstuff").forEach { url ->
                assertNull(ShortsDetector.detect(pkg, Node(id, url), enabled))
            }
            assertNull(ShortsDetector.detect(pkg, Node("page_text", "youtube.com/shorts/123"), enabled))
            assertNull(ShortsDetector.detect(pkg, Node(id, "youtube.com/shorts/123"), setOf(GuardSurface.YOUTUBE)))
        } }
    }
    @Test fun walkIsBoundedAndAcquiredNodesAreReleased() {
        val children = List(399) { Node() } + Node("shorts_container")
        val root = Node(children = children)
        assertNull(ShortsDetector.detect("com.google.android.youtube", root, enabled))
        assertTrue(root.recycled)
        assertTrue(children.take(399).all { it.recycled })
        assertFalse(children.last().recycled)
        var deep = Node("shorts_container")
        repeat(31) { deep = Node(children = listOf(deep)) }
        assertNull(ShortsDetector.detect("com.google.android.youtube", deep, enabled))
        val hit = Node("shorts_container")
        val parent = Node(children = listOf(hit))
        assertEquals(GuardSurface.YOUTUBE, ShortsDetector.detect("com.google.android.youtube", parent, enabled))
        assertTrue(parent.recycled && hit.recycled)
    }
}
