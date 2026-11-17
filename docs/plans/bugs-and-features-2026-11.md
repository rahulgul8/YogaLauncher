# Bugs and feature changes, 17 Nov 2026

Captured from the founder's dictated discussion (17 Nov 2026). No product requirements beyond these are implied. The tracker is [TODO.md](../../TODO.md), section L.

Each item keeps the founder's wording of the requirement. "Related" points to existing code or past work, as a starting point for whoever picks it up.

## Bugs

### L-B1. Launcher crashes; default launcher not retained

The launcher crashes frequently. After a crash, pressing Home sometimes shows Android's launcher selection popup again, which suggests the default launcher choice is not kept.

- Find the crashes first (logcat, Play Console crash reports for 1.5 and 1.6). The popup is most likely a consequence: Android drops or questions the default home after the home app crashes repeatedly.
- Done when: no crash on the founder's phone over normal use, and Home always goes to Yoga after a restart of the app.

### L-B2. Usage statistics are wrong

Total phone usage and distracting-app usage are still inaccurate, despite the earlier fix. Analyse the calculations thoroughly and fix them.

- Related: 534eef2 (Usage labels and all-apps total), the Usage screen, home display styles.
- Done when: today's and last-7-days totals match Android Digital Wellbeing on the same phone within a small, explained margin, with tests for the calculation (overlapping sessions, sessions crossing midnight, the launcher itself, screen off).

#### How Yoga counts time

Yoga reconstructs app foreground sessions from Android's activity events, split at local midnight and local clock hours. It excludes Yoga (including its pause/block UI), home apps, Android/System UI and lock-screen time. Simultaneously resumed multi-window or picture-in-picture apps can overlap, matching Android's per-app foreground semantics. The all-app number includes essential apps and learning videos; the allowance number includes distracting apps by default, or every non-essential app when that setting is selected, and subtracts recognised learning-video time. Android can delay or omit OEM usage events, and Yoga stores whole minutes without a Room migration, so a comparison with Digital Wellbeing can still differ by roughly a minute plus any event-source difference. Repeated/skipped DST hours are merged/skipped under their local hour while the daily boundary remains local midnight.

### L-B3. App usage limit may not be enforced (needs testing)

When an app has a usage limit (for example 15 minutes), it may stay usable after the limit runs out instead of being closed or restricted.

- First verify whether the bug exists on a real phone. Fix it only if confirmed.
- Done when: the result of the check is recorded here, and, if the bug was real, the app is restricted when its limit runs out.

## Features and enhancements

### L-F1. Simpler Settings

Settings are cluttered and take too many menus and taps. Reorganise them so options are easy to find and understand.

- Example from the founder: moving Chrome from distracting (with an opening delay) to essential is hard to find today.
- Done when: common changes, such as an app's category, are reachable in few taps from an obvious place.

### L-F2. Urgent access to distracting apps

- Keep the existing growing wait for normal attempts to open a distracting app.
- Add an urgent bypass, started by a **3-second press and hold**.
- An urgent bypass gives **5 minutes** of normal use.
- At most **3 urgent bypasses a day**. Track them separately in the statistics.
- After 5 minutes, **do not force-close the app**.
- Where the phone lets the launcher control greyscale, turn the screen grey after 5 minutes, and restore colour when the user leaves the distracting app.
- Do **not** turn on Do Not Disturb, and do not interfere with calls or notifications.
- Where greyscale can't be controlled, show periodic reminder notifications that encourage the user to leave the app.
- Check whether the launcher can control greyscale on the device before relying on it.

Related: J3 pause screen (d1c7c79: growing wait, hold to open), a3bf52e (greyscale workflow), ADR 0015 (bypass with resistance).

### L-F3. Daily progress note on home

- Show yesterday's achievements as a **separate note-style message** on home, presented like the existing app-update message.
- Show it **the next morning**, when the user starts using the phone, so it's the first thing they see.
- Tapping the note shows yesterday's achievements.
- Do **not** replace or add to the existing usage progress bar.
- Leave the app-update message unchanged.

Related: piece A (update and What's new home line, 96b8b0b).

### L-F4. Usage details view

When the user taps the usage progress bar or another usage indicator, open a detailed view of usage, including completed goals and how long they took. Use Android Digital Wellbeing as the reference for presentation, refined to the Yoga look.

Related: home display styles (e9d2a99), the Usage screen.
