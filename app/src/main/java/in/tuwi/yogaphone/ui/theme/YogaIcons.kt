package `in`.tuwi.yogaphone.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Yoga's own line icons: 24-unit grid, 1.6 stroke, round caps. One family for the whole app so
 * nothing looks borrowed. Tint them with `Icon(tint = …)`.
 */
object YogaIcons {
    val Settings = line("settings",
        "M4 7h9", "M19 7h1", "M4 17h3", "M13 17h7",
        "M14 7a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0 -5 0",
        "M7 17a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0 -5 0")
    val Search = line("search", "M4.5 11a6.5 6.5 0 1 0 13 0a6.5 6.5 0 1 0 -13 0", "M16 16l4.5 4.5")
    val Phone = line("phone", "M6.5 3.5h3l1.6 4.4-2.2 1.4a11.5 11.5 0 0 0 5.8 5.8l1.4-2.2 4.4 1.6v3a2 2 0 0 1-2.2 2A16.5 16.5 0 0 1 4.5 5.7a2 2 0 0 1 2-2.2z")
    val Camera = line("camera", "M3.5 8.5a1.5 1.5 0 0 1 1.5-1.5h2.5l1.8-2.5h5.4l1.8 2.5H19a1.5 1.5 0 0 1 1.5 1.5v9.5A1.5 1.5 0 0 1 19 19.5H5A1.5 1.5 0 0 1 3.5 18z",
        "M8.5 13a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0 -7 0")
    val Message = line("message", "M20.5 11.5a8 8 0 0 1-11.7 7.1L4 19.8l1.2-4.4A8 8 0 1 1 20.5 11.5z")
    val Browser = line("browser", "M3.5 12a8.5 8.5 0 1 0 17 0a8.5 8.5 0 1 0 -17 0", "M3.5 12h17", "M12 3.5c2.4 2.4 3.5 5.3 3.5 8.5s-1.1 6.1-3.5 8.5", "M12 3.5C9.6 5.9 8.5 8.8 8.5 12s1.1 6.1 3.5 8.5")
    val Timebox = line("timebox", "M7 3.5h10", "M7 20.5h10", "M8 3.5c0 4.5 8 4.5 8 8.5s-8 4-8 8.5", "M16 3.5c0 4.5-8 4.5-8 8.5s8 4 8 8.5")
    val Focus = line("focus", "M19.5 14.5A8 8 0 1 1 9.5 4.5a6.2 6.2 0 0 0 10 10z")
    val Insights = line("insights", "M4 20h16", "M6.5 16.5v-5", "M11 16.5V6", "M15.5 16.5v-8", "M20 16.5v-3")
    val ChevronDown = line("chevron_down", "M7 10l5 5 5-5")
    val ChevronRight = line("chevron_right", "M10 7l5 5-5 5")
    val Back = line("back", "M15 5l-7 7 7 7")
    val Close = line("close", "M6.5 6.5l11 11", "M17.5 6.5l-11 11")
    val Plus = line("plus", "M12 5v14", "M5 12h14")
    val App = line("app", "M5 5h5v5H5z", "M14 5h5v5h-5z", "M5 14h5v5H5z", "M14 14h5v5h-5z")
    val Check = line("check", "M5 12.5l4.5 4.5L19 7.5")

    private fun line(name: String, vararg paths: String): ImageVector {
        val builder = ImageVector.Builder(name = "Yoga.$name", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f)
        paths.forEach { data ->
            builder.addPath(
                pathData = addPathNodes(data),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return builder.build()
    }
}
