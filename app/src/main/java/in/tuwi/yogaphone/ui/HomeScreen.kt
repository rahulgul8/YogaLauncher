package `in`.tuwi.yogaphone.ui

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
    onOpenAppDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenClock: () -> Unit,
    onOpenCalendar: () -> Unit,
    onExpandNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current
    val preferences = uiState.preferences
    val horizontalAlignment = when (preferences.homeTextAlignment) {
        HomeTextAlignment.LEFT -> Alignment.Start
        HomeTextAlignment.CENTER -> Alignment.CenterHorizontally
        HomeTextAlignment.RIGHT -> Alignment.End
    }

    LaunchedEffect(Unit) {
        while (true) { now = System.currentTimeMillis(); delay(30_000) }
    }

    Box(
        modifier = modifier.fillMaxSize().pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var delta = 0f
                var consumedByChild = false
                var pressed = true
                do {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    delta = change.position.y - down.position.y
                    consumedByChild = consumedByChild || change.isConsumed
                    pressed = change.pressed
                } while (pressed)
                if (!consumedByChild) when {
                    delta < -100f -> onOpenAppDrawer()
                    delta > 100f -> onExpandNotifications()
                }
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = horizontalAlignment,
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onOpenSettings, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("settings", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (preferences.showClock) {
                Text(
                    text = DateFormat.getTimeFormat(context).format(Date(now)),
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.combinedClickable(onClick = onOpenClock, onLongClick = onOpenSettings)
                        .padding(vertical = 4.dp),
                )
            }
            if (preferences.showDate) {
                Text(
                    text = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(now)),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.combinedClickable(onClick = onOpenCalendar, onLongClick = onOpenSettings)
                        .padding(vertical = 8.dp),
                )
            }
            HomeStatusArea()
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                when {
                    uiState.isLoading -> CalmHint("Loading apps…")
                    uiState.favoriteApps.isEmpty() -> CalmHint("Swipe up or search to choose home apps")
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceEvenly,
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
            Spacer(Modifier.size(64.dp))
        }

        FloatingActionButton(
            onClick = onOpenAppDrawer,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp).size(56.dp),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Icon(Icons.Outlined.Search, contentDescription = "Search apps")
        }
    }
}

@Composable
fun HomeStatusArea(modifier: Modifier = Modifier) { Spacer(modifier = modifier) }

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
        fontSize = fontSize.sp,
        textAlign = textAlign,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 8.dp),
    )
}

fun homeFontSize(option: FontSizeOption): Int = when (option) {
    FontSizeOption.SMALL -> 18
    FontSizeOption.MEDIUM -> 22
    FontSizeOption.LARGE -> 26
    FontSizeOption.EXTRA_LARGE -> 30
}
