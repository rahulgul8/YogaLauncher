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
import `in`.tuwi.yogaphone.data.Palette

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, weight: Int) =
    Font(res, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

private fun family(res: Int, vararg weights: Int) = FontFamily(weights.map { variable(res, it) })

private val Cormorant = family(R.font.cormorant_garamond, 300, 400, 500)
private val Manrope = family(R.font.manrope, 300, 400, 500, 600)
private val Urbanist = family(R.font.urbanist, 200, 300, 400, 500, 600)
private val Jost = family(R.font.jost, 200, 300, 400, 500, 600)

/** Each palette's display face (clock, titles, big numbers) and text face, plus the display weights it looks best at. */
private data class Faces(val display: FontFamily, val text: FontFamily, val clockWeight: Int, val titleWeight: Int, val displayTracking: Double)

private fun facesOf(palette: Palette) = when (palette) {
    Palette.OBSIDIAN_BRASS -> Faces(Cormorant, Manrope, clockWeight = 300, titleWeight = 400, displayTracking = -0.01)
    Palette.STONE_SAFFRON -> Faces(Urbanist, Urbanist, clockWeight = 200, titleWeight = 300, displayTracking = -0.02)
    Palette.MIDNIGHT_SAGE -> Faces(Jost, Jost, clockWeight = 200, titleWeight = 300, displayTracking = -0.015)
}

fun typographyFor(palette: Palette): Typography {
    val f = facesOf(palette)
    fun text(size: Int, line: Int, weight: Int = 400, tracking: Double = 0.0) = TextStyle(
        fontFamily = f.text, fontWeight = FontWeight(weight), fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.em)
    fun display(size: Int, line: Int, weight: Int, tracking: Double = f.displayTracking) = TextStyle(
        fontFamily = f.display, fontWeight = FontWeight(weight), fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.em)
    return Typography(
        displayLarge = display(88, 88, f.clockWeight, tracking = f.displayTracking - 0.01),
        displayMedium = display(56, 60, f.clockWeight),
        displaySmall = display(40, 46, f.titleWeight),
        headlineLarge = display(36, 42, f.titleWeight),
        headlineMedium = display(31, 37, f.titleWeight),
        headlineSmall = display(26, 32, f.titleWeight + 100),
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
}

/** The clock face for a palette, for previews drawn outside that palette's theme. */
fun clockStyleFor(palette: Palette, sizeSp: Int): TextStyle {
    val f = facesOf(palette)
    return TextStyle(fontFamily = f.display, fontWeight = FontWeight(f.clockWeight), fontSize = sizeSp.sp, lineHeight = sizeSp.sp,
        letterSpacing = (f.displayTracking - 0.01).em, fontFeatureSettings = "lnum, tnum")
}
