# Brief 23: L-B3 (app limits are not enforced) and L-F2 (urgent access), ADR 0016

## L-B3: what we found
A per-app allowance (`UsageRule` scope APP, set in the app sheet in `ui/UsageComponents.kt`) only ever becomes a reminder notification (`usage/UsageNotifications.kt`). Nothing restricts the app once its allowance runs out: `MainActivity.kt` and `focus/FocusViewModel.decide(...)` only look at the overall (TOTAL) rule. Category rules behave the same. Confirm this reading, then fix it:
- Opening an app (drawer, home, dock, search, widgets) whose own or category allowance is OVER shows a "Time's up" screen instead: the app name, "Your N min for today are used", a way home, and the urgent access below as the only way through. Reuse the look of `ui/MindfulDelayScreen.kt`.
- If the user is inside the app when its allowance runs out and the Yoga guard (accessibility service, `guard/YogaGuardService.kt`) is on, take them back to Yoga's home showing that screen. Without the guard, keep the notification, and make it say the app is closed for today.
- Re-check often enough that a 15-minute limit trips within about a minute of running out while the app is open. The guard already sees foreground changes; reuse that rather than adding a polling loop that drains the battery.

## L-F2: urgent access to distracting apps
Keep the existing growing wait for normal opens (`MindfulDelayScreen`, J3 in `TODO.md`, `docs/adr/0015-bypass-with-resistance.md`). Add:
- An **Urgent** control on the pause screen and on the Time's up screen: a **3-second press and hold** with visible progress (copy the existing hold-to-open pattern), plus an accessible alternative for TalkBack.
- It opens the app at once for **5 minutes** of normal use. At most **3 urgent opens a day** (local day). When none are left, say so and show only the normal way.
- Track urgent opens separately, per day and per app. Show them on the Usage screen ("Urgent opens today: 1 of 3"), and include them in backups if `privacy/BackupCodec.kt` covers similar counters.
- After 5 minutes **do not close the app**. If Yoga can control greyscale (`quiet/QuietRepository.kt`: `canWriteSecureSettings()`, the mechanism behind the over-limit greyscale), turn the screen grey and restore colour as soon as the user leaves that app. Never leave the phone grey by mistake: restore on service restart, reboot, and when the window ends while the user is elsewhere. Don't fight the existing over-limit and bedtime greyscale states.
- Where greyscale is not available, post a gentle reminder notification every 5 minutes while the user stays in that app, stopping when they leave.
- Never turn on Do Not Disturb, and never silence or hold calls or notifications.
- Settings → Greyscale shows whether greyscale control is available on this phone, and how to enable it if not.

## Write down the decision
Add `docs/adr/0016-limits-and-urgent-access.md` (copy the shape of 0015) and an OpenSpec change `openspec/changes/add-urgent-access/` following `openspec/AGENTS.md` (proposal, tasks, spec deltas). Keep both short.

## Done when
- Pure logic (the daily urgent quota, window expiry, which rule blocks an app, grey and restore decisions) lives in small testable classes with unit tests in `app/src/test/`.
- The report lists what needs a real phone check (guard sending home, greyscale on and off, reminder cadence).
