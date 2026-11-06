You are working in the repo at C:\me\code\mobile\YogaLauncher, an Android launcher (Kotlin, Jetpack Compose, Material 3, minSdk 26, targetSdk 36, package `in.tuwi.yogaphone`). Read `YogaLauncher_Product_Specification.md` sections 4.7 ("Money value of time"), 9 (privacy) and 10 (risks) first. Then read these files before you change anything:
- `usage/UsageRepository.kt`, `usage/UsagePrefs.kt`, `usage/TimeValueCalculator.kt` and `usage/UsageCalculator.kt` (holds `RuleEvaluation` and `excessMinutes`)
- `usage/AppClassifier.kt`, `ui/ValueOfTimeSettingsScreen.kt`, `ui/PrivacyScreens.kt`
- `YogaApplication.kt`, `MainActivity.kt` (the `LauncherNavigation` routes) and `AndroidManifest.xml`

ADR / piece number: **8**.

## Goal: Piece 8: "Yoga guard", an optional accessibility service that does two things

### A. Live "time burning" indicator over other apps
The spec's floating opportunity-cost indicator. The user asked for it explicitly: "as they use something wasteful, show them the money burning".

1. Detect the foreground app from `TYPE_WINDOW_STATE_CHANGED` events. Ignore our own package, the system UI, IME/keyboard windows and the launcher itself.
2. **Eligible app** means the same thing as in `UsageRepository.collect`: not essential, and distracting (or any non-essential app when `AllowanceCounts.ALL_EXCEPT_ESSENTIAL` is set).
3. New setting **"Start counting"**:
   - `AFTER_LIMIT` (the brief's original default; changed to FROM_FIRST_MINUTE on 2026-11-06): count only once today's TOTAL rule is OVER.
   - `FROM_FIRST_MINUTE`: count every minute in an eligible app.
4. **Amount** = baseline minutes + the live seconds since this eligible app came to the foreground, × hourly value / 3600.
   - The baseline is today's `overall.excessMinutes` for AFTER_LIMIT, or today's eligible minutes for FROM_FIRST_MINUTE.
   - Take the baseline from a fresh `usageRepository.check()` when an eligible app comes to the foreground. Throttle these refreshes to at most one per 60 seconds and reuse the last snapshot in between.
   - Without a TOTAL rule, AFTER_LIMIT shows nothing.
   - Requires `timeValueEnabled`. Hidden when usage measuring is off or today is snoozed (`UsagePrefs.isSnoozed`).
5. **Display:** a small pill drawn with `TYPE_ACCESSIBILITY_OVERLAY`, so no draw-over-apps permission is needed. Use a plain Android `View`, not Compose.
   - Placement: near the top, horizontally centred. Text: "🔥 ₹42.50 burning" (Burning style) or "₹42 of time past your limit" (Gentle style).
   - It updates once a second. Use the currency's minor units while the per-minute rate is below 1 whole unit, so the number visibly moves; otherwise whole units. Put this in a pure `BurnMeter` and extend `TimeValueCalculator` if needed.
   - Burning style: warm colour and a gentle flame flicker. Disable animation when the system's animator duration scale is 0.
   - Tap collapses the pill to a small flame dot, and tapping again expands it. Dragging moves it vertically; remember the position.
   - It must never steal focus or touches outside itself (`FLAG_NOT_FOCUSABLE`, `FLAG_NOT_TOUCH_MODAL`).
   - Content description: "Time value past your limit, 42 rupees. Illustrative, not money spent."
6. **Never show it**:
   - in essential apps, dialer or in-call screens (`AudioManager.mode` is `MODE_IN_CALL` or `MODE_IN_COMMUNICATION`), camera apps, or apps in the Finance/payment category;
   - on the lock screen (`KeyguardManager.isKeyguardLocked`), or when the screen is off.
   Put these rules in a pure, tested `OverlayPolicy`.
7. **Settings, in the existing Value of time screen:**
   - "Show live indicator over apps" (needs Yoga guard).
   - Style: Burning (default) / Gentle.
   - Start counting: After my limit / From the first minute.
   - Keep the existing "illustrative, not money lost" wording visible next to these.

### B. Block YouTube Shorts and Instagram Reels
1. Use a table-driven `ShortsDetector` over a small `NodeView` interface (viewId, className, text, contentDescription, children). The service wraps `AccessibilityNodeInfo`; tests use fakes.
   - Surfaces:
     - **YouTube Shorts:** `com.google.android.youtube`. View ids such as `reel_recycler`, `reel_watch_player`, `reel_player_page_container`, `shorts_container`. Use the real ids you know of and keep the list easy to extend.
     - **Instagram Reels:** `com.instagram.android`. Ids such as `clips_viewer_view_pager`, `clips_viewer_pager`, `clips_video_container`, `root_clips_layout`.
     - **Browsers (optional, off by default):** `youtube.com/shorts` or `instagram.com/reels` in the address bar of Chrome, Samsung Internet and Firefox.
   - Limit the tree walk to about 400 nodes and a depth of about 30.
   - On Android below 33, recycle nodes.
2. Only inspect window content for packages of enabled surfaces. Do so on window-state changes and on content changes throttled to at most one every 500 ms. **Never store, log or send screen content.**
3. Per surface, choose **Block** (default) or **Allow N minutes a day**, with chips for 5/10/15/30 minutes. Prefill the user's last choice. Time spent in an allowed surface counts against today's allowance; when it runs out, the surface is blocked.
4. **Blocking:**
   - Do `GLOBAL_ACTION_BACK` and show a 2.5-second overlay card: "Shorts are off · Regular videos still work · Blocked 3 times today".
   - Debounce. If the screen is still Shorts or Reels after 2 backs within 3 seconds, do `GLOBAL_ACTION_HOME`. That covers Shorts opened from a link or as the app's first screen.
   - Put this in a pure, tested `ShortsGate` that takes detection plus time and returns NONE, BACK or HOME, and keeps the allowance and debounce state.
5. Count blocks per day per surface in DataStore. Show "Shorts and Reels blocked today: N" on the Usage dashboard. Include the counts in the backup if that is simple in `BackupCodec`; otherwise skip it and say so.

### Shared
1. **Service:** `guard/YogaGuardService.kt` extends `AccessibilityService`.
   - Config in `res/xml/yoga_guard_service.xml`:
     - `accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged"`
     - `canRetrieveWindowContent="true"`
     - `accessibilityFlags="flagReportViewIds|flagIncludeNotImportantViews"`
     - `notificationTimeout="300"`
     - `isAccessibilityTool="false"`
     - a description string explaining both uses.
   - Declare it in the manifest with `BIND_ACCESSIBILITY_SERVICE`.
   - When neither A nor B is turned on, the service does nothing except remember the foreground package.
2. **New screen** `settings/guard`, titled "Shorts and live time value", linked from Settings and from the Value of time screen.
   - Before sending anyone to `Settings.ACTION_ACCESSIBILITY_SETTINGS`, show a **prominent disclosure** that Google Play requires. It must say:
     - what Yoga guard reads: which app is open, and the screen structure of YouTube, Instagram and the selected browsers, only to recognise Shorts and Reels;
     - that nothing is stored or sent;
     - that it can be turned off any time.
     The user must tap "Agree and open settings"; "Not now" is the other choice.
   - Show the service status (on or off, from `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`), the toggles for A and B, the per-surface choices, and today's block counts.
   - Add a help line: on Android 13+ when the app was not installed from Play, the user may first need App info → ⋮ → "Allow restricted settings".
3. **Privacy and data screen:** add a permission row for Yoga guard.
4. **Docs:**
   - `docs/launch/privacy-policy.md`: add a section on accessibility use.
   - `docs/launch/play-listing.md`: add the Accessibility API permission declaration text and the core-functionality justification.
   - `docs/launch/launch-audit.md`: add items for the disclosure, OEM checks, and re-checking the Shorts/Reels view ids after app updates.
   - Spec: tick the line "Design optional overlay/notification cost indicator and safety exclusions" with a short note. Add a short line about the new Shorts/Reels blocking under the relevant section.
   - Update `ROADMAP.md` (piece 8).
5. **Tests (JUnit, `app/src/test/.../guard/`):**
   - `BurnMeter`: both modes, the no-limit case, precision switching, a zero hourly value.
   - `OverlayPolicy`: every exclusion.
   - `ShortsDetector`: positive and negative fake trees for each surface, browser URLs, and the node limit.
   - `ShortsGate`: block, allowance running out, debounce, escalation to HOME, day rollover.

## Constraints
- Follow the existing manual DI in `YogaApplication`: add a lazy `guardPrefs` or `guardRepository`.
- No new third-party dependencies.
- No Room schema change unless needed. If you change it, bump the version; `fallbackToDestructiveMigration` is acceptable for now.
- Keep the code style of the repo.
- Files must be UTF-8 without a BOM. Never write mojibake: use the real characters 🔥, ·, ₹ and ….
- Your sandbox cannot run Gradle or write the git index. Do not run git commands that write. Write the code carefully so it compiles, and list anything you are unsure compiles.

## Done means
A and B are implemented as above, with settings, disclosure, docs and unit tests. Finish with a short summary: files changed, anything skipped or uncertain, and the YouTube/Instagram view ids you chose.
