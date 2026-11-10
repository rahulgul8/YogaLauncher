# Brief 17: Home usage display styles (piece D)

Repo: Yoga Phone launcher (Kotlin, Jetpack Compose). Work only in this worktree (`YogaLauncher-styles`, branch `home-styles`). Don't commit; Claude reviews, builds and commits. You can't run Gradle, so write code that compiles first time: check every import and signature against the existing code.

Read first:
- `docs/plans/feature-discovery.md`, section "Home usage display";
- `ui/HomeUsageIndicator.kt` (today's only style);
- `usage/UsagePrefs.kt`, `usage/UsageRepository.kt` (`UsageSnapshot`) and `ui/theme/YogaComponents.kt` (kit).

## Goal

The founder wants today's usage visible right on the home screen, as a chart, a number or a graph, with the person choosing the style. Onboarding will offer the same choice. Another task is building the new onboarding; **do not edit `ui/OnboardingScreen.kt`**. Instead, expose a picker composable the onboarding will call (see "API for onboarding").

## Styles

Add `enum class HomeDisplayStyle { LINE, NUMBER, RING, WEEK, OFF }` in `usage/UsagePrefs.kt`.
- Store it as `homeStyle` (string key `usage_home_style`) in `UsagePreferences`, default `LINE`.
- **Migration:** if the new key is missing and the old `showHome` is false, read the style as `OFF`.
- Keep the `showHome` field working for any other reader, derived as `homeStyle != OFF`; grep its uses.
- Writing a style also keeps the old key consistent.

| Style | What it shows |
|---|---|
| **LINE** (today's behaviour) | "42 min of 90 today" and a hairline (`YogaProgress`); past the limit, "N min past today's limit" in tertiary |
| **NUMBER** | Today's time in large lining figures (`displaySmall`, `fontFeatureSettings = "lnum, tnum"`), formatted like "1 h 12 m" (under 1 h, "42 min"), with a small "of 1 h 30 m" when there's an allowance |
| **RING** | A thin arc (`Canvas`, 2.dp stroke, about 56.dp) filling against the allowance, with the time in the middle in small lining figures. Track `outlineVariant`, fill `primary`, tertiary past the limit. With no allowance, the full ring is the track and the centre shows the time. |
| **WEEK** | 7 thin bars (Mon…Sun ordering ending today; about 40.dp tall, 6.dp wide, 6.dp gaps). Today's bar is `primary`, the rest `onSurfaceVariant` at 40% alpha. When there's an allowance, a hairline marks it. Under the bars, a small "1 h 12 m today". |
| **OFF** | Nothing |

- **Today's minutes:** with a TOTAL allowance rule, use `snapshot.evaluation.overall.used`. With no allowance, sum today's eligible minutes from `snapshot.today` (`it.eligible`, `foregroundMinutes`), excluding Yoga and home apps the same way the dashboard does (find the dashboard's total for "Today" in `UsageDashboardScreen.kt` and reuse that calculation).
  - **Important:** today the indicator returns early when there's no allowance rule. The new display must show the usage even with no allowance, for every style except OFF.
- **Week:** per-day eligible minutes from `snapshot.week` (rows have `date`). Days with no data show an empty bar.
- **Shared behaviour:**
  - the existing no-access and loading states and the value-of-time line;
  - tapping opens Usage (`onOpenUsage`);
  - the value-of-time line keeps the existing rule (Pro + over + enabled + `valueOnHome`), under any style except OFF.
- **Accessibility:** each style gets a `semantics { contentDescription = "1 h 12 m used today of 1 h 30 m" }` (or "…used today" with no allowance).

## Files

- Rewrite `ui/HomeUsageIndicator.kt` as `HomeUsageDisplay(state, onOpenUsage, onDetails)` dispatching on `state.preferences.homeStyle`. Keep the old function name as a thin wrapper if MainActivity calls it, or update the call in MainActivity (one line).
- Pull the data into a small pure model, `HomeUsageNumbers(todayMinutes, allowanceMinutes: Int?, over, excessMinutes, week: List<Int?>)`, built by a pure function `homeUsageNumbers(snapshot, rules, today: LocalDate)`, so it's unit-testable.
- Each style is a `@Composable` taking `HomeUsageNumbers`, so the picker can preview with sample numbers.

## API for onboarding and settings

```kotlin
/** One row per style with a live preview; [numbers] null means sample numbers, labelled "Sample". */
@Composable fun HomeDisplayPicker(selected: HomeDisplayStyle, numbers: HomeUsageNumbers?, onSelect: (HomeDisplayStyle) -> Unit)
```

- **Layout:** a vertical list of selectable cards, one per style. Each card shows the style's name (Line, Number, Ring, Week, Off) and the preview at a reduced scale. The selected card gets the accent border used by `YogaChip` selected, or the kit's selected-card style if one exists.
- **Off** shows "Nothing on home" as its preview.
- **Sample numbers:** today 72 min, allowance 90, week [45, 80, 62, 95, 70, 40, 72]. Add a small "Sample" eyebrow when the sample is used.
- Put it in `ui/HomeDisplayPicker.kt`, public.

## Settings

- In `ui/UsageSettingsScreen.kt`, replace the "Show usage on home" switch with a "Usage on home" section using `HomeDisplayPicker`, with real numbers when available.
- In `ui/AppearanceSettingsScreen.kt`, add a row "Usage on home", showing the current style name, that navigates to the same picker. Simplest: put the picker inline in a new section of Appearance too. Avoid a new route unless it's trivial; inline is fine.

## Tests

`app/src/test/java/in/tuwi/yogaphone/ui/HomeUsageNumbersTest.kt`:
- with and without an allowance;
- over the limit;
- week ordering and missing days;
- exclusion of Yoga and the home app;
- the migration rule (`showHome` false and no style → OFF), if it's in a pure function.

## Constraints

- No new dependencies. Don't touch `intro/*`, `update/*`, `guard/*`, `ui/OnboardingScreen.kt`, `ui/HomeScreen.kt` (beyond nothing), or `TODO.md`.
- Keep the lining figures and calm, minimal look. No shadows and no extra colours beyond primary, tertiary, onSurfaceVariant and outlineVariant.

## Done when

- It compiles. Claude runs `./gradlew :app:testDebugUnitTest :app:assembleProfile`.
- Your summary lists the files changed and any assumptions.
