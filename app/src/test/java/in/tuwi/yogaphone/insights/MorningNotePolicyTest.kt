package `in`.tuwi.yogaphone.insights

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class MorningNotePolicyTest {

    private val today: LocalDate = LocalDate.of(2026, 11, 17)

    private fun at(hour: Int, minute: Int): LocalDateTime = today.atTime(hour, minute)

    @Test
    fun hiddenBeforeStartHour() {
        assertFalse(MorningNotePolicy.shouldShow(at(3, 59), null, true))
    }

    @Test
    fun shownAtStartHour() {
        assertTrue(MorningNotePolicy.shouldShow(at(4, 0), null, true))
    }

    @Test
    fun shownJustBeforeEndHour() {
        assertTrue(MorningNotePolicy.shouldShow(at(13, 59), null, true))
    }

    @Test
    fun hiddenAtEndHour() {
        assertFalse(MorningNotePolicy.shouldShow(at(14, 0), null, true))
    }

    @Test
    fun hiddenWhenClosedToday() {
        assertFalse(MorningNotePolicy.shouldShow(at(8, 0), today, true))
    }

    @Test
    fun shownWhenClosedYesterday() {
        assertTrue(MorningNotePolicy.shouldShow(at(8, 0), today.minusDays(1), true))
    }

    @Test
    fun hiddenWithoutContent() {
        assertFalse(MorningNotePolicy.shouldShow(at(8, 0), null, false))
    }
}
