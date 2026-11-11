package `in`.tuwi.yogaphone.guard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LoosenRulesTest {
    private val blocked = SurfaceSettings(enabled = true, allowanceMinutes = 0)

    @Test fun turningOffOrAllowingMoreIsLooser() {
        assertTrue(isLooser(blocked, blocked.copy(enabled = false)))
        assertTrue(isLooser(blocked, blocked.copy(allowanceMinutes = 5)))
        assertTrue(isLooser(blocked.copy(allowanceMinutes = 5), blocked.copy(allowanceMinutes = 10)))
    }

    @Test fun tighteningIsNotLooser() {
        assertFalse(isLooser(blocked.copy(enabled = false), blocked))
        assertFalse(isLooser(blocked.copy(allowanceMinutes = 10), blocked))
        assertFalse(isLooser(blocked.copy(allowanceMinutes = 10), blocked.copy(allowanceMinutes = 5)))
        assertFalse(isLooser(blocked.copy(enabled = false), SurfaceSettings(enabled = false, allowanceMinutes = 30)))
    }

    @Test fun strictEndsTheNextDay() {
        val today = LocalDate.of(2026, 11, 11)
        val pending = GuardPreferences(strict = true, strictOffFrom = strictOffFrom(today))
        assertEquals("2026-11-12", pending.strictOffFrom)
        assertTrue(isStrict(pending, today))
        assertFalse(strictEnded(pending, today))
        assertFalse(isStrict(pending, today.plusDays(1)))
        assertTrue(strictEnded(pending, today.plusDays(1)))
        assertFalse(strictEnded(GuardPreferences(strict = true), today))
    }
}
