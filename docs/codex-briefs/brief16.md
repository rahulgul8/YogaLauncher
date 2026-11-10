# Brief 16: Onboarding v3 (piece E): images, Shorts and Reels, search in pickers

Repo: Yoga Phone launcher (Kotlin, Jetpack Compose). Work only in this worktree (`YogaLauncher-onboarding`, branch `onboarding-v3`). Don't commit; Claude reviews, builds and commits. You can't run Gradle, so write code that compiles first time: check every import and signature against the existing code.

Read first:
- `docs/plans/feature-discovery.md`, section 2 (the flow table and copy rules);
- `app/src/main/java/in/tuwi/yogaphone/ui/OnboardingScreen.kt` (today's walkthrough);
- `docs/design/brand.md` (if present).

## What the founder asked for

1. **A search field** in both app lists in onboarding: "Pick your home apps" (`HomeAppsStep`) and the distracting-apps list (`AllowanceStep`).
2. **An onboarding flow that offers Shorts blocking, Reels blocking and the other features.** Feature introductions (the `intro/` engine) are only a second chance for people who skipped them here. The engine already ignores a surface that's already enabled, so turning something on here retires its introduction; don't change `intro/`.
3. **Images on the onboarding screens.** They exist but no screen uses them yet: `res/drawable-nodpi/onboarding_{welcome,awareness,shorts,reels,allowance,nudges,done}.webp`. Each is a white alpha mask; draw it with `ColorFilter.tint(MaterialTheme.colorScheme.primary)` so it takes the current look's accent.
4. **Minimal text and a premium look.** One title per step and at most one short line, following the table in the plan.

## New step list (`private enum class Step`)

`WELCOME, HOME, APPS, USAGE, WEEK, BLOCKS, ALLOWANCE, NUDGES, LOOK, DONE`

- **VALUE (value of time) leaves onboarding.** It stays in Settings and becomes a later introduction (not part of this brief).
- **The step list is computed** (`remember` plus derived state), not `Step.entries`:
  - WEEK only appears when Usage Access is granted;
  - BLOCKS only appears when `BuildConfig.GUARD` is true, `ProGate.allows(ProFeature.SHORTS_BLOCKING, pro)` (the trial counts) and YouTube or Instagram is installed;
  - recompute when returning from settings, so granting Usage Access on the USAGE step makes WEEK appear next;
  - step dots use the computed size.
- **Copy** (title / line; nothing else unless stated):

| Step | Title | Line | Image |
|---|---|---|---|
| WELCOME | Your phone, on purpose. | A calm home that shows where time goes. | welcome, plus the existing live clock if one is there |
| HOME | Make Yoga your home. | Switch back any time. | none; keep the existing button and done line |
| APPS | Apps you reach for on purpose. | Up to 8. The rest is one swipe away. | none; this step has the search and list |
| USAGE | See where time goes. | Stays on this phone. Never sent anywhere. | awareness; keep `UsageAccessCard` and the done line |
| WEEK | Your last 7 days. | none | none (real numbers, see below) |
| BLOCKS | Keep videos. Lose the scroll. | Shorts and Reels off. Everything else works. | shorts (or reels if only Instagram is installed) |
| ALLOWANCE | How much is enough? | none | allowance; the slider and the distracting list with search |
| NUDGES | Start gently. | none | nudges; the 3 existing switches, summaries kept short |
| LOOK | Choose your look. | none | none; the existing picker |
| DONE | Ready. | Everything is unlocked for 14 days. | done; keep the 4 tips list |

- Delete the other body text and "You can change this later" lines.

## Images

- Add a private `OnboardingImage(@DrawableRes id: Int)` composable:
  - `Image(painterResource(id), contentDescription = null, colorFilter = ColorFilter.tint(primary), contentScale = ContentScale.Fit)`;
  - full width within `ScreenMargin`, height 200.dp (160.dp when the screen is shorter than 700 dp: use `LocalConfiguration.current.screenHeightDp`);
  - placed above the title.
- Steps with long lists (ALLOWANCE) use a 120.dp image, so the list keeps room. If the content doesn't fit, the step's column scrolls rather than clipping.
- A gentle fade-in (`AnimatedVisibility` or `alpha` with a 400 ms tween) on each step. No other animation.

## WEEK step: "Your last 7 days."

- Read `usage.snapshot.week` (already passed in).
  - Show the 7-day total in large lining figures: `MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "lnum, tnum")`, formatted like "18 h 40 m".
  - Then the top 3 apps by minutes, as rows with label and duration.
- Exclude Yoga itself and the default home. If YouTube or Instagram is in the week at all, show its row (in the top 3 or as a 4th row), with the minutes in `primary`.
- **If the week has no data yet** (access just granted), show "Your numbers fill in as you use the phone." and continue. Don't block.

## BLOCKS step: "Keep videos. Lose the scroll."

- One `YogaSwitchRow` per installed app (`com.google.android.youtube`, `com.instagram.android`):
  - "YouTube Shorts", with the summary showing the week's YouTube time if known ("6 h 40 m on YouTube this week"), else "Regular videos still work";
  - "Instagram Reels", with the summary "N on Instagram this week", else "Posts and messages still work".
- **Initial state:** the current guard prefs. A switch reads on when that surface is enabled with `allowanceMinutes == 0`.
- **Turning a switch on:**
  - **guard already on** (`YogaGuardService.isEnabled(context)`): write the surface straight away. `app.guardPrefs.update { p -> p.copy(surfaces = p.surfaces + (surface to p.surface(surface).copy(enabled = true, allowanceMinutes = 0))) }`, the same as `IntroActions.apply` does.
  - **guard off:**
    1. Remember the wanted surfaces in `rememberSaveable` state.
    2. Show `GuardDisclosureDialog` (ui/GuardDisclosureDialog.kt). Its text must stay exactly as it is.
    3. On confirm, open `Settings.ACTION_ACCESSIBILITY_SETTINGS`.
    4. On the next `ON_RESUME` (use `LocalLifecycleOwner` with a `LifecycleEventObserver`, like `IntroActionHost`):
       - if the guard is now enabled, write all wanted surfaces and show a done line, "Shorts are off" / "Reels are off" / "Shorts and Reels are off";
       - if not, show one muted line, "Yoga guard isn't on yet", with a "Try again" text button that reopens the dialog;
       - either way, turn the switches back to match reality.
- **Turning a switch off** writes `enabled = false` for that surface.
- **Continue never forces anything.** Leaving the switches off is fine; the introductions offer them later.
- Wire guard access through `LocalContext.current.applicationContext as YogaApplication` (`guardPrefs`, `proRepository`), as `IntroActions` does. Don't add OnboardingActions parameters for this.

## ALLOWANCE step

- **Prefill:** when the person has at least 3 days of data, prefill the slider at 80% of their average distracting minutes per day, rounded down to 15 and kept between 30 and 240. Otherwise keep 90.
  - The average comes from `usage.snapshot` / `InsightEngine`: find the existing helper used by `IntroSignalsReader` (`avgDistractingPerDay`, `eligibleByDay`). Reuse it or compute it from `usage.snapshot.week` rows with `eligible == true`.
  - Only prefill when there's no existing allowance (`initial.allowanceMinutes == null`).
- Keep the "only write what differs" rule.
- **List:** a search field (below), then the list.

## Search in both pickers (APPS and ALLOWANCE)

- A compact `YogaTextField` with a search icon and the placeholder "Search apps", pinned above the list.
- Filter with `rankAppSearch` (logic/LauncherLogic.kt): `rankAppSearch(apps.map { SearchableApp(it.displayLabel, it) }, query)`.
- **Blank query:** keep today's order, with chosen apps first.
- **Non-blank query:** show only the ranked matches. Chosen ones stay checked, and toggling works as today.
- **Clear button** (X) when not blank. IME action Done hides the keyboard.
- Don't steal focus or raise the keyboard on entering the step.

## Existing users: a short "what's new" run

- Bump `ONBOARDING_VERSION` to 3.
- Someone whose stored version is 2 (they finished the old walkthrough) sees only `WEEK` (if it applies), `BLOCKS` (if it applies) and `DONE`, with the DONE title "That's what's new."
- `OnboardingScreen` decides from a new `OnboardingInitial.previousVersion: Int`. MainActivity passes `insightsPrefs?.onboardingVersion ?: 0`; find where `OnboardingInitial` is built.
- If none of WEEK and BLOCKS applies for a version-2 user, `finish()` straight away; don't show a lone DONE.
- **Replay setup** (Help) still shows the full walkthrough: when navigated from Help, pass a flag so `previousVersion` is treated as 0. Find the Help replay call (`navController.navigate(ONBOARDING)` after `returnHome()`).

## Constraints

- No new dependencies. Don't change `intro/*`, `guard/YogaGuardService.kt`, `update/*` or the text of `GuardDisclosureDialog`.
- Keep the Skip button, the dots, the back handler and the slide transition.
- **Unit tests** (`app/src/test/java/in/tuwi/yogaphone/ui/OnboardingStepsTest.kt`): pull the step-list logic into a pure function, `onboardingSteps(previousVersion, usageGranted, guardAvailable, youtube, instagram, replay): List<String or the enum>`. Make the enum `internal` if needed. Cover:
  - a new user with and without access;
  - guard unavailable;
  - neither app installed;
  - a version-2 user, with and without anything new;
  - replay.
- Also test the prefill helper: rounding, clamping, fewer than 3 days.

## Done when

- It compiles. Claude runs `./gradlew :app:testDebugUnitTest :app:assembleProfile`.
- Your summary lists the files changed and any assumptions.
