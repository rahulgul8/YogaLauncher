package `in`.tuwi.yogaphone.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.data.FontSizeOption
import `in`.tuwi.yogaphone.data.HomeTextAlignment
import `in`.tuwi.yogaphone.data.Palette
import `in`.tuwi.yogaphone.data.ThemeMode
import `in`.tuwi.yogaphone.data.UserPreferences
import `in`.tuwi.yogaphone.ui.theme.PalettePicker
import `in`.tuwi.yogaphone.ui.theme.YogaChip
import `in`.tuwi.yogaphone.ui.theme.YogaChipRow
import `in`.tuwi.yogaphone.ui.theme.YogaIcons
import `in`.tuwi.yogaphone.ui.theme.YogaNote
import `in`.tuwi.yogaphone.ui.theme.YogaRow
import `in`.tuwi.yogaphone.ui.theme.YogaScreen
import `in`.tuwi.yogaphone.ui.theme.YogaSection
import `in`.tuwi.yogaphone.ui.theme.YogaSwitchRow

/** Light, dark, follow the phone, or greyscale; shared by Appearance and onboarding. */
val ThemeModeChoices = listOf(
    ThemeMode.FOLLOW_SYSTEM to "Follow phone",
    ThemeMode.DARK to "Dark",
    ThemeMode.LIGHT to "Light",
    ThemeMode.GREYSCALE to "Greyscale",
)

@Composable
fun AppearanceSettingsScreen(
    preferences: UserPreferences,
    onBack: () -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onPalette: (Palette) -> Unit,
    onFontSize: (FontSizeOption) -> Unit,
    onShowClock: (Boolean) -> Unit,
    onShowDate: (Boolean) -> Unit,
    onHomeAppCount: (Int) -> Unit,
    onAlignment: (HomeTextAlignment) -> Unit,
) {
    YogaScreen("Appearance", onBack, subtitle = "Three looks, each with its own colours and type. Pick the one that feels like yours.") {
        item { YogaSection("Look") }
        item { PalettePicker(preferences.palette, preferences.themeMode, onPalette) }
        item { YogaSection("Light") }
        item {
            YogaChipRow {
                ThemeModeChoices.forEach { (mode, label) -> YogaChip(label, preferences.themeMode == mode, { onTheme(mode) }) }
            }
        }
        item { YogaNote("Greyscale turns colour off everywhere in Yoga. It also switches on by itself past your limit or at bedtime, if you set that up.") }
        item { YogaSection("App icon") }
        item {
            val context = androidx.compose.ui.platform.LocalContext.current
            var icon by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(AppIcons.current(context)) }
            AppIconPicker(icon, { choice -> if (choice != icon) { AppIcons.set(context, choice); icon = choice } })
        }
        item { YogaNote("The icon other launchers show for Yoga. Some phones take a few seconds to update it and may remove a shortcut to Yoga from another launcher's home screen. Android's app info page keeps the dial.") }
        item { YogaSection("Text size") }
        item {
            YogaChipRow {
                listOf(FontSizeOption.SMALL to "Small", FontSizeOption.MEDIUM to "Medium", FontSizeOption.LARGE to "Large",
                    FontSizeOption.EXTRA_LARGE to "Extra large").forEach { (size, label) ->
                    YogaChip(label, preferences.fontSize == size, { onFontSize(size) })
                }
            }
        }
        item { YogaSection("Home") }
        item { YogaSwitchRow("Show the clock", preferences.showClock, onShowClock) }
        item { YogaSwitchRow("Show the date", preferences.showDate, onShowDate) }
        item {
            YogaRow("Home apps", summary = "${preferences.homeAppCount} at most", trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onHomeAppCount(preferences.homeAppCount - 1) }, enabled = preferences.homeAppCount > 4) {
                        Text("−", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                    }
                    Text("${preferences.homeAppCount}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 4.dp))
                    IconButton(onClick = { onHomeAppCount(preferences.homeAppCount + 1) }, enabled = preferences.homeAppCount < 12) {
                        Icon(YogaIcons.Plus, contentDescription = "More home apps", tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(20.dp))
                    }
                }
            })
        }
        item { YogaSection("Align home text") }
        item {
            YogaChipRow {
                listOf(HomeTextAlignment.LEFT to "Left", HomeTextAlignment.CENTER to "Centre", HomeTextAlignment.RIGHT to "Right")
                    .forEach { (a, label) -> YogaChip(label, preferences.homeTextAlignment == a, { onAlignment(a) }) }
            }
        }
    }
}
