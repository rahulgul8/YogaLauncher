package `in`.tuwi.yogaphone.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.YogaApplication
import `in`.tuwi.yogaphone.data.AppModel
import `in`.tuwi.yogaphone.focus.FocusPreferences
import `in`.tuwi.yogaphone.pro.*
import `in`.tuwi.yogaphone.ui.theme.*
import `in`.tuwi.yogaphone.usage.*
import kotlinx.coroutines.launch

/**
 * Everything about one app in one place (L-F1): its type, the pause before opening, its daily limit, focus and its
 * drawer category. Loosening any of them (towards Essential, pause off, a higher or removed limit) goes through
 * [LoosenSheet], as ADR 0015 asks; tightening is instant.
 */
@Composable
fun AppSettingsSheet(
    app: AppModel,
    state: UsageUiState,
    viewModel: UsageViewModel,
    onDismiss: () -> Unit,
    categoryName: String? = null,
    onCategory: (() -> Unit)? = null,
) {
    val access = LocalProAccess.current
    val context = LocalContext.current
    val yoga = context.applicationContext as YogaApplication
    val scope = rememberCoroutineScope()
    val focus by yoga.focusPrefs.preferences.collectAsState(initial = FocusPreferences())
    val current = state.apps.firstOrNull { it.appPackage == app.appPackage } ?: app
    val type = AppSettingsLogic.typeOf(current.isEssential, current.isDistracting)
    val rule = state.rules.firstOrNull { it.scope == "APP" && it.targetPackage == app.appPackage }
        ?: state.rules.firstOrNull { it.targetPackage == app.appPackage && it.scope != "CATEGORY" }
    val freeIds = freeQuota(state.rules.filter { it.enabled }.map { it.id }, 1)
    val canEditLimit = access.allows(ProFeature.EXTRA_ALLOWANCES) || freeIds.isEmpty() || "app:${app.appPackage}" in freeIds
    val mandatory = remember(app.appPackage) { app.appPackage in AppClassifier(context).mandatoryEssential() }
    val pauseOn = app.appPackage !in focus.delayOptOut
    var editLimit by remember { mutableStateOf(false) }
    var loosen by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    fun setType(next: AppType) {
        if (next == type) return
        val apply = { val (essential, distracting) = AppSettingsLogic.flags(next); viewModel.classify(app.appPackage, essential, distracting); Unit }
        if (AppSettingsLogic.isLoosening(type, next)) loosen = "Make ${current.displayLabel} ${next.label.lowercase()}?" to apply
        else apply()
    }

    YogaSheet(onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Text(current.displayLabel, style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = ScreenMargin).padding(top = 16.dp))
            Text(AppSettingsLogic.summary(type, pauseOn, rule?.dailyAllowanceMinutes), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = ScreenMargin, vertical = 4.dp))

            YogaSection("Type")
            YogaChipRow {
                AppType.entries.forEach { option ->
                    YogaChip(option.label, type == option, onClick = { setType(option) }, enabled = !mandatory || option == AppType.ESSENTIAL)
                }
            }
            YogaNote(when {
                mandatory -> "Phone, SMS, system settings and Yoga are always essential."
                type == AppType.ESSENTIAL -> "Never counted, never paused."
                type == AppType.DISTRACTING -> "Counted towards your daily allowance, and Yoga can pause before it opens."
                else -> if (state.preferences.counts == AllowanceCounts.ALL_EXCEPT_ESSENTIAL) "Counted towards your daily allowance, never paused."
                    else "Not counted and never paused, but you can still give it its own limit."
            })

            if (type == AppType.DISTRACTING) {
                val activeIds = state.apps.filter { it.isDistracting && it.appPackage !in focus.delayOptOut }.map { it.appPackage }
                val withinFree = app.appPackage in freeQuota(activeIds, 3) || activeIds.size < 3
                if (!access.allows(ProFeature.EXTRA_MINDFUL_APPS) && !withinFree && !pauseOn)
                    ProRow("Pause before opening", ProFeature.EXTRA_MINDFUL_APPS, onClick = access.open)
                else YogaSwitchRow("Pause before opening", pauseOn, { on ->
                    val apply = { scope.launch { yoga.focusPrefs.setDelayOptOut(app.appPackage, !on) }; Unit }
                    if (on) apply() else loosen = "Turn the pause off for ${current.displayLabel}?" to apply
                })
                if (!focus.delayEnabled) YogaNote("The pause is off for every app. Turn it on in Focus and timebox.")
            }

            if (type != AppType.ESSENTIAL) {
                if (!canEditLimit) ProRow("Daily limit", ProFeature.EXTRA_ALLOWANCES, onClick = access.open)
                else YogaRow("Daily limit", summary = rule?.let { "${it.dailyAllowanceMinutes} min a day · closes the app when used" } ?: "Off",
                    onClick = { editLimit = true })
            }

            if (access.allows(ProFeature.FOCUS) && type == AppType.DISTRACTING) {
                val allowed = app.appPackage in focus.focusAllowed
                YogaSwitchRow("Allowed during focus", allowed, { on ->
                    val apply = { scope.launch { yoga.focusPrefs.setFocusAllowed(app.appPackage, on) }; Unit }
                    if (on) loosen = "Allow ${current.displayLabel} during focus?" to apply else apply()
                })
            }

            onCategory?.let { open -> YogaRow("Drawer category", summary = categoryName ?: "None", onClick = open) }
        }
    }
    if (editLimit) AllowanceEditor("${current.displayLabel} a day", rule?.dailyAllowanceMinutes, { editLimit = false }) { minutes ->
        val apply = { viewModel.saveRule(app.appPackage, minutes); Unit }
        val previous = rule?.dailyAllowanceMinutes
        if (AppSettingsLogic.limitIsLoosening(previous, minutes))
            loosen = (if (minutes == null) "Remove the limit for ${current.displayLabel}?" else "Raise the limit for ${current.displayLabel}?") to apply
        else apply()
    }
    loosen?.let { (title, apply) -> LoosenSheet(title, null, "change it", apply) { loosen = null } }
}

