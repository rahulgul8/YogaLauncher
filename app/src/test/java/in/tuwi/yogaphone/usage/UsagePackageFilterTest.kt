package `in`.tuwi.yogaphone.usage

import org.junit.Assert.assertEquals
import org.junit.Test

class UsagePackageFilterTest {
    @Test fun yogaAndHomePackagesAreExcludedFromUsage() {
        val values = linkedMapOf("in.tuwi.yogaphone" to 2L, "com.motorola.launcher3" to 8L, "com.example.reader" to 12L)
        assertEquals(mapOf("com.example.reader" to 12L),
            excludeUsagePackages(values, setOf("in.tuwi.yogaphone", "com.motorola.launcher3")))
    }
}
