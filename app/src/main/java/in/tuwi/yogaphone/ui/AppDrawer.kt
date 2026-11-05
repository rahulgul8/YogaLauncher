package `in`.tuwi.yogaphone.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TextButton
import `in`.tuwi.yogaphone.data.AppModel
import `in`.tuwi.yogaphone.logic.SearchableApp
import `in`.tuwi.yogaphone.logic.rankAppSearch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerBottomSheet(
    apps: List<AppModel>,
    recentSearches: List<String>,
    fontSize: Int,
    onAppClick: (AppModel, String) -> Unit,
    onAppLongClick: (AppModel) -> Unit,
    onDismiss: () -> Unit,
    replacingApp: AppModel? = null,
    isPaused: (AppModel) -> Boolean = { false },
    /** Section title for each app when the drawer is grouped by category; null keeps one alphabetical list. */
    groupOf: ((AppModel) -> String)? = null,
    /** Apps the active profile tucks away; reachable with "All apps". */
    otherApps: List<AppModel> = emptyList(),
    modifier: Modifier = Modifier,
) {
    var showPaused by remember { mutableStateOf(false) }
    var showOthers by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val searchable = if (query.isNotBlank() || showOthers) apps + otherApps else apps
    val matches = remember(searchable, query) {
        rankAppSearch(searchable.map { SearchableApp(it.displayLabel, it) }, query)
    }

    LaunchedEffect(Unit) { delay(150); focusRequester.requestFocus(); keyboard?.show() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier,
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                placeholder = { Text(if (replacingApp == null) "Search apps" else "Replace ${replacingApp.displayLabel}") },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    matches.firstOrNull()?.let { onAppClick(it, query) }
                }, onDone = { matches.firstOrNull()?.let { onAppClick(it, query) } }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
            )
            if (query.isBlank() && recentSearches.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
                ) {
                    recentSearches.forEach { term ->
                        AssistChip(
                            onClick = { query = term },
                            label = { Text(term) },
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                }
            }
            LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                // While focus is on, paused apps sit in a collapsed section unless the user searches for them.
                val grouped = query.isBlank() && replacingApp == null
                val visible = if (grouped) matches.filterNot(isPaused) else matches
                val paused = if (grouped) matches.filter(isPaused) else emptyList()
                val sections = if (grouped && groupOf != null) visible.groupBy(groupOf).toSortedMap().toList()
                else listOf("" to visible)
                sections.forEach { (title, sectionApps) ->
                    if (title.isNotEmpty()) item(key = "section:$title") {
                        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 16.dp, bottom = 4.dp))
                    }
                    items(sectionApps, key = { "${it.appPackage}:${it.activityClassName}" }) { app ->
                        DrawerAppRow(
                            app = app,
                            fontSize = fontSize,
                            highlighted = query.isNotBlank() && matches.size == 1,
                            deemphasized = isPaused(app),
                            onClick = { onAppClick(app, query) },
                            onLongClick = { if (replacingApp == null) onAppLongClick(app) },
                        )
                    }
                }
                if (query.isBlank() && otherApps.isNotEmpty() && !showOthers) item(key = "all-apps") {
                    TextButton(onClick = { showOthers = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text("All apps (${otherApps.size} more)", color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth())
                    }
                }
                if (paused.isNotEmpty()) {
                    item(key = "paused-header") {
                        TextButton(onClick = { showPaused = !showPaused }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text((if (showPaused) "Hide" else "Show") + " ${paused.size} paused during focus",
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    if (showPaused) items(paused, key = { "paused:${it.appPackage}:${it.activityClassName}" }) { app ->
                        DrawerAppRow(app = app, fontSize = fontSize, highlighted = false, deemphasized = true,
                            onClick = { onAppClick(app, query) }, onLongClick = { onAppLongClick(app) })
                    }
                }
                if (matches.isEmpty()) item {
                    Text(
                        "No apps found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerAppRow(
    app: AppModel,
    fontSize: Int,
    highlighted: Boolean,
    deemphasized: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Surface(
        color = if (highlighted) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Text(app.displayLabel, fontSize = fontSize.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            color = if (deemphasized) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f) else MaterialTheme.colorScheme.onSurface)
    }
}
