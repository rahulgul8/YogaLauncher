package `in`.tuwi.yogaphone.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.data.ThemeMode

private val DarkScheme = darkColorScheme(
    primary = Brand.Brass, onPrimary = Color(0xFF1A1408),
    primaryContainer = Color(0xFF3A2F1A), onPrimaryContainer = Color(0xFFF0DDB2),
    inversePrimary = Brand.BrassDeep,
    secondary = Color(0xFFB8AE9C), onSecondary = Color(0xFF1E1B15),
    secondaryContainer = Brand.ObsidianHigh, onSecondaryContainer = Brand.Ivory,
    tertiary = Brand.Ember, onTertiary = Color(0xFF231206),
    tertiaryContainer = Color(0xFF3B2416), onTertiaryContainer = Color(0xFFF6D3BC),
    background = Brand.Obsidian, onBackground = Brand.Ivory,
    surface = Brand.Obsidian, onSurface = Brand.Ivory,
    surfaceVariant = Brand.ObsidianHigh, onSurfaceVariant = Brand.Stone,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Color(0xFF0A0A09), surfaceContainerLow = Color(0xFF141412),
    surfaceContainer = Brand.ObsidianRaised, surfaceContainerHigh = Brand.ObsidianHigh,
    surfaceContainerHighest = Color(0xFF2A2824), surfaceBright = Color(0xFF2E2C28), surfaceDim = Brand.Obsidian,
    inverseSurface = Brand.Ivory, inverseOnSurface = Brand.Obsidian,
    outline = Color(0xFF4A473F), outlineVariant = Brand.ObsidianLine,
    error = Color(0xFFE58C7C), onError = Color(0xFF2A0D07),
    errorContainer = Color(0xFF4A1F17), onErrorContainer = Color(0xFFFAD6CD),
    scrim = Color.Black,
)

private val LightScheme = lightColorScheme(
    primary = Brand.BrassDeep, onPrimary = Color.White,
    primaryContainer = Color(0xFFF1E4C6), onPrimaryContainer = Color(0xFF2E2205),
    inversePrimary = Brand.Brass,
    secondary = Color(0xFF5E584E), onSecondary = Color.White,
    secondaryContainer = Brand.PorcelainHigh, onSecondaryContainer = Brand.Ink,
    tertiary = Color(0xFFB4592A), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF8DCCB), onTertiaryContainer = Color(0xFF3A1606),
    background = Brand.Porcelain, onBackground = Brand.Ink,
    surface = Brand.Porcelain, onSurface = Brand.Ink,
    surfaceVariant = Brand.PorcelainHigh, onSurfaceVariant = Brand.InkMuted,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Color.White, surfaceContainerLow = Brand.PorcelainRaised,
    surfaceContainer = Color(0xFFF7F6F3), surfaceContainerHigh = Color(0xFFEEECE8),
    surfaceContainerHighest = Brand.PorcelainHigh, surfaceBright = Brand.PorcelainRaised, surfaceDim = Color(0xFFE2E0DA),
    inverseSurface = Brand.Ink, inverseOnSurface = Brand.Porcelain,
    outline = Color(0xFFB5B0A6), outlineVariant = Brand.PorcelainLine,
    error = Color(0xFFB3402C), onError = Color.White,
    errorContainer = Color(0xFFF9DAD3), onErrorContainer = Color(0xFF410E05),
    scrim = Color.Black,
)

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
fun YogaPhoneTheme(themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM, content: @Composable () -> Unit) {
    val dark = themeMode == ThemeMode.DARK || (themeMode == ThemeMode.FOLLOW_SYSTEM && isSystemInDarkTheme())
    val scheme = when {
        themeMode == ThemeMode.GREYSCALE -> GreyscaleScheme
        dark -> DarkScheme
        else -> LightScheme
    }
    MaterialTheme(colorScheme = scheme, typography = Typography, shapes = YogaShapes, content = content)
}
