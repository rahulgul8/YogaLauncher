# Brief 25: L-F1, simpler Settings ("Your apps" in one place)

The founder finds Settings cluttered: too many menus and taps. Their example: changing Chrome from a distracting app (with the opening pause) to an essential app is hard to find. Today an app's state is spread over three places:
- Essential / Distracting toggles and the app allowance: `UsageAppSheet` in `ui/UsageComponents.kt`, reachable only from the Usage screen's app list.
- "Pause before opening" per app: `ui/FocusSettingsScreen.kt` (`FocusPrefs.delayOptOut`, `FocusViewModel.setDelayOptOut`).
- Drawer category: the drawer's long-press menu ("Category…" in `MainActivity.kt` around line 889).

## Build
1. **One sheet per app.** Extend `UsageAppSheet` (or make a new `AppSettingsSheet` that it delegates to) so one sheet holds everything about an app, in plain words:
   - "Type": a three-way choice with `YogaChip`s: **Essential** (never counted, never paused), **Normal**, **Distracting** (counted, paused). This maps onto the existing `UsageViewModel.classify(pkg, essential, distracting)`.
   - When Distracting: a "Pause before opening" switch (the existing `delayOptOut`).
   - "Daily limit" row: the existing allowance editor, with its Pro gating unchanged.
   - Keep the mandatory-essential note and Pro gating exactly as they are.
   - Keep the existing resistance on loosening: making a distracting app normal or essential, turning its pause off, or raising or removing its limit must still go through `ui/LoosenSheet.kt` the way it does today (find where the current toggles call it; if they don't, check `docs/adr/0015-bypass-with-resistance.md` and apply it the same way as the other loosening paths).
2. **"Your apps" screen**, the first row in Settings (above Awareness, under the Pro card and the home card): a search field (copy the drawer's search behaviour) and a list of every drawer app with its icon, name and one summary line, e.g. "Distracting · pause · 30 min a day", "Essential", "Normal". Tapping a row opens the sheet from step 1. Sort distracting first, then normal, then essential, each alphabetically.
3. **From the drawer and home**: add "App settings…" to the long-press menus in `MainActivity.kt` (drawer and home favourites) that opens the same sheet. Keep the other actions; put this one after "Category…" in the drawer menu and after "Rename" on home.
4. **Settings main list:** keep the sections but tidy them: remove anything that is now covered by "Your apps", and make each row's summary say what's inside in plain words. Don't remove any feature or screen; just make every setting reachable in at most two taps from Settings, or one from an app's long-press.

The Chrome example must take: long-press Chrome → App settings… → Essential. Or Settings → Your apps → Chrome → Essential.

## Done when
- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest` pass.
- Any pure logic (summary line text, sort order, search match) is in a testable function with a unit test.
- Report the before/after tap count for the Chrome example and list what moved.
