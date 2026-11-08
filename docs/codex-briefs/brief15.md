# Brief 15: Home gestures and opening a single search match

Repo: Yoga Phone launcher (Kotlin, Jetpack Compose). Work only in this worktree (`YogaLauncher-home`, branch `home-gestures`). Don't commit; Claude reviews, builds and commits. You can't run Gradle here, so write code that compiles first time: check every import and signature against the existing code.

## Goal

The founder asked for three things:

1. **Open the only match.** In the app drawer search, when the typed query leaves exactly one app, open it automatically.
2. **Tap empty home space to open the drawer**, with the search field focused and the keyboard up.
3. **Long-press empty home space for a Home options sheet**, which includes adding apps to the home list (the "quick apps", `uiState.favoriteApps`).

Today:
- Apps reach home only through drawer long-press → "Add to home".
- Empty home space ignores taps and long-presses.
- Long-pressing the clock or date opens Settings; keep that.

## 1. Open the only match (`ui/AppDrawer.kt`, `AppDrawerLayer`)

- Trigger when all of these hold:
  - `query` is not blank;
  - `matches.size == 1`;
  - the query got longer since the last change (typing forward, not a backspace or clear);
  - `pickingTitle == null` (never in replace or dock picking mode);
  - the preference is on.
- Wait 300 ms after the last keystroke, cancelling if the query changes, then call the same `onAppClick(app, query)` the row uses. That keeps the mindful pause, recent searches and launch accounting.
- Fire at most once per query value, so recomposition can't relaunch.
- Hide the keyboard before launching.
- **Preference:** add `autoOpenSingleMatch: Boolean = true` to `UserPreferences` in `data/Prefs.kt`, following the existing key, read and setter pattern.
- **Setting:** add a switch to the Appearance settings screen, or wherever the drawer and search settings already live (look for an existing drawer or search section first). Label "Open the only match", supporting text "When a search leaves one app, open it".
- Pass the value into `AppDrawerLayer` as a parameter from `MainActivity.kt`.

## 2. Tap empty home space (`ui/HomeScreen.kt`)

- Add `onEmptyTap: () -> Unit = {}` and `onEmptyLongPress: () -> Unit = {}` parameters to `HomeScreen`.
- Extend the existing `awaitEachGesture` block on the root `Box`. Don't add a second competing `pointerInput`.
- **Tap:** pointer up with no drag, total movement under touch slop, held for less than `viewConfiguration.longPressTimeoutMillis`, and no child consumed the down, any move or the up. Children such as app rows, the clock, the date, icon buttons and widgets consume their own clicks. Check `change.isConsumed` on the up event before breaking: events reach the parent on the Main pass after children.
- **Long press:** still pressed with no drag at `longPressTimeoutMillis`, and nothing consumed. Use `withTimeoutOrNull` around the pointer loop, or track the uptime. Fire once, then consume the rest of that gesture so the up isn't also a tap.
- Swipe up (drawer drag) and swipe down (notifications) must behave exactly as before.
- **MainActivity wiring:**
  - `onEmptyTap = { drawer.open() }`. `DrawerController.open()` already bumps `keyboardRequests`, which focuses search and shows the keyboard.
  - `onEmptyLongPress = { showHomeOptions = true }`.

## 3. Home options sheet (`MainActivity.kt` + a small composable in `ui/HomeScreen.kt` or a new `ui/HomeOptionsSheet.kt`)

- Use the kit's `YogaSheet` (ui/theme/YogaComponents.kt) and the same row style as `AppActionSheet` in `ui/AppActionMenus.kt`; reuse its row component if it's reusable.
- Title "Home". Rows:
  1. **Add apps to home** → `homePicking = true; drawer.open()`.
  2. **Widgets** → navigate to `WIDGETS`.
  3. **Appearance** → navigate to `APPEARANCE`.
  4. **Settings** → navigate to `SETTINGS`.
- **Home picking mode** (add-to-home):
  - `AppDrawerLayer` gets `pickingTitle = "Choose apps for home"` (the existing replace and dock titles keep priority if set).
  - Add an optional `isSelected: (AppModel) -> Boolean` to `AppDrawerLayer`. When it's non-null, each row shows a small check at the end for selected apps: `YogaIcons` has a check icon, or use a 6 dp accent dot like the home intro line.
  - Tapping an app toggles `viewModel.setFavorite(app, !viewModel.isOnHome(app))`. The drawer stays open and the query is kept.
  - Show a "Done" text button in the search row while picking; it closes the drawer.
  - `onClosed` clears `homePicking`. `homeRequest` (the Home button) clears it too, like `replacingApp`.
  - If the home app count has a limit (`setHomeAppCount` and the max favourites in the view model), respect it. When full, adding another shows the existing snackbar or toast pattern if there is one, otherwise does nothing; never silently drop an existing favourite.
- Change the empty-home hint `CalmHint("Swipe up to choose the apps you want here")` to "Touch and hold to add apps here", since long-press is now the direct path.

## Constraints

- Match the existing code style: compact Kotlin, no new dependencies, and comments only where the why isn't obvious.
- Don't touch `intro/*`, `update/*`, `guard/*`, `TODO.md` or onboarding. Another session is editing intro files on main.
- Don't change the clock and date long-press, the dock, or the profile chip.
- **Unit tests:** pull the auto-open decision into a small pure function (for example `shouldAutoOpen(previousQuery, query, matchCount, picking, enabled)`) in AppDrawer.kt or a new `ui/DrawerSearch.kt`, and add JUnit tests in `app/src/test/java/in/tuwi/yogaphone/ui/` covering forward typing, backspace, picking mode, disabled and match count ≠ 1.

## Done when

- Code compiles (Claude runs `./gradlew :app:testDebugUnitTest :app:assembleProfile`).
- Your summary lists the files changed and any assumption you made.
