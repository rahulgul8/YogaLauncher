package `in`.tuwi.yogaphone.quiet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GreyscalePolicyTest {
    private val all = GreyTriggers(overLimit = true, duringFocus = true, bedtime = true)

    @Test fun triggersInOrderAndSnooze() {
        assertEquals(GreyReason.FOCUS, GreyscalePolicy.reason(all, true, true, true, 0, 10))
        assertEquals(GreyReason.BEDTIME, GreyscalePolicy.reason(all, false, true, true, 0, 10))
        assertEquals(GreyReason.OVER_LIMIT, GreyscalePolicy.reason(all, false, false, true, 0, 10))
        assertNull(GreyscalePolicy.reason(all, true, true, true, snoozedUntil = 11, now = 10))
        assertNull(GreyscalePolicy.reason(GreyTriggers(false, false, false), true, true, true, 0, 10))
    }
}

class HoldDeciderTest {
    private fun n(category: String? = null, ongoing: Boolean = false, fgs: Boolean = false, media: Boolean = false, summary: Boolean = false, pkg: String = "com.social") =
        IncomingNotification(pkg, category, ongoing, fgs, media, summary)

    @Test fun neverHoldsCallsAlarmsOngoingMediaOrOwn() {
        listOf(n("call"), n("alarm"), n(ongoing = true), n(fgs = true), n(media = true), n(summary = true), n(pkg = "me")).forEach {
            assertFalse(it.toString(), HoldDecider.shouldHold(it, NotificationPolicy.ALWAYS_HOLD, true, "me"))
        }
    }

    @Test fun policies() {
        assertTrue(HoldDecider.shouldHold(n("social"), NotificationPolicy.HOLD_DURING_FOCUS, true, "me"))
        assertFalse(HoldDecider.shouldHold(n("social"), NotificationPolicy.HOLD_DURING_FOCUS, false, "me"))
        assertTrue(HoldDecider.shouldHold(n(), NotificationPolicy.ALWAYS_HOLD, false, "me"))
        assertFalse(HoldDecider.shouldHold(n(), NotificationPolicy.ALLOW, true, "me"))
        assertEquals(NotificationPolicy.ALLOW, HoldDecider.defaultPolicy(distracting = true, essential = true))
        assertEquals(NotificationPolicy.HOLD_DURING_FOCUS, HoldDecider.defaultPolicy(distracting = true, essential = false))
    }
}

class DigestTest {
    @Test fun groupsNewestFirstAndDropsOld() {
        val now = Digest.KEEP_MS + 1000
        val items = listOf(
            HeldItem("1", "a", "t", "x", now - 10), HeldItem("2", "b", "t", "x", now - 5),
            HeldItem("3", "a", "t", "x", now - 1), HeldItem("4", "c", "t", "x", 0),
        )
        val groups = Digest.group(items, now)
        assertEquals(listOf("a", "b"), groups.map { it.packageName })
        assertEquals(listOf("3", "1"), groups.first().items.map { it.key })
        assertEquals("3 notifications held from 2 apps", Digest.summary(groups))
    }

    @Test fun nextDigestTime() {
        val times = listOf(12 * 60 + 30, 18 * 60 + 30)
        assertEquals(30, Digest.minutesUntilNext(times, 12 * 60))
        assertEquals(360, Digest.minutesUntilNext(times, 12 * 60 + 30))
        assertEquals(18 * 60, Digest.minutesUntilNext(times, 18 * 60 + 30))
        assertNull(Digest.minutesUntilNext(emptyList(), 0))
    }
}
