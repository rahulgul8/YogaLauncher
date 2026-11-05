package `in`.tuwi.yogaphone.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.MAX_PROFILE_APPS
import `in`.tuwi.yogaphone.MainUiState
import `in`.tuwi.yogaphone.MainViewModel
import `in`.tuwi.yogaphone.data.AppModel
import `in`.tuwi.yogaphone.data.ThemeMode
import `in`.tuwi.yogaphone.data.db.CategoryEntity
import `in`.tuwi.yogaphone.data.db.FocusScheduleEntity
import `in`.tuwi.yogaphone.data.db.ProfileEntity
import `in`.tuwi.yogaphone.profiles.HomeList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSwitcherSheet(state: MainUiState, onSelect: (Long?) -> Unit, onManage: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Profile", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 8.dp))
            ProfileOption("Personal", "Your usual home", state.activeProfile == null) { onSelect(null); onDismiss() }
            state.profiles.forEach { profile ->
                val apps = HomeList.decode(profile.homeApps).size
                ProfileOption(profile.name, "$apps apps" + if (profile.hideOtherApps) " · others tucked away" else "",
                    state.activeProfile?.id == profile.id) { onSelect(profile.id); onDismiss() }
            }
            TextButton(onClick = { onDismiss(); onManage() }, modifier = Modifier.padding(vertical = 8.dp)) { Text("Manage profiles") }
        }
    }
}

@Composable
private fun ProfileOption(name: String, summary: String, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(name) },
        supportingContent = { Text(summary) },
        leadingContent = { RadioButton(selected = selected, onClick = onClick) },
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick),
    )
}

