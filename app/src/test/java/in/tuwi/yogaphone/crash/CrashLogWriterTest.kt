package `in`.tuwi.yogaphone.crash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CrashLogWriterTest {
    @get:Rule val folder = TemporaryFolder()

    private fun writer(max: Int = 3) = CrashLogWriter(folder.root, maxEntries = max)
    private fun CrashLogWriter.crash(message: String) =
        write(IllegalStateException(message), "main", "1.6", 8, "12", timestamp = 0L)

    @Test fun emptyLogHasNothing() {
        val log = writer()
        assertNull(log.latest())
        assertFalse(log.hasEntries())
    }

    @Test fun latestHoldsTheNewestTraceWithContext() {
        val log = writer()
        log.crash("first"); log.crash("second")
        val latest = log.latest()!!
        assertTrue(latest.contains("second"))
        assertTrue(latest.contains("Version: 1.6 (8)"))
        assertTrue(latest.contains("Thread: main"))
    }

    @Test fun keepsOnlyTheLastEntries() {
        val log = writer(max = 3)
        (1..5).forEach { log.crash("crash $it") }
        val all = log.all()
        assertEquals(3, all.size)
        assertTrue(all.first().contains("crash 3"))
        assertTrue(all.last().contains("crash 5"))
    }

    @Test fun clearRemovesEverything() {
        val log = writer()
        log.crash("gone")
        log.clear()
        assertNull(log.latest())
        assertFalse(log.hasEntries())
    }

    @Test fun longTracesAreCapped() {
        val deep = Exception("x".repeat(20_000))
        val entry = CrashLogWriter.formatEntry(deep, "t", "1", 1, "12", 0L)
        assertTrue(entry.length < 9_000)
        assertTrue(entry.endsWith("(truncated)"))
    }
}
