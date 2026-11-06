package `in`.tuwi.yogaphone.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import `in`.tuwi.yogaphone.R

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, weight: Int) =
    Font(res, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

/** Display face: the clock and big numbers. A light Garamond reads like a watch dial. */
val DisplayFamily = FontFamily(
    variable(R.font.cormorant_garamond, 300),
    variable(R.font.cormorant_garamond, 400),
    variable(R.font.cormorant_garamond, 500),
)

/** Text face for everything else. */
val TextFamily = FontFamily(
    variable(R.font.manrope, 300),
    variable(R.font.manrope, 400),
    variable(R.font.manrope, 500),
    variable(R.font.manrope, 600),
)

private fun text(size: Int, line: Int, weight: Int = 400, tracking: Double = 0.0) = TextStyle(
    fontFamily = TextFamily, fontWeight = FontWeight(weight), fontSize = size.sp, lineHeight = line.sp,
    letterSpacing = tracking.em,
)

private fun display(size: Int, line: Int, weight: Int = 300, tracking: Double = -0.01) = TextStyle(
    fontFamily = DisplayFamily, fontWeight = FontWeight(weight), fontSize = size.sp, lineHeight = line.sp,
    letterSpacing = tracking.em,
)

val Typography = Typography(
    displayLarge = display(88, 88, tracking = -0.02),
    displayMedium = display(56, 60),
    displaySmall = display(42, 48),
    headlineLarge = display(38, 44, weight = 400),
    headlineMedium = display(32, 38, weight = 400),
    headlineSmall = display(27, 33, weight = 500),
    titleLarge = text(20, 28, weight = 500, tracking = -0.005),
    titleMedium = text(16, 24, weight = 500),
    titleSmall = text(14, 20, weight = 600),
    bodyLarge = text(16, 24),
    bodyMedium = text(14, 21),
    bodySmall = text(12, 18, tracking = 0.01),
    labelLarge = text(14, 20, weight = 600, tracking = 0.01),
    labelMedium = text(12, 16, weight = 600, tracking = 0.04),
    labelSmall = text(11, 16, weight = 600, tracking = 0.12),
)
