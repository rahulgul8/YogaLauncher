package `in`.tuwi.yogaphone.intro

import `in`.tuwi.yogaphone.YogaApplication
import `in`.tuwi.yogaphone.guard.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class IntroNotice(val text: String, val actionLabel: String? = null, val action: (suspend () -> Unit)? = null)

/** A single application-owned action coordinator; only the root UI hosts consent and confirmations. */
class IntroActions(private val app: YogaApplication) {
    private val mutex = Mutex()
    private val mutableDisclosure = MutableStateFlow<IntroId?>(null)
    val disclosure = mutableDisclosure.asStateFlow()
    private val mutableNotice = MutableStateFlow<IntroNotice?>(null)
    val notice = mutableNotice.asStateFlow()
    // Actions outlive the card that started them: a card leaves the screen as soon as its action lands.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Runs [block] in the application scope; a real error shows the failure notice, cancellation never does. */
    fun launch(block: suspend IntroActions.() -> Unit) {
        scope.launch {
            try { block() } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { failure() }
        }
    }
    fun closeDisclosure() { mutableDisclosure.value = null }
    fun clearNotice() { mutableNotice.value = null }
    fun failure() { mutableNotice.value = IntroNotice("Couldn't change that setting. Try again.") }
    fun settingsFailure() { mutableNotice.value = retryNotice("Couldn't open accessibility settings. Try again.") }
    private fun retryNotice(text: String) = IntroNotice(text, "Try again") {
        app.introStore.pendingAction.first()?.let { mutableDisclosure.value = it }
    }
    suspend fun agree(): Boolean {
        val id = mutableDisclosure.value ?: return false
        app.introStore.setPending(id)
        mutableDisclosure.value = null
        return true
    }
    suspend fun resume() = mutex.withLock {
        app.introSignals.refresh()
        val id = app.introStore.pendingAction.first() ?: return@withLock
        if (!YogaGuardService.isEnabled(app)) {
            // Say it once, not on every later home resume; "Try again" reopens the disclosure.
            app.introStore.setPending(null)
            mutableNotice.value = IntroNotice("Yoga guard isn't on yet", "Try again") { mutableDisclosure.value = id }
            return@withLock
        }
        val target = if (id == IntroId.SHORTS_BLOCK) GuardSurface.YOUTUBE else GuardSurface.INSTAGRAM
        val intro = Intro(id, IntroSurface.USAGE, "", "", impactMinutesPerWeek = 0, targetSurface = target)
        apply(intro)
        app.introStore.setPending(null)
    }
    suspend fun request(intro: Intro) = mutex.withLock {
        val signals = app.introSignals.read()
        if (intro.id !in signals.allowed) return@withLock
        val current = IntroEngine.candidates(intro.surface, signals).firstOrNull {
            it.id == intro.id && it.targetSurface == intro.targetSurface && it.targetMinutes == intro.targetMinutes
        } ?: return@withLock
        if (intro.targetSurface != null && !signals.guardEnabled) {
            if (intro.id == IntroId.SHORTS_BLOCK || intro.id == IntroId.REELS_BLOCK) mutableDisclosure.value = intro.id
            return@withLock
        }
        apply(current)
    }
    private suspend fun apply(intro: Intro) {
        val signals = app.introSignals.read()
        if (intro.id !in signals.allowed) return
        val undo: suspend () -> Unit
        val confirmation: String
        when (intro.id) {
            IntroId.SHORTS_BLOCK, IntroId.REELS_BLOCK, IntroId.BROWSER_SURFACES -> {
                if (!signals.guardEnabled) return
                val surface = intro.targetSurface ?: return
                var previous = SurfaceSettings()
                app.guardPrefs.update { p ->
                    previous = p.surface(surface)
                    p.copy(surfaces = p.surfaces + (surface to previous.copy(enabled = true, allowanceMinutes = 0)))
                }
                undo = { app.guardPrefs.update { p -> p.copy(surfaces = p.surfaces + (surface to previous)) } }
                confirmation = when (intro.id) {
                    IntroId.SHORTS_BLOCK -> "Shorts are off. Regular videos still work."
                    IntroId.REELS_BLOCK -> "Reels are off. Posts and messages still work."
                    else -> "Browser blocking is on."
                }
            }
            IntroId.DAILY_ALLOWANCE, IntroId.LOWER_ALLOWANCE -> {
                val minutes = intro.targetMinutes ?: return
                val previous = app.database.usageDao().rules().firstOrNull { it.id == "total" }
                app.usageRepository.saveRule(null, minutes)
                undo = { app.usageRepository.restoreTotalRule(previous); app.usageRepository.check() }
                confirmation = "Daily allowance set to ${introDuration(minutes)}"
                runCatching { app.usageRepository.check() }
            }
            IntroId.GREY_PAST_LIMIT -> {
                if (signals.allowanceMinutes == null) return
                val previous = app.quietRepository.preferences.first().greyOverLimit
                app.quietRepository.setGreyOverLimit(true)
                runCatching { app.quietRepository.reconcileGreyscale() }
                undo = { app.quietRepository.setGreyOverLimit(previous); app.quietRepository.reconcileGreyscale() }
                confirmation = "Grey past your limit is on."
            }
        }
        app.introStore.resetForSuggestion(intro.id)
        app.introStore.markShown(intro)
        app.introStore.acted(intro.id)
        app.introSignals.refresh()
        mutableNotice.value = IntroNotice(confirmation, "Undo") { undo(); app.introSignals.refresh() }
    }
}
