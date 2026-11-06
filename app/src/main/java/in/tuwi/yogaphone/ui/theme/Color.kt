package `in`.tuwi.yogaphone.ui.theme

import androidx.compose.ui.graphics.Color
import `in`.tuwi.yogaphone.data.Palette

/** The tokens one palette needs in one brightness. Everything else in a colour scheme is derived from these. */
data class Tokens(
    val background: Color,
    val raised: Color,
    val high: Color,
    val highest: Color,
    val line: Color,
    val outline: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val onAccent: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    /** Time burning and over-limit states only. */
    val ember: Color,
)

/** Brand look: name for people, a short line, light and dark tokens. See docs/design/brand.md. */
data class PaletteSpec(val palette: Palette, val name: String, val line: String, val light: Tokens, val dark: Tokens)

// A · Obsidian and Brass: watch-dial luxury.
private val ObsidianDark = Tokens(
    background = Color(0xFF0F0F0E), raised = Color(0xFF181715), high = Color(0xFF211F1C), highest = Color(0xFF2A2824),
    line = Color(0xFF2E2C27), outline = Color(0xFF4A473F), text = Color(0xFFECE6DA), muted = Color(0xFF8E877A),
    accent = Color(0xFFC9A45C), onAccent = Color(0xFF1A1408), accentContainer = Color(0xFF3A2F1A), onAccentContainer = Color(0xFFF0DDB2),
    ember = Color(0xFFD9824A),
)
private val ObsidianLight = Tokens(
    background = Color(0xFFF3F2EF), raised = Color(0xFFFBFAF8), high = Color(0xFFEEECE8), highest = Color(0xFFE9E7E2),
    line = Color(0xFFD9D5CD), outline = Color(0xFFB5B0A6), text = Color(0xFF15140F), muted = Color(0xFF6A655C),
    accent = Color(0xFF8A6726), onAccent = Color.White, accentContainer = Color(0xFFF1E4C6), onAccentContainer = Color(0xFF2E2205),
    ember = Color(0xFFB4592A),
)

// B · Stone and Saffron: bright and architectural; dark mode is graphite.
private val StoneLight = Tokens(
    background = Color(0xFFECEBE7), raised = Color(0xFFF7F6F3), high = Color(0xFFE4E3DE), highest = Color(0xFFDCDBD5),
    line = Color(0xFFD6D5D0), outline = Color(0xFFADAFAA), text = Color(0xFF171A18), muted = Color(0xFF6D716C),
    accent = Color(0xFFB85E17), onAccent = Color.White, accentContainer = Color(0xFFF6DFC8), onAccentContainer = Color(0xFF3A1A02),
    ember = Color(0xFFB4462A),
)
private val StoneDark = Tokens(
    background = Color(0xFF151716), raised = Color(0xFF1C1F1D), high = Color(0xFF252826), highest = Color(0xFF2E322F),
    line = Color(0xFF303431), outline = Color(0xFF4C514D), text = Color(0xFFECEDEA), muted = Color(0xFF8D928D),
    accent = Color(0xFFE08A3C), onAccent = Color(0xFF241204), accentContainer = Color(0xFF45280F), onAccentContainer = Color(0xFFFADDC2),
    ember = Color(0xFFE5704F),
)

// C · Midnight Sage: forest black and mist; light mode is mist and deep sage.
private val SageDark = Tokens(
    background = Color(0xFF0C1310), raised = Color(0xFF131C18), high = Color(0xFF1A2520), highest = Color(0xFF222E28),
    line = Color(0xFF24322B), outline = Color(0xFF3F5148), text = Color(0xFFDCE7DF), muted = Color(0xFF7F948A),
    accent = Color(0xFF93B8A0), onAccent = Color(0xFF10241A), accentContainer = Color(0xFF26402F), onAccentContainer = Color(0xFFCDE6D4),
    ember = Color(0xFFD9824A),
)
private val SageLight = Tokens(
    background = Color(0xFFEEF2EF), raised = Color(0xFFF8FAF8), high = Color(0xFFE6ECE8), highest = Color(0xFFDDE5DF),
    line = Color(0xFFCFDAD3), outline = Color(0xFFA6B5AC), text = Color(0xFF121A16), muted = Color(0xFF5F7067),
    accent = Color(0xFF3F6B53), onAccent = Color.White, accentContainer = Color(0xFFD3E6D9), onAccentContainer = Color(0xFF0E2618),
    ember = Color(0xFFB4592A),
)

val Palettes: List<PaletteSpec> = listOf(
    PaletteSpec(Palette.OBSIDIAN_BRASS, "Obsidian and Brass", "Black or porcelain, with brass", ObsidianLight, ObsidianDark),
    PaletteSpec(Palette.STONE_SAFFRON, "Stone and Saffron", "Stone or graphite, with saffron", StoneLight, StoneDark),
    PaletteSpec(Palette.MIDNIGHT_SAGE, "Midnight Sage", "Forest or mist, with sage", SageLight, SageDark),
)

fun specOf(palette: Palette): PaletteSpec = Palettes.first { it.palette == palette }
