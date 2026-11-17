# Brief 22: L-B2, usage totals are wrong (root cause and fix)

The founder says total phone usage and distracting-app usage are still inaccurate, after an earlier fix (534eef2, which only relabelled the lines). Find every real cause and fix it. This is time maths: be rigorous.

## Where the numbers come from
- `usage/AndroidUsageReader.kt` turns `UsageStatsManager.queryEvents` into `ForegroundEvent`s (it queries from 24 h before the day start) and runs `UsageCalculator`.
- `usage/UsageCalculator.kt` builds sessions per package from RESUMED / PAUSED / STOPPED / SCREEN_OFF / SHUTDOWN.
- `usage/UsageRepository.kt` `collect()` rounds each app to whole minutes (`(millis + 30_000) / 60_000`), stores `UsageAggregateEntity` rows, and the screens sum those; it also subtracts learning-video seconds for YouTube and excludes launcher packages.
- Shown by `ui/HomeUsageIndicator.kt`, `ui/UsageComponents.kt`, `ui/UsageDashboardScreen.kt`, onboarding, notifications and insights. The guard keeps its own counts in `guard/` (e.g. `GuardDaily`); check whether any screen mixes the two.

## Suspects to confirm or rule out (not a complete list)
1. Rounding each app to minutes before summing: 40 apps of 20 s each show 0; many apps just over 30 s each inflate. Totals should come from milliseconds and be rounded once.
2. Sessions that are never closed, or closed late: missing PAUSED, activity transitions inside one app, multi-window, PiP, the notification shade, keyguard over a resumed app, screen on without a new RESUMED. Compare with how Digital Wellbeing counts (ACTIVITY_RESUMED to ACTIVITY_PAUSED/STOPPED, capped by screen-off).
3. Sessions crossing midnight or hour boundaries, DST, and the 24 h lookback.
4. Which apps "total phone usage" and "distracting" include: launchers (Yoga and others), Yoga's own pause and block screens, system UI, essential apps, `AllowanceCounts`. Make sure the "in all apps" and "on distracting apps" lines each mean exactly what their label says, everywhere they appear.
5. Stale rows: `collect()` clears and rewrites a day; check yesterday's archive and the week query against the time zone.

## Done when
- Each confirmed cause is fixed at its root, with a unit test in `app/src/test/` that fails before the fix (feed `UsageCalculator` synthetic events; extract any pure logic you need into testable functions).
- Today's and last-7-days totals would match Android Digital Wellbeing within about a minute per app, and the total within a few minutes, on the same phone.
- Your report lists every cause you found, which you fixed, and which you ruled out and why. Add a short "How Yoga counts time" section under L-B2 in `docs/plans/bugs-and-features-2026-11.md`.

Avoid a Room schema change. If a stored minute column truly must become milliseconds, say so in the report and keep minutes for now.
