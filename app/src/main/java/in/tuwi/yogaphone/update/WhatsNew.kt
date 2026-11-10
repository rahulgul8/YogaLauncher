package `in`.tuwi.yogaphone.update

data class WhatsNewEntry(val versionCode: Int, val versionName: String, val lines: List<String>)

sealed interface WhatsNewAction {
    data class None(val versionCodeToStore: Int? = null) : WhatsNewAction
    data class Show(val entry: WhatsNewEntry) : WhatsNewAction
}

object WhatsNew {
    fun action(
        lastRunVersionCode: Int?,
        currentVersionCode: Int,
        entries: List<WhatsNewEntry>,
    ): WhatsNewAction {
        if (lastRunVersionCode == null) return WhatsNewAction.None(currentVersionCode)
        if (currentVersionCode <= lastRunVersionCode) return WhatsNewAction.None()
        // A fix release has no entry of its own: show the newest entry this person hasn't seen yet, if any.
        val entry = entries.filter { it.versionCode in (lastRunVersionCode + 1)..currentVersionCode }
            .maxByOrNull { it.versionCode } ?: return WhatsNewAction.None(currentVersionCode)
        return WhatsNewAction.Show(entry)
    }
}
