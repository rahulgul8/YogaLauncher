# Brief 19: Shorts and Reels, a way through with resistance (pieces J1 + J2)

Repo: Yoga Phone launcher (Kotlin, Jetpack Compose). Work only in this worktree (`YogaLauncher-shorts`, branch `resist-shorts`). Don't commit; Claude reviews, builds and commits. You can't run Gradle, so write code that compiles first time: check every import and signature against the existing code. Another Codex task is changing the mindful pause in a separate worktree at the same time: **do not edit** `ui/MindfulDelayScreen.kt`, `focus/*`, or `MainActivity.kt`.

Read first:
- `docs/plans/bypass-with-resistance.md` (the plan; decisions at the top: a way through by default, all free, strict changes apply tomorrow);
- `resist/ResistanceLadder.kt` (already written and tested: waits, choices, preselect, ordinal; use it, don't duplicate it);
- `guard/ShortsGate.kt`, `guard/GuardModels.kt`, `guard/GuardPrefs.kt`, `guard/YogaGuardService.kt`, `guard/BlockCardIntroduction.kt`, and their tests in `app/src/test/java/in/tuwi/yogaphone/guard/`;
- `ui/theme/YogaComponents.kt` (the kit) and `ui/MindfulDelayScreen.kt` (read only: the visual model for the new screen, with the breathing clock);
- `review/ReviewController.kt` (`recordInterruption`), `src/guard/AndroidManifest.xml`.

## Goal

Today the Shorts/Reels block has no way through, so people who really want Shorts turn the whole guard off. Give them a way through that is always there but takes a real choice: a wait that grows, choosing how long, seeing the cost, and holding a button. Saying no stays one tap and counts as a win ("resisted").

## J1. Model and gate (pure, tested)

- `GuardModels.kt`:
  - `GuardDaily` gains `passes: Map<GuardSurface, Int>` and `resisted: Map<GuardSurface, Int>` (defaults empty).
  - `GuardPreferences` gains `strict: Boolean = false` (no way through; today's behaviour), `strictOffFrom: String? = null` (ISO date when a pending "strict off" takes effect; J4 builds the UI, you only store and honour it), `passUntil: Map<GuardSurface, Long>` (epoch ms; a live pass), and `lastPassMinutes: Int? = null`.
  - Passes are per **app family**: a pass for YOUTUBE also covers BROWSER_YOUTUBE, and INSTAGRAM covers BROWSER_INSTAGRAM. Add `fun GuardSurface.family(): Set<GuardSurface>`.
- `GuardPrefs.kt`: persist the new fields (keys `guard_strict`, `guard_strict_off_from`, `guard_<SURFACE>_pass_until`, `guard_last_pass_minutes`, `guard_<SURFACE>_passes`, `guard_<SURFACE>_resisted`), following the existing key style. `saveDaily` saves the new counters; day rollover resets them like `blocks`. Add:
  - `suspend fun startPass(surface: GuardSurface, minutes: Int, now: Long)`: sets `passUntil` for the whole family, increments `passes` for today, remembers `lastPassMinutes`.
  - `suspend fun recordResisted(surface: GuardSurface)`: increments `resisted` for today.
  - `fun isStrict(prefs, today: LocalDate): Boolean` (pure, in GuardModels): strict and not yet reached `strictOffFrom`.
  - Keep the 7-day block history; add passes and resists to it the same way only if it's simple, otherwise skip and say so.
- `ShortsGate.evaluate`: takes `passUntil` (epoch ms) and `nowEpoch`. While a pass covers the detected surface, return NONE and keep charging time to `usedMillis` as today. Daily counters for passes/resisted live in `GuardDaily` but are changed through `GuardPrefs`, not by the gate (the gate must not overwrite them: merge carefully in `saveDailyIfChanged`, or keep the gate's copy in sync from prefs). Explain your choice in the summary.
- Tests in `ShortsGateTest` / a new `GuardPassTest`: a pass lets Shorts through, it expires, it covers the browser surface of the same family but not the other family, strict ignores nothing (strict just hides the way through; the gate is the same), and the `isStrict` date rule.

## J2. The way through

### Block card
- When not strict: the card gets a second, quiet action **"Watch anyway"** (tappable text in the accent colour, at least 48 dp tall), and stays up 6 s instead of 2.5 s. The card's layout follows how `BlockCardIntroduction.kt` adds its line and keeps the card visible; reuse that mechanism, and don't show both the introduction line and "Watch anyway" at once (the introduction wins only on the first block of the day).
- Tapping it starts `ResistActivity` with the surface as an extra (`FLAG_ACTIVITY_NEW_TASK`, `FLAG_ACTIVITY_NO_ANIMATION`), and hides the card.
- When strict: today's card, unchanged.
- When a pass ends while the person is still on Shorts, the next block shows the card text "Your 10 minutes are up" (use the minutes of that pass) instead of the usual text, once.

### Resist screen
- New `ui/ResistScreen.kt` (a composable) hosted by new `ResistActivity` (a `ComponentActivity` in the main source set, using the app theme like `MainActivity`; `excludeFromRecents`, `noHistory`, `taskAffinity=""`, portrait not forced). Declare it in `src/guard/AndroidManifest.xml` (it only makes sense with the guard), not exported.
- Layout, top to bottom, matching `MindfulDelayScreen`'s look:
  - eyebrow "Shorts" or "Reels"; title "Watch anyway?";
  - the breathing clock with the countdown (`ResistanceLadder.waitSeconds(SHORTS_BASE_SECONDS, passesToday, SHORTS_CAP_SECONDS)`), "Breathe" when it reaches 0;
  - the cost line in lining figures: "${ordinal(passesToday + 1)} time today · blocked N times · resisted M times". Add "· 48 min this week" only if a week number is cheaply available from `GuardPrefs`' 7-day history; otherwise leave it out;
  - "For how long?" chips from `ResistanceLadder.choices(passesToday)`, preselected with `preselect(lastPassMinutes, choices)`;
  - **Not now** (`YogaPrimaryButton`, always active): records a resist, closes the screen; the person is left on the app's regular feed;
  - **Hold to watch for N min** (secondary): enabled only after the wait. Pressing and holding for `ResistanceLadder.HOLD_MILLIS` fills a thin ring or bar (primary colour) and then starts the pass and closes. Releasing early resets it. For accessibility, a TalkBack user activates it with a custom accessibility action "Watch for N minutes" (use `semantics { onClick(label) {...} }` that starts the pass directly once the wait is over). Reduced motion: the fill is a plain progress without animation.
- Back = Not now.
- Opening the screen calls `reviewController.recordInterruption()`.

### Pass pill
- While a pass is live and Shorts/Reels of that family is in front, the guard shows a small pill "Shorts · 7 min left" (or "Reels · …"), updated every 30 s or so, using the existing pill view style (`label()`, `styleOverlay`). This pill shows **regardless** of the Pro value-of-time setting. If the value-of-time pill would also show, show only the pass pill.
- When the pass ends: hide the pill; the gate blocks on the next detection as usual.

## Docs
- ADR `docs/adr/0015-bypass-with-resistance.md` (context, decision, consequences: why a way through with resistance beats a hard block or an instant bypass; strict mode; passes per family; nothing leaves the phone).
- OpenSpec change `openspec/changes/add-bypass-with-resistance/` (proposal, tasks, spec delta for the guard), following `openspec/changes/add-yoga-guard/`. Read `openspec/AGENTS.md`. Cover J1–J4 in the proposal; mark only J1 and J2 tasks done.
- `docs/launch/privacy-policy.md`: where it says guard block counts are kept 7 days, add passes and "not now" counts in the same sentence.

## Constraints
- No new dependencies. Don't touch `TODO.md`, `review/*` (beyond calling it), the files listed at the top, or the Shorts detection itself (`ShortsDetector`).
- Calm, minimal look: kit components, lining figures, no new colours, no shadows. Text short and plain, no exclamation marks, never scolding.

## Done when
- It compiles. Claude runs `./gradlew :app:testDebugUnitTest :app:assembleDebug`.
- Your summary lists the files changed, how the gate and prefs share the daily counters, and any assumptions or skipped parts.
