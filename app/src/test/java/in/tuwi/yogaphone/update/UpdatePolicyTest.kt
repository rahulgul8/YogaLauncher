package `in`.tuwi.yogaphone.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePolicyTest {
    private val day = UpdatePolicy.DAY_MILLIS
    private fun facts(
        available: Boolean = false,
        inProgress: Boolean = false,
        priority: Int = 2,
        status: UpdateStatus = UpdateStatus.NONE,
        downloadedAt: Long? = null,
        now: Long = day,
        foreground: Boolean = true,
    ) = UpdateFacts(available, inProgress, priority, null, status, downloadedAt, null, now, foreground)

    @Test fun checkHasExactTwentyFourHourBoundary() {
        assertTrue(UpdatePolicy.shouldCheck(null, day))
        assertFalse(UpdatePolicy.shouldCheck(1, day))
        assertFalse(UpdatePolicy.shouldCheck(day, day - 1))
        assertTrue(UpdatePolicy.shouldCheck(1, day + 1))
    }

    @Test fun normalPrioritiesShowQuietLine() {
        (0..3).forEach { assertEquals(UpdateAction.ShowUpdateLine, UpdatePolicy.action(facts(true, priority = it))) }
    }

    @Test fun pendingAndDownloadingShowDownloadingLine() {
        assertEquals(UpdateAction.ShowDownloadingLine, UpdatePolicy.action(facts(status = UpdateStatus.PENDING)))
        assertEquals(UpdateAction.ShowDownloadingLine, UpdatePolicy.action(facts(status = UpdateStatus.DOWNLOADING)))
    }

    @Test fun highPrioritiesStartTheirFlows() {
        assertEquals(UpdateAction.StartFlexibleDialog, UpdatePolicy.action(facts(true, priority = 4)))
        assertEquals(UpdateAction.StartImmediate, UpdatePolicy.action(facts(true, priority = 5)))
        assertEquals(UpdateAction.StartImmediate, UpdatePolicy.action(facts(inProgress = true)))
    }

    @Test fun downloadedCompletesOnlyInBackground() {
        assertEquals(UpdateAction.CompleteNow, UpdatePolicy.action(facts(status = UpdateStatus.DOWNLOADED, foreground = false)))
        assertEquals(UpdateAction.None, UpdatePolicy.action(facts(status = UpdateStatus.DOWNLOADED, downloadedAt = 1, now = day)))
    }

    @Test fun restartLineHasExactTwentyFourHourBoundary() {
        assertEquals(UpdateAction.None, UpdatePolicy.action(facts(status = UpdateStatus.DOWNLOADED, downloadedAt = 1, now = day)))
        assertEquals(UpdateAction.ShowRestartLine,
            UpdatePolicy.action(facts(status = UpdateStatus.DOWNLOADED, downloadedAt = 1, now = day + 1)))
    }

    @Test fun unusableAndTerminalStatesShowNothing() {
        assertEquals(UpdateAction.None, UpdatePolicy.action(facts()))
        assertEquals(UpdateAction.None, UpdatePolicy.action(facts(true, status = UpdateStatus.INSTALLING)))
        assertEquals(UpdateAction.None, UpdatePolicy.action(facts(true, status = UpdateStatus.FAILED)))
    }
}
