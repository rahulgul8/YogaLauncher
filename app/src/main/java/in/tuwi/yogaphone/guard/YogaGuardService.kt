package `in`.tuwi.yogaphone.guard

import `in`.tuwi.yogaphone.pro.*
import android.accessibilityservice.AccessibilityService
import android.animation.ValueAnimator
import android.app.KeyguardManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.Settings
import android.telecom.TelecomManager
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.compose.ui.graphics.toArgb
import `in`.tuwi.yogaphone.YogaApplication
import `in`.tuwi.yogaphone.data.Prefs
import `in`.tuwi.yogaphone.profiles.Categories
import `in`.tuwi.yogaphone.ui.theme.Tokens
import `in`.tuwi.yogaphone.ui.theme.specOf
import `in`.tuwi.yogaphone.usage.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.Currency
import kotlin.math.abs

/** Screen structure is inspected transiently, only for opted-in packages. Never logged or retained. */
class YogaGuardService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val requests = Channel<Unit>(Channel.CONFLATED)
    private val yoga get() = application as YogaApplication
    private val windows get() = getSystemService(WindowManager::class.java)
    private var preferences = GuardPreferences()
    private var foreground = ""
    private var foregroundActivity: String? = null
    private var obstructed = false
    private var keyboards = emptySet<String>()
    private var homePackages = emptySet<String>()
    private var cameraPackages = emptySet<String>()
    private fun refreshPackageRoles() {
        keyboards = getSystemService(InputMethodManager::class.java).inputMethodList.map { it.packageName }.toSet()
        homePackages = packagesFor(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
        cameraPackages = packagesFor(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)) + packagesFor(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
    }
    private var foregroundSince = 0L
    private var liveSuspended = false
    private var generation = 0
    private var inspect = false
    private var lastInspection = -1_000L
    private var lastRefresh = -60_000L
    private var snapshot: UsageSnapshot? = null
    private var baselineSince = 0L
    private var detected: GuardSurface? = null
    private var watchInfo = WatchInfo()
    private var learningPreferences = LearningPreferences()
    private var learningPro = false
    private var learningNow = false
    private var learningTickAt: Long? = null
    private var focusGate = LearningFocusGate()
    private var gate = ShortsGate()
    private var ticker: Job? = null
    private var pill: TextView? = null
    private var passPill = false
    private var passPillUpdatedAt = 0L
    private var passPillSurface: GuardSurface? = null
    private var card: TextView? = null
    private var cardRemoval: Job? = null
    private var flame: ValueAnimator? = null
    private var collapsed = false
    private var pillAction: (() -> Unit)? = null
    private var pillY = 80
    private var registered = false
    private var overlayTokens: Tokens = specOf(`in`.tuwi.yogaphone.data.Palette.OBSIDIAN_BRASS).dark
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!serviceActive()) return
            // Never bridge a screen-off/locked interval with the local live-seconds clock.
            foregroundSince = SystemClock.elapsedRealtime()
            liveSuspended = true
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                generation++; detected = null; watchInfo = WatchInfo(); learningNow = false; learningTickAt = null; hidePill(); hideCard()
                gate.evaluate(null, SystemClock.elapsedRealtime(), today(), preferences.surfaces)
            }
            if (serviceActive()) requests.trySend(Unit)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val filter = IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_USER_PRESENT) }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(screenReceiver, filter, RECEIVER_NOT_EXPORTED)
        else @Suppress("DEPRECATION") registerReceiver(screenReceiver, filter)
        registered = true
        scope.launch {
            learningPro = ProGate.allows(ProFeature.LEARNING_VIDEOS, yoga.proRepository.current())
            launch { yoga.proRepository.state.collect {
                learningPro = ProGate.allows(ProFeature.LEARNING_VIDEOS, it)
                if (!ProGate.allows(ProFeature.VALUE_OF_TIME, it) && !learningOn() && !passPill) hidePill()
                restartTicker()
            } }
            launch { yoga.learningPrefs.preferences.collect {
                val wasOn = learningOn()
                learningPreferences = it
                if (learningOn() != wasOn) restartTicker()
                inspect = true; requests.trySend(Unit)
            } }
            gate = ShortsGate(yoga.guardPrefs.daily.first())
            savedDaily = canonical(gate.daily)
            launch {
                yoga.guardPrefs.daily.distinctUntilChanged().collect { stored ->
                    // A delete/reset/import is authoritative, including while the service is connected.
                    if (canonical(stored) != savedDaily) { gate = ShortsGate(stored); savedDaily = canonical(stored); detected = null; inspect = true }
                }
            }
            launch {
                kotlinx.coroutines.flow.combine(Prefs(this@YogaGuardService).userPreferences, yoga.proRepository.state) { preferences, pro ->
                    if (ProGate.allows(ProFeature.LOOKS, pro)) preferences.palette else `in`.tuwi.yogaphone.data.Palette.OBSIDIAN_BRASS
                }.distinctUntilChanged().collect { palette ->
                    overlayTokens = specOf(palette).dark
                    pill?.let(::styleOverlay)
                    card?.let(::styleOverlay)
                }
            }
            launch {
                yoga.guardPrefs.preferences.collect { p ->
                    generation++
                    preferences = p; pillY = p.positionY
                    ticker?.cancel()
                    if (serviceActive()) {
                        refreshPackageRoles()
                        inspect = true
                        ticker = launch { while (isActive) { requests.trySend(Unit); delay(1_000) } }
                    } else {
                        detected = null; hidePill(); hideCard()
                        gate.evaluate(null, SystemClock.elapsedRealtime(), today(), p.surfaces)
                    }
                }
            }
            for (ignored in requests) {
                if (serviceActive()) runCatching { update() }.onFailure { hidePill(); hideCard() }
            }
        }
    }

    // Learning videos is opt-in: it acts only for Pro users who have added at least one channel.
    private fun learningOn() = learningPro && learningPreferences.channels.isNotEmpty()
    private fun serviceActive() = preferences.active || learningOn()
    private fun restartTicker() {
        ticker?.cancel()
        if (serviceActive()) {
            refreshPackageRoles()
            inspect = true
            ticker = scope.launch { while (isActive) { requests.trySend(Unit); delay(1_000) } }
        } else { hidePill(); hideCard() }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        val activityClass = event.className?.toString()?.takeUnless {
            it.startsWith("android.widget.") || it.startsWith("android.view.")
        }
        if (!serviceActive()) {
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                if (pkg != foreground) foregroundActivity = null
                if (activityClass != null) foregroundActivity = activityClass
                foreground = pkg; foregroundSince = SystemClock.elapsedRealtime(); generation++; obstructed = false
            }
            return
        }
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            // Own accessibility overlays must not replace the app underneath them.
            if (pkg == packageName && (event.className?.toString().orEmpty().startsWith("android.widget.") ||
                event.className?.toString().orEmpty().startsWith("android.view."))) return
            if (pkg != foreground) refreshPackageRoles()
            if (pkg == "com.android.systemui" || pkg in keyboards) {
                obstructed = true; generation++; foregroundSince = SystemClock.elapsedRealtime(); liveSuspended = true; detected = null; hidePill(); hideCard()
                if (serviceActive()) requests.trySend(Unit)
                return
            }
            obstructed = false
            if (pkg != foreground) {
                foregroundActivity = null
                foreground = pkg; foregroundSince = SystemClock.elapsedRealtime(); generation++
                detected = null; hidePill()
            }
            if (activityClass != null) foregroundActivity = activityClass
            if (serviceActive()) { inspect = true; requests.trySend(Unit) }
        } else if (serviceActive() && event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            pkg == foreground) {
            // Some keyboards close without another activity/window-state event.
            obstructed = false
            inspect = true; requests.trySend(Unit)
        }
    }

    private suspend fun update() {
        val now = SystemClock.elapsedRealtime()
        // Some launchers (e.g. Motorola) send no window-state event when Home is pressed:
        // confirm the app in the active window on every tick.
        val active = rootInActiveWindow?.let { root -> root.packageName?.toString().also { recycle(root) } }
        if (active != null && active != foreground && active != "com.android.systemui" && active !in keyboards) {
            if (active !in homePackages) refreshPackageRoles()
            foregroundActivity = null
            foreground = active; foregroundSince = now; generation++; obstructed = false
            detected = null; hidePill(); inspect = true
        }
        val token = generation
        val pkg = foreground
        val enabled = preferences.surfaces.filterValues { it.enabled }.keys
        val screenOn = getSystemService(PowerManager::class.java).isInteractive
        val locked = getSystemService(KeyguardManager::class.java).isKeyguardLocked
        val inCall = getSystemService(AudioManager::class.java).mode.let { it == AudioManager.MODE_IN_CALL || it == AudioManager.MODE_IN_COMMUNICATION }
        if (!screenOn || locked || inCall || obstructed) {
            foregroundSince = now
            liveSuspended = true
            detected = null; watchInfo = WatchInfo(); learningNow = false; learningTickAt = null; hidePill(); hideCard()
            gate.evaluate(null, now, today(), preferences.surfaces)
            saveDailyIfChanged()
            return
        }
        if (liveSuspended) { foregroundSince = now; liveSuspended = false }
        val entry = yoga.database.appEntryDao().get(pkg)
        val classification = AppClassifier(this).classify(pkg, entry)
        val category = yoga.organizeRepository.categoryOf(pkg, "", entry)
        val sensitive = classification.essential || category == Categories.FINANCE || pkg in cameraPackages ||
            pkg == getSystemService(TelecomManager::class.java).defaultDialerPackage || pkg.contains("incall", true)
        if (token != generation) return
        if (sensitive) { hidePill(); hideCard(); detected = null; watchInfo = WatchInfo() }
        // Keep the trailing content change, but never act on an obsolete detection while waiting.
        if (inspect && now - lastInspection < 500) return
        if (inspect && now - lastInspection >= 500) {
            inspect = false
            lastInspection = now
            val detectionSurfaces = enabled + if (learningOn()) setOf(GuardSurface.YOUTUBE) else emptySet()
            detected = if (!sensitive && pkg in ShortsDetector.supportedPackages(detectionSurfaces)) {
                ShortsDetector.detectByClass(pkg, foregroundActivity, detectionSurfaces) ?: rootInActiveWindow?.let { root ->
                    if (root.packageName?.toString() == pkg) ShortsDetector.detect(pkg, AndroidNode(root), detectionSurfaces)
                    else { recycle(root); null }
                }
            } else null
            watchInfo = if (!sensitive && learningOn() && pkg == LearningDetector.YOUTUBE_PACKAGE) {
                rootInActiveWindow?.let { root ->
                    if (root.packageName?.toString() == pkg) LearningDetector.read(pkg, AndroidNode(root))
                    else { recycle(root); WatchInfo() }
                } ?: WatchInfo()
            } else WatchInfo()
        }
        val learningEnabled = learningOn()
        val focusActive = learningEnabled && yoga.focusRepository.activeFocus.first() != null
        val channelKey = watchInfo.channel?.let(LearningChannels::normalize)?.takeIf(String::isNotEmpty)
        val learningKeys = learningPreferences.channels.map(LearningChannel::key).toSet()
        learningNow = learningEnabled && detected != GuardSurface.YOUTUBE && LearningPolicy.isLearning(watchInfo, learningKeys)
        val focusDecision = if (pkg == LearningDetector.YOUTUBE_PACKAGE && learningEnabled) LearningFocusPolicy.decide(
            focusActive, detected == GuardSurface.YOUTUBE, watchInfo, channelKey, learningKeys,
            learningPreferences.allowYouTubeHomeDuringFocus) else YouTubeFocusAction.ALLOW
        val surfaceSettings = if (focusActive && detected == GuardSurface.YOUTUBE) preferences.surfaces +
            (GuardSurface.YOUTUBE to preferences.surface(GuardSurface.YOUTUBE).copy(enabled = true, allowanceMinutes = 0))
            else preferences.surfaces
        // Focus keeps Shorts blocked even during a pass.
        val action = gate.evaluate(detected, now, today(), surfaceSettings,
            if (focusActive) emptyMap() else preferences.passUntil, System.currentTimeMillis())
        val focusAction = focusGate.evaluate(focusDecision != YouTubeFocusAction.ALLOW, now)
        updateLearningClock(now, learningNow, today())
        saveDailyIfChanged()
        if (token != generation || !serviceActive()) return
        val effectiveAction = if (action != GateAction.NONE) action else focusAction
        if (effectiveAction != GateAction.NONE) {
            performGlobalAction(if (effectiveAction == GateAction.HOME) GLOBAL_ACTION_HOME else GLOBAL_ACTION_BACK)
            if (action != GateAction.NONE) detected?.let { showBlockCard(it, yoga.guardPrefs.consumeExpiredPass(it, System.currentTimeMillis())) }
            else showLearningBlockCard(watchInfo.channel)
            // Verify the destination before the next decision (including first-screen/deep-link fallback).
            inspect = true
            if (action == GateAction.NONE) { hidePill(); return }
        }
        val passSurface = detected
        val left = passSurface?.let { preferences.passExpiry(it) - System.currentTimeMillis() } ?: 0L
        if (passSurface != null && left > 0 && !sensitive && pkg != packageName && pkg !in homePackages) {
            val text = "${passSurface.shortName()} \u00b7 ${(left + 59_999) / 60_000} min left"
            if (!passPill || passPillSurface != passSurface || now - passPillUpdatedAt >= 30_000) {
                showPill(text, text, calm = true, isPass = true)
                passPillUpdatedAt = now
                passPillSurface = passSurface
            }
            return
        }
        if (passPill) hidePill()
        if (learningNow) {
            val seconds = yoga.learningPrefs.seconds(today(), LearningDetector.YOUTUBE_PACKAGE) + learningPendingMillis / 1_000
            showPill("Learning · ${seconds / 60} min", "Learning video, ${seconds / 60} minutes today.", calm = true)
            return
        }
        // "Count as learning": shown prominently when the title suggests study or work; otherwise only
        // when no live time-value pill would show, so it never hides the value pill.
        val offerChannel = watchInfo.channel?.takeIf { learningEnabled && watchInfo.isWatchPage }
        fun showOffer(prominent: Boolean) {
            val channel = offerChannel ?: return
            showPill(if (prominent) "Is this learning? · Count as learning" else "Count as learning · $channel",
                "Add $channel to learning channels.", calm = true) {
                scope.launch { yoga.learningPrefs.add(channel); showAddedCard(channel); inspect = true; requests.trySend(Unit) }
            }
        }
        fun idle() { if (offerChannel != null) showOffer(false) else hidePill() }
        if (offerChannel != null && learningPreferences.titleHints && LearningChannels.suggestsLearning(watchInfo.title)) {
            showOffer(true); return
        }
        if (!preferences.liveIndicator || !yoga.proRepository.allows(ProFeature.VALUE_OF_TIME)) { idle(); return }
        val usage = yoga.usagePrefs.preferences.first()
        if (!usage.timeValueEnabled || !yoga.insightsRepository.preferences.first().usageTracking ||
            yoga.usagePrefs.isSnoozed(today())) { idle(); return }
        val eligible = !classification.essential && (usage.counts == AllowanceCounts.ALL_EXCEPT_ESSENTIAL || classification.distracting)
        val allowed = OverlayPolicy.shouldShow(eligible, essential = classification.essential,
            dialer = pkg == getSystemService(TelecomManager::class.java).defaultDialerPackage || pkg.contains("incall", true),
            camera = pkg in cameraPackages, finance = category == Categories.FINANCE,
            inCall = inCall, locked = locked, screenOn = screenOn, ownApp = pkg == packageName,
            systemUi = pkg == "com.android.systemui" || pkg == "android", keyboard = pkg in keyboards, launcher = pkg in homePackages)
        if (!allowed) { idle(); return }
        if (now - lastRefresh >= 60_000) {
            lastRefresh = now
            snapshot = yoga.usageRepository.check()
            baselineSince = SystemClock.elapsedRealtime()
        }
        if (token != generation || !preferences.liveIndicator) return
        if (!yoga.usagePrefs.preferences.first().timeValueEnabled || !yoga.insightsRepository.preferences.first().usageTracking ||
            yoga.usagePrefs.isSnoozed(today())) { idle(); return }
        if (token != generation || !preferences.liveIndicator) return
        val data = snapshot?.takeIf { it.available && it.date.toString() == today() } ?: run { idle(); return }
        val seconds = ((SystemClock.elapsedRealtime() - maxOf(foregroundSince, baselineSince)) / 1_000).coerceAtLeast(0)
        val amount = BurnMeter.amount(preferences.startCounting, data.evaluation.overall,
            data.today.filter { it.eligible }.sumOf { it.foregroundMinutes }, seconds, usage.hourlyAmount())
            ?: run { idle(); return }
        // Recheck sensitive state after the suspending usage refresh.
        if (getSystemService(KeyguardManager::class.java).isKeyguardLocked || !getSystemService(PowerManager::class.java).isInteractive ||
            getSystemService(AudioManager::class.java).mode in setOf(AudioManager.MODE_IN_CALL, AudioManager.MODE_IN_COMMUNICATION)) {
            foregroundSince = SystemClock.elapsedRealtime(); liveSuspended = true; hidePill(); return
        }
        val formatted = BurnMeter.format(amount, usage.hourlyAmount(), usage.currency)
        val currencyName = if (usage.currency == "INR") "rupees" else runCatching { Currency.getInstance(usage.currency).displayName }.getOrDefault(usage.currency)
        showPill(if (preferences.style == GuardStyle.BURNING) "🔥 $formatted burning" else "$formatted of time past your limit",
            "Time value past your limit, ${amount.setScale(BurnMeter.fractionDigits(usage.hourlyAmount(), usage.currency), java.math.RoundingMode.HALF_UP)} $currencyName. Illustrative, not money spent.")
    }

    // Updates arrive both from the 1 s ticker and from content-change events, so intervals are often
    // well under a second: accumulate milliseconds and write whole seconds in batches.
    private var learningPendingMillis = 0L
    private var learningPendingDate: String? = null
    private suspend fun updateLearningClock(now: Long, learning: Boolean, date: String) {
        val previous = learningTickAt
        learningTickAt = if (learning) now else null
        if (learningPendingDate != null && learningPendingDate != date) flushLearning(keepRemainder = false)
        if (learning && previous != null) {
            learningPendingDate = date
            learningPendingMillis += (now - previous).coerceIn(0, 2_000)
        }
        if (!learning) flushLearning(keepRemainder = false)
        else if (learningPendingMillis >= 15_000) flushLearning(keepRemainder = true)
    }
    private suspend fun flushLearning(keepRemainder: Boolean) {
        val date = learningPendingDate ?: return
        val seconds = learningPendingMillis / 1_000
        if (seconds > 0) yoga.learningPrefs.addSeconds(date, LearningDetector.YOUTUBE_PACKAGE, seconds)
        learningPendingMillis = if (keepRemainder) learningPendingMillis % 1_000 else 0
        if (!keepRemainder) learningPendingDate = null
    }

    private var savedDaily: GuardDaily? = null
    private fun canonical(value: GuardDaily) = value.copy(
        passes = emptyMap(), resisted = emptyMap(),
        blocks = GuardSurface.entries.associateWith { value.blocks[it] ?: 0 },
        usedMillis = GuardSurface.entries.associateWith { value.usedMillis[it] ?: 0L })
    private suspend fun saveDailyIfChanged() {
        val next = canonical(gate.daily)
        if (savedDaily != next) {
            savedDaily = next
            yoga.guardPrefs.saveGateDaily(next)
            val blocks = next.blocks.values.sum()
            if (blocks >= 5 && snapshot == null) snapshot = runCatching { yoga.usageRepository.check() }.getOrNull()
            val over = snapshot?.evaluation?.overall?.state == RuleState.OVER
            // Usage time changes every tick; the review moment only cares about blocks and the limit.
            val report = Triple(next.date, blocks, over)
            if (report != reportedStrongDay) {
                reportedStrongDay = report
                yoga.reviewController.recordStrongBlockDay(next.date, blocks, over)
            }
        }
    }
    private var reportedStrongDay: Triple<String, Int, Boolean>? = null
    private fun today() = LocalDate.now().toString()
    private fun packagesFor(intent: Intent): Set<String> = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).map { it.activityInfo.packageName }.toSet()
    private fun params(y: Int) = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; this.y = y }
    private fun styleOverlay(view: TextView) {
        view.setTextColor(overlayTokens.text.toArgb())
        view.background = GradientDrawable().apply {
            setColor(overlayTokens.raised.toArgb())
            setStroke(1, overlayTokens.accent.toArgb())
            cornerRadius = dp(999).toFloat()
        }
        view.elevation = 0f
    }
    private fun label() = TextView(this).apply {
        textSize = 14f; setPadding(dp(14), dp(9), dp(14), dp(9))
        maxWidth = resources.displayMetrics.widthPixels - dp(32)
        styleOverlay(this)
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun showPill(text: String, description: String, calm: Boolean = false, isPass: Boolean = false, action: (() -> Unit)? = null) {
        passPill = isPass
        pillAction = action
        val view = pill ?: label().also { v ->
            pill = v
            var downY = 0f; var startY = 0; var dragging = false
            v.setOnClickListener {
                val currentAction = pillAction
                if (passPill) Unit
                else if (!collapsed && currentAction != null) currentAction()
                else { collapsed = !collapsed; requests.trySend(Unit) }
            }
            v.setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> { downY = event.rawY; startY = pillY; dragging = false; true }
                    MotionEvent.ACTION_MOVE -> {
                        if (abs(event.rawY - downY) > ViewConfiguration.get(this).scaledTouchSlop) dragging = true
                        if (dragging) { pillY = (startY + event.rawY - downY).toInt().coerceIn(0, (resources.displayMetrics.heightPixels - v.height).coerceAtLeast(0)); windows.updateViewLayout(v, params(pillY)) }
                        true
                    }
                    MotionEvent.ACTION_UP -> { if (dragging) scope.launch { yoga.guardPrefs.update { it.copy(positionY = pillY) } } else v.performClick(); true }
                    MotionEvent.ACTION_CANCEL -> true
                    else -> false
                }
            }
            windows.addView(v, params(pillY))
        }
        view.setTextColor(if (calm) overlayTokens.accent.toArgb() else overlayTokens.text.toArgb())
        view.text = if (passPill) text else if (collapsed) { if (calm) "Learning" else "🔥" } else if (!calm && preferences.style == GuardStyle.BURNING) {
            SpannableString(text).apply {
                val start = text.indexOf(' ').let { if (it < 0) 0 else it + 1 }
                val end = text.lastIndexOf(" burning").let { if (it < start) text.length else it }
                setSpan(ForegroundColorSpan(overlayTokens.ember.toArgb()), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        } else text
        view.contentDescription = description + if (passPill) " Drag to move." else if (collapsed) " Tap to expand." else if (action != null) " Tap to add. Drag to move." else " Tap to collapse. Drag to move."
        val animate = !passPill && !calm && preferences.style == GuardStyle.BURNING && Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
        if (animate && flame == null) {
            flame = ValueAnimator.ofFloat(1f, 0.86f, 1f).apply { duration = 1_800; repeatCount = ValueAnimator.INFINITE
                addUpdateListener { view.alpha = it.animatedValue as Float }; start() }
        } else if (!animate) { flame?.cancel(); flame = null; view.alpha = 1f }
    }
    private fun showBlockCard(surface: GuardSurface, expiredMinutes: Int? = null) {
        hideCard()
        yoga.reviewController.recordInterruption()
        val name = surface.shortName()
        val headline = expiredMinutes?.let { "Your $it minutes are up" } ?: if (surface == GuardSurface.WHATSAPP_STATUS) "Status is off" else "$name are off"
        val available = if (surface == GuardSurface.WHATSAPP_STATUS) "Chats and calls still work" else "Regular videos still work"
        card = label().apply { text = "$headline \u00b7 $available \u00b7 Blocked ${(gate.daily.blocks[surface] ?: 0).let { if (it == 1) "once" else "$it times" }} today" }
        windows.addView(card, params(dp(140)))
        fun keepVisible(millis: Long) {
            cardRemoval?.cancel()
            cardRemoval = scope.launch { delay(millis); hideCard() }
        }
        keepVisible(2_500)
        card?.let { view ->
            val strict = isStrict(preferences, LocalDate.now())
            fun watchAnyway() {
                if (card !== view || strict) return
                addWatchAnyway(view, overlayTokens.accent.toArgb(), dp(48)) {
                    startActivity(Intent(this, `in`.tuwi.yogaphone.ResistActivity::class.java)
                        .putExtra(`in`.tuwi.yogaphone.ResistActivity.EXTRA_SURFACE, surface.name)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION))
                    hideCard()
                }
                keepVisible(6_000)
            }
            if (strict || gate.daily.blocks.values.sum() == 1) {
                addBlockIntroduction(scope, yoga, view, surface, overlayTokens.accent.toArgb(),
                    isCurrent = { card === view }, keepVisible = { keepVisible(8_000) },
                    onUnavailable = { watchAnyway() })
            } else watchAnyway()
        }
    }
    private fun showLearningBlockCard(channel: String?) {
        hideCard()
        card = label().apply {
            text = "Not on your learning list" + if (channel != null) " · Add channel" else ""
            if (channel != null) setOnClickListener { scope.launch { yoga.learningPrefs.add(channel); showAddedCard(channel) } }
        }
        windows.addView(card, params(dp(140)))
        cardRemoval = scope.launch { delay(2_500); hideCard() }
    }
    private fun showAddedCard(channel: String) {
        hideCard()
        card = label().apply { text = "Added $channel" }
        windows.addView(card, params(dp(140)))
        cardRemoval = scope.launch { delay(2_000); hideCard() }
    }
    private fun hidePill() { passPill = false; passPillSurface = null; flame?.cancel(); flame = null; pillAction = null; pill?.let { runCatching { windows.removeView(it) } }; pill = null }
    private fun hideCard() { cardRemoval?.cancel(); cardRemoval = null; card?.let { runCatching { windows.removeView(it) } }; card = null }
    override fun onInterrupt() { generation++; detected = null; hidePill(); hideCard() }
    override fun onDestroy() { if (registered) unregisterReceiver(screenReceiver); scope.cancel(); requests.close(); hidePill(); hideCard(); super.onDestroy() }

    private class AndroidNode(private val node: AccessibilityNodeInfo) : NodeView {
        override val viewId get() = node.viewIdResourceName
        override val className get() = node.className?.toString()
        override val text get() = node.text
        override val contentDescription get() = node.contentDescription
        override val childCount get() = node.childCount
        override fun child(index: Int): NodeView? = node.getChild(index)?.let(::AndroidNode)
        override fun recycle() = recycle(node)
    }
    companion object {
        private fun recycle(node: AccessibilityNodeInfo) { if (Build.VERSION.SDK_INT < 33) { @Suppress("DEPRECATION") node.recycle() } }
        fun isEnabled(context: Context): Boolean {
            val target = ComponentName(context, YogaGuardService::class.java)
            return Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                .orEmpty().split(':').any { ComponentName.unflattenFromString(it) == target }
        }
    }
}
