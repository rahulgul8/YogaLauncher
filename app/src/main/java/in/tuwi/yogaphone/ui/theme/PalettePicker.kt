package `in`.tuwi.yogaphone.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.tuwi.yogaphone.data.Palette
import `in`.tuwi.yogaphone.data.ThemeMode

/**
 * Three miniature home screens, each drawn in its own palette's colours and typefaces, so people
 * choose from what they will actually see. Uses the light or dark variant that [themeMode] would show.
 */
@Composable
fun PalettePicker(selected: Palette, themeMode: ThemeMode, onSelect: (Palette) -> Unit, modifier: Modifier = Modifier) {
    val dark = themeMode == ThemeMode.DARK || (themeMode != ThemeMode.LIGHT && themeMode != ThemeMode.GREYSCALE && isSystemInDarkTheme())
    Row(
        modifier.fillMaxWidth().padding(horizontal = ScreenMargin, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Palettes.forEach { spec ->
            PaletteCard(spec, if (dark) spec.dark else spec.light, spec.palette == selected, Modifier.weight(1f)) { onSelect(spec.palette) }
        }
    }
}

@Composable
private fun PaletteCard(spec: PaletteSpec, t: Tokens, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val ring = MaterialTheme.colorScheme.primary
    Column(
        modifier
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = spec.name + if (selected) ", selected" else "" },
    ) {
        Box(
            Modifier.fillMaxWidth().height(228.dp)
                .border(if (selected) BorderStroke(2.dp, ring) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    RoundedCornerShape(22.dp))
                .padding(if (selected) 5.dp else 4.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(t.background),
        ) {
            MiniHome(spec.palette, t)
            if (selected) Box(
                Modifier.align(Alignment.TopEnd).padding(8.dp).size(20.dp).background(ring, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(YogaIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(12.dp)) }
        }
        Text(spec.name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 10.dp))
        Text(spec.line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun MiniHome(palette: Palette, t: Tokens) {
    val textStyle = typographyFor(palette).bodyMedium
    Column(Modifier.fillMaxSize().padding(PaddingValues(start = 10.dp, end = 8.dp, top = 26.dp, bottom = 10.dp))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("10", style = clockStyleFor(palette, 28), color = t.text)
            Column(Modifier.padding(horizontal = 2.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(2.5.dp).background(t.accent, CircleShape))
                Box(Modifier.size(2.5.dp).background(t.text.copy(alpha = 0.82f), CircleShape))
            }
            Text("10", style = clockStyleFor(palette, 28), color = t.text)
        }
        Text("Friday, 6 Nov", style = textStyle.copy(fontSize = 8.sp), color = t.muted)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(1.5.dp).background(t.line)) {
            Box(Modifier.fillMaxWidth(0.55f).height(1.5.dp).background(t.accent))
        }
        Spacer(Modifier.weight(1f))
        listOf("Messages", "Maps", "Notes", "Calendar").forEach {
            Text(it, style = textStyle.copy(fontSize = 11.sp), color = t.text, maxLines = 1, modifier = Modifier.padding(vertical = 1.5.dp))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Box(Modifier.width(58.dp).height(17.dp).background(t.high, CircleShape).border(1.dp, t.line, CircleShape),
                contentAlignment = Alignment.Center) {
                Text("Search", style = textStyle.copy(fontSize = 7.sp), color = t.muted)
            }
        }
    }
}
