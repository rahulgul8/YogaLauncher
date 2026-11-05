You are working in the Android repo at C:\me\code\mobile\YogaLauncher (Kotlin, Jetpack Compose, Material 3, package `in.tuwi.yogaphone`, minSdk 26, targetSdk 36). It is a minimalist "mindful" home-screen launcher. Already built: Room DB + manual DI in the Application class, `AppRepository` with per-app `isEssential`/`isDistracting`, DataStore prefs, navigation, settings, greyscale theme, usage measurement/allowances/reminders/value-of-time (`UsageCalculator`, `RuleEvaluator`, usage dashboard, `HomeStatusArea`). Read the code first, and read `YogaLauncher_Product_Specification.md` sections 3 (Journey D), 4.4, 4.6, 6 (interventions by intensity), 7 ("Focus and timebox") and `yoga_launcher_features.md` sections 2 and 5 for intent. Add no new dependencies.

## Goal: Piece 3 — timebox timer, focus sessions and the mindful launch delay (spec Phase 1)

### 1. Timebox timer (spec 4.6, Journey D)
- Start from a quiet "Timebox" text action on home (put a minimal quick-actions row — "Timebox" and "Focus" — below the favourites or above the search button; calm, text-only) and from the app drawer long-press menu ("Timebox this app").
- Start sheet: presets 5 / 15 / 25 / 45 / 60 min + custom (1–180), optional purpose label, optional bound app (launch it right after starting). **Prefill** the last used duration and show the last 3 distinct purposes as tap-to-fill chips (stored on device, newest first, de-duplicated).
- State held in a `TimeboxRepository` (Room `TimeboxSessionEntity(id, purpose, boundPackage, plannedMinutes, startedAt, pausedAt, accumulatedPauseMs, endedAt, status = RUNNING | PAUSED | COMPLETED | CANCELLED, extendedMinutes)`), so it survives process death and reboot (a `BOOT_COMPLETED` receiver reschedules).
- Ongoing notification while running (channel "Timebox", low importance, `setUsesChronometer(true)` + `setChronometerCountDown(true)` with `setWhen(endAt)`), actions Pause/Resume and Stop. Schedule the end with `AlarmManager`: exact (`setExactAndAllowWhileIdle`) when `canScheduleExactAlarms()` is true, otherwise `setWindow`/`setAndAllowWhileIdle` — do NOT require the exact-alarm permission; offer an optional settings row to grant it for precise endings. No foreground service.
- On end: a gentle notification ("Your 25-minute timebox is done") with actions "+5 min", "Done"; if the launcher is opened afterwards, show a completion sheet: Stop / Extend 5 or 10 / Start a focus session. Record completion or extension.
- Home: subtle remaining time in `HomeStatusArea` ("Reading · 18 min left") with tap → pause/stop/extend sheet.

### 2. Focus sessions (spec 4.4)
- Start from the home "Focus" action: duration presets 25 / 45 / 60 / 90 min or "Until I stop", prefilled from last time. Room `FocusSessionEntity(id, startAt, plannedEndAt, endedAt, mode = MANUAL | SCHEDULED, status, overrideCount)`.
- **Schedules**: Settings group "Focus and timebox" → focus schedules (name, days of week, start/end time, e.g. "Work 09:00–13:00 Mon–Fri", "Bedtime 22:00–07:00 daily", including windows that cross midnight). Activation by AlarmManager (inexact is fine) with recomputation on boot, time change and timezone change; a pure `ScheduleEvaluator` decides "is a schedule active at instant X, and when is the next transition" — unit-test it thoroughly, including midnight-crossing windows and DST.
- While focus is active:
  - Home shows "Focus · 32 min left" (or "Focus until 13:00") with an End button (ending early asks nothing — it is reversible; just record it).
  - Distracting apps are removed from the home favourites view and listed in the drawer in a collapsed, de-emphasised "Paused during focus" section. Essential apps are never restricted.
  - Optional (setting, default off): switch the launcher to the greyscale theme during focus, restoring the previous theme afterwards.
  - Optional (setting, default off): Do Not Disturb via `NotificationManager.setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)` when notification-policy access is granted (explain, and link to `Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS` in context). Only restore the filter afterwards if Yoga was the one that changed it.
  - Allowed-apps list: the user can mark extra apps as allowed during focus (setting).
- Session completion: a short, calm completion message ("45 minutes of focus done.") and record it. Keep a simple streak count (consecutive local days with ≥ 1 completed focus session or timebox) exposed from a pure `StreakCalculator` — show it in the completion message only ("3-day streak"); a later piece builds the insights UI.

### 3. Mindful launch delay (spec 4.4, intervention level 2)
- When the user launches a distracting app from Yoga (home, drawer, search Go, timebox bound app excluded), show a full-screen pause first: app name, a slow breathing ring/countdown (configurable 2–10 s, default 5; during focus default is +5 s), today's usage line from the existing rule evaluator ("You've used 72 of 90 min today"), an optional "What for?" purpose with the last 3 purposes as chips, then "Open <App>" (enabled after the countdown) and "Not now" (primary, returns home).
- Do not nag: after the user continues, don't ask again for that app for N minutes (setting: 0 / 5 / 15 / 30, default 5).
- Settings: enable mindful delay (default on for distracting apps), delay length, the "don't ask again" window, and a per-app opt-out from the apps list.
- Room `InterventionEventEntity(id, type = DELAY | FOCUS_OVERRIDE | REMINDER, packageName, shownAt, actionTaken = CONTINUED | WENT_BACK | DISMISSED, purpose)`; record each delay outcome (and continuing during focus increments `overrideCount`).
- Respect reduced motion (`Settings.Global.ANIMATOR_DURATION_SCALE == 0` → no ring animation, just the number).
- Be honest in copy: in Settings add one line "Apps opened from notifications or other apps skip this pause."

### 4. Tests
JVM unit tests for timebox remaining-time with pause/resume/extend math, `ScheduleEvaluator`, `StreakCalculator`, the delay "don't ask again" window logic, and purpose-chip list updates.

### 5. Docs
Tick the matching items in `ROADMAP.md` and the spec section 13 backlog ("Implement timebox lifecycle and completion prompts", "Prototype deliberate launch delay and limitations", "Prototype focus sessions and essential-app exceptions"), and in `yoga_launcher_features.md` ("Add real-time delay and lock overlay" — note it is launcher-level; "Implement deep link integration with Android DND").

## Constraints
- MVVM, StateFlow, one screen per file, logic in plain classes. Calm, text-first visuals for light/dark/greyscale themes; accessible (contentDescription, ≥48dp, font scaling).
- Register receivers/permissions correctly in the manifest (`RECEIVE_BOOT_COMPLETED`, `SCHEDULE_EXACT_ALARM` optional, `ACCESS_NOTIFICATION_POLICY`), `PendingIntent.FLAG_IMMUTABLE`, notification channels created once.
- No network. Do not run git commands that write; I review and commit.
- If Gradle runs in your sandbox, verify with `gradlew.bat --offline assembleDebug testDebugUnitTest`; otherwise say so and be careful with imports and API-level guards.

## Done means
Compiles, tests pass, items 1–5 done; finish with a short summary of files changed, decisions you made, and anything left undone.
