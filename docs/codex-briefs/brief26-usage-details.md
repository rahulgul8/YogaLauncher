# Brief 26: L-F4, usage details view

When the user taps the usage progress bar on home (or another usage indicator), show a detailed view of their usage, including completed goals and how long they took. Android Digital Wellbeing is the reference for presentation, refined to Yoga's look.

## Today
Tapping the home indicator (`ui/HomeUsageIndicator.kt`, `HomeUsageDisplay`) navigates to `"usage"`, the Usage screen in `ui/UsageDashboardScreen.kt`, which has a Today / Last 7 days view, totals and an app list. Read it and `ui/UsageComponents.kt` first.

## Build (refine the existing Usage screen; don't add a second one)
1. **Header:** the day's total for the chosen line ("on distracting apps" and "in all apps", with the same meaning as on home), and the limit if one is set.
2. **Chart:** Today shows an hourly bar chart (`UsageDao.hours(...)`, `UsageHourEntity`); Last 7 days shows one bar per day with the allowance as a hairline. Tapping a bar selects that hour or day, and the list below follows the selection. Draw with Compose `Canvas` in the kit colours; no chart library.
3. **Day picker:** step back through the last 7 days with arrows, like Digital Wellbeing's date switcher.
4. **Apps:** each app with icon, minutes, a thin proportional bar, and its type (Essential / Distracting); tapping opens the existing per-app sheet.
5. **Goals done:** a section listing that day's completed timeboxes and focus sessions with their title, time taken and planned time. Use `insights/DayAchievements.kt` (`DayAchievementsBuilder.build(...)`, `GoalDone`) which already computes this; don't duplicate the logic.
6. **Also show** that day's unlocks (`UsageDayEntity.unlocks`) and, when the guard is on, blocks; keep existing items (time value, learning videos) where they are.
7. Every usage indicator on home (all `HomeDisplayStyle`s: Line, Number, Ring, Week) opens this screen on today; Week opens it on Last 7 days.

## Done when
- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest` pass.
- Pure pieces (bar scaling, selected-range filtering, day stepping limits) have unit tests.
- Check all three looks (Obsidian, Stone, Midnight Sage) in light and dark by reading the theme code; list anything you couldn't check.
