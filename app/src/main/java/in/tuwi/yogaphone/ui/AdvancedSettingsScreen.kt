package `in`.tuwi.yogaphone.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.ui.theme.ScreenMargin
import `in`.tuwi.yogaphone.ui.theme.YogaNote
import `in`.tuwi.yogaphone.ui.theme.YogaPrimaryButton
import `in`.tuwi.yogaphone.ui.theme.YogaRow
import `in`.tuwi.yogaphone.ui.theme.YogaScreen
import `in`.tuwi.yogaphone.ui.theme.YogaSecondaryButton
import `in`.tuwi.yogaphone.ui.theme.YogaSection

@Composable
fun AdvancedSettingsScreen(onBack: () -> Unit, onResetNames: () -> Unit, onLeave: () -> Unit) {
    val context = LocalContext.current
    var namesReset by remember { mutableStateOf(false) }
    YogaScreen("Advanced", onBack) {
        item { YogaSection("Phone") }
        item { YogaRow("Android settings", summary = "Open the phone's own settings",
            onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS)) } }) }
        item { YogaSection("Apps") }
        item { YogaRow("Reset custom names", summary = if (namesReset) "Every app has its installed name again" else "Restore every app's installed name",
            onClick = { onResetNames(); namesReset = true }, trailing = null) }
        item { YogaSection("Home screen") }
        item { YogaRow("Leave Yoga Launcher", summary = "Use a different home screen app", onClick = onLeave) }
    }
}

/** The last step before handing home back to another launcher. Staying is the primary action. */
@Composable
fun LeaveYogaScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    YogaScreen("Leave Yoga?", onBack,
        subtitle = "Android will let you pick another home screen app. Your Yoga settings, allowances and history stay on this phone, so you can come back any time.") {
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = ScreenMargin, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                YogaPrimaryButton("Stay with Yoga", onBack, Modifier.fillMaxWidth())
                YogaSecondaryButton("Choose another home app",
                    { runCatching { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) } }, Modifier.fillMaxWidth())
            }
        }
        item { YogaNote("To remove Yoga completely, uninstall it from Android settings after choosing another home app.") }
    }
}
