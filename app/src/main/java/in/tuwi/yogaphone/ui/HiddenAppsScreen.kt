package `in`.tuwi.yogaphone.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.data.AppModel
import `in`.tuwi.yogaphone.ui.theme.*

@Composable
fun HiddenAppsScreen(hiddenApps: List<AppModel>, onBack: () -> Unit, onUnhide: (AppModel) -> Unit) {
    YogaScreen("Hidden apps", onBack) {
        if (hiddenApps.isEmpty()) item { YogaNote("No hidden apps.") }
        items(hiddenApps, key = AppModel::appPackage) { app ->
            YogaRow(app.displayLabel, summary = app.appPackage,
                trailing = { YogaSecondaryButton("Unhide", { onUnhide(app) }) })
        }
    }
}
