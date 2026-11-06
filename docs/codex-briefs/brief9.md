You are working in the repo at C:\me\code\mobile\YogaLauncher, an Android launcher (Kotlin, Jetpack Compose, Material 3, minSdk 26, package `in.tuwi.yogaphone`). The founder wants a **premium, minimalist** app: "every screen, every button, every text should look unique, organised, premium and custom".

ADR / piece number: **9**.

## Read first
- `docs/design/brand.md`: the design system. Follow it exactly.
- `app/src/main/java/in/tuwi/yogaphone/ui/theme/`: `Theme.kt`, `Type.kt`, `Color.kt`, `YogaIcons.kt`, `YogaComponents.kt` (the kit).
- These are already done and are the reference for the look: `ui/SettingsScreen.kt`, `ui/OnboardingScreen.kt`, `ui/AppearanceSettingsScreen.kt`, `ui/HomeScreen.kt`, `ui/AppDrawer.kt`, `ui/HomeDock.kt`.
- **The app has three user-selectable looks** (`Palette`: Obsidian and Brass, Stone and Saffron, Midnight Sage), each with its own colours and fonts, in light and dark. Never hard-code a colour, hex value or font family in a screen: use `MaterialTheme.colorScheme` and `MaterialTheme.typography` only, so every screen follows the chosen look. Check each screen reads correctly in all three looks and in light and dark.

## Goal
Restyle every remaining screen, sheet and dialog with the kit so the whole app reads as one object. **Keep behaviour identical.** This is a visual pass, plus the one bug fix listed below.

## Files to restyle
- `ui/FocusSettingsScreen.kt`
- `ui/FocusComponents.kt`: the timebox and focus start/control sheets and the status lines. `HomeQuickActions` is already done.
- `ui/GuardSettingsScreen.kt`, including the disclosure dialog
- `ui/HiddenAppsScreen.kt`
- `ui/InsightsScreen.kt`
- `ui/MindfulDelayScreen.kt`: the pause screen before a distracting app. Make it calm and beautiful, built around the brand clock style. It is seen often.
- `ui/OrganizeScreens.kt`: profiles and categories
- `ui/PrivacyScreens.kt`: privacy and help
- `ui/QuietScreens.kt`: visual interventions, quiet notifications and the digest
- `ui/UsageAccessCard.kt`
- `ui/UsageComponents.kt`
- `ui/UsageDashboardScreen.kt`
- `ui/UsageSettingsScreen.kt`
- `ui/ValueOfTimeSettingsScreen.kt`
- `ui/WidgetsUi.kt`
- `ui/AppActionMenus.kt`: the long-press action sheet and the rename dialog
- `ui/HomeUsageIndicator.kt`
- `guard/YogaGuardService.kt`: **only** the overlay colours and shapes.
  - The overlay follows the user's look: read `Prefs(context).userPreferences` (palette) and take colours from `specOf(palette).dark` (`Tokens`), refreshing when the palette changes.
  - Pill: `raised` background, `text` colour, a 1 px `accent` hairline border, fully rounded. In the Burning style the amount uses `ember`.
  - Block card: the same surface.
  - Keep its plain-View implementation.

## Rules
1. Build screens from the kit: `YogaScreen`/`YogaHeader`, `YogaSection`, `YogaRow`, `YogaSwitchRow`, `YogaRadioRow`, `YogaCheckRow`, `YogaChip`/`YogaChipRow`, `YogaPrimaryButton`/`YogaSecondaryButton`/`YogaTextButton`, `YogaCard`, `YogaNote`, `YogaTextField`, `YogaStat`.
   - Do not use `ListItem`, `FilterChip`, `AssistChip`, `OutlinedTextField`, `Checkbox`, `RadioButton`, raw `Switch` or `Button`/`TextButton` in screens.
2. Add to `YogaComponents.kt`, in the same style, whatever is missing:
   - `YogaSlider`: brass thumb and active track, hairline inactive track, no visible tick marks.
   - `YogaSheet`: a `ModalBottomSheet` with `surfaceContainer`, a 28 dp top radius and a slim hairline drag handle.
   - `YogaDialog`: an `AlertDialog` with `surfaceContainer`, a 26 dp radius, a display-face title, a primary action as `YogaPrimaryButton` and the dismiss action as `YogaTextButton`.
   - `YogaProgress`: a 2 dp hairline with a brass fill. Use ember when over the limit.
   - Use these everywhere instead of the Material versions.
3. **Icons:** use `YogaIcons` only. Remove every `androidx.compose.material.icons` use in `ui/`. If an icon is missing, add it to `YogaIcons.kt` on the same 24-unit grid with a 1.6 stroke and round caps.
4. **Layout:**
   - 24 dp side margins (`ScreenMargin`) everywhere.
   - Rows at least 64 dp tall.
   - Separate groups with `YogaSection` labels and space, not dividers. Use a hairline only where two lists touch.
5. **Colour:**
   - The accent (`colorScheme.primary`) only for the one primary or "on" thing in view.
   - Ember (`colorScheme.tertiary`) only for time value and over-limit states.
   - No other accent colours, no gradients, no shadows.
6. **Type:**
   - Screen titles and hero numbers use the display face (`headline*`/`display*`). Everything else uses the text face.
   - Numbers use `fontFeatureSettings = "lnum, tnum"`.
   - Section labels are uppercase `labelSmall`.
7. **Copy:**
   - Plain sentence case. Short. Write from the user's side.
   - Keep every existing safety and privacy statement (for example "illustrative, not money spent").
   - No emoji, except the existing 🔥 in the guard pill.
8. **Usage dashboard redesign:**
   - Hero: `YogaEyebrow("Today")`, then today's eligible time as a big display number (for example "1 h 12 m"). Below it, "of 1 h 30 m allowance" with a `YogaProgress`.
   - Today and Last 7 days as two chips.
   - Time value as one ember line with the illustrative note.
   - Categories, then apps: each app row shows its name, minutes and a thin progress bar.
   - Blocked Shorts/Reels count as a quiet note.
9. **Bug fix:** the usage lists show `com.motorola.launcher3`, a raw package name.
   - Show app labels everywhere.
   - Exclude home-screen apps from the usage lists and totals: Yoga itself and any app that answers `ACTION_MAIN` + `CATEGORY_HOME`.
   - Find where the label falls back to the package name and fix it at the source. Add a unit test if the logic is pure.
10. **Dialogs and sheets** across the app use `YogaDialog` and `YogaSheet`.
11. **Make the user's work easy:** where a screen has a free-text field people fill again and again (timebox labels, profile names, category names), check that it offers recent entries as chips. The project already has the recent-searches pattern. If any field lacks this, add it, scoped sensibly, and list what you added.

## Constraints
- No new dependencies. No Room schema changes. No behaviour changes beyond the bug fix.
- Keep public composable signatures that `MainActivity.kt` calls, or update the call sites there.
- UTF-8 without a BOM. Real characters, never mojibake.
- Your sandbox cannot run Gradle or write the git index. Don't run git commands that write. Write code that compiles; list anything you're unsure of.
- **Docs:**
  - Add `docs/adr/0009-premium-design-system.md`, covering the kit, the three user-selectable looks and their tokens.
  - Update `ROADMAP.md` with piece 9.
  - Add a "Screens done" checklist to `docs/design/brand.md`.

## Done means
Every listed file uses the kit and `YogaIcons`, with no Material icon imports left in `ui/`; the new kit components exist; the dashboard is redesigned; and the launcher-label bug is fixed. Finish with a short summary: files changed, new kit components and icons, anything skipped or uncertain.
