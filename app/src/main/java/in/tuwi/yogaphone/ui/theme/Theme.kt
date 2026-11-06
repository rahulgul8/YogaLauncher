package `in`.tuwi.yogaphone.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.data.Palette
import `in`.tuwi.yogaphone.data.ThemeMode

/** Builds a full Material colour scheme from one palette's tokens. */
fun schemeOf(t: Tokens, dark: Boolean): ColorScheme {
    val errorColor = if (dark) Color(0xFFE58C7C) else Color(0xFFB3402C)
    return if (dark) darkColorScheme(
        primary = t.accent, onPrimary = t.onAccent,
        primaryContainer = t.accentContainer, onPrimaryContainer = t.onAccentContainer, inversePrimary = t.accent,
        secondary = t.muted, onSecondary = t.background,
        secondaryContainer = t.high, onSecondaryContainer = t.text,
        tertiary = t.ember, onTertiary = t.background,
        tertiaryContainer = t.high, onTertiaryContainer = t.ember,
        background = t.background, onBackground = t.text,
        surface = t.background, onSurface = t.text,
        surfaceVariant = t.high, onSurfaceVariant = t.muted, surfaceTint = Color.Transparent,
        surfaceContainerLowest = t.background, surfaceContainerLow = t.raised, surfaceContainer = t.raised,
        surfaceContainerHigh = t.high, surfaceContainerHighest = t.highest, surfaceBright = t.highest, surfaceDim = t.background,
        inverseSurface = t.text, inverseOnSurface = t.background,
        outline = t.outline, outlineVariant = t.line,
        error = errorColor, onError = Color.Black, errorContainer = Color(0xFF4A1F17), onErrorContainer = Color(0xFFFAD6CD),
        scrim = Color.Black,
    ) else lightColorScheme(
        primary = t.accent, onPrimary = t.onAccent,
        primaryContainer = t.accentContainer, onPrimaryContainer = t.onAccentContainer, inversePrimary = t.accent,
        secondary = t.muted, onSecondary = Color.White,
        secondaryContainer = t.high, onSecondaryContainer = t.text,
        tertiary = t.ember, onTertiary = Color.White,
        tertiaryContainer = t.high, onTertiaryContainer = t.ember,
        background = t.background, onBackground = t.text,
        surface = t.background, onSurface = t.text,
        surfaceVariant = t.high, onSurfaceVariant = t.muted, surfaceTint = Color.Transparent,
        surfaceContainerLowest = Color.White, surfaceContainerLow = t.raised, surfaceContainer = t.raised,
        surfaceContainerHigh = t.high, surfaceContainerHighest = t.highest, surfaceBright = t.raised, surfaceDim = t.highest,
        inverseSurface = t.text, inverseOnSurface = t.background,
        outline = t.outline, outlineVariant = t.line,
        error = errorColor, onError = Color.White, errorContainer = Color(0xFFF9DAD3), onErrorContainer = Color(0xFF410E05),
        scrim = Color.Black,
    )
}

private val GreyscaleScheme = lightColorScheme(
    primary = Color(0xFF3F3F3F), onPrimary = Color.White,
    primaryContainer = Color(0xFFDADADA), onPrimaryContainer = Color(0xFF1D1D1D),
    inversePrimary = Color(0xFFC6C6C6),
    secondary = Color(0xFF555555), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E0E0), onSecondaryContainer = Color(0xFF202020),
    tertiary = Color(0xFF6A6A6A), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD7D7D7), onTertiaryContainer = Color(0xFF202020),
    background = Color(0xFFF5F5F5), onBackground = Color(0xFF171717),
    surface = Color(0xFFF5F5F5), onSurface = Color(0xFF171717),
    surfaceVariant = Color(0xFFE4E4E4), onSurfaceVariant = Color(0xFF555555),
    surfaceTint = Color.Transparent,
    surfaceContainerLow = Color(0xFFFAFAFA), surfaceContainer = Color(0xFFF0F0F0),
    surfaceContainerHigh = Color(0xFFEAEAEA), surfaceContainerHighest = Color(0xFFE4E4E4),
    inverseSurface = Color(0xFF303030), inverseOnSurface = Color(0xFFF2F2F2),
    outline = Color(0xFFA0A0A0), outlineVariant = Color(0xFFD6D6D6),
    error = Color(0xFF303030), onError = Color.White,
    errorContainer = Color(0xFFD5D5D5), onErrorContainer = Color(0xFF202020),
    scrim = Color.Black,
)

val YogaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

@Composable
fun themeUsesDarkIcons(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.LIGHT, ThemeMode.GREYSCALE -> true
    ThemeMode.DARK -> false
    ThemeMode.FOLLOW_SYSTEM -> !isSystemInDarkTheme()
}

@Composable
fun YogaPhoneTheme(
    themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    palette: Palette = Palette.OBSIDIAN_BRASS,
    content: @Composable () -> Unit,
) {
    val dark = themeMode == ThemeMode.DARK || (themeMode == ThemeMode.FOLLOW_SYSTEM && isSystemInDarkTheme())
    val spec = specOf(palette)
    val scheme = remember(themeMode, palette, dark) {
        if (themeMode == ThemeMode.GREYSCALE) GreyscaleScheme else schemeOf(if (dark) spec.dark else spec.light, dark)
    }
    val typography = remember(palette) { typographyFor(palette) }
    MaterialTheme(colorScheme = scheme, typography = typography, shapes = YogaShapes, content = content)
}
