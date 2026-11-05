package `in`.tuwi.yogaphone.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import `in`.tuwi.yogaphone.data.ThemeMode

private val DarkScheme = darkColorScheme(
    primary = CalmSageLight,
    onPrimary = Color(0xFF173222),
    primaryContainer = Color(0xFF294738),
    onPrimaryContainer = Color(0xFFC8E8D1),
    secondary = Color(0xFFB6CBB9),
    onSecondary = Color(0xFF223527),
    secondaryContainer = Color(0xFF314A38),
    onSecondaryContainer = Color(0xFFD2E8D5),
    background = Color(0xFF111311),
    surface = Color(0xFF171A17),
    onBackground = Color(0xFFE2E4DF),
    onSurface = Color(0xFFE2E4DF),
    outline = Color(0xFF8C938D),
)

private val LightScheme = lightColorScheme(
    primary = CalmSage,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5E8DA),
    onPrimaryContainer = Color(0xFF102A1A),
    secondary = Color(0xFF596E5E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDE9DF),
    onSecondaryContainer = Color(0xFF182D1E),
    background = Paper,
    surface = Color(0xFFFCFDF9),
    onBackground = Ink,
    onSurface = Ink,
    outline = Color(0xFF737A74),
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
    surface = Color(0xFFFAFAFA), onSurface = Color(0xFF171717),
    surfaceVariant = Color(0xFFE4E4E4), onSurfaceVariant = Color(0xFF444444),
    surfaceTint = Color(0xFF3F3F3F), inverseSurface = Color(0xFF303030), inverseOnSurface = Color(0xFFF2F2F2),
    outline = Color(0xFF707070), outlineVariant = Color(0xFFC4C4C4),
    error = Color(0xFF303030), onError = Color.White,
    errorContainer = Color(0xFFD5D5D5), onErrorContainer = Color(0xFF202020),
    scrim = Color.Black,
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
    MaterialTheme(colorScheme = scheme, typography = Typography, content = content)
}
