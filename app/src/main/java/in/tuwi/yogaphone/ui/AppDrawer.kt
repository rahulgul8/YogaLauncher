package `in`.tuwi.yogaphone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import `in`.tuwi.yogaphone.data.AppModel
import `in`.tuwi.yogaphone.logic.SearchableApp
import `in`.tuwi.yogaphone.logic.rankAppSearch
import `in`.tuwi.yogaphone.ui.theme.YogaIcons
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Position of the full-screen app drawer. [progress] is 1 when closed (off the bottom of the screen)
 * and 0 when open. Dragging writes it directly, so the drawer stays under the finger; releasing
 * settles it with a critically damped spring that carries the finger's velocity.
 */
@Stable
class DrawerController internal constructor(private val scope: CoroutineScope) {
    var progress by mutableFloatStateOf(1f)
        private set
    /** True once an open animation has finished; the keyboard waits for this so it never fights the slide. */
    var settledOpen by mutableStateOf(false)
        private set
    /** Opened from Search: show the keyboard once settled. A swipe opens the list for browsing instead. */
    var forSearch by mutableStateOf(false)
        private set
    internal var heightPx = 1f
    private var job: Job? = null

    val isClosed: Boolean get() = progress >= 1f

    fun dragBy(deltaPx: Float) {
        if (progress >= 1f) forSearch = false
        job?.cancel()
        settledOpen = false
        progress = (progress + deltaPx / heightPx).coerceIn(0f, 1f)
    }

    fun release(velocityPxPerSecond: Float) {
        val fractionPerSecond = velocityPxPerSecond / heightPx
        val open = when {
            fractionPerSecond < -0.6f -> true
            fractionPerSecond > 0.6f -> false
            else -> progress < 0.55f
        }
        if (open && progress >= 0.99f) forSearch = false
        settle(if (open) 0f else 1f, fractionPerSecond)
    }

    fun open(search: Boolean = false) { forSearch = search; settle(0f, 0f) }
    fun close() = settle(1f, 0f)
    fun snapClosed() { job?.cancel(); settledOpen = false; progress = 1f }

    private fun settle(target: Float, velocity: Float) {
        job?.cancel()
        if (target != 0f) settledOpen = false
        job = scope.launch {
            animate(progress, target, velocity, spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow)) { value, _ ->
                progress = value
            }
            settledOpen = target == 0f
        }
    }
}

