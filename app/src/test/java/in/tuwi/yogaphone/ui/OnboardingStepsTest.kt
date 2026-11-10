package `in`.tuwi.yogaphone.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class OnboardingStepsTest {
    private val full = listOf(Step.WELCOME, Step.HOME, Step.APPS, Step.USAGE, Step.WEEK,
        Step.BLOCKS, Step.ALLOWANCE, Step.NUDGES, Step.LOOK, Step.DONE)

    @Test fun newUserWithAccess() {
        assertEquals(full, onboardingSteps(0, true, true, true, true, false))
    }
    @Test fun newUserWithoutAccess() {
        assertEquals(full - Step.WEEK, onboardingSteps(0, false, true, true, false, false))
    }
    @Test fun guardUnavailable() {
        assertEquals(full - Step.BLOCKS, onboardingSteps(0, true, false, true, true, false))
    }
    @Test fun neitherAppInstalled() {
        assertFalse(Step.BLOCKS in onboardingSteps(0, true, true, false, false, false))
    }
    @Test fun instagramAloneOffersBlocks() {
        assertEquals(full, onboardingSteps(0, true, true, false, true, false))
    }
    @Test fun versionTwoOnlyNewSteps() {
        assertEquals(listOf(Step.WEEK, Step.BLOCKS, Step.DONE), onboardingSteps(2, true, true, true, true, false))
        assertEquals(listOf(Step.WEEK, Step.DONE), onboardingSteps(2, true, false, true, true, false))
        assertEquals(listOf(Step.BLOCKS, Step.DONE), onboardingSteps(2, false, true, false, true, false))
    }
    @Test fun versionTwoNothingNewFinishes() {
        assertEquals(emptyList<Step>(), onboardingSteps(2, false, false, true, true, false))
        assertEquals(emptyList<Step>(), onboardingSteps(2, false, true, false, false, false))
    }
    @Test fun replayRestoresFullWalkthrough() {
        assertEquals(full, onboardingSteps(2, true, true, true, true, true))
        assertEquals(full - Step.WEEK - Step.BLOCKS, onboardingSteps(2, false, false, false, false, true))
    }
    @Test fun allowanceRoundsDownToQuarterHour() {
        assertEquals(90, onboardingAllowancePrefill(120.0, 3))
        assertEquals(105, onboardingAllowancePrefill(140.0, 7))
        assertEquals(120, onboardingAllowancePrefill(150.0, 3))
    }
    @Test fun allowanceClampsBothEnds() {
        assertEquals(30, onboardingAllowancePrefill(10.0, 3))
        assertEquals(240, onboardingAllowancePrefill(1000.0, 7))
    }
    @Test fun insufficientDaysKeepsDefault() {
        assertEquals(90, onboardingAllowancePrefill(300.0, 0))
        assertEquals(90, onboardingAllowancePrefill(300.0, 2))
    }
}
