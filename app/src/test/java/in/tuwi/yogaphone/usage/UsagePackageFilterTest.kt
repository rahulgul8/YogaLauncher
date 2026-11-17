package `in`.tuwi.yogaphone.usage

import org.junit.Assert.assertEquals
import org.junit.Test

class UsagePackageFilterTest {
    @Test fun yogaAndHomePackagesAreExcludedFromUsage() {
        val values = linkedMapOf("in.tuwi.yogaphone" to 2L, "com.motorola.launcher3" to 8L,
            "com.android.systemui" to 5L, "android" to 3L, "com.example.reader" to 12L)
        assertEquals(mapOf("com.example.reader" to 12L),
            excludeUsagePackages(values, setOf("in.tuwi.yogaphone", "com.motorola.launcher3") + NON_USER_PACKAGES))
    }

    @Test fun notificationShadeSystemUiIsNotAnAppButDoesNotEndAStillResumedApp() {
        val resume = ForegroundEvent.Type.RESUMED
        val pause = ForegroundEvent.Type.PAUSED
        val totals = UsageCalculator("yoga").calculate(listOf(
            ForegroundEvent("reader", 10, resume),
            ForegroundEvent("com.android.systemui", 30, resume),
            ForegroundEvent("com.android.systemui", 50, pause),
            ForegroundEvent("reader", 70, pause),
        ), 0, 100)
        assertEquals(mapOf("reader" to 60L), excludeUsagePackages(totals, NON_USER_PACKAGES))
    }
}
