# Brief 20: Mindful pause, a way through that costs something (piece J3)

Repo: Yoga Phone launcher (Kotlin, Jetpack Compose). Work only in this worktree (`YogaLauncher-pause`, branch `resist-pause`). Don't commit; Claude reviews, builds and commits. You can't run Gradle, so write code that compiles first time: check every import and signature against the existing code. Another Codex task is changing the Shorts/Reels guard in a separate worktree at the same time: **do not edit** `guard/*`, `ui/GuardSettingsScreen.kt`, `src/guard/AndroidManifest.xml`, or any `docs/adr`/`openspec` file (the other task writes ADR 0015 and the OpenSpec change for both pieces).

Read first:
- `docs/plans/bypass-with-resistance.md`, "Part 2. App pause" (decisions at the top);
- `resist/ResistanceLadder.kt` (already written and tested; use it, don't duplicate it);
- `ui/MindfulDelayScreen.kt`, `focus/FocusViewModel.kt` (`decide`, `pauseOutcome`), `focus/FocusLogic.kt` (`DelayGate`), `focus/FocusPrefs.kt`, `focus/FocusRepository.kt` (`recordIntervention`), `focus/FocusSystem.kt` (alarms and notifications), and the `pendingPause` block in `MainActivity.kt`;
- `ui/FocusSettingsScreen.kt` (pause settings), `ui/theme/YogaComponents.kt`.

## Goal

Today the pause is the same few seconds every time, then one tap on "Open", then no pause for that app for 5 minutes. Make the way through cost a real choice, the same way the Shorts block will: a wait that grows, choosing how long, seeing the cost, holding the button. Saying no stays one tap and counts as resisted.

## Rules

- **Wait:** `ResistanceLadder.waitSeconds(prefs.delaySeconds, opensToday, PAUSE_CAP_SECONDS, overLimit)`, where `opensToday` is how many times today the person went through the pause for **this app** (CONTINUED interventions today for that package), and `overLimit` is today's overall allowance state `OVER`. During Focus, add 5 s as today.
- **How long:** chips from `ResistanceLadder.choices(opensToday, overLimit)`, preselected with `preselect(lastPassMinutes for this app, choices)`.
- **A pass replaces "Don't ask again for N min":** going through opens the app and records a pass for that package until now + chosen minutes. While a pass is live, launches of that app from Yoga open directly (no pause). Store passes like `DelayGate.encode/decode` does today (package → until), dropping expired entries.
- **Migration:** `dontAskMinutes` is no longer used for the gate. Remove its setting row from `FocusSettingsScreen`; keep the stored key readable so nothing crashes, and use its value as the first-time preselected chip if it matches a choice.
- **Time's up:** when the pass ends, post a notification "Your 10 minutes on Instagram are up" (app label and the chosen minutes) on the existing `CHANNEL_DONE` channel, with a content intent that opens Yoga home. Schedule it with `FocusSystem.setAlarm` (inexact is fine) and handle the action where the timebox alarms are handled. One alarm per package; a newer pass replaces it. If notifications aren't allowed, skip silently. (Interrupting the app itself would need the guard, which is a later piece.)
- **Cost line** (replaces today's `usageLine`, lining figures): "${ordinal(opensToday + 1)} open today · 52 min in Instagram today" when per-app minutes are available from `UsageUiState` (`snapshot.today`, rows by `packageName`), else "${ordinal} open today". Past the allowance, a second line: "You're 20 min past today's limit." Keep the existing allowance line when under the limit.
- **Resisted:** "Not now" records WENT_BACK as today. Show "Resisted N times today" as a small muted line on the pause screen when N > 0 (N = WENT_BACK interventions today across apps), and in the Usage screen next to "Shorts and Reels blocked…" as "Resisted the pause N times today." (`ui/UsageDashboardScreen.kt`: add a parameter, wire it from `MainActivity`).

## Screen (`ui/MindfulDelayScreen.kt`)

Keep the look (eyebrow, app name, breathing clock, "What for?" field with recent chips). Changes:
- the cost line(s) and the resisted line;
- "For how long?" chips under the purpose field;
- **Not now** stays the primary button, always active;
- the secondary button becomes **"Hold to open for N min"**, enabled after the wait. Pressing and holding for `ResistanceLadder.HOLD_MILLIS` fills a thin progress (primary colour), then continues. Releasing early resets. TalkBack: a custom accessibility action "Open for N minutes" that works once the wait is over. Reduced motion: no fill animation.
- `onContinue` now passes the chosen minutes as well as the purpose.

## Data

- `focus/FocusPrefs.kt`: `pausePasses` (package → until), `lastPassMinutes` (package → minutes), with read/write helpers; drop nothing that existing data needs.
- `FocusViewModel.decide` returns `LaunchDecision.Pause(seconds, duringFocus, choices, preselected, opensToday, resistedToday)` (add fields; update every `when`).
- Counting today's interventions: add a DAO query if one doesn't exist (count by type/action/package since local midnight). If a Room schema change is needed, **avoid it**: only add queries, not columns.
- Pure logic (pass active?, encode/decode, which chip) goes in `focus/FocusLogic.kt` with tests in `app/src/test/java/in/tuwi/yogaphone/focus/` (extend the existing test file for `DelayGate`).

## Constraints
- No new dependencies. Don't touch `TODO.md` or the files listed at the top.
- Calm look: kit components, lining figures, no new colours. Short, plain text; never scolding.

## Done when
- It compiles. Claude runs `./gradlew :app:testDebugUnitTest :app:assembleDebug`.
- Your summary lists the files changed and any assumptions or skipped parts.
