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
        val entry = entries.firstOrNull { it.versionCode == currentVersionCode }
            ?: return WhatsNewAction.None(currentVersionCode)
        return WhatsNewAction.Show(entry)
    }
}
