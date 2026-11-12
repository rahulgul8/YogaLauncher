package `in`.tuwi.yogaphone.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import android.graphics.Canvas
import `in`.tuwi.yogaphone.data.Palette
import `in`.tuwi.yogaphone.data.ThemeMode
import `in`.tuwi.yogaphone.pro.Entitlement
import `in`.tuwi.yogaphone.pro.LocalProAccess
import `in`.tuwi.yogaphone.pro.ProAccess
import `in`.tuwi.yogaphone.pro.ProState
import `in`.tuwi.yogaphone.ui.theme.YogaPhoneTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders screens to PNG without a device: build/screenshots/<name>.png.
 * Run with: ./gradlew :app:testDebugUnitTest --tests "*ScreenshotTest*"
 * These are for looking at, not golden comparisons.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = android.app.Application::class, qualifiers = "w411dp-h1500dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun settingsDarkTrial() = shoot("settings-dark-trial", ThemeMode.DARK, Palette.OBSIDIAN_BRASS, ProState(Entitlement.TRIAL, 9)) { settings() }
    @Test fun settingsLightFree() = shoot("settings-light-free", ThemeMode.LIGHT, Palette.OBSIDIAN_BRASS, ProState(Entitlement.FREE)) { settings() }
    @Test fun settingsSageDark() = shoot("settings-sage-dark", ThemeMode.DARK, Palette.MIDNIGHT_SAGE, ProState(Entitlement.TRIAL, 3)) { settings() }

    @Composable private fun settings() = SettingsScreen(
        onBack = {}, onAppearance = {}, onHiddenApps = {}, onResetNames = {}, onUsage = {}, onUsageRules = {},
        onValueOfTime = {}, onLearning = {}, onGuard = {}, onFocus = {}, onProfiles = {}, onCategories = {}, onVisual = {}, onQuiet = {},
        onInsights = {}, onPrivacy = {}, onHelp = {}, onWidgets = {}, onAdvanced = {},
    )

    private fun shoot(name: String, mode: ThemeMode, palette: Palette, pro: ProState, content: @Composable () -> Unit) {
        compose.setContent {
            YogaPhoneTheme(mode, palette) {
                CompositionLocalProvider(LocalProAccess provides ProAccess(pro)) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).testTag("root")) { content() }
                }
            }
        }
        compose.waitForIdle()
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        compose.runOnUiThread { view.draw(Canvas(bitmap)) }
        val out = File("build/screenshots/$name.png").apply { parentFile?.mkdirs() }
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
