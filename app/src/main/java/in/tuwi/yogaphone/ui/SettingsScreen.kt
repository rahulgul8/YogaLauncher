package `in`.tuwi.yogaphone.ui

import `in`.tuwi.yogaphone.pro.*
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment
import `in`.tuwi.yogaphone.ui.theme.YogaCard
import `in`.tuwi.yogaphone.ui.theme.YogaEyebrow
import `in`.tuwi.yogaphone.ui.theme.YogaHeader
import `in`.tuwi.yogaphone.ui.theme.YogaNote
import `in`.tuwi.yogaphone.ui.theme.YogaPrimaryButton
import `in`.tuwi.yogaphone.ui.theme.YogaRow
import `in`.tuwi.yogaphone.ui.theme.YogaScreen
import `in`.tuwi.yogaphone.ui.theme.YogaSecondaryButton
import `in`.tuwi.yogaphone.ui.theme.YogaSection
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import `in`.tuwi.yogaphone.ui.theme.RowChevron
import `in`.tuwi.yogaphone.ui.theme.ScreenMargin
import `in`.tuwi.yogaphone.ui.theme.YogaGroup
import `in`.tuwi.yogaphone.ui.theme.YogaGroupRow
import `in`.tuwi.yogaphone.ui.theme.YogaIconTile
import `in`.tuwi.yogaphone.ui.theme.YogaIcons
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAppearance: () -> Unit,
    onHiddenApps: () -> Unit,
    onResetNames: () -> Unit,
    onUsage: () -> Unit,
    onUsageRules: () -> Unit,
    onValueOfTime: () -> Unit,
    onGuard: () -> Unit,
    onFocus: () -> Unit,
    onProfiles: () -> Unit,
    onCategories: () -> Unit,
    onVisual: () -> Unit,
    onQuiet: () -> Unit,
    onInsights: () -> Unit,
    onPrivacy: () -> Unit,
    onHelp: () -> Unit,
    onWidgets: () -> Unit,
    onAdvanced: () -> Unit,
    onLearning: () -> Unit,
) {
    val context = LocalContext.current
    var isDefault by remember { mutableStateOf(isDefaultHome(context)) }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefault = isDefaultHome(context)
    }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) isDefault = isDefaultHome(context)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    val access = LocalProAccess.current
    YogaScreen("Settings", onBack) {
        item(key = "pro") { ProHeroCard(access.state, access.open) }
        // Only while Yoga isn't the home app. Leaving Yoga lives under Advanced, a few taps away on purpose.
        if (!isDefault) item(key = "home-card") {
            YogaGroup(Modifier.padding(top = 12.dp)) {
                YogaGroupRow("Make Yoga your home", YogaIcons.Home, summary = "Android asks once", divider = false,
                    onClick = { runCatching { roleLauncher.launch(defaultHomeIntent(context)) } },
                    trailing = { SmallPill("Set") { runCatching { roleLauncher.launch(defaultHomeIntent(context)) } } })
            }
        }
        item { YogaSection("Awareness") }
        item {
            YogaGroup {
                YogaGroupRow("Usage", YogaIcons.Insights, summary = "Today and this week", onClick = onUsage)
                GatedRow("Insights", YogaIcons.Trend, ProFeature.INSIGHTS, "Trends and goals", onInsights)
                YogaGroupRow("Allowances", YogaIcons.Gauge, summary = "Limits and reminders", onClick = onUsageRules)
                GatedRow("Value of time", YogaIcons.Coin, ProFeature.VALUE_OF_TIME, "Your hour, priced", onValueOfTime,
                    divider = `in`.tuwi.yogaphone.BuildConfig.GUARD)
                if (`in`.tuwi.yogaphone.BuildConfig.GUARD)
                    GatedRow("Learning videos", YogaIcons.Play, ProFeature.LEARNING_VIDEOS, "Channels that don't count", onLearning)
                if (`in`.tuwi.yogaphone.BuildConfig.GUARD)
                    YogaGroupRow("Yoga guard", YogaIcons.Shield, summary = "Shorts, Reels, Status, live pill", divider = false, onClick = onGuard)
            }
        }
        item { YogaSection("Focus") }
        item {
            YogaGroup {
                YogaGroupRow("Focus and timebox", YogaIcons.Focus, summary = "Pause, schedules, DND", onClick = onFocus)
                YogaGroupRow("Greyscale", YogaIcons.Contrast, summary = "Past limits, at bedtime", onClick = onVisual)
                GatedRow("Quiet notifications", YogaIcons.BellOff, ProFeature.QUIET_NOTIFICATIONS, "A calm digest", onQuiet, divider = false)
            }
        }
        item { YogaSection("Home") }
        item {
            YogaGroup {
                YogaGroupRow("Appearance", YogaIcons.Type, summary = "Looks, type, clock", onClick = onAppearance)
                GatedRow("Widgets", YogaIcons.Widgets, ProFeature.WIDGETS, "Under the clock", onWidgets, divider = false)
            }
        }
        item { YogaSection("Apps") }
        item {
            YogaGroup {
                GatedRow("Profiles", YogaIcons.Layers, ProFeature.PROFILES, "Homes for work and rest", onProfiles)
                YogaGroupRow("Categories", YogaIcons.Tag, summary = "Drawer groups", onClick = onCategories)
                YogaGroupRow("Hidden apps", YogaIcons.EyeOff, summary = "Review and unhide", divider = false, onClick = onHiddenApps)
            }
        }
        item { YogaSection("Privacy and help") }
        item {
            YogaGroup {
                YogaGroupRow("Privacy", YogaIcons.Lock, summary = "Stays on this phone", onClick = onPrivacy)
                YogaGroupRow("Help", YogaIcons.Help, summary = "Setup and questions", onClick = onHelp)
                YogaGroupRow("Advanced", YogaIcons.Settings, summary = "Android, names, home", divider = false, onClick = onAdvanced)
            }
        }
        item(key = "about") {
            Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                BrandColon(28.sp)
                Text("Yoga Phone", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 14.dp))
                Text("Version ${versionName(context)}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

/** A group row for a Pro feature: chevron when allowed, the Pro tag (and the Pro screen on tap) when not. */
@Composable
private fun GatedRow(title: String, icon: ImageVector, feature: ProFeature, summary: String, onClick: () -> Unit, divider: Boolean = true) {
    val access = LocalProAccess.current
    YogaGroupRow(title, icon, summary = summary, divider = divider, onClick = { access.run(feature, onClick) },
        trailing = { if (access.allows(feature)) RowChevron() else ProTag() })
}

/** The top of Settings: Yoga Pro's state at a glance, with the trial drawn as a thin brass line. */
@Composable
private fun ProHeroCard(state: ProState, onOpen: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val (title, line) = when (state.entitlement) {
        Entitlement.TRIAL -> "Everything is unlocked" to "Trial · ${state.trialDaysLeft} of 14 days left"
        Entitlement.PRO -> "You have Yoga Pro" to state.summary()
        Entitlement.FREE -> "Get more from Yoga" to "Monthly, yearly or lifetime"
    }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = ScreenMargin).padding(top = 8.dp)
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.20f), accent.copy(alpha = 0.05f))))
            .border(1.dp, accent.copy(alpha = 0.28f), MaterialTheme.shapes.large)
            .clickable(onClick = onOpen)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            YogaIconTile(YogaIcons.Sparkle)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                YogaEyebrow("Yoga Pro")
                Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp))
            }
            RowChevron()
        }
        if (state.entitlement == Entitlement.TRIAL) {
            Box(Modifier.padding(top = 18.dp).fillMaxWidth().height(3.dp).clip(CircleShape).background(accent.copy(alpha = 0.18f))) {
                Box(Modifier.fillMaxWidth(state.trialDaysLeft / 14f).height(3.dp).clip(CircleShape).background(accent))
            }
        }
        Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = if (state.entitlement == Entitlement.TRIAL) 8.dp else 12.dp))
    }
}

/** A compact ivory pill for a single action at the end of a row. */
@Composable
private fun SmallPill(label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = MaterialTheme.colorScheme.onBackground) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.background,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
    }
}

@Composable
fun SettingsHeader(title: String, onBack: () -> Unit) = YogaHeader(title, onBack)

@Composable
fun SectionTitle(text: String) = YogaSection(text)

internal fun defaultHomeIntent(context: Context): Intent {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roles = context.getSystemService(RoleManager::class.java)
        if (roles.isRoleAvailable(RoleManager.ROLE_HOME)) return roles.createRequestRoleIntent(RoleManager.ROLE_HOME)
    }
    return Intent(Settings.ACTION_HOME_SETTINGS)
}

internal fun isDefaultHome(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roles = context.getSystemService(RoleManager::class.java)
        if (roles.isRoleAvailable(RoleManager.ROLE_HOME)) return roles.isRoleHeld(RoleManager.ROLE_HOME)
    }
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val resolved = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
    return resolved?.activityInfo?.packageName == context.packageName
}

private fun versionName(context: Context): String = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0)).versionName
    } else {
        @Suppress("DEPRECATION") context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }
}.getOrNull() ?: "—"
