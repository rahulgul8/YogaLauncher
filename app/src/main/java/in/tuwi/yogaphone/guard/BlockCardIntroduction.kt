package `in`.tuwi.yogaphone.guard

import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import `in`.tuwi.yogaphone.YogaApplication
import `in`.tuwi.yogaphone.intro.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** The Android View overlay uses the same engine/store as Compose. No detection changes. */
fun addBlockIntroduction(scope: CoroutineScope, app: YogaApplication, view: TextView, blocked: GuardSurface,
    accent: Int, isCurrent: () -> Boolean, keepVisible: () -> Unit, onUnavailable: () -> Unit = {}) {
    scope.launch {
        val signals = app.introSignals.read().copy(blockedSurface = blocked)
        if (!isCurrent()) return@launch
        val intro = app.introStore.claim(IntroSurface.BLOCK_CARD, signals) ?: run { onUnavailable(); return@launch }
        if (!isCurrent()) return@launch
        val target = intro.targetSurface ?: return@launch
        val name = if (target == GuardSurface.INSTAGRAM) "Reels" else "Shorts"
        val original = view.text.toString()
        fun render(line: String, actions: List<Pair<String, () -> Unit>>) {
            val text = "$original\n$line" + actions.joinToString("") { "   ${it.first}" }
            view.text = SpannableString(text).apply {
                actions.forEach { (label, action) ->
                    val start = text.lastIndexOf(label)
                    setSpan(object : ClickableSpan() {
                        override fun onClick(widget: View) = action()
                        override fun updateDrawState(ds: TextPaint) { ds.color = accent; ds.isUnderlineText = false }
                    }, start, start + label.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
            view.movementMethod = LinkMovementMethod.getInstance()
            keepVisible()
        }
        var busy = false
        render("Also turn off $name?", listOf("Turn off $name" to {
            if (!busy) {
                busy = true
                scope.launch {
                    try {
                        var previous = SurfaceSettings()
                        app.guardPrefs.update { p ->
                            previous = p.surface(target)
                            p.copy(surfaces = p.surfaces + (target to previous.copy(enabled = true, allowanceMinutes = 0)))
                        }
                        app.introStore.acted(intro.id)
                        app.introSignals.refresh()
                        if (isCurrent()) render("$name are off too", listOf("Undo" to {
                            scope.launch {
                                app.guardPrefs.update { p -> p.copy(surfaces = p.surfaces + (target to previous)) }
                                app.introSignals.refresh()
                                if (isCurrent()) { view.text = original; keepVisible() }
                            }
                        }))
                    } catch (_: Exception) { busy = false }
                }
            }
        }, "Not now" to {
            scope.launch { app.introStore.dismiss(intro.id); if (isCurrent()) view.text = original }
        }))
    }
}

/** Same appended-line overlay mechanism as introductions, with a 48 dp action line. */
fun addWatchAnyway(view: TextView, accent: Int, lineHeight: Int, action: () -> Unit) {
    val text = "${view.text}\nWatch anyway"
    val start = text.lastIndexOf("Watch anyway")
    view.text = SpannableString(text).apply {
        setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) = action()
            override fun updateDrawState(ds: TextPaint) { ds.color = accent; ds.isUnderlineText = false }
        }, start, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan(object : android.text.style.LineHeightSpan {
            override fun chooseHeight(text: CharSequence, start: Int, end: Int, spanstartv: Int, v: Int, fm: android.graphics.Paint.FontMetricsInt) {
                val extra = (lineHeight - (fm.descent - fm.ascent)).coerceAtLeast(0)
                fm.descent += extra / 2
                fm.ascent -= extra - extra / 2
                fm.top = fm.ascent; fm.bottom = fm.descent
            }
        }, start, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    view.movementMethod = LinkMovementMethod.getInstance()
}
