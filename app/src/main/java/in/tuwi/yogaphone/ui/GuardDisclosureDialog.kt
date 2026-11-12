package `in`.tuwi.yogaphone.ui

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.ui.theme.YogaDialog

/** Google-reviewed disclosure: update the declaration and video before the next open-testing review. */
@Composable
fun GuardDisclosureDialog(onConfirm: () -> Unit, onDismissRequest: () -> Unit) {
    YogaDialog(
        title = "Allow Yoga guard accessibility access?", confirmText = "Agree and open settings",
        onConfirm = onConfirm, onDismissRequest = onDismissRequest, dismissText = "Not now",
        text = { Text("Yoga guard reads which app is open to show the optional live time-value indicator. For the surfaces you enable, it reads the screen structure of YouTube, Instagram, WhatsApp (including WhatsApp Business) and the selected browsers (Chrome, Samsung Internet and Firefox), to recognise Shorts, Reels and the full-screen Status viewer. Chats and calls still work. For Learning videos, it reads the YouTube channel name and video title in memory to match channels you add and offer learning hints. It can go back or home to block selected surfaces and enforce your Learning videos focus choices.\n\nScreen content is never stored, logged or sent anywhere. Only your settings, channels you choose to add, daily learning totals, daily allowance time, timed passes and aggregate block, pass and not now counts are saved on this phone. You can turn Yoga guard off any time in Android accessibility settings.", modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) },
    )
}
