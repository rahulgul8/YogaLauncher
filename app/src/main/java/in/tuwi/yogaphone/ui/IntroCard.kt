package `in`.tuwi.yogaphone.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.tuwi.yogaphone.intro.*
import `in`.tuwi.yogaphone.ui.theme.*

@Composable
fun IntroCard(intro: Intro, onAction: () -> Unit, onDismiss: (() -> Unit)?,
    spotIcon: (@Composable () -> Unit)? = null) {
    YogaCard {
        if (spotIcon != null) spotIcon() else Icon(when (intro.id) {
            IntroId.DAILY_ALLOWANCE, IntroId.LOWER_ALLOWANCE -> YogaIcons.Gauge
            IntroId.GREY_PAST_LIMIT -> YogaIcons.Contrast
            else -> YogaIcons.Shield
        }, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Text(intro.evidence, style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "lnum, tnum"))
        intro.note?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            YogaTextButton(intro.actionLabel, onAction)
            if (onDismiss != null) YogaTextButton("Not now", onDismiss, muted = true)
        }
    }
}
