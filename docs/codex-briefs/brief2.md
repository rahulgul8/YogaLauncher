You are working in the Android repo at C:\me\code\mobile\YogaLauncher (Kotlin, Jetpack Compose, Material 3, package `in.tuwi.yogaphone`, minSdk 26, targetSdk 36). It is a minimalist "mindful" home-screen launcher. The foundation is in place: Room database (`data/db/`), a manual DI holder in the Application class, `AppRepository` with per-app metadata (`isEssential`, `isDistracting`, `isHidden`, favourites), DataStore `Prefs`, navigation-compose routes, settings screens, and a `HomeStatusArea` slot under the home clock. Read the code first, and read `YogaLauncher_Product_Specification.md` sections 3 (Journeys B and C), 4.2, 4.7, 6 (copy and intervention levels), 7, 8 and 9 for product intent. Room, WorkManager, navigation-compose, icons-extended and coroutines-test are already dependencies — add no new dependencies. `java.time` is available (minSdk 26).

## Goal: Piece 2 — usage awareness, allowances, threshold reminders and the "value of time" indicator (spec Phase 1)

### 1. Usage Access
- A permission status helper (AppOpsManager `OPSTR_GET_USAGE_STATS` check) and a reusable `UsageAccessCard` composable that explains in plain words what is read and why ("Yoga reads which apps were in the foreground and for how long. This stays on your phone."), with a button to `Settings.ACTION_USAGE_ACCESS_SETTINGS` (try the package-specific `Uri` form first, fall back to the general screen). Re-check on resume.
- Every usage surface shows a helpful fallback state when access is not granted (never crash, never show zeros as if real).

### 2. Usage measurement (pure, testable core)
- `UsageCalculator` (plain Kotlin, no Android types in its API) that turns a list of foreground events (packageName, timestamp, type = RESUMED / PAUSED / STOPPED / SCREEN_OFF / SHUTDOWN) into per-package foreground milliseconds within a [start, end) window: clamps sessions that started before the window or are still open at `now`, closes all sessions on screen-off/shutdown, ignores duplicate RESUMED events, ignores the launcher's own package.
- An Android adapter reading `UsageStatsManager.queryEvents` (ACTIVITY_RESUMED/ACTIVITY_PAUSED/ACTIVITY_STOPPED on API 29+, MOVE_TO_FOREGROUND/BACKGROUND below; SCREEN_NON_INTERACTIVE, DEVICE_SHUTDOWN) for the window, starting a little before the window so already-open sessions are caught.
- Day boundaries in local time via `java.time` (`ZoneId.systemDefault()` evaluated at query time so timezone changes are respected); handle midnight rollover while the app is open.
- **Eligible minutes** = usage of apps counted by the allowance. Setting "Allowance counts": `Distracting apps` (default) or `All apps except essential`. Essential apps are never counted: anything `isEssential`, the default dialer, default SMS app, this launcher, and system settings.
- **Default classification** (only applied to apps the user has not classified themselves): distracting if `ApplicationInfo.category` is SOCIAL, VIDEO, GAME or NEWS, or the package is in a small built-in list (Instagram, Facebook, TikTok, YouTube, X/Twitter, Reddit, Snapchat, Netflix, Prime Video, Pinterest, Threads, LinkedIn, ShareChat, Moj, Josh). Essential if it is the default dialer/SMS/camera/clock/settings, Google Maps, or a known authenticator. Keep this in a separate `AppClassifier` so a later piece can extend it.
- A Room entity `UsageAggregateEntity(date: String yyyy-MM-dd, packageName, foregroundMinutes, eligible: Boolean)` (bump DB version; destructive migration is acceptable pre-release) written by a daily WorkManager job for yesterday and by the periodic job for today, so history survives beyond Android's event retention.

