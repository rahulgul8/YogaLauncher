You are working in the Android repo at C:\me\code\mobile\YogaLauncher (Kotlin, Jetpack Compose, Material 3, package `in.tuwi.yogaphone`, minSdk 26, targetSdk 36), a minimalist "mindful" launcher. Already built: Room DB + manual DI, `AppRepository` (per-app essential/distracting/hidden/favourite metadata, `AppClassifier`), DataStore prefs, settings, light/dark/greyscale themes, usage measurement + allowances + reminders + value of time, timebox, focus sessions with schedules, and the mindful launch delay. Read the code first, then `YogaLauncher_Product_Specification.md` sections 4.3, 4.5, 4.8, 4.9 and `yoga_launcher_features.md` sections 2 and 3 for intent. Add no new dependencies.

## Goal: Piece 4 — categories, profiles (Priority 2), grayscale workflow, notification quieting

### 1. Automatic categories (spec 4.8)
- Room `CategoryEntity(id, name, isBuiltIn, isDistractingByDefault, sortOrder)`; built-ins: Essentials, Communication, Social, Video & Music, Games, News & Reading, Work & Productivity, Shopping, Travel & Maps, Health, Finance, Tools, Other. Users can add, rename, delete (custom only) categories.
- Extend `AppClassifier` to assign a category locally from `ApplicationInfo.category` plus package-name heuristics (a built-in map for well-known packages and keyword rules like `bank`, `pay`, `mail`, `maps`, `music`, `game`). Only for apps the user has not categorised; newly installed apps get a category on install.
- Manual recategorisation from the drawer long-press menu ("Category…") and from an apps-by-category settings screen.
- Drawer option (setting, default off): group the list by category with sticky headers.
- Usage rules gain `scope = CATEGORY` (target category id) — wire it into `RuleEvaluator` and the usage-rules settings UI, and show per-category totals in the usage dashboard.

### 2. Profiles and app groups (spec 4.9, Priority 2)
- Room `ProfileEntity(id, name, homeAppPackages ordered, hideOtherApps: Boolean, focusOnActivate: Boolean, themeOverride nullable, scheduleId nullable)`. Built-in suggestions offered (not forced): Personal (default, the current home favourites), Work, Reading, Essentials-only.
- Profile switcher: tap a small active-profile label on home ("Personal ▾") → bottom sheet listing profiles, one tap to switch; long-press → edit. Active profile name is always visible on home (non-colour cue).
- A profile defines which apps appear on home (its own ordered list), optionally hides all other apps from the drawer (with an "All apps" escape that stays reachable), optionally starts focus, optionally overrides the theme.
- Profile manager screen: create, rename, reorder apps, delete (not the last one). New profiles prefill from the current home list.
- Time-based auto-switch: a profile may link to an existing focus schedule window (reuse `ScheduleEvaluator`); when the window starts the profile activates and when it ends the previous profile is restored. Manual switching always wins until the next transition. Wi-Fi/GPS triggers are out of scope.

### 3. Grayscale workflow (spec 4.3)
- Be honest about Android limits: the launcher can always switch its own UI to the greyscale theme; device-wide grayscale is done by the system.
- Settings group "Visual interventions": triggers (when over the daily allowance, during focus, during a bedtime schedule) for the **launcher** greyscale theme; a "Device-wide grayscale" row that explains how and deep-links to the best available system screen: try Digital Wellbeing bedtime mode (`com.google.android.apps.wellbeing` bedtime intent if resolvable), then `Settings.ACTION_ACCESSIBILITY_SETTINGS`, with short step text ("Accessibility → Colour correction → Greyscale").
- Advanced, optional: if `android.permission.WRITE_SECURE_SETTINGS` has been granted (check with `checkSelfPermission`), toggle `Settings.Secure` `accessibility_display_daltonizer_enabled` = 1 and `accessibility_display_daltonizer` = 0 for real device-wide greyscale on the same triggers, restoring the user's previous values afterwards. Show the adb command to grant it (`adb shell pm grant in.tuwi.yogaphone android.permission.WRITE_SECURE_SETTINGS`) in an expandable "For advanced users" section. Declare the permission in the manifest with `tools:ignore="ProtectedPermissions"`. Wrap every write in try/catch.
- A clear indicator on home when any greyscale intervention is active, with a one-tap "Snooze for 30 min".

### 4. Notification quieting (spec 4.5)
- Optional `NotificationListenerService` (`YogaNotificationListener`), only active after the user enables "Quiet notifications" and grants listener access (explain in context, link to `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`).
- Per-app setting: Allow / Hold during focus / Always hold (defaults: distracting apps → Hold during focus; essential and communication-from-contacts apps → Allow; never hold calls, alarms, ongoing/foreground-service, media-session or `CATEGORY_CALL`/`CATEGORY_ALARM` notifications).
- Held notifications are cancelled from the shade (`cancelNotification(key)`) and stored locally (Room `HeldNotificationEntity(key, packageName, title, text, postedAt)` — store only title/text, and tell the user this) for a digest.
- Digest: delivered when focus ends and at user-chosen digest times (default 12:30 and 18:30); one summary notification ("8 notifications held from 3 apps") opening a Digest screen grouped by app, each item tapping through to the app (launch intent; the original PendingIntent is gone). Clear-all and auto-delete after 24 h.
- Everything easy to disable; the feature must do nothing when the permission is missing.

### 5. Tests
JVM unit tests for category heuristics, CATEGORY rule evaluation, profile auto-switch/manual-override precedence, notification hold decisions (including the never-hold categories), and digest grouping.

### 6. Docs
Tick the matching items in `ROADMAP.md`, spec section 13 ("Prototype grayscale workflow and supported fallback", "Investigate notification quieting, consent, and digest options", "Add automatic categories and manual recategorization", "Add grouping and profile modes in Priority 2") and `yoga_launcher_features.md` section 3 ("Create visual indicator … showing active profile"; note the SQLite schema item is done via Room).

## Constraints
MVVM, StateFlow, one screen per file, logic in plain classes; calm text-first UI in all three themes; accessible. No network. Do not run git commands that write. If Gradle runs in your sandbox verify with `gradlew.bat --offline assembleDebug testDebugUnitTest`, otherwise say so and be careful with imports/API guards.

## Done means
Compiles, tests pass, items 1–6 done; finish with a short summary of files changed, decisions made, and anything left undone.
