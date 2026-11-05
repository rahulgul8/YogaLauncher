You are working in the Android repo at C:\me\code\mobile\YogaLauncher (Kotlin, Jetpack Compose, Material 3, package `in.tuwi.yogaphone`, minSdk 26, targetSdk 36), a minimalist "mindful" home-screen launcher. Read `MainActivity.kt`, `ui/HomeScreen.kt`, `data/db/YogaDatabase.kt` (Room, `fallbackToDestructiveMigration`), `YogaApplication.kt` (manual DI) and `YogaLauncher_Product_Specification.md` section 4.1 ("Widget support, including adding, removing, placing, and resizing widgets where supported"; acceptance: "widgets survive normal launcher recreation") before changing anything. Add no new dependencies.

## Goal: Piece 6 — home-screen widgets (spec 4.1, backlog "Define widget host behavior")

Design decision (already made — follow it): widgets live in one calm, optional column on home, **below the clock/status area and above the favourites**, collapsed to nothing when empty. No free-form grid.

1. **Host**: a `WidgetHost` wrapper around `AppWidgetHost` (host id constant) created in `YogaApplication`; `startListening()` in `MainActivity.onStart`, `stopListening()` in `onStop`, both in try/catch.
2. **Storage**: Room `WidgetEntity(appWidgetId PK, providerComponent: String, heightDp: Int, sortOrder: Int)` + DAO (bump the DB version).
3. **Add flow** (from home long-press menu → "Add widget", and from Settings → Home and appearance → "Widgets"):
   - A picker screen listing installed providers from `AppWidgetManager.getInstalledProvidersForProfile(myUserHandle)` grouped by app, with label and preview image (`loadPreviewImage` / `loadIcon`, fall back gracefully), searchable.
   - `allocateAppWidgetId()` → `bindAppWidgetIdIfAllowed()`; if false, launch `AppWidgetManager.ACTION_APPWIDGET_BIND` (extras `EXTRA_APPWIDGET_ID`, `EXTRA_APPWIDGET_PROVIDER`) via an activity-result launcher; if the provider has `configure`, start it with `AppWidgetHost.startAppWidgetConfigureActivityForResult` (API 26+ is fine) and only save the widget when it returns RESULT_OK. On any cancel/failure call `deleteAppWidgetId`.
   - Default height: provider `minHeight` (dp) clamped to 80–400.
4. **Display**: render each saved widget with `AndroidView` creating `host.createView(context, id, providerInfo)`; set `updateAppWidgetSize` (API 31+: `updateAppWidgetSize(Bundle)`/sizes list; older: options bundle with min/max width/height) whenever width or height changes. Missing provider (app uninstalled) → remove the entity and `deleteAppWidgetId`.
5. **Edit**: long-press a widget → sheet with Resize (Small 120dp / Medium 200dp / Large 320dp, plus the provider's own min), Move up / Move down, Remove (with a 5-second undo snackbar instead of a confirm dialog).
6. **Package changes**: when a package is removed, drop its widgets (hook into the existing package-change refresh).
7. Widgets are hidden while a profile with "tuck other apps away" is active and during focus (keep home calm) — show a one-line "Widgets hidden during focus" hint instead.
8. **Tests**: JVM tests for pure helpers you add (height clamping, ordering/move logic, size-bundle computation input → output if extracted).
9. Docs: tick "Define widget host behavior" in `YogaLauncher_Product_Specification.md` section 13 and add a "Piece 6 — Widgets" section to `ROADMAP.md`.

## Constraints
MVVM/StateFlow, one screen per file, logic in plain classes. Calm visuals in light/dark/greyscale; accessible (contentDescription, ≥48dp). Write files as UTF-8 **without BOM**. Do not run git commands that write. If Gradle runs in your sandbox verify with `gradlew.bat --offline assembleDebug testDebugUnitTest`, otherwise say so and be careful with imports and API-level guards.

## Done means
Compiles, tests pass, items 1–9 done; finish with a short summary of files changed, decisions made and anything left undone.