### 3. Allowances and rules
- Room entity `UsageRuleEntity(id, scope = TOTAL | APP, targetPackage nullable, dailyAllowanceMinutes, warnAtPercent default 80, enabled)`. One TOTAL rule exists by default only after the user sets it (default suggestion 90 minutes).
- `RuleEvaluator` (pure Kotlin, unit-tested) returning, per rule and overall: used, allowance, remaining, state `UNDER | NEAR | OVER`, excess minutes.
- Settings group **Usage rules**: daily allowance (slider/number: 15–480 min in 15-min steps, or "Off"), what counts (distracting / all except essential), per-app limits list (add/edit/remove), warning threshold (off / 70 / 80 / 90 %), and an apps list to toggle distracting/essential for any app.

### 4. Reminders (intervention levels 0–1 from the spec)
- A periodic WorkManager job (15 min) plus a check whenever the launcher resumes: when a rule crosses NEAR or OVER for the first time today, post one neutral notification (channel "Usage reminders", default importance low/default). Copy examples: "You've used 72 of your 90 minutes today." / "You're 12 minutes past today's limit." Never shaming language. Remember which notifications were already sent today (DataStore keyed by date) so nothing repeats.
- Request `POST_NOTIFICATIONS` (API 33+) in context — when the user turns reminders on — not at app start.

### 5. Home usage indicator (fill the existing `HomeStatusArea`)
- One quiet line plus a thin progress bar: "42 min of 90 today" / when over: "12 min past today's limit" in the restrained warning colour (with a non-colour cue such as a small icon or the word "past"). Hidden when no allowance is set or the setting "Show usage on home" is off. Tap → usage dashboard.
- When over the limit and the value-of-time feature is on, add "· ₹100 time value" (see 6).

### 6. Value of time (spec 4.7 / Journey C)
- Settings group **Value of time**: off by default; hourly value (number input), currency (default from the device locale via `java.util.Currency`, editable from a short list), a live example ("20 min past your limit ≈ ₹167 of your time"), and an always-visible explanation: "This is an illustrative value of your time — not money spent or income lost."
- `TimeValueCalculator` (pure, tested): equivalent = excessMinutes × hourlyValue / 60, rounded sensibly (whole units; no decimals for amounts ≥ 10), formatted with the currency symbol via `NumberFormat.getCurrencyInstance`.
- Indicator options: on home (default), and optionally an ongoing silent notification while over the limit that updates on each check ("12 min past today's limit · ₹100 time value"), with a "Snooze for today" action. No draw-over-other-apps overlay in this piece.
- A details sheet reachable from the indicator explaining the calculation, the contributing apps, and the period.

### 7. Usage dashboard screen (route `usage`)
- Today / Last 7 days toggle. Totals: eligible minutes vs allowance, all foreground time, number of over-limit days in the week; per-app list sorted by time with a thin bar, marked distracting/essential; tapping an app opens a sheet to set an app limit or change its classification.
- A short note explaining the difference between app foreground time and screen-on time.
- Weekly time-value equivalent when the feature is on.
- Reachable from home indicator and from settings.

### 8. Tests
JVM unit tests for `UsageCalculator` (open sessions, window clamping, screen-off closing, midnight split, duplicate events), `RuleEvaluator`, `TimeValueCalculator` (rounding, zero/negative excess), and the reminder "already sent today" logic.

### 9. Docs
Tick the corresponding items in `ROADMAP.md` and the spec's section 13 backlog checkboxes ("Build Usage Access explanation and permission status UI", "Normalize daily/app usage and define eligible minute rules", "Implement allowance editing, exclusions, and threshold evaluation", "Build usage dashboard and near/over-limit feedback", "Build hourly value setup and cost-equivalent calculation").

## Constraints
- MVVM, StateFlow, one screen per file, logic in plain classes. Calm, text-first visuals that work in light, dark and greyscale themes. Accessible: contentDescriptions, ≥48dp targets, font scaling.
- Local-only: no network.
- Do not run git commands that write; I review and commit.
- If Gradle runs in your sandbox, verify with `gradlew.bat --offline assembleDebug testDebugUnitTest`; otherwise say so and be careful with imports and API-level guards.

## Done means
Compiles, tests pass, items 1–9 done; finish with a short summary of files changed, decisions you made, and anything left undone.
