You are working in the Android repo at C:\me\code\mobile\YogaLauncher (Kotlin, Jetpack Compose, Material 3, package `in.tuwi.yogaphone`, minSdk 24, targetSdk 36). It is a minimalist "mindful" home-screen launcher. Read `YogaLauncher_Product_Specification.md` (sections 4.1, 6, 7) and `yoga_launcher_features.md` section 1 for product intent. Existing code is small: `MainActivity.kt`, `MainViewModel.kt`, `data/Prefs.kt` (DataStore), `data/AppModel.kt`, `ui/HomeScreen.kt`, `ui/AppDrawer.kt`, `utils/AppUtils.kt`, `receiver/AppChangeReceiver.kt`, `ui/theme/*`. Dependencies for Room (KSP), WorkManager, navigation-compose, material-icons-extended and kotlinx-coroutines-test are ALREADY added to `app/build.gradle.kts` and cached — do not add other dependencies.

## Goal: Piece 1 — a reliable, complete launcher foundation (spec Phase 1 "Foundation")

Implement all of the following:

1. **Architecture for the features that come next.**
   - A Room database `data/db/YogaDatabase.kt` (version 1, `fallbackToDestructiveMigration()` is fine pre-release) with an `AppEntryEntity` table: packageName (PK), customLabel (nullable), categoryId (nullable String), isFavorite, favoriteOrder, isHidden, isEssential, isDistracting. DAO with Flow queries. Later pieces will add more entities (usage rules, timebox/focus sessions, intervention events) — design so adding them is easy.
   - A simple manual DI holder (e.g. `YogaApplication : Application` with lazily created database, repositories, prefs) — no Hilt. Register it in the manifest.
   - An `AppRepository` that merges the installed-app list from PackageManager (launcher-intent activities, including work-profile apps via `LauncherApps` if simple; otherwise just the main user) with `AppEntryEntity` metadata, refreshes on package add/remove/replace (fix `AppChangeReceiver` so it is registered dynamically in code for PACKAGE_ADDED/REMOVED/REPLACED/CHANGED with data scheme `package`, since manifest-registered package broadcasts don't arrive on modern Android), and removes metadata for uninstalled apps.
   - Migrate favorites from the current DataStore slots into the Room table once (or just switch to Room if the migration is trivial).
   - Navigation with navigation-compose: routes `home`, `settings`, `settings/hidden_apps`, `settings/appearance` (or one settings screen with sections — your choice, keep it simple). Back on home must not exit the launcher (it is a HOME activity).

2. **Home screen** (text-first, calm):
   - Large clock and date at the top (toggle in settings), using the system 12/24h setting. Tapping the clock opens the system clock/alarm app; tapping the date opens the calendar app (use `Intent.ACTION_MAIN` + category or `AlarmClock.ACTION_SHOW_ALARMS` / `CalendarContract` content URI, with safe fallback if not resolvable).
   - Favorites list (up to 8 by default, configurable 4–12 in settings), using custom labels when set, adaptive spacing so the list fits the screen without scrolling where possible.
   - Long-press a favorite → a small bottom sheet/menu: Replace, Remove from home, Rename, Move up, Move down, App info. Reordering must persist.
   - Gestures: swipe up anywhere on home → open app drawer; swipe down → expand the notification shade (use reflection `statusbar` service `expandNotificationsPanel`, guarded with try/catch, requires `android.permission.EXPAND_STATUS_BAR` — add it); double-tap optional, skip it.
   - Keep the existing search FAB but make it calmer (theme-aware colors instead of hard-coded white/grey; it must look right in light, dark and greyscale themes).
   - A small, unobtrusive "settings" entry point (e.g. long-press empty area of home opens settings; also show a subtle gear text button at the top-right). Make sure there is always a visible way to reach settings.
   - Leave a clearly named slot (an empty composable parameter or `HomeStatusArea` composable rendering nothing yet) under the clock where later pieces will show the daily usage indicator and timebox progress.

3. **App drawer** (bottom sheet):
   - Search field auto-focuses with keyboard; filter by label (case/diacritic insensitive, word-prefix matches ranked first). Pressing the keyboard "Go/Done" launches the top match. If exactly one app matches, show it highlighted.
   - **Recent searches chips**: remember the last 3 distinct search terms that led to a launch (stored on device in DataStore, newest first, de-duplicated). Show them as tap-to-fill chips under the search field when the field is empty.
   - Alphabetical list, hidden apps excluded. Long-press an app → menu: Add to home / Remove from home, Rename, Hide, App info, Uninstall (via `Intent.ACTION_DELETE` with package URI; add `REQUEST_DELETE_PACKAGES` permission).
   - Optional setting "Search web if no app matches" is NOT needed; skip.

4. **Settings screen(s)** (scrollable, grouped like spec section 7, only the groups implemented in this piece plus placeholders are NOT wanted — only show what works):
   - **Launcher**: "Set as default home app" using `RoleManager.createRequestRoleIntent(ROLE_HOME)` on API 29+, fallback `Settings.ACTION_HOME_SETTINGS`; show whether Yoga is currently the default (`isRoleHeld` / resolveActivity on HOME intent). "Leave Yoga Launcher" guidance: opens `Settings.ACTION_HOME_SETTINGS`. Open Android system settings.
   - **Appearance**: theme Light / Dark / Follow system / Greyscale (greyscale = a fully monochrome Material color scheme — define it in `ui/theme`); font size (small/medium/large/extra-large); show clock; show date; number of home apps; home text alignment (left/center/right).
   - **Apps**: hidden apps list with unhide; reset custom names.
   - **About**: version name.
   - Store preferences in DataStore (`Prefs.kt`), exposed as a single `Flow<UserPreferences>` data class.

5. **Theme**: honour the chosen theme for the whole app; status/navigation bar icon colors must be readable in each theme (use `enableEdgeToEdge` with appropriate SystemBarStyle). Respect system font scaling; touch targets ≥ 48dp; all icon-only buttons have contentDescription.

6. **Manifest correctness**: keep HOME/DEFAULT/LAUNCHER filters; `singleTask`; handle `onNewIntent` for the HOME intent (pressing Home while drawer/settings is open returns to the home route and closes the drawer). Keep `QUERY_ALL_PACKAGES`.

7. **Tests**: add JVM unit tests (in `app/src/test`) for pure logic: search ranking/filtering, recent-searches list update (dedupe, cap 3, newest first), favorites reorder logic. Extract this logic into plain Kotlin functions/classes so it is testable without Android. Delete `ExampleUnitTest.kt`/`ExampleInstrumentedTest.kt` boilerplate if you want.

8. Update `ROADMAP.md` Phase 0/1 checkboxes that this piece completes.

## Constraints
- Kotlin + Compose only, MVVM (ViewModel + StateFlow). Keep files focused (one screen per file, logic in plain classes).
- No network, no analytics, no new dependencies.
- Calm visual design: neutral monochrome base, one subdued accent, generous spacing, brief motion only.
- Do not run `git` commands that write (no add/commit); I will review and commit.
- If you can run Gradle, verify with `./gradlew --offline assembleDebug testDebugUnitTest` from the repo root (Windows: `gradlew.bat`). If Gradle cannot run in your sandbox, say so and make sure the code is carefully type-correct anyway (check imports and API levels — guard API 29+/33+ calls with `Build.VERSION.SDK_INT` checks).

## Done means
The app compiles, unit tests pass, all eight items above are implemented, and you finish with a short summary listing files changed, anything you could not do, and anything you assumed.
