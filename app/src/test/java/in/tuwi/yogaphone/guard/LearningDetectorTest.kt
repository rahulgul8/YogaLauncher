package `in`.tuwi.yogaphone.guard

import org.junit.Assert.*
import org.junit.Test

class LearningDetectorTest {
    private class Node(override val viewId: String? = null, override val text: CharSequence? = null,
        override val contentDescription: CharSequence? = null, private val children: List<Node> = emptyList()) : NodeView {
        override val className: String? = null
        override val childCount get() = children.size
        var recycled = false
        override fun child(index: Int) = children[index]
        override fun recycle() { recycled = true }
    }
    @Test fun readsViewIdsFromWatchPage() {
        val root = Node(children = listOf(Node("com.google.android.youtube:id/watch_player"),
            Node("com.google.android.youtube:id/video_title", "A useful lesson"),
            Node("com.google.android.youtube:id/channel_name", "Khan Academy")))
        assertEquals(WatchInfo("Khan Academy", "A useful lesson", true), LearningDetector.read(LearningDetector.YOUTUBE_PACKAGE, root))
        assertTrue(root.recycled)
    }
    @Test fun readsDescriptionFallbackFromAnotherLayout() {
        val root = Node(children = listOf(Node("watch_metadata"), Node(contentDescription = "Go to channel: NPTEL"),
            Node("watch_metadata_title", "Signals lecture")))
        assertEquals(WatchInfo("NPTEL", "Signals lecture", true), LearningDetector.read(LearningDetector.YOUTUBE_PACKAGE, root))
    }
    @Test fun readsMiniPlayerAndRejectsNonWatchFeed() {
        val mini = Node(children = listOf(Node("mini_player"), Node(contentDescription = "MIT OpenCourseWare, channel avatar")))
        assertEquals("MIT OpenCourseWare", LearningDetector.read(LearningDetector.YOUTUBE_PACKAGE, mini).channel)
        val feed = Node(children = listOf(Node("home_feed", "Home"), Node("video_title", "A video"),
            Node("channel_name", "A creator"),
            Node(contentDescription = "A creator, channel avatar")))
        assertEquals(WatchInfo(), LearningDetector.read(LearningDetector.YOUTUBE_PACKAGE, feed))
    }
    @Test fun normalisesAndHintsWithoutAutoClassifying() {
        assertEquals("khan academy", LearningChannels.normalize("  KHAN   Academy 🎓 - Topic "))
        assertTrue(LearningChannels.suggestsLearning("How to prepare: interview prep tutorial"))
        assertFalse(LearningChannels.suggestsLearning("A day in my life"))
    }
    @Test fun walkIsBounded() {
        val root = Node(children = List(400) { Node() } + Node("channel_name", "Too late"))
        assertNull(LearningDetector.read(LearningDetector.YOUTUBE_PACKAGE, root).channel)
    }
}
