package `in`.tuwi.yogaphone.guard

import org.junit.Assert.*
import org.junit.Test

class ShortsGateTest {
    private val surface = GuardSurface.YOUTUBE
    private val block = mapOf(surface to SurfaceSettings(enabled = true))
    @Test fun blocksDebouncesAndEscalatesAfterTwoBacks() {
        val gate = ShortsGate()
        assertEquals(GateAction.BACK, gate.evaluate(surface, 0, "today", block))
        assertEquals(GateAction.NONE, gate.evaluate(surface, 500, "today", block))
        assertEquals(GateAction.BACK, gate.evaluate(surface, 1000, "today", block))
        assertEquals(GateAction.HOME, gate.evaluate(surface, 2000, "today", block))
        // One attempt counts once, however many steps it took.
        assertEquals(1, gate.daily.blocks[surface])
    }
    @Test fun aNewAttemptAfterLeavingCountsAgain() {
        val gate = ShortsGate()
        gate.evaluate(surface, 0, "today", block)
        gate.evaluate(null, 1500, "today", block)
        assertEquals(GateAction.BACK, gate.evaluate(surface, 5000, "today", block))
        assertEquals(2, gate.daily.blocks[surface])
    }
    @Test fun backsOutsideEscalationWindowDoNotCauseHome() {
        val gate = ShortsGate()
        gate.evaluate(surface, 0, "today", block)
        gate.evaluate(surface, 1000, "today", block)
        assertEquals(GateAction.BACK, gate.evaluate(surface, 4001, "today", block))
    }
    @Test fun allowanceRunsOutAndPersistsAcrossVisits() {
        val settings = mapOf(surface to SurfaceSettings(enabled = true, allowanceMinutes = 5))
        val gate = ShortsGate()
        assertEquals(GateAction.NONE, gate.evaluate(surface, 0, "today", settings))
        assertEquals(GateAction.NONE, gate.evaluate(null, 120_000, "today", settings))
        assertEquals(120_000L, gate.daily.usedMillis[surface])
        assertEquals(GateAction.NONE, gate.evaluate(surface, 200_000, "today", settings))
        assertEquals(GateAction.BACK, gate.evaluate(surface, 380_000, "today", settings))
        assertEquals(300_000L, gate.daily.usedMillis[surface])
        val restored = ShortsGate(gate.daily)
        assertEquals(GateAction.BACK, restored.evaluate(surface, 400_000, "today", settings))
    }
    @Test fun dayRolloverResetsCountsAllowanceAndEscalation() {
        val gate = ShortsGate(GuardDaily("yesterday", mapOf(surface to 7), mapOf(surface to 300_000L)))
        val settings = mapOf(surface to SurfaceSettings(enabled = true, allowanceMinutes = 5))
        assertEquals(GateAction.NONE, gate.evaluate(surface, 0, "today", settings))
        assertEquals(0, gate.daily.blocks[surface] ?: 0)
        assertEquals(0L, gate.daily.usedMillis[surface] ?: 0L)
        gate.evaluate(surface, 300_000, "today", settings)
        assertEquals(GateAction.NONE, gate.evaluate(surface, 301_000, "tomorrow", settings))
        assertEquals(0L, gate.daily.usedMillis[surface] ?: 0L)
    }
    @Test fun disabledAndOrdinaryScreensDoNothingAndReturningStartsNewAttempt() {
        val gate = ShortsGate()
        assertEquals(GateAction.NONE, gate.evaluate(surface, 0, "today", emptyMap()))
        assertEquals(GateAction.BACK, gate.evaluate(surface, 1, "today", block))
        assertEquals(GateAction.NONE, gate.evaluate(null, 2, "today", block))
        assertEquals(GateAction.BACK, gate.evaluate(surface, 3, "today", block))
    }
}
