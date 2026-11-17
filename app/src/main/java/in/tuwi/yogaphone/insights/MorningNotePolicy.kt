package `in`.tuwi.yogaphone.insights

object MorningNotePolicy {
    /** The note can appear from this hour (local time) on the first home view of the day. */
    const val START_HOUR = 4
    /** After this hour the note is no longer news and stays hidden. */
    const val END_HOUR = 14

    /**
     * True when the home screen should show the note about yesterday.
     * [closedOn] is the local date on which the user opened or dismissed the note (null if never).
     */
    fun shouldShow(now: java.time.LocalDateTime, closedOn: java.time.LocalDate?, hasContent: Boolean): Boolean {
        if (!hasContent) return false
        if (now.hour < START_HOUR || now.hour >= END_HOUR) return false
        if (closedOn == now.toLocalDate()) return false
        return true
    }
}