@Composable
fun rememberDrawerController(): DrawerController {
    val scope = rememberCoroutineScope()
    return remember { DrawerController(scope) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawerLayer(
    controller: DrawerController,
    apps: List<AppModel>,
    recentSearches: List<String>,
    fontSize: Int,
    onAppClick: (AppModel, String) -> Unit,
    onAppLongClick: (AppModel) -> Unit,
    /** Called once the drawer is fully closed, so the caller can reset any pick/replace mode. */
    onClosed: () -> Unit,
    /** Shown instead of "Search apps" while choosing an app for some purpose. */
    pickingTitle: String? = null,
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
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val searchable = if (query.isNotBlank() || showOthers) apps + otherApps else apps
    val matches = remember(searchable, query) {
        rankAppSearch(searchable.map { SearchableApp(it.displayLabel, it) }, query)
    }
    val closed by remember { derivedStateOf { controller.isClosed } }

    LaunchedEffect(controller.settledOpen) {
        if (controller.settledOpen && controller.forSearch) { runCatching { focusRequester.requestFocus() }; keyboard?.show() }
    }
    LaunchedEffect(closed) {
        if (closed) {
            keyboard?.hide(); focusManager.clearFocus(force = true)
            query = ""; showOthers = false; showPaused = false
            listState.scrollToItem(0)
            onClosed()
        }
    }
    BackHandler(enabled = !closed) { controller.close() }

    // Pull down past the top of the list to close; pushing back up re-opens before the list scrolls.
    val connection = remember(controller) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y < 0f && controller.progress > 0f) {
                    controller.dragBy(available.y); return Offset(0f, available.y)
                }
                return Offset.Zero
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    keyboard?.hide(); controller.dragBy(available.y); return Offset(0f, available.y)
                }
                return Offset.Zero
            }
            override suspend fun onPreFling(available: Velocity): Velocity {
                if (controller.progress > 0f) { controller.release(available.y); return available }
                return Velocity.Zero
            }
        }
    }
    val headerDrag = rememberDraggableState { delta -> if (delta > 0f || controller.progress > 0f) controller.dragBy(delta) }

    Box(
        modifier
            .fillMaxSize()
            .onSizeChanged { controller.heightPx = it.height.toFloat().coerceAtLeast(1f) }
            .graphicsLayer {
                translationY = controller.progress * size.height
                alpha = if (controller.progress >= 1f) 0f else 1f
            }
            .background(MaterialTheme.colorScheme.background)
            .nestedScroll(connection),
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Column(
                Modifier.fillMaxWidth()
                    .draggable(headerDrag, Orientation.Vertical, onDragStopped = { controller.release(it) })
                    .padding(start = 28.dp, end = 16.dp, top = 12.dp),
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 18.dp).size(width = 36.dp, height = 4.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant, CircleShape))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(YogaIcons.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(14.dp))
                    Box(Modifier.weight(1f)) {
                        val textStyle = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Normal)
                        if (query.isEmpty()) Text(pickingTitle ?: "Search apps", style = textStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = textStyle,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = { matches.firstOrNull()?.let { onAppClick(it, query) } }),
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        )
                    }
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                        Icon(YogaIcons.Close, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp))
                    } else Spacer(Modifier.size(48.dp))
                }
                HorizontalDivider(Modifier.padding(top = 10.dp, end = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                if (query.isBlank() && recentSearches.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        recentSearches.forEach { term ->
                            Surface(
                                onClick = { query = term },
                                shape = CircleShape,
                                color = Color.Transparent,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            ) {
                                Text(term, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                            }
                        }
                    }
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(top = 8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            ) {
                // While focus is on, paused apps sit in a collapsed section unless the user searches for them.
                val grouped = query.isBlank() && pickingTitle == null
                val visible = if (grouped) matches.filterNot(isPaused) else matches
                val paused = if (grouped) matches.filter(isPaused) else emptyList()
                val sections = if (grouped && groupOf != null) visible.groupBy(groupOf).toSortedMap().toList()
                else listOf("" to visible)
                sections.forEach { (title, sectionApps) ->
                    if (title.isNotEmpty()) item(key = "section:$title") { SectionLabel(title) }
                    items(sectionApps, key = { "${it.appPackage}:${it.activityClassName}" }) { app ->
                        DrawerAppRow(
                            app = app,
                            fontSize = fontSize,
                            highlighted = query.isNotBlank() && matches.firstOrNull() == app,
                            deemphasized = isPaused(app),
                            onClick = { onAppClick(app, query) },
                            onLongClick = { if (pickingTitle == null) onAppLongClick(app) },
                        )
                    }
                }
                if (query.isBlank() && otherApps.isNotEmpty() && !showOthers) item(key = "all-apps") {
                    QuietAction("All apps · ${otherApps.size} more") { showOthers = true }
                }
                if (paused.isNotEmpty()) {
                    item(key = "paused-header") {
                        QuietAction((if (showPaused) "Hide" else "Show") + " ${paused.size} paused during focus") { showPaused = !showPaused }
                    }
                    if (showPaused) items(paused, key = { "paused:${it.appPackage}:${it.activityClassName}" }) { app ->
                        DrawerAppRow(app = app, fontSize = fontSize, highlighted = false, deemphasized = true,
                            onClick = { onAppClick(app, query) }, onLongClick = { onAppLongClick(app) })
                    }
                }
                if (matches.isEmpty()) item {
                    Text(
                        "No app called “$query”",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 32.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        title.uppercase(Locale.getDefault()),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 22.dp, bottom = 6.dp),
    )
}

@Composable
private fun QuietAction(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(start = 0.dp))
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            .background(if (highlighted) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
                MaterialTheme.shapes.small)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            app.displayLabel,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal, letterSpacing = (-0.01).em),
            fontSize = (fontSize - 2).coerceAtLeast(16).sp,
            color = if (deemphasized) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (highlighted) Text("Go", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
}

