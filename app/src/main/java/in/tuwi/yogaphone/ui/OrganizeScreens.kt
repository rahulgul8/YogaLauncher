package `in`.tuwi.yogaphone.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.MAX_PROFILE_APPS
import `in`.tuwi.yogaphone.MainUiState
import `in`.tuwi.yogaphone.MainViewModel
import `in`.tuwi.yogaphone.data.AppModel
import `in`.tuwi.yogaphone.data.ThemeMode
import `in`.tuwi.yogaphone.data.db.*
import `in`.tuwi.yogaphone.profiles.HomeList
import `in`.tuwi.yogaphone.ui.theme.*

@Composable
fun ProfileSwitcherSheet(state: MainUiState, onSelect: (Long?) -> Unit, onManage: () -> Unit, onDismiss: () -> Unit) {
    YogaSheet(onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Text("Profile", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = ScreenMargin, vertical = 12.dp))
            ProfileOption("Personal", "Your usual home", state.activeProfile == null) { onSelect(null); onDismiss() }
            state.profiles.forEach { profile ->
                val apps = HomeList.decode(profile.homeApps).size
                ProfileOption(profile.name, "$apps apps${if (profile.hideOtherApps) " · others tucked away" else ""}", state.activeProfile?.id == profile.id) {
                    onSelect(profile.id); onDismiss()
                }
            }
            YogaTextButton("Manage profiles", { onDismiss(); onManage() }, Modifier.padding(horizontal = TextButtonInset))
        }
    }
}

@Composable
private fun ProfileOption(name: String, summary: String, selected: Boolean, onClick: () -> Unit) =
    YogaRadioRow(name, selected, onClick = onClick, summary = summary)

