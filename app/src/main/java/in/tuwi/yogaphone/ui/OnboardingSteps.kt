package `in`.tuwi.yogaphone.ui

internal enum class Step { WELCOME, HOME, APPS, USAGE, WEEK, BLOCKS, ALLOWANCE, VALUE, DISPLAY, NUDGES, LOOK, DONE }

internal fun onboardingSteps(
    previousVersion: Int,
    usageGranted: Boolean,
    guardAvailable: Boolean,
    youtube: Boolean,
    instagram: Boolean,
    replay: Boolean,
): List<Step> {
    val additions = buildList {
        if (usageGranted) add(Step.WEEK)
        if (guardAvailable && (youtube || instagram)) add(Step.BLOCKS)
    }
    // The home display choice is new in v3 too, so people who finished v2 always get a short run.
    if (previousVersion == 2 && !replay) return additions + Step.DISPLAY + Step.DONE
    return listOf(Step.WELCOME, Step.HOME, Step.APPS, Step.USAGE) + additions +
        listOf(Step.ALLOWANCE, Step.VALUE, Step.DISPLAY, Step.NUDGES, Step.LOOK, Step.DONE)
}

internal fun onboardingAllowancePrefill(averageMinutes: Double, recordedDays: Int): Int {
    if (recordedDays < 3 || !averageMinutes.isFinite()) return 90
    return (kotlin.math.floor(averageMinutes * 0.8 / 15) * 15).coerceIn(30.0, 240.0).toInt()
}