/** Every drawer app with what Yoga does with it, searchable; tapping one opens [AppSettingsSheet]. */
@Composable
fun YourAppsScreen(
    apps: List<AppModel>,
    state: UsageUiState,
    viewModel: UsageViewModel,
    onBack: () -> Unit,
    categoryName: (AppModel) -> String? = { null },
    onCategory: ((AppModel) -> Unit)? = null,
) {
    val yoga = LocalContext.current.applicationContext as YogaApplication
    val focus by yoga.focusPrefs.preferences.collectAsState(initial = FocusPreferences())
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<AppModel?>(null) }
    val classified = apps.distinctBy { it.appPackage }.map { listed -> state.apps.firstOrNull { it.appPackage == listed.appPackage } ?: listed }
    val ordered = AppSettingsLogic.order(classified, query, { it.displayLabel }) { AppSettingsLogic.typeOf(it.isEssential, it.isDistracting) }
    YogaScreen("Your apps", onBack, subtitle = "Choose how Yoga treats each app") {
        item(key = "search") { YogaTextField(query, { query = it }, "Find an app") }
        if (ordered.isEmpty()) item(key = "empty") { YogaNote("No app matches \"$query\".") }
        items(ordered, key = { "your-app:${it.appPackage}" }) { app ->
            val rule = state.rules.firstOrNull { it.scope == "APP" && it.targetPackage == app.appPackage }
            YogaRow(app.displayLabel, summary = AppSettingsLogic.summary(AppSettingsLogic.typeOf(app.isEssential, app.isDistracting),
                app.appPackage !in focus.delayOptOut, rule?.dailyAllowanceMinutes), onClick = { selected = app })
        }
    }
    selected?.let { app -> AppSettingsSheet(app, state, viewModel, { selected = null }, categoryName(app), onCategory?.let { open -> { selected = null; open(app) } }) }
}
