package `in`.tuwi.yogaphone.review

enum class FeedbackFeature(val label: String) { GUARD("Guard"), ALLOWANCE("Daily allowance"), PAUSE("Mindful pause") }

data class FeedbackAnswer(val feature: FeedbackFeature, val answer: String, val atMillis: Long)

fun buildFeedbackEmailBody(
    versionName: String,
    versionCode: Int,
    androidVersion: String,
    deviceModel: String,
    answers: List<FeedbackAnswer>,
): String = buildString {
    appendLine("Yoga Phone feedback")
    appendLine()
    appendLine("Version: $versionName ($versionCode)")
    appendLine("Android: $androidVersion")
    appendLine("Device: $deviceModel")
    if (answers.isNotEmpty()) {
        appendLine()
        appendLine("Recent feedback:")
        answers.take(3).forEach { appendLine("- ${it.feature.label}: ${it.answer}") }
    }
    appendLine()
    append("What would you like us to know?\n")
}
