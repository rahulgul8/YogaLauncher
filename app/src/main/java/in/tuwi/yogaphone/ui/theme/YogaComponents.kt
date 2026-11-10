package `in`.tuwi.yogaphone.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Locale

/*
 * Yoga's component kit. Every screen builds from these so the app reads as one object:
 * display-face titles, uppercase section labels, generous rows with hairlines, monochrome pills,
 * and brass only for the one thing that is "on" or primary. See docs/design/brand.md.
 */

val ScreenMargin: Dp = 24.dp

/** A full settings-style screen: back arrow, display title, optional lede, then the content. */
@Composable
fun YogaScreen(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 48.dp)) {
        item(key = "yoga-header") { YogaHeader(title, onBack, eyebrow, subtitle) }
        content()
    }
}

@Composable
fun YogaHeader(title: String, onBack: (() -> Unit)?, eyebrow: String? = null, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = ScreenMargin).padding(top = 4.dp, bottom = 12.dp)) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
                Icon(YogaIcons.Back, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(22.dp))
            }
        } else Spacer(Modifier.height(48.dp))
        Spacer(Modifier.height(20.dp))
        if (eyebrow != null) YogaEyebrow(eyebrow, Modifier.padding(bottom = 10.dp))
        Text(title, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
    }
}

/** Small uppercase label in brass, used above a title or a hero number. */
@Composable
fun YogaEyebrow(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(Locale.getDefault()), style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary, modifier = modifier)
}

/** Section label between groups of rows. */
@Composable
fun YogaSection(title: String, modifier: Modifier = Modifier) {
    Text(title.uppercase(Locale.getDefault()), style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth().padding(start = ScreenMargin, end = ScreenMargin, top = 32.dp, bottom = 8.dp))
}

@Composable
fun YogaHairline(modifier: Modifier = Modifier, inset: Dp = ScreenMargin) {
    HorizontalDivider(modifier.padding(start = inset, end = inset), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

/** A tappable row: optional icon, title, optional summary, and a trailing chevron or custom content. */
@Composable
fun YogaRow(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = if (onClick != null) { { RowChevron() } } else null,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 64.dp)
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
            .padding(horizontal = ScreenMargin, vertical = 14.dp)
            .alpha(if (enabled) 1f else 0.45f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(18.dp))
        }
        RowText(title, summary, Modifier.weight(1f))
        if (trailing != null) { Spacer(Modifier.width(16.dp)); trailing() }
    }
}

@Composable
private fun RowText(title: String, summary: String?, modifier: Modifier) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onBackground)
        if (summary != null) Text(summary, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun RowChevron() {
    Icon(YogaIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
}

/** A row whose whole surface toggles a switch. */
@Composable
fun YogaSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 64.dp)
            .toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = ScreenMargin, vertical = 14.dp)
            .alpha(if (enabled) 1f else 0.45f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(18.dp))
        }
        RowText(title, summary, Modifier.weight(1f))
        Spacer(Modifier.width(16.dp))
        YogaSwitch(checked, null, enabled = enabled)
    }
}

/** Brass when on, a hairline outline when off. Pass a null handler when the parent row toggles it. */
@Composable
fun YogaSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, enabled: Boolean = true) {
    val c = MaterialTheme.colorScheme
    Switch(
        checked = checked, onCheckedChange = onCheckedChange, enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = c.onPrimary, checkedTrackColor = c.primary, checkedBorderColor = c.primary,
            uncheckedThumbColor = c.onSurfaceVariant, uncheckedTrackColor = Color.Transparent, uncheckedBorderColor = c.outline,
        ),
    )
}

/** One choice in a single-choice list; the whole row selects. */
@Composable
fun YogaRadioRow(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, summary: String? = null) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 64.dp)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = ScreenMargin, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        YogaRadio(selected)
        Spacer(Modifier.width(16.dp))
        RowText(label, summary, Modifier.weight(1f))
    }
}

@Composable
fun YogaRadio(selected: Boolean) {
    val c = MaterialTheme.colorScheme
    Box(
        Modifier.size(20.dp).border(1.5.dp, if (selected) c.primary else c.outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(10.dp).background(c.primary, CircleShape))
    }
}

/** Primary action: a solid ivory (dark) or ink (light) pill. One per screen. */
@Composable
fun YogaPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick, enabled = enabled, shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.onBackground, contentColor = MaterialTheme.colorScheme.background,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        contentPadding = PaddingValues(horizontal = 28.dp),
        modifier = modifier.heightIn(min = 52.dp),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

/** Secondary action: a hairline pill. */
@Composable
fun YogaSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick, enabled = enabled, shape = CircleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
        contentPadding = PaddingValues(horizontal = 24.dp),
        modifier = modifier.heightIn(min = 52.dp),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

/** Side padding that lines a standalone YogaTextButton's label up with the 24 dp margin (TextButton adds 12 dp inside). */
val TextButtonInset = ScreenMargin - 12.dp

/** Quiet text action, in brass. */
@Composable
fun YogaTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, muted: Boolean = false) {
    TextButton(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge,
            color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
    }
}

/** A choice pill. Selected pills are filled and edged in brass. */
@Composable
fun YogaChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = MaterialTheme.colorScheme
    Surface(
        selected = selected, onClick = onClick, enabled = enabled, shape = CircleShape,
        color = if (selected) c.surfaceContainerHigh else Color.Transparent,
        border = BorderStroke(1.dp, if (selected) c.primary else c.outlineVariant),
        modifier = modifier.heightIn(min = 40.dp),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selected) { Box(Modifier.size(5.dp).background(c.primary, CircleShape)); Spacer(Modifier.width(8.dp)) }
            Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) c.onBackground else c.onSurfaceVariant)
        }
    }
}

