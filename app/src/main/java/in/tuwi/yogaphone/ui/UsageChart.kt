package `in`.tuwi.yogaphone.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.ui.theme.ScreenMargin
import `in`.tuwi.yogaphone.usage.UsageDetails

/**
 * A quiet bar chart for the Usage view: one bar per hour or day, an optional allowance hairline, and a label under
 * any bar whose [labels] entry is not blank. Tapping a bar calls [onSelect] with its index. A null value is a day
 * that wasn't recorded and is drawn as a small dot, never as zero.
 */
@Composable
fun UsageBarChart(
    values: List<Long?>,
    labels: List<String>,
    selected: Int?,
    reference: Long?,
    description: String,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colours = MaterialTheme.colorScheme
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = colours.onSurfaceVariant)
    val measurer = rememberTextMeasurer()
    val fractions = UsageDetails.barFractions(values, reference)
    val line = UsageDetails.referenceFraction(values, reference)
    val count = values.size.coerceAtLeast(1)
    val select by rememberUpdatedState(onSelect)
    Canvas(
        modifier.fillMaxWidth().padding(horizontal = ScreenMargin).height(156.dp)
            .semantics { contentDescription = description }
            .pointerInput(count) {
                detectTapGestures { offset ->
                    select((offset.x / (size.width.toFloat() / count)).toInt().coerceIn(0, count - 1))
                }
            },
    ) {
        val labelSpace = 22.dp.toPx()
        val plot = size.height - labelSpace
        val slot = size.width / count
        val barWidth = (slot * if (count > 12) 0.62f else 0.5f).coerceAtMost(28.dp.toPx())
        val minimum = 2.dp.toPx()
        drawLine(colours.outlineVariant, Offset(0f, plot), Offset(size.width, plot), strokeWidth = 1.dp.toPx())
        fractions.forEachIndexed { index, fraction ->
            val centre = slot * index + slot / 2
            val active = selected == null || selected == index
            if (fraction == null) {
                drawCircle(colours.outlineVariant, radius = 2.dp.toPx(), center = Offset(centre, plot - 4.dp.toPx()))
            } else if (fraction > 0f) {
                val height = (plot * fraction).coerceAtLeast(minimum)
                drawRoundRect(
                    color = if (active) colours.primary else colours.primary.copy(alpha = 0.32f),
                    topLeft = Offset(centre - barWidth / 2, plot - height),
                    size = Size(barWidth, height),
                    cornerRadius = CornerRadius(minOf(barWidth / 2, 4.dp.toPx())),
                )
            }
            val label = labels.getOrNull(index).orEmpty()
            if (label.isNotBlank()) {
                val measured = measurer.measure(label, labelStyle.copy(
                    color = if (selected == index) colours.onBackground else colours.onSurfaceVariant))
                drawText(measured, topLeft = Offset(
                    (centre - measured.size.width / 2f).coerceIn(0f, size.width - measured.size.width),
                    plot + 6.dp.toPx()))
            }
        }
        line?.let { fraction ->
            val y = plot - plot * fraction
            drawLine(colours.tertiary.copy(alpha = 0.8f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())))
        }
    }
}
