package `in`.tuwi.yogaphone.resist

import org.junit.Assert.assertEquals
import org.junit.Test

class ResistanceLadderTest {
    @Test fun waitDoublesPerPassUpToTheCap() {
        val waits = (0..4).map { ResistanceLadder.waitSeconds(15, it, 120) }
        assertEquals(listOf(15, 30, 60, 120, 120), waits)
    }

    @Test fun overLimitDoublesTheBase() {
        assertEquals(10, ResistanceLadder.waitSeconds(5, 0, 60, overLimit = true))
        assertEquals(60, ResistanceLadder.waitSeconds(5, 5, 60, overLimit = true))
    }

    @Test fun waitNeverDropsBelowTheBase() {
        assertEquals(90, ResistanceLadder.waitSeconds(90, 0, 60))
        assertEquals(1, ResistanceLadder.waitSeconds(0, 0, 60))
    }

    @Test fun choicesShrinkAfterThreePassesOrPastTheLimit() {
        assertEquals(listOf(5, 10, 15), ResistanceLadder.choices(2))
        assertEquals(listOf(5), ResistanceLadder.choices(3))
        assertEquals(listOf(5), ResistanceLadder.choices(0, overLimit = true))
    }

    @Test fun preselectKeepsLastChoiceWhenOffered() {
        assertEquals(15, ResistanceLadder.preselect(15, ResistanceLadder.PASS_MINUTES))
        assertEquals(10, ResistanceLadder.preselect(null, ResistanceLadder.PASS_MINUTES))
        assertEquals(5, ResistanceLadder.preselect(15, ResistanceLadder.SHORT_PASS))
    }

    @Test fun ordinals() {
        assertEquals(listOf("1st", "2nd", "3rd", "4th", "11th", "12th", "13th", "21st", "22nd"),
            listOf(1, 2, 3, 4, 11, 12, 13, 21, 22).map(ResistanceLadder::ordinal))
    }
}
