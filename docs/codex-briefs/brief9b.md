You are working in the repo at C:\me\code\mobile\YogaLauncher, an Android launcher (Kotlin, Jetpack Compose, Material 3, minSdk 26, package `in.tuwi.yogaphone`).

ADR / piece number: **9, part 2**. Part 1 is committed (`e56d893`).

## Context
Read `docs/codex-briefs/brief9.md` first: it is the full brief for piece 9. Its Rules section (kit only, `YogaIcons` only, 24 dp margins, 64 dp rows, accent for the one "on" thing, ember only for time value and over-limit, display face for titles and hero numbers, `lnum, tnum` on numbers, uppercase `labelSmall` section labels, sentence-case copy, keep every safety and privacy statement) applies unchanged.

Part 1 already did:
- Added `YogaSlider`, `YogaSheet`, `YogaDialog` and `YogaProgress` to `ui/theme/YogaComponents.kt`. Use them; don't redefine them.
- Restyled the usage dashboard, `UsageComponents.kt`, `AppActionMenus.kt`, `HiddenAppsScreen.kt`, `UsageAccessCard.kt` and `HomeUsageIndicator.kt`, and fixed the launcher-label bug.
- Look at `UsageDashboardScreen.kt` and `UsageComponents.kt` for how the new kit pieces are used.

## Your job: restyle these files, keeping behaviour identical
- `ui/FocusSettingsScreen.kt`
- `ui/FocusComponents.kt`: the timebox and focus start/control sheets and the status lines. `HomeQuickActions` is already done.
- `ui/GuardSettingsScreen.kt`, including the disclosure dialog
- `ui/InsightsScreen.kt`
- `ui/MindfulDelayScreen.kt`: the pause before a distracting app. Make it calm and beautiful, built around the brand clock style. It is seen often.
- `ui/OrganizeScreens.kt`: profiles and categories
- `ui/PrivacyScreens.kt`: privacy and help
- `ui/QuietScreens.kt`: visual interventions, quiet notifications and the digest
- `ui/UsageSettingsScreen.kt`
- `ui/ValueOfTimeSettingsScreen.kt`
- `ui/WidgetsUi.kt`

When you're done, none of these files may contain `ListItem`, `FilterChip`, `AssistChip`, `OutlinedTextField`, `Checkbox`, `RadioButton`, a raw `Switch`, `Slider`, `ModalBottomSheet`, `AlertDialog`, `LinearProgressIndicator`, `Button` or `TextButton`. Use the kit equivalents instead.

## Kotlin gotcha (part 1 hit it)
`YogaChip(label, selected, onClick, modifier, enabled)` has `enabled: Boolean` as its last parameter, so a trailing lambda doesn't compile. Always pass `onClick = { ... }` by name. Check every other kit call whose last parameter isn't a lambda for the same thing.

## Recent entries
Brief rule 11: timebox labels, profile names and category names. If a field people fill again and again has no recent-entry chips, add them with the project's existing recent-searches pattern, stored on the device, newest first, de-duplicated, about 3 entries. List what you added.

## Docs
Tick the finished screens in the "Screens done" checklist in `docs/design/brand.md`.

## Constraints
- No new dependencies. No Room schema changes. No behaviour changes.
- Keep public composable signatures that `MainActivity.kt` calls, or update the call sites there.
- UTF-8 without a BOM.
- Your sandbox can't run Gradle or write the git index. Write code that compiles, and list anything you're unsure of.
- If you run short of time, finish whole files rather than leaving several half done, and say which ones are left.

Finish with a short summary: files changed, icons added, recent-entry chips added, anything skipped or uncertain.
