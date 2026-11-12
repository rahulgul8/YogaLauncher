package `in`.tuwi.yogaphone.pro

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.YogaApplication
import `in`.tuwi.yogaphone.ui.theme.*

data class ProAccess(val state: ProState = ProState(), val open: () -> Unit = {}) {
    fun allows(feature: ProFeature) = ProGate.allows(feature, state)
    fun run(feature: ProFeature, action: () -> Unit) { if (allows(feature)) action() else open() }
}
val LocalProAccess = staticCompositionLocalOf { ProAccess() }

@Composable
fun ProTag() { Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
    Text("Pro", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
} }

@Composable
fun ProRow(title: String, feature: ProFeature, summary: String? = null, onClick: () -> Unit) {
    val access = LocalProAccess.current
    YogaRow(title, summary = summary, onClick = { access.run(feature, onClick) }, trailing = {
        if (!access.allows(feature)) ProTag() else RowChevron()
    })
}

@Composable
fun ProSwitchRow(title: String, feature: ProFeature, checked: Boolean, onCheckedChange: (Boolean) -> Unit, summary: String? = null) {
    val access = LocalProAccess.current
    if (access.allows(feature)) YogaSwitchRow(title, checked, onCheckedChange, summary = summary)
    else ProRow(title, feature, summary, access.open)
}

fun ProState.summary(): String = when (entitlement) {
    Entitlement.TRIAL -> "Trial · $trialDaysLeft days left"
    Entitlement.PRO -> when (plan) { ProPlan.LIFETIME -> "Lifetime"; ProPlan.MONTHLY -> "Monthly"; else -> "Yearly" }
    Entitlement.FREE -> "Free · See what Pro adds"
}

@Composable
fun ProScreen(repository: ProRepository, onBack: () -> Unit) {
    val state by repository.state.collectAsState()
    val products by repository.products.collectAsState()
    val message by repository.message.collectAsState()
    var selected by remember { mutableStateOf(ProPlan.YEARLY) }
    val activity = LocalContext.current as Activity
    val product = products.firstOrNull { it.plan == selected }
    YogaScreen("Yoga Pro", onBack, eyebrow = "More room for what matters") {
        if (state.entitlement == Entitlement.PRO) {
            item { YogaCard { Text("You have Yoga Pro", style = MaterialTheme.typography.headlineSmall); Text(state.summary(), style = MaterialTheme.typography.bodyLarge) } }
        } else {
            if (state.entitlement == Entitlement.TRIAL) item { YogaNote("Everything is unlocked. Your trial has ${state.trialDaysLeft} days left.") }
            item { YogaNote("Your home screen and apps stay free, always.") }
            item { YogaSection("Included") }
            item { YogaNote("All looks and icons · Value of time and its live pill\nUnlimited allowances and mindful pauses · Reminders\nFocus, timebox and Do Not Disturb\nInsights, weekly reports and longer history\nQuiet notifications · Profiles · Widgets\nLearning videos") }
            if (products.isEmpty()) item { YogaNote("Yoga Pro isn't available yet") }
            else {
                products.forEach { offer -> item {
                    YogaCard(Modifier.padding(top = 12.dp)) {
                        YogaRadioRow(when (offer.plan) { ProPlan.YEARLY -> "Yearly"; ProPlan.MONTHLY -> "Monthly"; ProPlan.LIFETIME -> "Lifetime" },
                            selected == offer.plan, { selected = offer.plan }, summary = offer.price)
                        Text(when (offer.plan) { ProPlan.YEARLY -> "Best value"; ProPlan.MONTHLY -> "Month to month"; ProPlan.LIFETIME -> "Once, forever" },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } }
                item { YogaPrimaryButton("Get Yoga Pro", { product?.let { repository.buy(activity, it) } },
                    Modifier.padding(horizontal = ScreenMargin, vertical = 16.dp), enabled = product != null) }
                if (selected != ProPlan.LIFETIME) item { YogaNote("Billed by Google Play. Cancel any time in Play Store → Subscriptions.") }
            }
        }
        item { YogaTextButton("Restore purchases", repository::restore, Modifier.padding(horizontal = TextButtonInset)) }
        message?.let { item { YogaNote(it) } }
    }
}

@Composable
fun TrialEndingCard(blockedToday: Int, hourlyValue: String?) {
    val repository = (LocalContext.current.applicationContext as YogaApplication).proRepository
    val state by repository.state.collectAsState()
    val dismissed by repository.trialDismissed.collectAsState(initial = true)
    val access = LocalProAccess.current
    if (dismissed || state.entitlement != Entitlement.TRIAL || state.trialDaysLeft !in 1..5) return
    YogaCard(Modifier.padding(vertical = 8.dp)) {
        Row {
            Text("Your Yoga Pro trial ends in ${state.trialDaysLeft} days", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = repository::dismissTrialCard) { Icon(YogaIcons.Close, contentDescription = "Dismiss trial reminder") }
        }
        if (blockedToday > 0) Text("Shorts and Reels blocked $blockedToday times today.", style = MaterialTheme.typography.bodyMedium)
        hourlyValue?.let { Text("Your time value: $it per hour.", style = MaterialTheme.typography.bodySmall) }
        YogaTextButton("Keep Yoga Pro", access.open)
    }
}

@Composable
fun DebugProControls() {
    if (!`in`.tuwi.yogaphone.BuildConfig.DEBUG) return
    val repository = (LocalContext.current.applicationContext as YogaApplication).proRepository
    val override by repository.debugOverride.collectAsState(initial = null)
    var shown by remember { mutableStateOf(false) }
    YogaTextButton("Developer testing", { shown = !shown }, muted = true)
    if (shown) {
        YogaNote("Entitlement override (debug build only)")
        YogaChipRow {
            YogaChip("Automatic", override == null, { repository.setDebugOverride(null) })
            Entitlement.entries.forEach { value -> YogaChip(value.name, override == value.name, { repository.setDebugOverride(value) }) }
        }
    }
}
