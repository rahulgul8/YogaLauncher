package `in`.tuwi.yogaphone.update

import org.junit.Assert.assertEquals
import org.junit.Test

class WhatsNewTest {
    private val entry = WhatsNewEntry(3, "1.2", listOf("Calmer settings"))

    @Test fun firstRunStoresCurrentVersionAndShowsNothing() {
        assertEquals(WhatsNewAction.None(3), WhatsNew.action(null, 3, listOf(entry)))
    }

    @Test fun versionIncreaseWithEntryShowsIt() {
        assertEquals(WhatsNewAction.Show(entry), WhatsNew.action(2, 3, listOf(entry)))
    }

    @Test fun sameOrLowerVersionShowsNothing() {
        assertEquals(WhatsNewAction.None(), WhatsNew.action(3, 3, listOf(entry)))
        assertEquals(WhatsNewAction.None(), WhatsNew.action(4, 3, listOf(entry)))
    }

    @Test fun missingEntryIsMarkedSeenWithoutShowing() {
        assertEquals(WhatsNewAction.None(4), WhatsNew.action(3, 4, listOf(entry)))
    }
}
