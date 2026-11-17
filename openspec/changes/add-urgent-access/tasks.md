## 1. Logic and storage

- [ ] 1.1 Add pure rule-selection, daily quota, window-expiry and intervention policies with unit tests.
- [ ] 1.2 Persist per-day/per-app urgent counts and transient five-minute windows without a Room change.

## 2. Enforcement and UI

- [ ] 2.1 Gate Yoga launches and show Time's up for over-limit app or category rules.
- [ ] 2.2 Add the three-second Urgent hold and TalkBack action to the pause and Time's up screens.
- [ ] 2.3 Recheck the foreground app in Yoga guard and return an over-limit app to Yoga.
- [ ] 2.4 Add post-window greyscale or five-minute reminders with safe restoration.
- [ ] 2.5 Show today's urgent count in Usage and greyscale availability in Settings.

## 3. Data and verification

- [ ] 3.1 Include daily urgent counters in backup while excluding active windows.
- [ ] 3.2 Run unit tests, compile debug Kotlin, and record real-phone checks.

