package `in`.tuwi.yogaphone.review

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import `in`.tuwi.yogaphone.data.dataStore
import kotlinx.coroutines.flow.first

data class ReviewStoredState(
    val homeSessions: Int = 0,
    val asks: List<Long> = emptyList(),
    val lastInterruptionAt: Long? = null,
    val completedSessions: Int = 0,
    val pendingMoment: String? = null,
    val pendingR1At: Long? = null,
    val pendingR3At: Long? = null,
    val usedMoments: Set<ReviewMoment> = emptySet(),
    val feedbackAnswers: List<FeedbackAnswer> = emptyList(),
)

class ReviewStore(private val context: Context) {
    suspend fun read(): ReviewStoredState {
        val p = context.dataStore.data.first()
        return ReviewStoredState(
            homeSessions = p[HOME_SESSIONS] ?: 0,
            asks = p[ASKS].orEmpty().mapNotNull(String::toLongOrNull).sorted(),
            lastInterruptionAt = p[LAST_INTERRUPTION],
            completedSessions = p[COMPLETED_SESSIONS] ?: 0,
            pendingMoment = p[PENDING_MOMENT],
            pendingR1At = p[PENDING_R1_AT],
            pendingR3At = p[PENDING_R3_AT],
            usedMoments = p[USED_MOMENTS].orEmpty().mapNotNull { runCatching { ReviewMoment.valueOf(it) }.getOrNull() }.toSet(),
            feedbackAnswers = p[FEEDBACK_ANSWERS].orEmpty().mapNotNull(::decodeAnswer)
                .sortedByDescending(FeedbackAnswer::atMillis).take(3),
        )
    }

    suspend fun recordHomeResume() {
        context.dataStore.edit { it[HOME_SESSIONS] = (it[HOME_SESSIONS] ?: 0) + 1 }
    }

    suspend fun recordInterruption(atMillis: Long) {
        context.dataStore.edit { it[LAST_INTERRUPTION] = atMillis }
    }

    suspend fun recordCompletedSession(atMillis: Long): Int {
        var count = 0
        context.dataStore.edit { p ->
            count = (p[COMPLETED_SESSIONS] ?: 0) + 1
            p[COMPLETED_SESSIONS] = count
            if (count == 3) p[PENDING_R3_AT] = atMillis
        }
        return count
    }

    suspend fun setPendingStrongDay(date: String) {
        context.dataStore.edit { p -> p[PENDING_MOMENT] = date }
    }

    suspend fun clearPendingStrongDay(date: String) {
        context.dataStore.edit { p -> if (p[PENDING_MOMENT] == date) p.remove(PENDING_MOMENT) }
    }

    suspend fun queueOnboardingR1(atMillis: Long) {
        context.dataStore.edit { it[PENDING_R1_AT] = atMillis }
    }

    suspend fun recordAsk(moment: ReviewMoment, atMillis: Long) {
        context.dataStore.edit { p ->
            p[ASKS] = p[ASKS].orEmpty() + atMillis.toString()
            p[USED_MOMENTS] = p[USED_MOMENTS].orEmpty() + moment.name
            if (moment == ReviewMoment.R4_STRONG_BLOCK_DAY) p.remove(PENDING_MOMENT)
            if (moment == ReviewMoment.R1_BLOCK_FROM_DATA) p.remove(PENDING_R1_AT)
            if (moment == ReviewMoment.R3_THIRD_SESSION) p.remove(PENDING_R3_AT)
        }
    }

    suspend fun featureChanged(feature: FeedbackFeature, enabled: Boolean, nowMillis: Long): Boolean {
        var ask = false
        context.dataStore.edit { p ->
            val key = featureOnKey(feature)
            val stateKey = featureStateKey(feature)
            val wasEnabled = p[stateKey] == true
            if (enabled) {
                if (!wasEnabled) p[key] = nowMillis
            } else if (wasEnabled) {
                ask = ReviewPolicy.shouldAskWhatDidntWork(p[key], nowMillis)
                p.remove(key)
            }
            p[stateKey] = enabled
        }
        return ask
    }

    suspend fun recordFeedback(feature: FeedbackFeature, answer: String, atMillis: Long) {
        val clean = answer.trim().take(160)
        if (clean.isEmpty()) return
        context.dataStore.edit { p ->
            val answers = (p[FEEDBACK_ANSWERS].orEmpty().mapNotNull(::decodeAnswer) + FeedbackAnswer(feature, clean, atMillis))
                .sortedByDescending(FeedbackAnswer::atMillis).take(3)
            p[FEEDBACK_ANSWERS] = answers.map(::encodeAnswer).toSet()
            if (clean.startsWith("Other: ")) {
                val text = clean.removePrefix("Other: ").trim()
                val others = (p[OTHER_TEXTS].orEmpty().mapNotNull(::decodeOther) + (atMillis to text))
                    .sortedByDescending { it.first }.distinctBy { it.second }.take(3)
                p[OTHER_TEXTS] = others.map { "${it.first}|${Uri.encode(it.second)}" }.toSet()
            }
        }
    }

    suspend fun recentOtherTexts(): List<String> = context.dataStore.data.first()[OTHER_TEXTS].orEmpty()
        .mapNotNull(::decodeOther).sortedByDescending { it.first }.map { it.second }.distinct().take(3)

    companion object {
        private val HOME_SESSIONS = intPreferencesKey("review_home_sessions")
        private val ASKS = stringSetPreferencesKey("review_asks")
        private val LAST_INTERRUPTION = longPreferencesKey("review_last_interruption")
        private val COMPLETED_SESSIONS = intPreferencesKey("review_completed_sessions")
        private val PENDING_MOMENT = stringPreferencesKey("review_pending_moment")
        private val PENDING_R1_AT = longPreferencesKey("review_pending_r1_at")
        private val PENDING_R3_AT = longPreferencesKey("review_pending_r3_at")
        private val USED_MOMENTS = stringSetPreferencesKey("review_used_moments")
        private val FEEDBACK_ANSWERS = stringSetPreferencesKey("review_feedback_answers")
        private val OTHER_TEXTS = stringSetPreferencesKey("review_feedback_other_texts")
        private fun featureOnKey(feature: FeedbackFeature) = longPreferencesKey("review_feature_on_${feature.name.lowercase()}")
        private fun featureStateKey(feature: FeedbackFeature) = booleanPreferencesKey("review_feature_state_${feature.name.lowercase()}")
        private fun encodeAnswer(value: FeedbackAnswer) = "${value.atMillis}|${value.feature.name}|${Uri.encode(value.answer)}"
        private fun decodeAnswer(value: String): FeedbackAnswer? {
            val parts = value.split('|', limit = 3)
            return FeedbackAnswer(
                runCatching { FeedbackFeature.valueOf(parts.getOrNull(1).orEmpty()) }.getOrNull() ?: return null,
                Uri.decode(parts.getOrNull(2) ?: return null),
                parts.getOrNull(0)?.toLongOrNull() ?: return null,
            )
        }
        private fun decodeOther(value: String): Pair<Long, String>? {
            val parts = value.split('|', limit = 2)
            return (parts.getOrNull(0)?.toLongOrNull() ?: return null) to Uri.decode(parts.getOrNull(1) ?: return null)
        }
    }
}
