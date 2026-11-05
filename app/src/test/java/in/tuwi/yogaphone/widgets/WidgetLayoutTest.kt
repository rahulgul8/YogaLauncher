package `in`.tuwi.yogaphone.widgets

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetLayoutTest {
    @Test fun heights() {
        assertEquals(120, WidgetLayout.defaultHeight(40))
        assertEquals(250, WidgetLayout.defaultHeight(250))
        assertEquals(400, WidgetLayout.defaultHeight(900))
        assertEquals(180, WidgetLayout.resize(120, 180))
        assertEquals(320, WidgetLayout.resize(320, 180))
        assertEquals(400, WidgetLayout.resize(320, 500))
    }

    @Test fun moves() {
        assertEquals(listOf("b", "a", "c"), WidgetLayout.move(listOf("a", "b", "c"), 1, -1))
        assertEquals(listOf("a", "b", "c"), WidgetLayout.move(listOf("a", "b", "c"), 2, 1))
        assertEquals(listOf("a", "b"), WidgetLayout.move(listOf("a", "b"), 5, 1))
    }
}
