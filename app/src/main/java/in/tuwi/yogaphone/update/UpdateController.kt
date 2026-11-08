package `in`.tuwi.yogaphone.update

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import `in`.tuwi.yogaphone.BuildConfig
import `in`.tuwi.yogaphone.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray

sealed interface UpdateUi {
    data object Hidden : UpdateUi
    data class Ready(val versionCode: Int) : UpdateUi
    data class Downloading(val versionCode: Int) : UpdateUi
    data class Restart(val versionCode: Int) : UpdateUi
}

enum class UpdateCheckNote(val text: String) {
    UP_TO_DATE("You're up to date"),
    UPDATE_READY("Update ready"),
    FROM_PLAY("Updates come from Google Play"),
}

data class WhatsNewUi(val entry: WhatsNewEntry)

class UpdateController internal constructor(
    private val context: Context,
    private val manager: AppUpdateManager,
    private val store: UpdatePersistence,
    private val scope: CoroutineScope,
    private val nowMillis: () -> Long,
) {
    constructor(context: Context) : this(
        context.applicationContext,
        AppUpdateManagerFactory.create(context.applicationContext),
        UpdateStore(context.applicationContext),
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        System::currentTimeMillis,
    )

    private val mutableUi = MutableStateFlow<UpdateUi>(UpdateUi.Hidden)
    val ui: StateFlow<UpdateUi> = mutableUi.asStateFlow()
    private val mutableCheckNote = MutableStateFlow<UpdateCheckNote?>(null)
    val checkNote: StateFlow<UpdateCheckNote?> = mutableCheckNote.asStateFlow()
    private val mutableWhatsNew = MutableStateFlow<WhatsNewUi?>(null)
    val whatsNew: StateFlow<WhatsNewUi?> = mutableWhatsNew.asStateFlow()

    private var launcher: ActivityResultLauncher<IntentSenderRequest>? = null
    private var latestInfo: AppUpdateInfo? = null
    private var installStatus = UpdateStatus.NONE
    private var foreground = false
    private val installListener = InstallStateUpdatedListener { state ->
        installStatus = state.installStatus().toUpdateStatus()
        scope.launch {
            when (installStatus) {
                UpdateStatus.PENDING, UpdateStatus.DOWNLOADING -> {
                    val version = latestInfo?.availableVersionCode() ?: return@launch
                    mutableUi.value = UpdateUi.Downloading(version)
                }
                UpdateStatus.DOWNLOADED -> {
                    store.recordDownloaded(nowMillis())
                    val version = latestInfo?.availableVersionCode() ?: BuildConfig.VERSION_CODE
                    mutableUi.value = UpdateUi.Hidden
                    if (!foreground) completeUpdate()
                    else applyDismissal(UpdateUi.Restart(version), onlyAfterDay = true)
                }
                UpdateStatus.FAILED -> {
                    store.setDownloadPending(false)
                    mutableUi.value = UpdateUi.Hidden
                }
                UpdateStatus.INSTALLING -> mutableUi.value = UpdateUi.Hidden
                else -> Unit
            }
        }
    }

    init {
        runCatching { manager.registerListener(installListener) }
        scope.launch { initialiseWhatsNew() }
    }

    fun attachLauncher(value: ActivityResultLauncher<IntentSenderRequest>) {
        launcher = value
    }

    fun onHomeResume(activity: Activity) {
        foreground = true
        scope.launch {
            val saved = runCatching { store.read() }.getOrElse { return@launch hideForPlayFailure() }
            val shouldQuery = UpdatePolicy.shouldCheck(saved.lastCheckMillis, nowMillis()) ||
                saved.downloadPending || saved.downloadedAtMillis != null || ui.value is UpdateUi.Downloading
            if (shouldQuery) query(activity, forced = false)
        }
    }

    fun onHomeStop() {
        foreground = false
        if (installStatus == UpdateStatus.DOWNLOADED) completeUpdate()
    }

    fun forceCheck(activity: Activity) {
        mutableCheckNote.value = null
        query(activity, forced = true)
    }

    fun startFlexible(activity: Activity) {
        val info = latestInfo ?: return
        startFlow(activity, info, AppUpdateType.FLEXIBLE)
    }

    fun completeUpdate() {
        runCatching {
            manager.completeUpdate()
                .addOnSuccessListener { scope.launch { store.clearDownload() } }
                .addOnFailureListener { if (foreground) showRestartIfVisible() }
        }.onFailure { if (foreground) showRestartIfVisible() }
    }

    fun dismissUpdate(versionCode: Int) {
        scope.launch {
            runCatching { store.dismiss(versionCode, nowMillis()) }
            mutableUi.value = UpdateUi.Hidden
        }
    }

    fun onUpdateFlowResult(resultCode: Int) {
        if (resultCode == Activity.RESULT_OK) return
        scope.launch {
            store.setDownloadPending(false)
            val info = latestInfo
            mutableUi.value = if (info != null && info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE)
                UpdateUi.Ready(info.availableVersionCode()) else UpdateUi.Hidden
        }
    }

    fun markWhatsNewSeen() {
        scope.launch {
            runCatching { store.setLastRunVersion(BuildConfig.VERSION_CODE) }
            mutableWhatsNew.value = null
        }
    }

    private fun query(activity: Activity, forced: Boolean) {
        val checkedAt = nowMillis()
        runCatching {
            manager.appUpdateInfo
                .addOnSuccessListener { info ->
                    scope.launch {
                        if (forced || UpdatePolicy.shouldCheck(store.read().lastCheckMillis, checkedAt)) {
                            store.recordCheck(checkedAt)
                        }
                        handleInfo(activity, info, forced)
                    }
                }
                .addOnFailureListener { hideForPlayFailure(forced) }
        }.onFailure { hideForPlayFailure(forced) }
    }

    private suspend fun handleInfo(activity: Activity, info: AppUpdateInfo, forced: Boolean) {
        latestInfo = info
        installStatus = info.installStatus().toUpdateStatus()
        val saved = store.read()
        if (installStatus == UpdateStatus.DOWNLOADED && saved.downloadedAtMillis == null) {
            store.recordDownloaded(nowMillis())
        } else if (installStatus !in setOf(UpdateStatus.PENDING, UpdateStatus.DOWNLOADING, UpdateStatus.DOWNLOADED) &&
            (saved.downloadPending || saved.downloadedAtMillis != null)) {
            store.clearDownload()
        }
        val available = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
        val immediateInProgress = info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
        val usable = when {
            immediateInProgress -> info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            !available -> false
            info.updatePriority() >= 5 -> info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            else -> info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
        }
        if (forced) mutableCheckNote.value = when {
            available && usable -> UpdateCheckNote.UPDATE_READY
            info.updateAvailability() == UpdateAvailability.UPDATE_NOT_AVAILABLE -> UpdateCheckNote.UP_TO_DATE
            else -> UpdateCheckNote.FROM_PLAY
        }
        val downloadedAt = store.read().downloadedAtMillis
        val action = UpdatePolicy.action(UpdateFacts(
            available = available && usable,
            inProgress = immediateInProgress && usable,
            priority = info.updatePriority(),
            stalenessDays = info.clientVersionStalenessDays(),
            status = installStatus,
            downloadedAtMillis = downloadedAt,
            lastCheckMillis = saved.lastCheckMillis,
            nowMillis = nowMillis(),
            foreground = foreground,
        ))
        val version = info.availableVersionCode()
        when (action) {
            UpdateAction.None -> mutableUi.value = UpdateUi.Hidden
            UpdateAction.ShowUpdateLine -> applyDismissal(UpdateUi.Ready(version))
            UpdateAction.ShowDownloadingLine -> applyDismissal(UpdateUi.Downloading(version))
            UpdateAction.StartFlexibleDialog -> if (!forced) startFlow(activity, info, AppUpdateType.FLEXIBLE)
            UpdateAction.StartImmediate -> if (!forced) startFlow(activity, info, AppUpdateType.IMMEDIATE)
            UpdateAction.CompleteNow -> completeUpdate()
            UpdateAction.ShowRestartLine -> applyDismissal(UpdateUi.Restart(version))
        }
    }

    private fun startFlow(activity: Activity, info: AppUpdateInfo, type: Int) {
        val target = launcher ?: return
        runCatching {
            if (type == AppUpdateType.FLEXIBLE) scope.launch {
                store.setDownloadPending(true)
                mutableUi.value = UpdateUi.Downloading(info.availableVersionCode())
            }
            manager.startUpdateFlowForResult(info, target, AppUpdateOptions.newBuilder(type).build())
        }.onFailure {
            scope.launch { store.setDownloadPending(false) }
            hideForPlayFailure()
        }
    }

    private suspend fun applyDismissal(value: UpdateUi, onlyAfterDay: Boolean = false) {
        val version = when (value) {
            is UpdateUi.Ready -> value.versionCode
            is UpdateUi.Downloading -> value.versionCode
            is UpdateUi.Restart -> value.versionCode
            UpdateUi.Hidden -> return
        }
        if (onlyAfterDay) {
            val downloadedAt = store.read().downloadedAtMillis
            if (downloadedAt == null || nowMillis() - downloadedAt < UpdatePolicy.DAY_MILLIS) return
        }
        mutableUi.value = if (store.read().isDismissed(version, nowMillis())) UpdateUi.Hidden else value
    }

    private fun showRestartIfVisible() {
        val version = latestInfo?.availableVersionCode() ?: BuildConfig.VERSION_CODE
        scope.launch { applyDismissal(UpdateUi.Restart(version)) }
    }

    private fun hideForPlayFailure(forced: Boolean = false) {
        mutableUi.value = UpdateUi.Hidden
        if (forced) mutableCheckNote.value = UpdateCheckNote.FROM_PLAY
    }

    private suspend fun initialiseWhatsNew() {
        runCatching {
            val entries = parseWhatsNew(context.resources.openRawResource(R.raw.whats_new).bufferedReader().use { it.readText() })
            // The first version with this code has nothing stored yet: an update (not a fresh install) still counts.
            val lastRun = store.read().lastRunVersionCode ?: runCatching {
                val info = context.packageManager.getPackageInfo(context.packageName, 0)
                if (info.lastUpdateTime > info.firstInstallTime) BuildConfig.VERSION_CODE - 1 else null
            }.getOrNull()
            when (val action = WhatsNew.action(lastRun, BuildConfig.VERSION_CODE, entries)) {
                is WhatsNewAction.None -> action.versionCodeToStore?.let { store.setLastRunVersion(it) }
                is WhatsNewAction.Show -> mutableWhatsNew.value = WhatsNewUi(action.entry)
            }
        }
    }
}

internal fun parseWhatsNew(json: String): List<WhatsNewEntry> {
    val array = JSONArray(json)
    return buildList {
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val linesJson = item.getJSONArray("lines")
            val lines = buildList { for (line in 0 until minOf(3, linesJson.length())) add(linesJson.getString(line)) }
            add(WhatsNewEntry(item.getInt("versionCode"), item.getString("versionName"), lines))
        }
    }
}

private fun Int.toUpdateStatus(): UpdateStatus = when (this) {
    InstallStatus.PENDING -> UpdateStatus.PENDING
    InstallStatus.DOWNLOADING -> UpdateStatus.DOWNLOADING
    InstallStatus.DOWNLOADED -> UpdateStatus.DOWNLOADED
    InstallStatus.INSTALLING -> UpdateStatus.INSTALLING
    InstallStatus.FAILED, InstallStatus.CANCELED -> UpdateStatus.FAILED
    else -> UpdateStatus.NONE
}
