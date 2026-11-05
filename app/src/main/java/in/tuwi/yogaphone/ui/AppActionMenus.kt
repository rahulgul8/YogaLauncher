package `in`.tuwi.yogaphone.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.data.AppModel

data class AppMenuAction(val label: String, val enabled: Boolean = true, val action: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionSheet(app: AppModel, actions: List<AppMenuAction>, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(app.displayLabel, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            actions.forEach { menu ->
                TextButton(
                    onClick = { onDismiss(); menu.action() },
                    enabled = menu.enabled,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(menu.label, modifier = Modifier.fillMaxWidth()) }
            }
        }
    }
}

@Composable
fun RenameAppDialog(app: AppModel, onDismiss: () -> Unit, onSave: (String?) -> Unit) {
    var value by remember(app.appPackage) { mutableStateOf(app.customLabel ?: app.appLabel) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename ${app.appLabel}") },
        text = {
            OutlinedTextField(value = value, onValueChange = { value = it }, singleLine = true,
                label = { Text("Home label") })
        },
        confirmButton = { TextButton(onClick = { onSave(value); onDismiss() }) { Text("Save") } },
        dismissButton = {
            TextButton(onClick = { onSave(null); onDismiss() }) { Text("Use installed name") }
        },
    )
}
