package `in`.tuwi.yogaphone.ui

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import `in`.tuwi.yogaphone.widgets.WidgetEntity
import `in`.tuwi.yogaphone.widgets.WidgetLayout
import `in`.tuwi.yogaphone.widgets.WidgetRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** One calm column of widgets on home. */
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
            factory = { context ->
                repository.host.createView(context.applicationContext, widget.appWidgetId, info).apply { setPadding(0, 0, 0, 0) }
            },
            update = { view -> view.updateSize(widthDp, heightDp) },
        )
    }
}

private fun AppWidgetHostView.updateSize(widthDp: Float, heightDp: Float) {
    runCatching {
        if (Build.VERSION.SDK_INT >= 31) {
            updateAppWidgetSize(Bundle(), listOf(SizeF(widthDp, heightDp)))
        } else {
            @Suppress("DEPRECATION")
            updateAppWidgetSize(null, widthDp.toInt(), heightDp.toInt(), widthDp.toInt(), heightDp.toInt())
        }
    }
}

@Composable
fun WidgetsSettingsScreen(widgets: List<WidgetEntity>, repository: WidgetRepository, onAdd: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<Pair<WidgetEntity, Job>?>(null) }
    val shown = widgets.filter { it.appWidgetId != pending?.first?.appWidgetId }
    Box(Modifier.fillMaxSize()) {
        LazyColumn {
            item { SettingsHeader("Widgets", onBack) }
            item {
                Text("Widgets sit in one column under the clock. They step aside during focus and in profiles that tuck apps away.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            items(shown, key = { it.appWidgetId }) { widget ->
                val label = remember(widget.appWidgetId) {
                    repository.info(widget.appWidgetId)?.loadLabel(context.packageManager) ?: "Widget"
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(label, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        WidgetLayout.presets.forEach { (name, dp) ->
                            FilterChip(selected = widget.heightDp == dp, onClick = { scope.launch { repository.resize(widget, dp) } },
                                label = { Text(name) })
                        }
                    }
                    Row {
                        TextButton(onClick = { scope.launch { repository.move(widget, -1) } }, enabled = shown.first() != widget) { Text("Up") }
                        TextButton(onClick = { scope.launch { repository.move(widget, 1) } }, enabled = shown.last() != widget) { Text("Down") }
                        TextButton(onClick = {
                            pending?.second?.cancel()
                            pending = widget to scope.launch { delay(5_000); repository.remove(widget.appWidgetId); pending = null }
                        }) { Text("Remove") }
                    }
                }
            }
            item { TextButton(onClick = onAdd, modifier = Modifier.padding(8.dp)) { Text("Add a widget") } }
        }
        pending?.let { (_, job) ->
            Snackbar(action = { TextButton(onClick = { job.cancel(); pending = null }) { Text("Undo") } },
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) { Text("Widget removed") }
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
            val app = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, 0)).toString() }
                .getOrDefault(info.provider.packageName)
            Triple(app, info.loadLabel(pm), info)
        }.sortedWith(compareBy({ it.first.lowercase() }, { it.second.lowercase() }))
    }
    val density = context.resources.displayMetrics.densityDpi
    LazyColumn {
        item { SettingsHeader("Add a widget", onBack) }
        item {
            OutlinedTextField(query, { query = it }, label = { Text("Find a widget") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        }
        if (labelled.isEmpty()) item { Text("No widgets are installed.", modifier = Modifier.padding(16.dp)) }
        items(labelled.filter { (app, label, _) -> app.contains(query, true) || label.contains(query, true) },
            key = { it.third.provider.flattenToString() }) { (app, label, info) ->
            val icon = remember(info.provider) {
                runCatching { (info.loadPreviewImage(context, density) ?: info.loadIcon(context, density))?.toBitmap(96, 96)?.asImageBitmap() }.getOrNull()
            }
            ListItem(
                headlineContent = { Text(label) },
                supportingContent = { Text(app) },
                leadingContent = { icon?.let { Image(it, contentDescription = null, modifier = Modifier.size(48.dp)) } },
                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable { onPick(info) },
            )
        }
    }
}
