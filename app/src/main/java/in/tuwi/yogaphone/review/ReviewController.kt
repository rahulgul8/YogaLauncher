package `in`.tuwi.yogaphone.review

import android.app.Activity
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.testing.FakeReviewManager
import `in`.tuwi.yogaphone.BuildConfig
import `in`.tuwi.yogaphone.YogaApplication
import `in`.tuwi.yogaphone.ui.isDefaultHome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ReviewController(private val app: YogaApplication) {
    val store = ReviewStore(app)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val momentMutex = Mutex()
    @Volatile private var onboarding = true
    @Volatile private var overLimitToday = false
    @Volatile private var fakeReview = false

    fun setFakeReview(enabled: Boolean) { fakeReview = BuildConfig.DEBUG && enabled }

    fun recordHomeResume() { scope.launch { runCatching { store.recordHomeResume() } } }

    fun updateEnvironment(onboarding: Boolean, overLimitToday: Boolean) {
        this.onboarding = onboarding
        this.overLimitToday = overLimitToday
    }

    fun onHomeReady(activity: Activity) {
        scope.launch {
            runCatching {
                val state = store.read()
                val nowMillis = System.currentTimeMillis()
                if (ReviewPolicy.isFresh(state.pendingR1At, nowMillis) && ReviewMoment.R1_BLOCK_FROM_DATA !in state.usedMoments) {
                    onMoment(activity, ReviewMoment.R1_BLOCK_FROM_DATA)
                }
                if (ReviewPolicy.isFresh(state.pendingR3At, nowMillis) && ReviewMoment.R3_THIRD_SESSION !in state.usedMoments) {
                    onMoment(activity, ReviewMoment.R3_THIRD_SESSION)
                }
                val zone = ZoneId.systemDefault()
                val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
                val pending = state.pendingMoment?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                if (ReviewPolicy.shouldAskStrongBlockDay(pending, now.toLocalDate(), now.hour,
                        ReviewMoment.R4_STRONG_BLOCK_DAY in state.usedMoments)) {
                    onMoment(activity, ReviewMoment.R4_STRONG_BLOCK_DAY)
                }
            }
        }
    }

    suspend fun onMoment(activity: Activity, moment: ReviewMoment) {
        momentMutex.withLock { runCatching {
            val now = System.currentTimeMillis()
            val state = store.read()
            if (moment in state.usedMoments) return@runCatching
            val installedAt = activity.packageManager.getPackageInfo(activity.packageName, 0).firstInstallTime
            val signals = ReviewSignals(now, installedAt, state.homeSessions, isDefaultHome(activity), onboarding,
                state.lastInterruptionAt, overLimitToday, state.asks)
            if (!ReviewPolicy.mayAsk(signals)) return@runCatching
            val manager = if (fakeReview && BuildConfig.DEBUG) FakeReviewManager(app) else ReviewManagerFactory.create(app)
            val info = manager.requestReview()
            store.recordAsk(moment, now)
            manager.launchReview(activity, info)
        } }
    }

    fun recordInterruption() { scope.launch { runCatching { store.recordInterruption(System.currentTimeMillis()) } } }

    fun queueOnboardingR1() { scope.launch { runCatching { store.queueOnboardingR1(System.currentTimeMillis()) } } }

    fun recordCompletedSession() { scope.launch { runCatching { store.recordCompletedSession(System.currentTimeMillis()) } } }

    fun recordStrongBlockDay(date: String, blocks: Int, overLimit: Boolean) {
        if (overLimit) {
            scope.launch { runCatching { store.clearPendingStrongDay(date) } }
            return
        }
        if (!ReviewPolicy.isStrongBlockDay(blocks, overLimit)) return
        scope.launch { runCatching { store.setPendingStrongDay(date) } }
    }

    fun onGoodWeek(activity: Activity, daysWithin: Int, knownDays: Int, thisWeekMinutes: Long, lastWeekMinutes: Long? = null) {
        if (!ReviewPolicy.isGoodWeek(daysWithin, knownDays, thisWeekMinutes, lastWeekMinutes)) return
        scope.launch { onMoment(activity, ReviewMoment.R2_GOOD_WEEK) }
    }

    suspend fun featureChanged(feature: FeedbackFeature, enabled: Boolean): Boolean =
        runCatching { store.featureChanged(feature, enabled, System.currentTimeMillis()) }.getOrDefault(false)

    fun saveFeedback(feature: FeedbackFeature, answer: String) {
        scope.launch { runCatching { store.recordFeedback(feature, answer, System.currentTimeMillis()) } }
    }
}
