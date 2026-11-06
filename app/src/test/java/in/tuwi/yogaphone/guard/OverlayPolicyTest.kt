package `in`.tuwi.yogaphone.guard

import org.junit.Assert.*
import org.junit.Test

class OverlayPolicyTest {
    @Test fun permitsEligibleOrdinaryAppOnly() {
        assertTrue(OverlayPolicy.shouldShow(eligible = true))
        assertFalse(OverlayPolicy.shouldShow(eligible = false))
    }
    @Test fun everySafetyExclusionOverridesEligibility() {
        assertFalse(OverlayPolicy.shouldShow(true, essential = true))
        assertFalse(OverlayPolicy.shouldShow(true, dialer = true))
        assertFalse(OverlayPolicy.shouldShow(true, camera = true))
        assertFalse(OverlayPolicy.shouldShow(true, finance = true))
        assertFalse(OverlayPolicy.shouldShow(true, inCall = true))
        assertFalse(OverlayPolicy.shouldShow(true, locked = true))
        assertFalse(OverlayPolicy.shouldShow(true, screenOn = false))
        assertFalse(OverlayPolicy.shouldShow(true, ownApp = true))
        assertFalse(OverlayPolicy.shouldShow(true, systemUi = true))
        assertFalse(OverlayPolicy.shouldShow(true, keyboard = true))
        assertFalse(OverlayPolicy.shouldShow(true, launcher = true))
    }
}
