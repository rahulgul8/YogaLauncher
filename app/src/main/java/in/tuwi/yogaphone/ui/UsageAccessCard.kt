package `in`.tuwi.yogaphone.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import `in`.tuwi.yogaphone.usage.UsageAccess
import `in`.tuwi.yogaphone.ui.theme.*

@Composable
fun UsageAccessCard() {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var granted by remember { mutableStateOf(UsageAccess.granted(context)) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) granted = UsageAccess.granted(context) }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    YogaCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (granted) "Usage Access is on" else "Enable usage awareness", style = MaterialTheme.typography.titleMedium)
            Text("Yoga reads which apps were in the foreground and for how long. This stays on your phone.")
            if (!granted) Text("Allow Usage Access in Android settings to see your usage and allowance progress.")
            YogaSecondaryButton(if (granted) "Manage Usage Access" else "Open Usage Access settings", { UsageAccess.openSettings(context) })
        }
    }
}
