You are working in the Android repo at C:\me\code\mobile\YogaLauncher (Kotlin, Jetpack Compose, Material 3, package `in.tuwi.yogaphone`, minSdk 26, targetSdk 36), a minimalist "mindful" launcher. Already built: Room DB + manual DI, apps/categories/profiles, settings, themes, usage measurement + daily `UsageAggregateEntity` history + allowances + reminders + value of time, timebox and focus sessions (Room), `StreakCalculator`, mindful delay with `InterventionEventEntity`, greyscale workflow, notification quieting. Read the code first, then `YogaLauncher_Product_Specification.md` sections 3 (Journey A), 4.10, 4.11, 9, 11 and `yoga_launcher_features.md` sections 4–8 for intent. Add no new dependencies (no chart library — draw charts with Compose Canvas).

## Goal: Piece 5 — insights, onboarding, privacy and backup (spec Phase 2–3)

### 1. Insights (spec 4.10, route `insights`)
- A pure `InsightEngine` producing a list of insight cards from local data, each with a template, the numbers behind it and an eligibility rule. Rules from the spec: never show a percentage change without a valid comparable earlier period (require ≥ 5 days of data in both weeks); define "late-night" as 23:00–05:00 local; "screen-free" stretch = the longest gap today with no eligible foreground usage while awake (07:00–23:00) — label it "longest break from distracting apps", not "screen-free".
- Cards (shown only when eligible): week-over-week eligible minutes change ("This week you spent 22% less time in distracting apps than last week."), late-night usage change, days within allowance this week, timeboxes completed/extended, focus minutes and current streak, most-used distracting category, weekly time-value equivalent (labelled illustrative), today's longest break.
- Encouraging, neutral tone; a "Coaching tone" setting (Gentle / Data-driven) switching between two template sets.
- Charts with Compose Canvas: 7-day bar chart of eligible minutes with the allowance line, and a 4-week × 24-hour heatmap of eligible usage. Both with contentDescription summaries for TalkBack and visible numbers (not colour alone). Light/dark/greyscale correct.
- Optional weekly summary notification (Sunday 19:00, setting default on once notifications are granted) and an optional reflection prompt after a completed focus session ("How did that feel?" with three quick chips, stored locally; no free text needed).
- Focus goals: daily focus-minutes goal and weekly days-within-allowance goal, editable, with progress on the insights screen. A simple adaptive suggestion: if the user beat the allowance on ≥ 5 of the last 7 days, suggest lowering it by 15 min (one tap to accept, dismissible); never auto-change.

### 2. Onboarding walkthrough (spec Journey A, 4.11)
- First-run flow (shown when `onboardingVersion` < 1; skippable at every step; "Replay setup" in Settings → Help):
  1. Welcome — what Yoga changes, in two sentences.
  2. Make Yoga your home app (RoleManager), with "Later".
  3. Pick home apps — prefill with the user's most-used non-distracting apps from usage stats if access is already granted, else common essentials (dialer, messages, camera, maps, browser); edit by tapping.
  4. Usage Access — explanation + grant button, skippable.
  5. Daily allowance — prefilled 90 min for distracting apps, with the auto-classified distracting apps listed and toggleable.
  6. Value of time — optional, off by default, with the "illustrative, not money lost" explanation.
  7. Interventions — toggles for reminders, mindful delay, greyscale when over limit; prefilled sensible defaults.
  8. Theme — Light / Dark / Greyscale / System.
  9. Done — a 3-item preview of gestures (swipe up for apps, long-press for options, Timebox/Focus actions) and land on home.
- Each step writes straight to the existing repositories/prefs (no duplicate storage). Returning users (prefs already set) skip automatically.
- Contextual one-time tips (e.g. first time on home: "Swipe up to see all apps"), dismissible, tracked in prefs.

### 3. Privacy and data (spec 9, features §8)
- Settings → Privacy dashboard: what is stored (counts of days of usage history, sessions, held notifications, intervention events), which permissions are granted (Usage Access, notifications, notification listener, DND access, exact alarms, secure settings) with a button for each to change it, and "Delete all usage history" / "Reset everything" (with undo snackbar for 5 s before actually deleting, instead of an "are you sure" dialog).
- Data retention setting: keep history 30 / 90 / 365 days (default 90); a daily worker prunes older rows.
- Toggles to turn analytics-like features off entirely (usage tracking, insights).

### 4. Backup and restore
- Export preferences and configuration (not raw usage history unless the user ticks "include history") to a JSON file via the Storage Access Framework (`ACTION_CREATE_DOCUMENT`), and import via `ACTION_OPEN_DOCUMENT` with validation and a summary of what will change. Pure `BackupCodec` with a version field, unit-tested round-trip. (Google Drive is out of scope.)
- Set `android:allowBackup` rules so Android auto-backup includes DataStore and the Room DB but excludes held notifications.

### 5. Help
Settings → Help: replay setup, short FAQ (why some apps skip the pause, why usage differs from Digital Wellbeing, how to leave Yoga as home app, battery-optimisation guidance with a button to `Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`).

### 6. Tests
JVM unit tests for `InsightEngine` eligibility and numbers (including the no-baseline case), heatmap bucketing across midnight, adaptive-goal suggestion, retention pruning cutoff, and `BackupCodec` round-trip / version rejection.

### 7. Docs
Tick the matching items in `ROADMAP.md`, the spec section 13 backlog ("Build credible weekly insights and late-night reporting", "Complete onboarding walkthrough and tutorial replay") and `yoga_launcher_features.md` sections 4–8 that this completes (local analytics pipeline, templating for insight cards, reflection prompts, time-tracking database, scoring, dashboard cards, Compose charting, Compose onboarding, connect onboarding preferences, skip mode for returning users, simple UI for AI toggles). Leave AI/TF Lite/NLP, Google Drive, billing and marketplace items unticked, and add a one-line note under each saying it is deferred and why (local templates instead of ML for now; no backend; monetization undecided).

## Constraints
MVVM, StateFlow, one screen per file, logic in plain classes; calm text-first UI in all three themes; accessible. No network. Do not run git commands that write. If Gradle runs in your sandbox verify with `gradlew.bat --offline assembleDebug testDebugUnitTest`, otherwise say so and be careful with imports/API guards.

## Done means
Compiles, tests pass, items 1–7 done; finish with a short summary of files changed, decisions made, and anything left undone.
