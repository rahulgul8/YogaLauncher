# Brief 14: Feature introductions engine (piece C)

You are working in a git worktree of the Yoga Phone Android app on branch `feature-intros`. The app is Kotlin and Jetpack Compose; the code namespace is `in.tuwi.yogaphone` and the app id is `com.tuwi.yogaphone`. Claude reviews, builds and commits. Your sandbox can't run Gradle or fetch packages, so **don't add dependencies**.

## Goal

Yoga should introduce each feature at the moment it would help, backed by the person's own numbers. For example: "6 h 40 m on YouTube this week" with the button **Turn off Shorts**. This piece builds the engine, the three surfaces, the Suggestions list, and the first six introductions.

## Read first

- `openspec/changes/add-feature-introductions/`: the proposal, tasks and spec. **This is the approved scope.**
- `docs/plans/feature-discovery.md` section 1 (context only; don't edit the plan).
- **Usage data:**
  - `usage/UsageRepository.kt`: `UsageSnapshot.week` is per-package `UsageAggregateEntity` rows for the last 7 days, and `knownDays` lists the recorded days;
  - `usage/UsageViewModel.kt`, `UsagePrefs.kt`;
  - the TOTAL allowance rule and `RuleEvaluation`.
- `insights/InsightEngine.kt`: the pure-engine-plus-tests pattern to copy. `suggestedAllowance` drives I5.
- `insights/InsightsRepository.kt`: `onboardingVersion`, `tipsSeen` / `markTip`, and `eligibleByDay`.
- **Guard:**
  - `guard/GuardModels.kt`, `GuardPrefs.kt` (surfaces, `daily` block counts for today only);
  - `YogaGuardService.kt`: `isEnabled(context)`, and `showBlockCard` (around line 328), which builds an Android View card;
  - `ui/GuardSettingsScreen.kt`: the **guard disclosure dialog** (line ~105).
- `quiet/QuietRepository.kt`: `greyOverLimit`.
- `pro/ProState.kt`, `ProGate`: respect gates. I1–I5 and I8 should be free, but check `ProGate` for grey past limit and gate it if it's Pro.
- `MainActivity.kt`: the home `statusContent` column (around line 455) and the navigation routes (`HELP = "settings/help"`).
- `ui/UsageDashboardScreen.kt` and `ui/PrivacyScreens.kt` (`HelpScreen`; there's an existing Snackbar-with-Undo pattern at line ~113).
- `ui/theme/YogaComponents.kt`, `YogaIcons.kt`, `docs/design/brand.md`. Use the Yoga kit only, and never hard-code colours or fonts.
- ADR format: `docs/adr/0010-yoga-pro.md`. **Your ADR is 0012.**

## Build

### 1. Engine (`intro/`, pure Kotlin, no Android types)

- **`IntroModels.kt`:**
  - `enum IntroId { SHORTS_BLOCK, REELS_BLOCK, BROWSER_SURFACES, DAILY_ALLOWANCE, LOWER_ALLOWANCE, GREY_PAST_LIMIT }`. Leave room for more later.
  - `enum IntroSurface { USAGE, HOME, BLOCK_CARD }`.
  - `IntroSignals`, all values precomputed:
    - `youtubeMinutes7d`, `instagramMinutes7d`, `browserMinutes7d`;
    - `recordedDays`;
    - `youtubeInstalled`, `instagramInstalled`;
    - `guardEnabled`, `surfacesEnabled: Set<GuardSurface>`;
    - `blocks7d: Map<GuardSurface, Int>`;
    - `allowanceMinutes: Int?`, `avgDistractingPerDay: Double?` (null under 3 recorded days);
    - `suggestedAllowance: Int?`;
    - `daysOverLimitThisWeek`, `greyOverLimitOn`;
    - the Pro state (or a pre-computed `allowed: Set<IntroId>`).
  - `IntroRecord(shownCount, firstShownDate, snoozedUntil, dismissals, retired)`.
  - `IntroState(records, lastNewIntroDate, onboardingFinishedAtMillis)`.
  - `Intro(id, surface, evidence: String, actionLabel: String, note: String?, impactMinutesPerWeek: Int, homeWorthy: Boolean)`.
- **`IntroEngine.kt`:** `fun pick(surface, signals, state, today: LocalDate, nowMillis): Intro?`. Rules, all unit-tested:
  - **Eligibility per introduction:**
    - **SHORTS_BLOCK:** YouTube installed, `youtubeMinutes7d ≥ 180` or (≥ 3 recorded days and ≥ 30 min/day average), and YOUTUBE not in `surfacesEnabled`.
      - Evidence: "6 h 40 m on YouTube this week".
      - Action: "Turn off Shorts". Note: "Regular videos still work."
      - Impact = YouTube minutes ÷ 2.
    - **REELS_BLOCK:** the same for Instagram. Action: "Turn off Reels".
    - **BROWSER_SURFACES:** the app surface is blocked, `blocks7d` for it ≥ 5, `browserMinutes7d ≥ 60`, and the matching browser surface is off.
      - Evidence: "Shorts blocked 14 times this week".
      - Action: "Block in browsers too".
    - **DAILY_ALLOWANCE:** no allowance, and `avgDistractingPerDay ≥ 120`.
      - Evidence: "2 h 40 m a day in distracting apps".
      - Action: "Set 2 h a day". The value is 80% of the average, rounded **down** to 15 min, minimum 30.
    - **LOWER_ALLOWANCE:** `suggestedAllowance != null`.
      - Evidence: "5 of the last 7 days under 1 h 15 m".
      - Action: "Lower to 1 h 15 m".
    - **GREY_PAST_LIMIT:** allowance set, `daysOverLimitThisWeek ≥ 3`, grey off.
      - Evidence: "Past your limit on 4 days this week".
      - Action: "Turn grey past my limit".
  - **Never** within 24 h of `onboardingFinishedAtMillis`; never retired; never while snoozed.
  - **At most one introduction not shown before per local day** (`lastNewIntroDate`). An introduction already shown may keep appearing on its surface.
  - **Surfaces:**
    - USAGE: any eligible introduction;
    - HOME: only `homeWorthy` ones (SHORTS_BLOCK, REELS_BLOCK, DAILY_ALLOWANCE), and at most once per local day;
    - BLOCK_CARD: only the sibling (YouTube blocked → REELS_BLOCK if eligible ignoring the time threshold, but Instagram must be installed; and the reverse).
  - Highest impact wins. Ties go to the declared enum order.
  - **Dismissal:** the first "Not now" snoozes for 7 days; the second retires.
  - Format durations with a shared helper: "40 min", "1 h", "6 h 40 m".
- **Tests** (`IntroEngineTest.kt`): every eligibility rule and its boundaries, the 24 h quiet period, one-new-per-day across midnight, snooze and retire, surface filtering, and impact ordering.

### 2. State and signals

- **`intro/IntroStore.kt`** (DataStore):
  - records, `lastNewIntroDate`, `homeShownDate`, `onboardingFinishedAtMillis`;
  - record `onboardingFinishedAtMillis` when onboarding finishes. If it's missing but `onboardingVersion ≥ 1`, treat it as long ago.
  - `markShown`, `dismiss` (snooze/retire), `resetForSuggestion` (when turned on from Suggestions).
- **Guard weekly block counts:**
  - `GuardPrefs` keeps block counts **for today only**;
  - add a 7-day ring of daily block counts per surface (aggregate counts only, the same kind of data already saved; no content);
  - roll it over when the day changes;
  - include it in "delete history" / `clearHistory`.
- **`intro/IntroSignalsReader.kt`:**
  - builds `IntroSignals` from the repositories and the package manager;
  - YouTube is `com.google.android.youtube` and Instagram is `com.instagram.android`;
  - browsers are the packages in `ShortsDetector.browserAddressIds`.
  - Expose a `Flow`/`StateFlow` that the UI can collect cheaply. Don't do heavy work on home recomposition.

### 3. UI

- **`ui/IntroCard.kt`** (kit style):
  - the evidence in the number style (lining figures), the optional note in muted text, a primary text action, and "Not now";
  - an optional small spot-icon slot (piece B will supply images later; use `YogaIcons` for now: Shield for guard, Gauge for allowance, Contrast for grey).
- **Usage screen:** the picked USAGE introduction as the first item of `UsageDashboardScreen`.
- **Home:** one quiet line in the `DefaultHomePrompt` style: accent dot, evidence text, action button.
  - Add it to `statusContent` with **one line** in `MainActivity`, right after `DefaultHomePrompt`. The composable and its logic live in a new file.
  - Tapping the line runs the action.
- **Block card** (`YogaGuardService.showBlockCard`):
  - when the engine returns a BLOCK_CARD sibling, add a second line ("Also turn off Reels?") with a tappable action;
  - tapping enables that surface directly in `GuardPrefs`, since the guard is already on, and the card text becomes "Reels are off too";
  - keep the existing card behaviour otherwise.

### 4. Actions (one tap, prefilled, Undo)

- **SHORTS_BLOCK / REELS_BLOCK:**
  - **If the guard is already enabled:** enable the surface (`allowanceMinutes = 0`, i.e. Block), then show a Snackbar "Shorts are off. Regular videos still work." with **Undo**.
  - **If the guard is not enabled:**
    - extract the disclosure dialog from `GuardSettingsScreen` into a reusable `GuardDisclosureDialog`. **Keep its text exactly as it is**, because Google reviewed it, and use it in both places;
    - on "Agree and open settings", remember the pending surface and open Accessibility settings;
    - on the next resume, if `YogaGuardService.isEnabled`, enable the surface and show the confirmation with Undo;
    - if it's still not enabled, show the note "Yoga guard isn't on yet" with "Try again". Don't leave the surface pref on.
- **BROWSER_SURFACES:** enable the matching browser surface, then a Snackbar with Undo.
- **DAILY_ALLOWANCE / LOWER_ALLOWANCE:** set the TOTAL allowance through the existing usage path, then a Snackbar "Daily allowance set to 2 h" with Undo (restore the previous value).
- **GREY_PAST_LIMIT:** turn on `greyOverLimit`, then a Snackbar with Undo.
- **Every action** calls `IntroStore.markShown`/acted so the introduction disappears.

### 5. Help → Suggestions (`ui/SuggestionsScreen.kt`, route `settings/suggestions`)

- A row in `HelpScreen`: "Suggestions", summary "Ideas based on your phone".
- The screen lists every introduction that is snoozed or retired and still relevant (the feature isn't on), with its evidence and action. If there's nothing, show a calm note: "Nothing to suggest right now."

### 6. Docs

- ADR 0012 (`docs/adr/0012-feature-introductions.md`).
- Tick the done items in `openspec/changes/add-feature-introductions/tasks.md`.
- Under "C." in `TODO.md`, tick the items you finished. Leave the heading's `[~]` alone.
- If the 7-day block ring is stored, add one sentence to `docs/launch/privacy-policy.md` where guard data is described: "block counts for the last 7 days".

## Constraints

- Don't change onboarding, `HomeUsageIndicator`, update or what's-new code, billing, the drawer, Shorts detection logic, or the **text** of the guard disclosure.
- Keep `MainActivity` edits minimal. Another branch also adds one home line and a Help row, so put your logic in new files.
- Copy rules: short, calm, sentence case, no exclamation marks, no guilt. Numbers use the existing lining-figure style.
- Plain UTF-8 without BOM. No mojibake in "·", "…" or "→".
- When finished, list the files you changed, the tests you added, and anything you couldn't complete or verify.