@Composable
fun ProfilesScreen(state: MainUiState, viewModel: MainViewModel, onBack: () -> Unit, onEdit: (Long) -> Unit) {
    var creating by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val recentNames = rememberRecentEntries(RECENT_PROFILE_NAMES)
    YogaScreen("Profiles", onBack) {
        item { YogaNote("Profiles give you a different home for different parts of the day. Personal is your usual home.") }
        item { YogaRow("Personal", summary = "Your usual home apps", trailing = if (state.activeProfile == null) ({ Text("Active", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }) else null) }
        items(state.profiles, key = { it.id }) { profile ->
            YogaRow(profile.name, summary = "${HomeList.decode(profile.homeApps).size} home apps", onClick = { onEdit(profile.id) },
                trailing = if (state.activeProfile?.id == profile.id) ({ Text("Active", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }) else null)
        }
        item { YogaPrimaryButton("New profile", { creating = true }, Modifier.padding(horizontal = ScreenMargin, vertical = 20.dp)) }
    }
    if (creating) NewProfileDialog(state.profiles.map { it.name }, recentNames.value, { creating = false }) { name, essentialsOnly ->
        recordRecentEntry(context, RECENT_PROFILE_NAMES, recentNames, name)
        creating = false; viewModel.createProfile(name, fromCurrentHome = !essentialsOnly, essentialsOnly = essentialsOnly) { onEdit(it) }
    }
}

@Composable
private fun NewProfileDialog(existing: List<String>, recent: List<String>, onDismiss: () -> Unit, onCreate: (String, Boolean) -> Unit) {
    val suggestions = listOf("Work", "Reading", "Essentials only").filterNot { it in existing }
    var name by remember { mutableStateOf(suggestions.firstOrNull() ?: "") }
    YogaDialog("New profile", "Create", { onCreate(name.trim(), name.trim() == "Essentials only") }, onDismiss,
        confirmEnabled = name.isNotBlank(), text = {
            Column {
                YogaTextField(name, { name = it.take(24) }, "Name")
                YogaChipRow { (recent + suggestions).distinct().take(6).forEach { value -> YogaChip(value, name == value, onClick = { name = value }) } }
                YogaNote(if (name == "Essentials only") "Starts with your essential apps and tucks the rest away." else "Starts with your current home apps; edit them next.")
            }
        })
}

@Composable
fun ProfileEditScreen(profile: ProfileEntity, state: MainUiState, schedules: List<FocusScheduleEntity>, viewModel: MainViewModel, onBack: () -> Unit) {
    var name by remember(profile.id) { mutableStateOf(profile.name) }
    var picking by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val homeApps = HomeList.decode(profile.homeApps)
    val labels = (state.drawerApps + state.otherApps).associate { it.appPackage to it.displayLabel }
    val context = LocalContext.current
    val recentNames = rememberRecentEntries(RECENT_PROFILE_NAMES)
    YogaScreen(profile.name, onBack) {
        item {
            YogaTextField(name, { name = it.take(24) }, "Name")
            if (recentNames.value.isNotEmpty()) YogaChipRow { recentNames.value.forEach { recent -> YogaChip(recent, name == recent, onClick = { name = recent }) } }
            if (name.isNotBlank() && name != profile.name) YogaTextButton("Save name", {
                recordRecentEntry(context, RECENT_PROFILE_NAMES, recentNames, name); viewModel.saveProfile(profile.copy(name = name.trim()))
            }, Modifier.padding(horizontal = ScreenMargin))
        }
        item { YogaSection("Home apps") }
        if (homeApps.isEmpty()) item { YogaNote("No apps yet.") }
        items(homeApps, key = { "home:$it" }) { pkg ->
            YogaRow(labels[pkg] ?: pkg, trailing = { Row {
                YogaTextButton("Up", { viewModel.editProfileApps(profile.id) { HomeList.move(it, pkg, -1) } }, enabled = homeApps.first() != pkg)
                YogaTextButton("Down", { viewModel.editProfileApps(profile.id) { HomeList.move(it, pkg, 1) } }, enabled = homeApps.last() != pkg)
                YogaTextButton("Remove", { viewModel.editProfileApps(profile.id) { it - pkg } }, muted = true)
            } })
        }
        item { YogaTextButton("Choose apps", { picking = true }, Modifier.padding(horizontal = TextButtonInset), enabled = homeApps.size < MAX_PROFILE_APPS) }
        item { YogaSection("When this profile is on") }
        item { YogaSwitchRow("Tuck other apps away", profile.hideOtherApps, { viewModel.saveProfile(profile.copy(hideOtherApps = it)) },
            summary = "The drawer shows this profile's apps and essentials, with an “All apps” link") }
        item { YogaSwitchRow("Start focus", profile.focusOnActivate, { viewModel.saveProfile(profile.copy(focusOnActivate = it)) },
            summary = "Turn on focus when you switch to this profile") }
        item { YogaSection("Theme") }
        item { YogaChipRow { listOf(null to "Keep", ThemeMode.LIGHT.name to "Light", ThemeMode.DARK.name to "Dark", ThemeMode.GREYSCALE.name to "Greyscale").forEach { (value, label) ->
            YogaChip(label, profile.themeOverride == value, onClick = { viewModel.saveProfile(profile.copy(themeOverride = value)) })
        } } }
        item { YogaSection("Switch automatically") }
        if (schedules.isEmpty()) item { YogaNote("Add a schedule in Focus and timebox settings to switch to this profile automatically.") }
        item { YogaRadioRow("Only when I switch", profile.scheduleId == null, onClick = { viewModel.saveProfile(profile.copy(scheduleId = null)) }) }
        items(schedules, key = { it.id }) { schedule -> YogaRadioRow("During “${schedule.name}”", profile.scheduleId == schedule.id,
            onClick = { viewModel.saveProfile(profile.copy(scheduleId = schedule.id)) }) }
        item { YogaNote("Switching by hand during a scheduled window wins until the window ends.") }
        item { YogaTextButton("Delete profile", { confirmDelete = true }, Modifier.padding(horizontal = TextButtonInset), muted = true) }
    }
    if (picking) AppMultiPicker((state.drawerApps + state.otherApps).distinctBy { it.appPackage }.sorted(), homeApps, MAX_PROFILE_APPS,
        { chosen -> picking = false; viewModel.editProfileApps(profile.id) { current -> current.filter { it in chosen } + chosen.filterNot { it in current } } }, { picking = false })
    if (confirmDelete) YogaDialog("Delete ${profile.name}?", "Delete", { confirmDelete = false; viewModel.deleteProfile(profile.id); onBack() },
        { confirmDelete = false }, text = { Text("Your Personal home is not affected.") })
}

@Composable
private fun AppMultiPicker(apps: List<AppModel>, selected: List<String>, limit: Int, onDone: (List<String>) -> Unit, onDismiss: () -> Unit) {
    var chosen by remember { mutableStateOf(selected) }
    var query by remember { mutableStateOf("") }
    YogaDialog("Choose up to $limit apps", "Done (${chosen.size})", { onDone(chosen) }, onDismiss, text = {
        Column {
            YogaTextField(query, { query = it }, "Find an app")
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                items(apps.filter { it.displayLabel.contains(query, true) }, key = { it.appPackage }) { app ->
                    val checked = app.appPackage in chosen
                    YogaCheckRow(app.displayLabel, checked, checked || chosen.size < limit) {
                        chosen = if (checked) chosen - app.appPackage else chosen + app.appPackage
                    }
                }
            }
        }
    })
}

@Composable
fun CategoryPickerDialog(app: AppModel, categories: List<CategoryEntity>, current: String?, onPick: (String) -> Unit,
    onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    var newName by remember { mutableStateOf("") }
    val context = LocalContext.current
    val recent = rememberRecentEntries(RECENT_CATEGORY_NAMES)
    YogaDialog("Category for ${app.displayLabel}", "Add", {
        recordRecentEntry(context, RECENT_CATEGORY_NAMES, recent, newName); onAdd(newName.trim()); onDismiss()
    }, onDismiss,
        confirmEnabled = newName.isNotBlank(), dismissText = "Close", text = {
            Column {
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(categories, key = { it.id }) { category -> YogaRadioRow(category.name, category.id == current,
                        onClick = { onPick(category.id); onDismiss() }) }
                }
                YogaTextField(newName, { newName = it.take(24) }, "Or a new category")
                if (recent.value.isNotEmpty()) YogaChipRow { recent.value.forEach { name -> YogaChip(name, newName == name, onClick = { newName = name }) } }
            }
        })
}

