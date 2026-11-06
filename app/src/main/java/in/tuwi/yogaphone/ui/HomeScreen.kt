package `in`.tuwi.yogaphone.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.unit.em
import `in`.tuwi.yogaphone.ui.theme.YogaIcons
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.tuwi.yogaphone.MainUiState
import `in`.tuwi.yogaphone.data.AppModel
import `in`.tuwi.yogaphone.data.FontSizeOption
import `in`.tuwi.yogaphone.data.HomeTextAlignment
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    uiState: MainUiState,
    onAppClick: (AppModel) -> Unit,
    onAppLongClick: (AppModel) -> Unit,
    /** Finger movement in pixels while the app drawer is being pulled up (negative is up). */
    onDrawerDrag: (Float) -> Unit,
    /** The finger lifted; vertical velocity in pixels per second. */
    onDrawerRelease: (Float) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenClock: () -> Unit,
    onOpenCalendar: () -> Unit,
    onExpandNotifications: () -> Unit,
    statusContent: @Composable () -> Unit = {},
    topActions: @Composable RowScope.() -> Unit = {},
    profileLabel: String? = null,
    widgets: @Composable () -> Unit = {},
    onProfileClick: () -> Unit = {},
    dock: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val preferences = uiState.preferences
    val horizontalAlignment = when (preferences.homeTextAlignment) {
        HomeTextAlignment.LEFT -> Alignment.Start
        HomeTextAlignment.CENTER -> Alignment.CenterHorizontally
        HomeTextAlignment.RIGHT -> Alignment.End
    }

    LaunchedEffect(Unit) {
        while (true) { now = System.currentTimeMillis(); delay(60_000 - now % 60_000 + 50) }
    }

    // A swipe that starts on the home app list is taken by the list; whatever it can't scroll moves the drawer.
    val drag by rememberUpdatedState(onDrawerDrag)
    val release by rememberUpdatedState(onDrawerRelease)
    val listToDrawer = remember {
        object : NestedScrollConnection {
            var pulling = false
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || (available.y >= 0f && !pulling)) return Offset.Zero
                pulling = true
                drag(available.y)
                return Offset(0f, available.y)
            }
            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!pulling) return Velocity.Zero
                pulling = false
                release(available.y)
                return available
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize().nestedScroll(listToDrawer).pointerInput(Unit) {
            // Swipe up pulls the drawer with the finger; swipe down opens the notification shade.
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val tracker = VelocityTracker().apply { addPosition(down.uptimeMillis, down.position) }
                var total = 0f
                var dragging = false
                var claimedByChild = false
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    if (change.isConsumed && !dragging) claimedByChild = true
                    val dy = change.position.y - change.previousPosition.y
                    total += dy
                    tracker.addPosition(change.uptimeMillis, change.position)
                    if (!claimedByChild && !dragging && total < -viewConfiguration.touchSlop) {
                        dragging = true
                        onDrawerDrag(total)
                    } else if (dragging) onDrawerDrag(dy)
                    if (dragging) change.consume()
                }
                if (dragging) onDrawerRelease(tracker.calculateVelocity().y)
                else if (!claimedByChild && total > 120f) onExpandNotifications()
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(start = 28.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
            horizontalAlignment = horizontalAlignment,
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (profileLabel != null) ProfileChip(profileLabel, onProfileClick)
                Spacer(Modifier.weight(1f))
                topActions()
                HomeIconButton(YogaIcons.Settings, "Settings", onOpenSettings)
            }
            if (preferences.showClock) {
                BrandClock(now, onClick = onOpenClock, onLongClick = onOpenSettings, modifier = Modifier.padding(top = 28.dp))
            }
            if (preferences.showDate) {
                Text(
                    text = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(now)),
                    style = MaterialTheme.typography.bodyLarge.copy(letterSpacing = 0.02.em),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.combinedClickable(onClick = onOpenCalendar, onLongClick = onOpenSettings)
                        .padding(vertical = 6.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            HomeStatusArea(content = statusContent)
            widgets()
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.BottomStart) {
                when {
                    uiState.isLoading -> Unit
                    uiState.favoriteApps.isEmpty() -> CalmHint("Swipe up to choose the apps you want here")
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom),
                        horizontalAlignment = horizontalAlignment,
                    ) {
                        items(uiState.favoriteApps, key = AppModel::appPackage) { app ->
                            FavoriteAppRow(
                                app = app,
                                fontSize = homeFontSize(preferences.fontSize),
                                textAlign = when (preferences.homeTextAlignment) {
                                    HomeTextAlignment.LEFT -> TextAlign.Start
                                    HomeTextAlignment.CENTER -> TextAlign.Center
                                    HomeTextAlignment.RIGHT -> TextAlign.End
                                },
                                onClick = { onAppClick(app) },
                                onLongClick = { onAppLongClick(app) },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            dock()
        }
    }
}

@Composable
fun HomeIconButton(icon: ImageVector, label: String, onClick: () -> Unit, active: Boolean = false) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp),
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProfileChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.heightIn(min = 36.dp),
    ) {
        Row(Modifier.padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label.uppercase(Locale.getDefault()), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(YogaIcons.ChevronDown, contentDescription = "Switch profile", tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp).size(16.dp))
        }
    }
}

@Composable
fun HomeStatusArea(modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) { Column(modifier) { content() } }

@Composable
private fun CalmHint(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(24.dp))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteAppRow(
    app: AppModel,
    fontSize: Int,
    textAlign: TextAlign,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Text(
        text = app.displayLabel,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal, letterSpacing = (-0.01).em),
        fontSize = fontSize.sp,
        textAlign = textAlign,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 9.dp),
    )
}

fun homeFontSize(option: FontSizeOption): Int = when (option) {
    FontSizeOption.SMALL -> 18
    FontSizeOption.MEDIUM -> 22
    FontSizeOption.LARGE -> 26
    FontSizeOption.EXTRA_LARGE -> 30
}

