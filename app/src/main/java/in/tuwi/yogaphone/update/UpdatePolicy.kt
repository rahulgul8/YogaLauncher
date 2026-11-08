package `in`.tuwi.yogaphone.update

enum class UpdateStatus { NONE, PENDING, DOWNLOADING, DOWNLOADED, INSTALLING, FAILED }

data class UpdateFacts(
    val available: Boolean,
    val inProgress: Boolean,
    val priority: Int,
    val stalenessDays: Int?,
    val status: UpdateStatus,
    val downloadedAtMillis: Long?,
    val lastCheckMillis: Long?,
    val nowMillis: Long,
    val foreground: Boolean,
)

sealed interface UpdateAction {
    data object None : UpdateAction
    data object ShowUpdateLine : UpdateAction
    data object ShowDownloadingLine : UpdateAction
    data object StartFlexibleDialog : UpdateAction
    data object StartImmediate : UpdateAction
    data object CompleteNow : UpdateAction
    data object ShowRestartLine : UpdateAction
}

object UpdatePolicy {
    const val DAY_MILLIS = 24L * 60L * 60L * 1_000L

    fun shouldCheck(lastCheckMillis: Long?, nowMillis: Long): Boolean =
        lastCheckMillis == null || nowMillis - lastCheckMillis >= DAY_MILLIS

    fun action(facts: UpdateFacts): UpdateAction = when {
        facts.status == UpdateStatus.DOWNLOADED && !facts.foreground -> UpdateAction.CompleteNow
        facts.status == UpdateStatus.DOWNLOADED && facts.downloadedAtMillis != null &&
            facts.nowMillis - facts.downloadedAtMillis >= DAY_MILLIS -> UpdateAction.ShowRestartLine
        facts.status == UpdateStatus.DOWNLOADED -> UpdateAction.None
        facts.inProgress -> UpdateAction.StartImmediate
        facts.status == UpdateStatus.PENDING || facts.status == UpdateStatus.DOWNLOADING ->
            UpdateAction.ShowDownloadingLine
        !facts.available || facts.status == UpdateStatus.INSTALLING || facts.status == UpdateStatus.FAILED ->
            UpdateAction.None
        facts.priority >= 5 -> UpdateAction.StartImmediate
        facts.priority == 4 -> UpdateAction.StartFlexibleDialog
        else -> UpdateAction.ShowUpdateLine
    }
}