@Composable
fun CategoriesScreen(state: MainUiState, viewModel: MainViewModel, onBack: () -> Unit) {
    var expanded by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }
    var adding by remember { mutableStateOf(false) }
    var recategorize by remember { mutableStateOf<AppModel?>(null) }
    val apps = (state.drawerApps + state.otherApps).distinctBy { it.appPackage }.sorted()
    val byCategory = apps.groupBy { state.categoryOf[it.appPackage] }
    val context = LocalContext.current
    val recentNames = rememberRecentEntries(RECENT_CATEGORY_NAMES)
    YogaScreen("Categories", onBack) {
        item { YogaSwitchRow("Group the app drawer by category", state.organize.groupDrawerByCategory, viewModel::setGroupDrawer) }
        item { YogaNote("Yoga guesses categories on your phone from each app's details. Tap a category to see its apps and move them.") }
        state.categories.forEach { category ->
            val members = byCategory[category.id].orEmpty()
            item(key = "cat:${category.id}") { YogaRow(category.name, summary = "${members.size} apps${if (category.isBuiltIn) "" else " · yours"}",
                onClick = { expanded = if (expanded == category.id) null else category.id }, trailing = if (!category.isBuiltIn) ({ YogaTextButton("Edit", { editing = category }) }) else null) }
            if (expanded == category.id) items(members, key = { "member:${category.id}:${it.appPackage}" }) { app ->
                YogaRow(app.displayLabel, Modifier.padding(start = 16.dp), onClick = { recategorize = app })
            }
        }
        item { YogaPrimaryButton("New category", { adding = true }, Modifier.padding(horizontal = ScreenMargin, vertical = 20.dp)) }
    }
    if (adding) NameDialog("New category", "", recentNames.value, onDismiss = { adding = false }) {
        recordRecentEntry(context, RECENT_CATEGORY_NAMES, recentNames, it); viewModel.addCategory(it); adding = false
    }
    editing?.let { category -> NameDialog("Rename category", category.name, recentNames.value,
        onDelete = { viewModel.deleteCategory(category); editing = null }, onDismiss = { editing = null }) {
            recordRecentEntry(context, RECENT_CATEGORY_NAMES, recentNames, it); viewModel.renameCategory(category, it); editing = null
        } }
    recategorize?.let { app -> CategoryPickerDialog(app, state.categories, state.categoryOf[app.appPackage],
        { viewModel.setAppCategory(app, it) }, { viewModel.addCategory(it, app) }, { recategorize = null }) }
}

@Composable
private fun NameDialog(title: String, initial: String, recent: List<String>, onDelete: (() -> Unit)? = null, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    YogaDialog(title, "Save", { onSave(name.trim()) }, onDismiss, confirmEnabled = name.isNotBlank(), text = {
        Column {
            YogaTextField(name, { name = it.take(24) }, "Name")
            if (recent.isNotEmpty()) YogaChipRow { recent.forEach { value -> YogaChip(value, name == value, onClick = { name = value }) } }
            onDelete?.let { YogaTextButton("Delete", it, Modifier.padding(horizontal = TextButtonInset), muted = true) }
        }
    })
}
