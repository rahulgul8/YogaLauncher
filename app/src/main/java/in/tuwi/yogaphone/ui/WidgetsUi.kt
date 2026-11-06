package `in`.tuwi.yogaphone.ui

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import `in`.tuwi.yogaphone.ui.theme.*
import `in`.tuwi.yogaphone.widgets.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeWidgets(widgets: List<WidgetEntity>, repository: WidgetRepository, hiddenReason: String?) {
    if (widgets.isEmpty()) return
    if (hiddenReason != null) {
        Text("Widgets hidden $hiddenReason", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
        return
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        widgets.forEach { widget -> HostedWidget(widget, repository) }
    }
}

@Composable
private fun HostedWidget(widget: WidgetEntity, repository: WidgetRepository) {
    val info = remember(widget.appWidgetId) { repository.info(widget.appWidgetId) } ?: return
    BoxWithConstraints(Modifier.fillMaxWidth().height(widget.heightDp.dp)) {
        val widthDp = maxWidth.value
        val heightDp = widget.heightDp.toFloat()
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context -> repository.host.createView(context.applicationContext, widget.appWidgetId, info).apply { setPadding(0, 0, 0, 0) } },
            update = { view -> view.updateSize(widthDp, heightDp) },
        )
    }
}

private fun AppWidgetHostView.updateSize(widthDp: Float, heightDp: Float) {
    runCatching {
        if (Build.VERSION.SDK_INT >= 31) updateAppWidgetSize(Bundle(), listOf(SizeF(widthDp, heightDp)))
        else { @Suppress("DEPRECATION") updateAppWidgetSize(null, widthDp.toInt(), heightDp.toInt(), widthDp.toInt(), heightDp.toInt()) }
    }
}

@Composable
fun WidgetsSettingsScreen(widgets: List<WidgetEntity>, repository: WidgetRepository, onAdd: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<Pair<WidgetEntity, Job>?>(null) }
    val shown = widgets.filter { it.appWidgetId != pending?.first?.appWidgetId }
    Box(Modifier.fillMaxSize()) {
        YogaScreen("Widgets", onBack) {
            item { YogaNote("Widgets sit in one column under the clock. They step aside during focus and in profiles that tuck apps away.") }
            items(shown, key = { it.appWidgetId }) { widget ->
                val label = remember(widget.appWidgetId) { repository.info(widget.appWidgetId)?.loadLabel(context.packageManager) ?: "Widget" }
                YogaSection(label)
                YogaChipRow {
                    WidgetLayout.presets.forEach { (name, dp) ->
                        YogaChip(name, widget.heightDp == dp, onClick = { scope.launch { repository.resize(widget, dp) } })
                    }
                }
                Row(Modifier.padding(horizontal = ScreenMargin), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    YogaTextButton("Up", { scope.launch { repository.move(widget, -1) } }, enabled = shown.first() != widget)
                    YogaTextButton("Down", { scope.launch { repository.move(widget, 1) } }, enabled = shown.last() != widget)
                    YogaTextButton("Remove", {
                        pending?.second?.cancel()
                        pending = widget to scope.launch { delay(5_000); repository.remove(widget.appWidgetId); pending = null }
                    }, muted = true)
                }
            }
            item { YogaPrimaryButton("Add a widget", onAdd, Modifier.padding(horizontal = ScreenMargin, vertical = 20.dp)) }
        }
        pending?.let { (_, job) ->
            Snackbar(action = { YogaTextButton("Undo", { job.cancel(); pending = null }) },
                modifier = Modifier.align(Alignment.BottomCenter).padding(ScreenMargin)) { Text("Widget removed") }
        }
    }
}

@Composable
fun WidgetPickerScreen(repository: WidgetRepository, onPick: (AppWidgetProviderInfo) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val pm = context.packageManager
    var query by remember { mutableStateOf("") }
    val providers = remember { repository.providers() }
    val labelled = remember(providers) {
        providers.map { info ->
            val app = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, 0)).toString() }.getOrDefault(info.provider.packageName)
            Triple(app, info.loadLabel(pm), info)
        }.sortedWith(compareBy({ it.first.lowercase() }, { it.second.lowercase() }))
    }
    val density = context.resources.displayMetrics.densityDpi
    YogaScreen("Add a widget", onBack) {
        item { YogaTextField(query, { query = it }, "Find a widget") }
        if (labelled.isEmpty()) item { YogaNote("No widgets are installed.") }
        items(labelled.filter { (app, label, _) -> app.contains(query, true) || label.contains(query, true) },
            key = { it.third.provider.flattenToString() }) { (app, label, info) ->
            val icon = remember(info.provider) {
                runCatching { (info.loadPreviewImage(context, density) ?: info.loadIcon(context, density))?.toBitmap(96, 96)?.asImageBitmap() }.getOrNull()
            }
            YogaRow(label, summary = app, onClick = { onPick(info) }, trailing = {
                icon?.let { Image(it, contentDescription = null, modifier = Modifier.size(48.dp)) } ?: RowChevron()
            })
        }
    }
}
