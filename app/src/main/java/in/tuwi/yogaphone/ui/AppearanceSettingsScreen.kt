package `in`.tuwi.yogaphone.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.data.FontSizeOption
import `in`.tuwi.yogaphone.data.HomeTextAlignment
import `in`.tuwi.yogaphone.data.ThemeMode
import `in`.tuwi.yogaphone.data.UserPreferences

@Composable
fun AppearanceSettingsScreen(
    preferences: UserPreferences,
    onBack: () -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onFontSize: (FontSizeOption) -> Unit,
    onShowClock: (Boolean) -> Unit,
    onShowDate: (Boolean) -> Unit,
    onHomeAppCount: (Int) -> Unit,
    onAlignment: (HomeTextAlignment) -> Unit,
) {
    LazyColumn {
        item { SettingsHeader("Appearance", onBack) }
        item { SectionTitle("Theme") }
        ThemeMode.entries.forEach { value ->
            item { ChoiceRow(value.readableName(), preferences.themeMode == value) { onTheme(value) } }
        }
        item { SectionTitle("Font size") }
        FontSizeOption.entries.forEach { value ->
            item { ChoiceRow(value.readableName(), preferences.fontSize == value) { onFontSize(value) } }
        }
        item { SectionTitle("Home") }
        item { ToggleRow("Show clock", preferences.showClock, onShowClock) }
        item { ToggleRow("Show date", preferences.showDate, onShowDate) }
        item {
            ListItem(
                headlineContent = { Text("Number of home apps") },
                supportingContent = { Text("${preferences.homeAppCount} apps") },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onHomeAppCount(preferences.homeAppCount - 1) }, enabled = preferences.homeAppCount > 4) { Text("−") }
                        Text("${preferences.homeAppCount}")
                        IconButton(onClick = { onHomeAppCount(preferences.homeAppCount + 1) }, enabled = preferences.homeAppCount < 12) { Text("+") }
                    }
                },
            )
        }
        item { SectionTitle("Home text alignment") }
        HomeTextAlignment.entries.forEach { value ->
            item { ChoiceRow(value.readableName(), preferences.homeTextAlignment == value) { onAlignment(value) } }
        }
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { RadioButton(selected = selected, onClick = onClick); Text(label, Modifier.padding(start = 8.dp)) }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(headlineContent = { Text(label) }, trailingContent = { Switch(checked, onCheckedChange) })
}

private fun Enum<*>.readableName() = name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase)
