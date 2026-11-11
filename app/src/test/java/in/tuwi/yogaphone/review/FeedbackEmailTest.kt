package `in`.tuwi.yogaphone.review

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackEmailTest {
    @Test fun bodyIncludesDeviceVersionAndRecentAnswers() {
        val body = buildFeedbackEmailBody("1.4", 14, "16", "Yoga Test Phone", listOf(
            FeedbackAnswer(FeedbackFeature.PAUSE, "Too slow", 2),
            FeedbackAnswer(FeedbackFeature.GUARD, "Other: A detail", 1),
        ))
        assertTrue(body.contains("Version: 1.4 (14)"))
        assertTrue(body.contains("Android: 16"))
        assertTrue(body.contains("Device: Yoga Test Phone"))
        assertTrue(body.contains("- Mindful pause: Too slow"))
        assertTrue(body.contains("- Guard: Other: A detail"))
        assertTrue(body.endsWith("What would you like us to know?\n"))
    }

    @Test fun bodyWithoutAnswersHasNoRecentFeedbackHeading() {
        val body = buildFeedbackEmailBody("1", 1, "16", "Phone", emptyList())
        assertEquals(false, body.contains("Recent feedback:"))
    }
}
