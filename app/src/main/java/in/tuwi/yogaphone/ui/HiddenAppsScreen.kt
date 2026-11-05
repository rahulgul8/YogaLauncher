package `in`.tuwi.yogaphone.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.data.AppModel

@Composable
fun HiddenAppsScreen(hiddenApps: List<AppModel>, onBack: () -> Unit, onUnhide: (AppModel) -> Unit) {
    LazyColumn {
        item { SettingsHeader("Hidden apps", onBack) }
        if (hiddenApps.isEmpty()) item {
            Text("No hidden apps", color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(24.dp))
        }
        items(hiddenApps, key = AppModel::appPackage) { app ->
            ListItem(
                headlineContent = { Text(app.displayLabel) },
                supportingContent = { Text(app.appPackage) },
                trailingContent = { Button(onClick = { onUnhide(app) }) { Text("Unhide") } },
            )
        }
    }
}
