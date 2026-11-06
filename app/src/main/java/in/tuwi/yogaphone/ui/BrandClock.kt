package `in`.tuwi.yogaphone.ui

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import java.util.Calendar
import java.util.Date

/** The home clock. Its colon is the brand mark: a brass dot over a plain one. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrandClock(now: Long, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val is24 = DateFormat.is24HourFormat(context)
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    val hour = if (is24) cal.get(Calendar.HOUR_OF_DAY) else cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
    val hours = if (is24) "%02d".format(hour) else hour.toString()
    val minutes = "%02d".format(cal.get(Calendar.MINUTE))
    val style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "lnum, tnum")
    val spoken = DateFormat.getTimeFormat(context).format(Date(now))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick, role = Role.Button, onClickLabel = "Open clock")
            .clearAndSetSemantics { contentDescription = spoken },
    ) {
        Text(hours, style = style, color = MaterialTheme.colorScheme.onBackground)
        BrandColon(style.fontSize)
        Text(minutes, style = style, color = MaterialTheme.colorScheme.onBackground)
        if (!is24) Text(
            if (cal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Top).padding(top = 22.dp, start = 10.dp),
        )
    }
}

@Composable
fun BrandColon(fontSize: TextUnit, modifier: Modifier = Modifier) {
    val dot = with(LocalDensity.current) { (fontSize * 0.075f).toDp() }
    Column(
        verticalArrangement = Arrangement.spacedBy(dot * 1.9f),
        modifier = modifier.padding(horizontal = dot * 1.1f).offset(y = dot * 0.35f),
    ) {
        Box(Modifier.size(dot).background(MaterialTheme.colorScheme.primary, CircleShape))
        Box(Modifier.size(dot).background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.82f), CircleShape))
    }
}
