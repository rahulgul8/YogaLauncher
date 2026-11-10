package `in`.tuwi.yogaphone.ui

internal enum class Step { WELCOME, HOME, APPS, USAGE, WEEK, BLOCKS, ALLOWANCE, NUDGES, LOOK, DONE }

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
    if (previousVersion == 2 && !replay) return if (additions.isEmpty()) emptyList() else additions + Step.DONE
    return listOf(Step.WELCOME, Step.HOME, Step.APPS, Step.USAGE) + additions +
        listOf(Step.ALLOWANCE, Step.NUDGES, Step.LOOK, Step.DONE)
}

internal fun onboardingAllowancePrefill(averageMinutes: Double, recordedDays: Int): Int {
    if (recordedDays < 3 || !averageMinutes.isFinite()) return 90
    return (kotlin.math.floor(averageMinutes * 0.8 / 15) * 15).coerceIn(30.0, 240.0).toInt()
}
