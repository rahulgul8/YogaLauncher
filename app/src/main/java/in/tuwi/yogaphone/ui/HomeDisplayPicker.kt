package `in`.tuwi.yogaphone.ui

import `in`.tuwi.yogaphone.ui.theme.ScreenMargin
import `in`.tuwi.yogaphone.ui.theme.YogaEyebrow
import `in`.tuwi.yogaphone.usage.HomeDisplayStyle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

fun homeDisplayStyleLabel(style: HomeDisplayStyle): String = style.name.lowercase().replaceFirstChar { it.titlecase() }

private val sampleHomeUsageNumbers = HomeUsageNumbers(
    todayMinutes = 72,
    allowanceMinutes = 90,
    over = false,
    excessMinutes = 0,
    week = listOf(45, 80, 62, 95, 70, 40, 72),
)

/** One row per style with a live preview; [numbers] null means sample numbers, labelled "Sample". */
@Composable
fun HomeDisplayPicker(selected: HomeDisplayStyle, numbers: HomeUsageNumbers?, onSelect: (HomeDisplayStyle) -> Unit) {
    val previewNumbers = numbers ?: sampleHomeUsageNumbers
    Column(Modifier.fillMaxWidth().padding(horizontal = ScreenMargin), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (numbers == null) YogaEyebrow("Sample", Modifier.padding(bottom = 2.dp))
        HomeDisplayStyle.entries.forEach { style ->
            val isSelected = style == selected
            Surface(
                onClick = { onSelect(style) },
                shape = MaterialTheme.shapes.medium,
                color = Color.Transparent,
                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(homeDisplayStyleLabel(style), style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(top = 8.dp)) {
                        if (style == HomeDisplayStyle.OFF) {
                            Text("Nothing on home", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Box(Modifier.fillMaxWidth().graphicsLayer {
                                scaleX = 0.82f
                                scaleY = 0.82f
                                transformOrigin = TransformOrigin(0f, 0f)
                            }) { HomeUsageStyle(style, previewNumbers) }
                        }
                    }
                }
            }
        }
    }
}
