package `in`.tuwi.yogaphone.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.Telephony
import android.telecom.TelecomManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import `in`.tuwi.yogaphone.data.AppModel
import `in`.tuwi.yogaphone.data.DOCK_DEFAULT
import `in`.tuwi.yogaphone.data.DOCK_EMPTY
import `in`.tuwi.yogaphone.ui.theme.YogaIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The phone's own app for each dock position, used while a slot is left on its default. */
enum class DockRole(val label: String, val icon: ImageVector) {
    PHONE("Phone", YogaIcons.Phone),
    CAMERA("Camera", YogaIcons.Camera),
    MESSAGES("Messages", YogaIcons.Message),
    BROWSER("Browser", YogaIcons.Browser),
}

/** One resolved dock slot. [role] is set when the slot shows the phone's default app for that position. */
data class DockSlot(val index: Int, val app: AppModel?, val role: DockRole?) {
    val isEmpty: Boolean get() = app == null
    val label: String get() = app?.displayLabel ?: "Add an app"
}

fun resolveDock(context: Context, slots: List<String>, apps: List<AppModel>): List<DockSlot> {
    val byPackage = apps.associateBy { it.appPackage }
    return slots.mapIndexed { index, value ->
        val role = DockRole.entries.getOrNull(index)
        when (value) {
            DOCK_EMPTY -> DockSlot(index, null, null)
            DOCK_DEFAULT -> {
                val app = role?.let { defaultPackage(context, it) }?.let(byPackage::get)
                DockSlot(index, app, role.takeIf { app != null })
            }
            else -> DockSlot(index, byPackage[value], null)
        }
    }
}

private fun defaultPackage(context: Context, role: DockRole): String? = runCatching {
    when (role) {
        DockRole.PHONE -> context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage
            ?: resolve(context, Intent(Intent.ACTION_DIAL))
        DockRole.MESSAGES -> Telephony.Sms.getDefaultSmsPackage(context)
            ?: resolve(context, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")))
        DockRole.CAMERA -> resolve(context, Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
        DockRole.BROWSER -> resolve(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
            .addCategory(Intent.CATEGORY_BROWSABLE))
    }
}.getOrNull()

/** The default handler, or the first one when the user hasn't picked a default (the resolver is "android"). */
private fun resolve(context: Context, intent: Intent): String? {
    val pm = context.packageManager
    val default = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
    if (default != null && default != "android") return default
    return pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).firstOrNull()?.activityInfo?.packageName
}

/**
 * Quick-launch dock: two slots, the search pill, two slots. Default slots use Yoga's own line icons;
 * apps the user picked show their icon in monochrome so the row stays calm.
 */
@Composable
fun HomeDock(
    slots: List<DockSlot>,
    onSearch: () -> Unit,
    onSlotClick: (DockSlot) -> Unit,
    onSlotLongClick: (DockSlot) -> Unit,
    modifier: Modifier = Modifier,
    /** Apps not loaded yet: keep the slots blank instead of flashing "+" for apps that are there. */
    loading: Boolean = false,
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val slot: @Composable (DockSlot) -> Unit = {
            if (loading) Spacer(Modifier.size(52.dp)) else DockButton(it, onSlotClick, onSlotLongClick)
        }
        slots.take(2).forEach { slot(it) }
        SearchPill(onSearch)
        slots.drop(2).forEach { slot(it) }
    }
}

@Composable
private fun SearchPill(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.height(48.dp),
    ) {
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(YogaIcons.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text("Search", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockButton(slot: DockSlot, onClick: (DockSlot) -> Unit, onLongClick: (DockSlot) -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(52.dp).clip(CircleShape)
            .combinedClickable(
                onClick = { onClick(slot) }, onLongClick = { onLongClick(slot) }, role = Role.Button,
                onClickLabel = if (slot.isEmpty) "Choose an app" else "Open ${slot.label}",
                onLongClickLabel = "Change this shortcut",
            ),
    ) {
        val tint = MaterialTheme.colorScheme.onBackground
        when {
            slot.isEmpty -> Icon(YogaIcons.Plus, contentDescription = "Add an app to the dock", tint = tint,
                modifier = Modifier.size(20.dp).alpha(0.35f))
            slot.role != null -> Icon(slot.role.icon, contentDescription = slot.label, tint = tint.copy(alpha = 0.9f),
                modifier = Modifier.size(24.dp))
            else -> AppGlyph(slot.app!!, Modifier.size(28.dp))
        }
    }
}

private val Monochrome = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/** An app's own icon, desaturated. */
@Composable
fun AppGlyph(app: AppModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, app.appPackage) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.packageManager.getApplicationIcon(app.appPackage).toBitmap(96, 96).asImageBitmap() }.getOrNull()
        }
    }
    bitmap?.let { Image(it, contentDescription = app.displayLabel, colorFilter = Monochrome, modifier = modifier.alpha(0.85f)) }
        ?: Icon(YogaIcons.App, contentDescription = app.displayLabel, tint = MaterialTheme.colorScheme.onBackground, modifier = modifier)
}