@Composable
fun ProfilesScreen(state: MainUiState, viewModel: MainViewModel, onBack: () -> Unit, onEdit: (Long) -> Unit) {
    var creating by remember { mutableStateOf(false) }
    LazyColumn {
        item { SettingsHeader("Profiles", onBack) }
        item {
            Text("Profiles give you a different home for different parts of the day. Personal is your usual home.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
        item {
            ListItem(headlineContent = { Text("Personal") }, supportingContent = { Text("Your usual home apps") },
                trailingContent = { if (state.activeProfile == null) Text("Active") })
        }
        items(state.profiles, key = { it.id }) { profile ->
            ListItem(
                headlineContent = { Text(profile.name) },
                supportingContent = { Text("${HomeList.decode(profile.homeApps).size} home apps") },
                trailingContent = { if (state.activeProfile?.id == profile.id) Text("Active") },
                modifier = Modifier.clickable { onEdit(profile.id) },
            )
        }
        item { TextButton(onClick = { creating = true }, modifier = Modifier.padding(8.dp)) { Text("New profile") } }
    }
    if (creating) NewProfileDialog(
        existing = state.profiles.map { it.name },
        onDismiss = { creating = false },
        onCreate = { name, essentialsOnly ->
            creating = false
            viewModel.createProfile(name, fromCurrentHome = !essentialsOnly, essentialsOnly = essentialsOnly) { onEdit(it) }
        },
    )
}

@Composable
private fun NewProfileDialog(existing: List<String>, onDismiss: () -> Unit, onCreate: (String, Boolean) -> Unit) {
    val suggestions = listOf("Work", "Reading", "Essentials only").filterNot { it in existing }
    var name by remember { mutableStateOf(suggestions.firstOrNull() ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New profile") },
        text = {
            Column {
                OutlinedTextField(name, { name = it.take(24) }, label = { Text("Name") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    suggestions.forEach { FilterChip(selected = name == it, onClick = { name = it }, label = { Text(it) }) }
                }
                Text(
                    if (name == "Essentials only") "Starts with your essential apps and tucks the rest away."
                    else "Starts with your current home apps; edit them next.",
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onCreate(name.trim(), name.trim() == "Essentials only") }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun ProfileEditScreen(
    profile: ProfileEntity,
    state: MainUiState,
    schedules: List<FocusScheduleEntity>,
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    var name by remember(profile.id) { mutableStateOf(profile.name) }
    var picking by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val homeApps = HomeList.decode(profile.homeApps)
    val labels = (state.drawerApps + state.otherApps).associate { it.appPackage to it.displayLabel }

    LazyColumn {
        item { SettingsHeader(profile.name, onBack) }
        item {
            OutlinedTextField(name, { name = it.take(24) }, label = { Text("Name") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                trailingIcon = {
                    if (name.isNotBlank() && name != profile.name) TextButton(onClick = { viewModel.saveProfile(profile.copy(name = name.trim())) }) { Text("Save") }
                })
        }
        item { SectionTitle("Home apps") }
        if (homeApps.isEmpty()) item {
            Text("No apps yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
        }
        items(homeApps, key = { "home:$it" }) { pkg ->
            ListItem(
                headlineContent = { Text(labels[pkg] ?: pkg) },
                trailingContent = {
                    Row {
                        TextButton(onClick = { viewModel.editProfileApps(profile.id) { HomeList.move(it, pkg, -1) } }, enabled = homeApps.first() != pkg) { Text("Up") }
                        TextButton(onClick = { viewModel.editProfileApps(profile.id) { HomeList.move(it, pkg, 1) } }, enabled = homeApps.last() != pkg) { Text("Down") }
                        TextButton(onClick = { viewModel.editProfileApps(profile.id) { it - pkg } }) { Text("Remove") }
                    }
                },
            )
        }
        item {
            TextButton(onClick = { picking = true }, enabled = homeApps.size < MAX_PROFILE_APPS, modifier = Modifier.padding(horizontal = 8.dp)) {
                Text("Choose apps")
            }
        }
        item { SectionTitle("When this profile is on") }
        item {
            ToggleRow("Tuck other apps away", "The drawer shows this profile's apps and essentials, with an \"All apps\" link",
                profile.hideOtherApps) { viewModel.saveProfile(profile.copy(hideOtherApps = it)) }
        }
        item {
            ToggleRow("Start focus", "Turn on focus when you switch to this profile", profile.focusOnActivate) {
                viewModel.saveProfile(profile.copy(focusOnActivate = it))
            }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Theme")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(null to "Keep", ThemeMode.LIGHT.name to "Light", ThemeMode.DARK.name to "Dark", ThemeMode.GREYSCALE.name to "Greyscale").forEach { (value, label) ->
                        FilterChip(selected = profile.themeOverride == value, onClick = { viewModel.saveProfile(profile.copy(themeOverride = value)) },
                            label = { Text(label) })
                    }
                }
            }
        }
        item { SectionTitle("Switch automatically") }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                if (schedules.isEmpty()) Text("Add a schedule in Focus and timebox settings to switch to this profile automatically.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                ScheduleOption("Only when I switch", profile.scheduleId == null) { viewModel.saveProfile(profile.copy(scheduleId = null)) }
                schedules.forEach { schedule ->
                    ScheduleOption("During \"${schedule.name}\"", profile.scheduleId == schedule.id) {
                        viewModel.saveProfile(profile.copy(scheduleId = schedule.id))
                    }
                }
                Text("Switching by hand during a scheduled window wins until the window ends.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            TextButton(onClick = { confirmDelete = true }, modifier = Modifier.padding(8.dp)) {
                Text("Delete profile", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (picking) AppMultiPicker(
        apps = (state.drawerApps + state.otherApps).distinctBy { it.appPackage }.sorted(),
        selected = homeApps,
        limit = MAX_PROFILE_APPS,
        onDone = { chosen -> picking = false; viewModel.editProfileApps(profile.id) { current -> current.filter { it in chosen } + chosen.filterNot { it in current } } },
        onDismiss = { picking = false },
    )
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete ${profile.name}?") },
        text = { Text("Your Personal home is not affected.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.deleteProfile(profile.id); onBack() }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
    )
}

@Composable
private fun ScheduleOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label)
    }
}

@Composable
private fun ToggleRow(title: String, summary: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(headlineContent = { Text(title) }, supportingContent = { Text(summary) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
        modifier = Modifier.clickable { onChange(!checked) })
}

@Composable
private fun AppMultiPicker(apps: List<AppModel>, selected: List<String>, limit: Int, onDone: (List<String>) -> Unit, onDismiss: () -> Unit) {
    var chosen by remember { mutableStateOf(selected) }
    var query by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose up to $limit apps") },
        text = {
            Column {
                OutlinedTextField(query, { query = it }, label = { Text("Find an app") }, singleLine = true)
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(apps.filter { it.displayLabel.contains(query, ignoreCase = true) }, key = { it.appPackage }) { app ->
                        val checked = app.appPackage in chosen
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(enabled = checked || chosen.size < limit) {
                                chosen = if (checked) chosen - app.appPackage else chosen + app.appPackage
                            },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Text(app.displayLabel, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onDone(chosen) }) { Text("Done (${chosen.size})") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun CategoryPickerDialog(
    app: AppModel,
    categories: List<CategoryEntity>,
    current: String?,
    onPick: (String) -> Unit,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Category for ${app.displayLabel}") },
        text = {
            Column {
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(categories, key = { it.id }) { category ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onPick(category.id); onDismiss() },
                            verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = category.id == current, onClick = { onPick(category.id); onDismiss() })
                            Text(category.name)
                        }
                    }
                }
                OutlinedTextField(newName, { newName = it.take(24) }, label = { Text("Or a new category") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(enabled = newName.isNotBlank(), onClick = { onAdd(newName.trim()); onDismiss() }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
fun CategoriesScreen(state: MainUiState, viewModel: MainViewModel, onBack: () -> Unit) {
    var expanded by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }
    var adding by remember { mutableStateOf(false) }
    var recategorize by remember { mutableStateOf<AppModel?>(null) }
    val apps = (state.drawerApps + state.otherApps).distinctBy { it.appPackage }.sorted()
    val byCategory = apps.groupBy { state.categoryOf[it.appPackage] }

    LazyColumn {
        item { SettingsHeader("Categories", onBack) }
        item {
            ListItem(
                headlineContent = { Text("Group the app drawer by category") },
                trailingContent = { Switch(checked = state.organize.groupDrawerByCategory, onCheckedChange = viewModel::setGroupDrawer) },
            )
        }
        item {
            Text("Yoga guesses categories on your phone from each app's details. Tap a category to see its apps and move them.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
        state.categories.forEach { category ->
            val members = byCategory[category.id].orEmpty()
            item(key = "cat:" + category.id) {
                ListItem(
                    headlineContent = { Text(category.name) },
                    supportingContent = { Text("${members.size} apps" + if (category.isBuiltIn) "" else " · yours") },
                    trailingContent = { if (!category.isBuiltIn) TextButton(onClick = { editing = category }) { Text("Edit") } },
                    modifier = Modifier.clickable { expanded = if (expanded == category.id) null else category.id },
                )
            }
            if (expanded == category.id) items(members, key = { "member:${category.id}:${it.appPackage}" }) { app ->
                Text(app.displayLabel, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .clickable { recategorize = app }.padding(start = 40.dp, top = 12.dp, bottom = 12.dp))
            }
        }
        item { TextButton(onClick = { adding = true }, modifier = Modifier.padding(8.dp)) { Text("New category") } }
    }

    if (adding) NameDialog("New category", "", onDismiss = { adding = false }) { viewModel.addCategory(it); adding = false }
    editing?.let { category ->
        NameDialog("Rename category", category.name, onDelete = { viewModel.deleteCategory(category); editing = null },
            onDismiss = { editing = null }) { viewModel.renameCategory(category, it); editing = null }
    }
    recategorize?.let { app ->
        CategoryPickerDialog(app, state.categories, state.categoryOf[app.appPackage],
            onPick = { viewModel.setAppCategory(app, it) }, onAdd = { viewModel.addCategory(it, app) }, onDismiss = { recategorize = null })
    }
}

@Composable
private fun NameDialog(title: String, initial: String, onDelete: (() -> Unit)? = null, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(name, { name = it.take(24) }, label = { Text("Name") }, singleLine = true) },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onSave(name.trim()) }) { Text("Save") } },
        dismissButton = {
            Row {
                onDelete?.let { TextButton(onClick = it) { Text("Delete") } }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
