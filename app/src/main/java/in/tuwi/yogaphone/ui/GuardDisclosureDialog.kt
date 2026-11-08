package `in`.tuwi.yogaphone.ui

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.ui.theme.YogaDialog

/** Google-reviewed disclosure: keep this copy verbatim. */
@Composable
fun GuardDisclosureDialog(onConfirm: () -> Unit, onDismissRequest: () -> Unit) {
    YogaDialog(
        title = "Allow Yoga guard accessibility access?", confirmText = "Agree and open settings",
        onConfirm = onConfirm, onDismissRequest = onDismissRequest, dismissText = "Not now",
        text = { Text("Yoga guard reads which app is open to show the optional live time-value indicator. For the surfaces you enable, it reads the screen structure of YouTube, Instagram and the selected browsers (Chrome, Samsung Internet and Firefox), only to recognise Shorts and Reels. It can go back or home to block those surfaces.\n\nScreen content is never stored, logged or sent anywhere. Only your settings, daily allowance time and block counts are saved on this phone. You can turn Yoga guard off any time in Android accessibility settings.", modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) },
    )
}
