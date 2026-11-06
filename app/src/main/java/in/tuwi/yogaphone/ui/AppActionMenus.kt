package `in`.tuwi.yogaphone.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.data.AppModel
import `in`.tuwi.yogaphone.ui.theme.*

data class AppMenuAction(val label: String, val enabled: Boolean = true, val action: () -> Unit)

@Composable
fun AppActionSheet(app: AppModel, actions: List<AppMenuAction>, onDismiss: () -> Unit) {
    YogaSheet(onDismiss) {
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(app.displayLabel, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = ScreenMargin, vertical = 12.dp))
            actions.forEach { menu ->
                YogaRow(menu.label, enabled = menu.enabled, onClick = { onDismiss(); menu.action() })
            }
        }
    }
}

@Composable
fun RenameAppDialog(app: AppModel, onDismiss: () -> Unit, onSave: (String?) -> Unit) {
    var value by remember(app.appPackage) { mutableStateOf(app.customLabel ?: app.appLabel) }
    YogaDialog("Rename ${app.appLabel}", "Save", { onSave(value); onDismiss() }, onDismiss,
        dismissText = "Use installed name", onDismissAction = { onSave(null); onDismiss() },
        text = { YogaTextField(value, { value = it }, "Home label") })
}
