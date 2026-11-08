package `in`.tuwi.yogaphone.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerSearchTest {
    @Test fun `forward typing with one match opens`() {
        assertTrue(shouldAutoOpen("yo", "yog", 1, picking = false, enabled = true))
    }

    @Test fun `backspace does not open`() {
        assertFalse(shouldAutoOpen("yog", "yo", 1, picking = false, enabled = true))
    }

    @Test fun `picking mode does not open`() {
        assertFalse(shouldAutoOpen("yo", "yog", 1, picking = true, enabled = true))
    }

    @Test fun `disabled preference does not open`() {
        assertFalse(shouldAutoOpen("yo", "yog", 1, picking = false, enabled = false))
    }

    @Test fun `match count other than one does not open`() {
        assertFalse(shouldAutoOpen("yo", "yog", 0, picking = false, enabled = true))
        assertFalse(shouldAutoOpen("yo", "yog", 2, picking = false, enabled = true))
    }
}
