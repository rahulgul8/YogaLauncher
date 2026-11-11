package `in`.tuwi.yogaphone

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.lifecycle.lifecycleScope
import `in`.tuwi.yogaphone.data.Prefs
import `in`.tuwi.yogaphone.data.Palette
import `in`.tuwi.yogaphone.guard.*
import `in`.tuwi.yogaphone.pro.*
import `in`.tuwi.yogaphone.ui.ResistScreen
import `in`.tuwi.yogaphone.ui.theme.YogaPhoneTheme
import `in`.tuwi.yogaphone.ui.theme.themeUsesDarkIcons
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class ResistActivity : ComponentActivity() {
    private var finishingChoice = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val surface = intent.getStringExtra(EXTRA_SURFACE)?.let { runCatching { GuardSurface.valueOf(it) }.getOrNull() }
        if (surface == null) { finish(); return }
        val app = application as YogaApplication
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { choose { app.guardPrefs.recordResisted(surface) } }
        })
        app.reviewController.recordInterruption()
        lifecycleScope.launch {
            val prefs = app.guardPrefs.preferences.first()
            if (isStrict(prefs, LocalDate.now())) { finish(); return@launch }
            val daily = app.guardPrefs.daily.first().takeIf { it.date == LocalDate.now().toString() } ?: GuardDaily()
            val user = Prefs(this@ResistActivity).userPreferences.first()
            val pro = app.proRepository.current()
            setContent {
                val darkIcons = themeUsesDarkIcons(user.themeMode)
                SideEffect {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { !darkIcons },
                        navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { !darkIcons })
                }
                YogaPhoneTheme(user.themeMode, if (ProGate.allows(ProFeature.LOOKS, pro)) user.palette else Palette.OBSIDIAN_BRASS) {
                    ResistScreen(surface, daily, prefs.lastPassMinutes,
                        onNotNow = { choose { app.guardPrefs.recordResisted(surface) } },
                        onPass = { minutes -> choose { app.guardPrefs.startPass(surface, minutes, System.currentTimeMillis()) } })
                }
            }
        }
    }
    private fun choose(action: suspend () -> Unit) {
        if (finishingChoice || !lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) return
        finishingChoice = true
        lifecycleScope.launch {
            try { action(); finish() } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { finishingChoice = false }
        }
    }
    companion object { const val EXTRA_SURFACE = "guard_surface" }
}
