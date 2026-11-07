package `in`.tuwi.yogaphone.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.R
import `in`.tuwi.yogaphone.ui.theme.ScreenMargin

/** The app-list icons Yoga can show. Each is an activity-alias in the manifest; exactly one is enabled. */
/** Activity-alias names resolve against the code namespace, which differs from the app id (com.tuwi.yogaphone). */
private const val CODE_PACKAGE = "in.tuwi.yogaphone"

enum class AppIcon(val alias: String, val label: String, val note: String, val foreground: Int) {
    TEN_PAST_TEN(".IconTenPastTen", "Ten past ten", "A dial whose hands form a Y", R.drawable.ic_launcher_foreground),
    COLON(".IconColon", "The colon", "The clock's colon, a pause", R.drawable.ic_launcher_colon_foreground),
}

object AppIcons {
    private fun component(context: Context, icon: AppIcon) = ComponentName(context.packageName, CODE_PACKAGE + icon.alias)

    fun current(context: Context): AppIcon = AppIcon.entries.firstOrNull { icon ->
        when (context.packageManager.getComponentEnabledSetting(component(context, icon))) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> icon == AppIcon.TEN_PAST_TEN // the manifest default
            else -> false
        }
    } ?: AppIcon.TEN_PAST_TEN

    /** Enables the chosen entry first, then disables the other, so there is never a moment with no app-list entry. */
    fun set(context: Context, choice: AppIcon) {
        val pm = context.packageManager
        pm.setComponentEnabledSetting(component(context, choice), PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP)
        AppIcon.entries.filter { it != choice }.forEach {
            pm.setComponentEnabledSetting(component(context, it), PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP)
        }
    }
}

/** Two icon cards, each shown as a circle and a squircle the way phones mask adaptive icons. */
@Composable
fun AppIconPicker(selected: AppIcon, onSelect: (AppIcon) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = ScreenMargin, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AppIcon.entries.forEach { icon ->
            val isSelected = icon == selected
            val c = MaterialTheme.colorScheme
            Column(
                Modifier.weight(1f)
                    .border(if (isSelected) BorderStroke(2.dp, c.primary) else BorderStroke(1.dp, c.outlineVariant), RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .selectable(isSelected, role = Role.RadioButton) { onSelect(icon) }
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconPreview(icon, 60.dp, CircleShape)
                    IconPreview(icon, 44.dp, RoundedCornerShape(13.dp))
                }
                Text(icon.label, style = MaterialTheme.typography.labelLarge, color = c.onBackground, modifier = Modifier.padding(top = 14.dp))
                Text(icon.note, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

/** Draws an adaptive icon: the 108-unit foreground, of which the central 72 units show through the mask. */
@Composable
private fun IconPreview(icon: AppIcon, size: Dp, shape: Shape) {
    Box(Modifier.size(size).clip(shape).background(androidx.compose.ui.graphics.Color(0xFF0F0F0E))
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape), contentAlignment = Alignment.Center) {
        Image(painterResource(icon.foreground), contentDescription = null, modifier = Modifier.requiredSize(size * 1.5f))
    }
}
