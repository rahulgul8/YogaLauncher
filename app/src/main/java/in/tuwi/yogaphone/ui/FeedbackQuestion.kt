package `in`.tuwi.yogaphone.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import `in`.tuwi.yogaphone.YogaApplication
import `in`.tuwi.yogaphone.review.FeedbackFeature
import `in`.tuwi.yogaphone.ui.theme.*

@Composable
fun FeedbackQuestion(feature: FeedbackFeature, reasons: List<String>, onDismiss: () -> Unit) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as YogaApplication
    var other by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var recent by remember { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(Unit) { recent = runCatching { app.reviewController.store.recentOtherTexts() }.getOrDefault(emptyList()) }
    fun answer(value: String) { app.reviewController.saveFeedback(feature, value); onDismiss() }
    Column(Modifier.fillMaxWidth().padding(horizontal = ScreenMargin)) {
        YogaNote("What didn't work?")
        YogaChipRow {
            reasons.forEach { reason -> YogaChip(reason, false, onClick = { answer(reason) }) }
            YogaChip("Other", other, onClick = { other = true })
        }
        if (other) {
            YogaTextField(text, { text = it.take(120) }, "Tell us briefly")
            if (recent.isNotEmpty()) YogaChipRow {
                recent.forEach { value -> YogaChip(value, text == value, onClick = { text = value }) }
            }
            YogaTextButton("Save", { if (text.isNotBlank()) answer("Other: ${text.trim()}") })
        }
        YogaTextButton("Dismiss", onDismiss, muted = true)
    }
}