/** A row of chips that wraps. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun YogaChipRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        modifier.fillMaxWidth().padding(horizontal = ScreenMargin, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

/** A raised panel for a summary or a hero number. No border, no shadow. */
@Composable
fun YogaCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = ScreenMargin)
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
            .padding(20.dp),
        content = content,
    )
}

/**
 * A rounded panel that holds a short list of [YogaGroupRow]s, iOS-settings style but in Yoga's tones:
 * raised surface, generous corners, hairlines inset past the icon.
 */
@Composable
fun YogaGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = ScreenMargin)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        content = content,
    )
}

/** A soft accent tile behind a line icon: the visual cue that starts every group row. */
@Composable
fun YogaIconTile(icon: ImageVector, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.primary) {
    Box(
        modifier.size(38.dp).background(tint.copy(alpha = 0.13f), MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp)) }
}

/** One row inside a [YogaGroup]: icon tile, title, a few words of summary, then a chevron or custom trailing content. */
@Composable
fun YogaGroupRow(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    summary: String? = null,
    divider: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = if (onClick != null) { { RowChevron() } } else null,
) {
    Column(modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            YogaIconTile(icon)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onBackground)
                if (summary != null) Text(summary, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 1.dp))
            }
            if (trailing != null) { Spacer(Modifier.width(12.dp)); trailing() }
        }
        if (divider) HorizontalDivider(Modifier.padding(start = 68.dp), thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    }
}


/** Small explanatory text under a group. */
@Composable
fun YogaNote(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth().padding(horizontal = ScreenMargin, vertical = 8.dp))
}

/** Underlined text field with a small uppercase label. */
@Composable
fun YogaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    prefix: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val c = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().padding(horizontal = ScreenMargin, vertical = 8.dp)) {
        if (label.isNotBlank()) Text(label.uppercase(Locale.getDefault()), style = MaterialTheme.typography.labelSmall, color = c.onSurfaceVariant)
        Row(Modifier.padding(top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            val style = MaterialTheme.typography.titleLarge.copy(color = c.onBackground, fontWeight = FontWeight.Normal)
            leadingIcon?.let { icon -> Box(Modifier.padding(end = 12.dp)) { icon() } }
            if (prefix != null) Text(prefix, style = style, color = c.onSurfaceVariant, modifier = Modifier.padding(end = 6.dp))
            Box(Modifier.weight(1f)) {
                if (value.isEmpty() && placeholder != null) Text(placeholder, style = style, color = c.outline)
                BasicTextField(value, onValueChange, singleLine = singleLine, textStyle = style,
                    cursorBrush = SolidColor(c.primary), keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
                    visualTransformation = visualTransformation, modifier = Modifier.fillMaxWidth())
            }
            trailingIcon?.invoke()
        }
        HorizontalDivider(thickness = 1.dp, color = c.outline)
    }
}

/** A big number with a caption, for dashboards. */
@Composable
fun YogaStat(value: String, caption: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "lnum, tnum"),
            color = MaterialTheme.colorScheme.onBackground)
        Text(caption.uppercase(Locale.getDefault()), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
    }
}

/** A list row with a round brass check on the left. */
@Composable
fun YogaCheckRow(label: String, checked: Boolean, enabled: Boolean = true, onToggle: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(enabled = enabled, role = Role.Checkbox, onClick = onToggle)
            .padding(horizontal = ScreenMargin, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(22.dp).then(if (checked) Modifier.background(c.primary, CircleShape) else Modifier.border(1.5.dp, c.outline, CircleShape)),
            contentAlignment = Alignment.Center,
        ) { if (checked) Icon(YogaIcons.Check, contentDescription = null, tint = c.onPrimary, modifier = Modifier.size(14.dp)) }
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) c.onBackground else c.onSurfaceVariant)
    }
}

/** A restrained value control: brass for the chosen amount and a hairline for the remainder. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YogaSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val c = MaterialTheme.colorScheme
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        colors = SliderDefaults.colors(
            thumbColor = c.primary,
            activeTrackColor = c.primary,
            inactiveTrackColor = c.outlineVariant,
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent,
        ),
        track = { state -> SliderDefaults.Track(state, modifier = Modifier.height(2.dp), colors = SliderDefaults.colors(
            activeTrackColor = c.primary, inactiveTrackColor = c.outlineVariant,
            activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent)) },
    )
}

/** Branded modal surface shared by every bottom sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YogaSheet(onDismissRequest: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        // Open fully: a half-open sheet hides the primary button of taller sheets below the screen.
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onBackground,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(Modifier.padding(vertical = 12.dp).size(width = 36.dp, height = 2.dp)
                .background(MaterialTheme.colorScheme.outlineVariant, CircleShape))
        },
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) { content() }
    }
}

/** Branded confirmation dialog with one clear primary action. */
@Composable
fun YogaDialog(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    dismissText: String = "Cancel",
    onDismissAction: () -> Unit = onDismissRequest,
    confirmEnabled: Boolean = true,
    text: (@Composable () -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = text,
        confirmButton = { YogaPrimaryButton(confirmText, onConfirm, enabled = confirmEnabled) },
        dismissButton = { YogaTextButton(dismissText, onDismissAction, muted = true) },
    )
}

/** Two-pixel usage hairline. Ember is reserved for values beyond the allowance. */
@Composable
fun YogaProgress(progress: Float, modifier: Modifier = Modifier, overLimit: Boolean = false) {
    val c = MaterialTheme.colorScheme
    Box(modifier.fillMaxWidth().height(2.dp).background(c.outlineVariant, CircleShape)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(2.dp)
            .background(if (overLimit) c.tertiary else c.primary, CircleShape))
    }
}
