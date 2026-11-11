package `in`.tuwi.yogaphone.resist

/**
 * The shared rules for every way through a block or pause (docs/plans/bypass-with-resistance.md).
 * Saying no is always free; each way through on the same day costs more. Pure and unit-tested.
 */
object ResistanceLadder {
    /** Minutes a person may choose for one way through. */
    val PASS_MINUTES = listOf(5, 10, 15)
    /** Only this is offered after [CHEAP_PASSES] ways through today, or past the daily allowance. */
    val SHORT_PASS = listOf(5)
    const val CHEAP_PASSES = 3
    /** How long the way-through button is held. */
    const val HOLD_MILLIS = 1_500L

    const val SHORTS_BASE_SECONDS = 15
    const val SHORTS_CAP_SECONDS = 120
    const val PAUSE_CAP_SECONDS = 60

    /**
     * Seconds to wait before the next way through. [passesToday] counts the ways through already taken today
     * (not the times someone said no). Past the allowance the base doubles. The wait doubles per pass up to
     * [capSeconds], but never drops below the base.
     */
    fun waitSeconds(baseSeconds: Int, passesToday: Int, capSeconds: Int, overLimit: Boolean = false): Int {
        val base = baseSeconds.coerceAtLeast(1) * if (overLimit) 2 else 1
        val doubled = base.toLong() shl passesToday.coerceIn(0, 20)
        return doubled.coerceAtMost(maxOf(capSeconds, base).toLong()).toInt()
    }

    /** The how-long choices for the next way through. */
    fun choices(passesToday: Int, overLimit: Boolean = false): List<Int> =
        if (overLimit || passesToday >= CHEAP_PASSES) SHORT_PASS else PASS_MINUTES

    /** Last time's choice when it's still offered, otherwise the middle choice. */
    fun preselect(lastMinutes: Int?, choices: List<Int>): Int =
        lastMinutes?.takeIf { it in choices } ?: choices[choices.size / 2]

    /** "1st", "2nd", "3rd", "4th"… for the cost line ("3rd time today"). */
    fun ordinal(n: Int): String {
        val suffix = if (n % 100 in 11..13) "th" else when (n % 10) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }
        return "$n$suffix"
    }
}
