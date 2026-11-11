package `in`.tuwi.yogaphone.guard

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class GuardPassTest {
    private val youtube = GuardSurface.YOUTUBE
    private val settings = GuardSurface.entries.associateWith { SurfaceSettings(enabled = true) }
    private val date = "2026-11-11"
    @Test fun passAllowsThenExpiresAtExactEpochBoundary() {
        val gate = ShortsGate()
        val pass = mapOf(youtube to 10_000L)
        assertEquals(GateAction.NONE, gate.evaluate(youtube, 0, date, settings, pass, 9_999))
        assertEquals(GateAction.BACK, gate.evaluate(youtube, 1, date, settings, pass, 10_000))
        assertEquals(1, gate.daily.blocks[youtube])
    }
    @Test fun familyPassCoversNativeAndBrowserButNotOtherFamily() {
        for (source in GuardSurface.entries) {
            for (target in GuardSurface.entries) {
                val action = ShortsGate().evaluate(target, 0, date, settings, mapOf(source to 10_000L), 1)
                assertEquals(if (target in source.family()) GateAction.NONE else GateAction.BACK, action)
            }
        }
    }
    @Test fun livePassStillChargesAllowanceAndDoesNotCountBlocks() {
        val gate = ShortsGate()
        val allowance = mapOf(youtube to SurfaceSettings(true, 5))
        val pass = mapOf(youtube to 1_000_000L)
        gate.evaluate(youtube, 0, date, allowance, pass, 1)
        assertEquals(GateAction.NONE, gate.evaluate(youtube, 300_000, date, allowance, pass, 300_001))
        assertEquals(300_000L, gate.daily.usedMillis[youtube])
        assertEquals(0, gate.daily.blocks[youtube] ?: 0)
        assertEquals(GateAction.BACK, gate.evaluate(youtube, 1_000_000, date, allowance, pass, 1_000_001))
    }
    @Test fun strictOnlyControlsNewChoicesAndDoesNotInvalidateLivePass() {
        val prefs = GuardPreferences(strict = true, passUntil = mapOf(youtube to 500L))
        assertTrue(isStrict(prefs, LocalDate.parse(date)))
        assertEquals(GateAction.NONE, ShortsGate().evaluate(youtube, 0, date, settings, prefs.passUntil, 1))
    }
    @Test fun strictOffDateIsInclusiveAndMalformedDateStaysStrict() {
        val today = LocalDate.parse(date)
        assertFalse(isStrict(GuardPreferences(), today))
        assertTrue(isStrict(GuardPreferences(strict = true), today))
        assertTrue(isStrict(GuardPreferences(strict = true, strictOffFrom = "invalid"), today))
        val prefs = GuardPreferences(strict = true, strictOffFrom = today.plusDays(1).toString())
        assertTrue(isStrict(prefs, today))
        assertFalse(isStrict(prefs, today.plusDays(1)))
        assertFalse(isStrict(prefs, today.plusDays(2)))
    }
    @Test fun staleGateSavePreservesTransactionalChoices() {
        val stored = GuardDaily(date, passes = mapOf(youtube to 2), resisted = mapOf(youtube to 3))
        val gate = GuardDaily(date, blocks = mapOf(youtube to 4), usedMillis = mapOf(youtube to 20L))
        val merged = mergeGuardDaily(stored, gate)
        assertEquals(stored.passes, merged.passes)
        assertEquals(stored.resisted, merged.resisted)
        assertEquals(gate.blocks, merged.blocks)
        assertEquals(gate.usedMillis, merged.usedMillis)
    }
    @Test fun rolloverClearsChoicesAndLateYesterdaySaveCannotOverwriteToday() {
        val old = GuardDaily("2026-11-10", passes = mapOf(youtube to 2), resisted = mapOf(youtube to 3))
        val today = GuardDaily(date, passes = mapOf(youtube to 1))
        assertEquals(today, mergeGuardDaily(today, old))
        assertTrue(mergeGuardDaily(old, today).passes.isEmpty())
        assertTrue(mergeGuardDaily(old, today).resisted.isEmpty())
        val gate = ShortsGate(old)
        gate.evaluate(null, 0, date, settings)
        assertTrue(gate.daily.passes.isEmpty())
        assertTrue(gate.daily.resisted.isEmpty())
    }
    @Test fun passResetsPriorEscalation() {
        val gate = ShortsGate()
        gate.evaluate(youtube, 0, date, settings)
        gate.evaluate(youtube, 1_000, date, settings)
        gate.evaluate(youtube, 2_000, date, settings, mapOf(youtube to 4_000L), 2_000)
        assertEquals(GateAction.BACK, gate.evaluate(youtube, 4_000, date, settings, mapOf(youtube to 4_000L), 4_000))
    }
}
