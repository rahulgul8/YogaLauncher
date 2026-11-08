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
    val Home = line("home", "M4 11l8-6.5 8 6.5", "M6 9.5v10h12v-10", "M10 19.5v-5h4v5")
    val Trend = line("trend", "M4 17l5-5 4 3 7-7", "M15 8h5v5")
    val Gauge = line("gauge", "M4 17a8 8 0 0 1 16 0", "M12 17l4-5", "M6.5 11.5l1 .8", "M17.5 11.5l-1 .8", "M12 9v1.2")
    val Coin = line("coin", "M4.5 12a7.5 7.5 0 1 0 15 0a7.5 7.5 0 1 0 -15 0", "M12 7.5v9",
        "M14.3 9.6c-.4-.8-1.2-1.3-2.3-1.3-1.3 0-2.3.7-2.3 1.8s1 1.5 2.3 1.9 2.3.8 2.3 1.9-1 1.8-2.3 1.8c-1.1 0-1.9-.5-2.3-1.3")
    val Shield = line("shield", "M12 3.5l7 2.6v5.4c0 4.4-2.9 7.7-7 9-4.1-1.3-7-4.6-7-9V6.1z", "M9 12l2.2 2.2L15.2 10")
    val Contrast = line("contrast", "M4.5 12a7.5 7.5 0 1 0 15 0a7.5 7.5 0 1 0 -15 0", "M12 4.5v15", "M12 8h5.5", "M12 12h7.5", "M12 16h5.5")
    val BellOff = line("bell_off", "M8.6 5.6A5.5 5.5 0 0 1 17.5 10v4.5", "M6.5 10.5v6l-1.5 1.5h12", "M10 20.5h4", "M4 4l16 16")
    val Type = line("type", "M5 7.5V5h14v2.5", "M12 5v14", "M9 19h6")
    val Widgets = line("widgets", "M4.5 4.5h6v6h-6z", "M13.5 4.5h6v6h-6z", "M4.5 13.5h6v6h-6z", "M16.5 13.5v6", "M13.5 16.5h6")
    val Layers = line("layers", "M12 4l8 4-8 4-8-4z", "M4 12l8 4 8-4", "M4 16l8 4 8-4")
    val Tag = line("tag", "M4 4h7l9 9-7 7-9-9z", "M7.5 8a.5.5 0 1 0 1 0a.5.5 0 1 0 -1 0")
    val EyeOff = line("eye_off", "M2.5 12s3.5-6.5 9.5-6.5c1.5 0 2.8.4 4 1", "M19.4 8.7c1.4 1.5 2.1 3.3 2.1 3.3s-3.5 6.5-9.5 6.5c-1.6 0-3-.5-4.2-1.1",
        "M9.9 13.6a2.5 2.5 0 0 1 3.7-3.4", "M4 4l16 16")
    val Play = line("play", "M3.5 7a2 2 0 0 1 2-2h13a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2h-13a2 2 0 0 1-2-2z", "M10 9.5v5l4.5-2.5z")
    val Lock = line("lock", "M5.5 11h13v9h-13z", "M8.5 11V8a3.5 3.5 0 0 1 7 0v3", "M12 14.5v2")
    val Help = line("help", "M4.5 12a7.5 7.5 0 1 0 15 0a7.5 7.5 0 1 0 -15 0", "M9.7 9.6a2.4 2.4 0 1 1 3.3 2.2c-.6.3-1 .8-1 1.4v.6", "M12 16.6v.1")
    val Sparkle = line("sparkle", "M12 3.5l1.8 5.2 5.2 1.8-5.2 1.8L12 17.5l-1.8-5.2L5 10.5l5.2-1.8z", "M18.5 16v4", "M16.5 18h4")

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
