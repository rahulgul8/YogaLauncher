# Launch audit checklist

Working list for spec section 13 "Launch" items. Tick as they are verified on real devices.

## Accessibility

- [ ] TalkBack pass on home, drawer, pause screen, timebox/focus sheets, insights charts (charts expose a text summary).
- [ ] Font scale 200%: home list, settings, onboarding still usable (no clipped buttons).
- [ ] Touch targets ≥ 48 dp everywhere (settings rows, chips, status lines).
- [ ] Colour contrast in light, dark and greyscale (warning states also use words, not colour alone).
- [ ] Reduced motion: breathing ring stays static when animations are off.
- [ ] Very small screens (≤ 640 dp tall): home list scrolls; onboarding and sheets scroll.

## Permissions and policy

- [ ] Every optional permission is requested in context, can be denied, and the feature degrades gracefully (Usage Access, notifications, notification access, DND, exact alarms).
- [ ] Privacy and data shows correct status for each permission after granting/revoking.
- [ ] `QUERY_ALL_PACKAGES` declaration submitted (launcher).
- [ ] Notification listener: only active when "Quiet notifications" is on; never holds calls, alarms, ongoing or media.
- [ ] Data safety form matches `play-listing.md`; privacy policy published at a public URL.
- [ ] Value-of-time copy always says "illustrative" and never implies money lost.

## Yoga guard (Piece 8) — not in 1.0; debug and profile builds only

- [ ] Verify prominent disclosure before every accessibility-settings handoff, including entry through Value of time and Privacy and data; Not now must not open system settings.
- [ ] Submit Accessibility API declaration and demonstration video; verify listing and privacy policy match the shipped service. Do not mark as an accessibility tool.
- [ ] Pixel, Samsung One UI and Motorola: enable, revoke, kill/reconnect service, reboot, sideload restricted-settings flow on Android 13+, and TalkBack coexistence.
- [ ] Live pill: no focus theft or outside-touch interception; tap collapse, vertical drag and restored position; animator scale 0; small screens, rotation and large fonts.
- [ ] Verify essential apps, every installed camera, Finance/payment categories, dialer, cellular/VoIP calls, lock screen and screen-off never display the pill.
- [ ] Check both counting modes, no TOTAL rule, crossing the limit without leaving an app, snooze, measuring off, permission revocation, midnight/timezone changes and fresh snapshots at most once per 60 seconds.
- [ ] Re-check YouTube Shorts and Instagram Reels view ids after every supported app update; test regular video screens as negative cases. Record app versions with results.
- [ ] Test browser address bars in Chrome, Samsung Internet and Firefox: valid Shorts/Reels URLs, ordinary videos, spoofed domains and hidden address bars; browser blocking defaults off.
- [ ] Test 5/10/15/30-minute allowances across surface switches, service restarts, midnight and screen-off; verify BACK debounce, two-BACK escalation to HOME and today's counts.
- [ ] Inspect logs and exported backups: no node content, text, URLs or content descriptions retained; bounded tree walks and pre-33 node recycling; no polling when all guard features are off.
- [ ] Run guard JUnit tests and Android build; then perform battery/long-session checks on actual devices. Implementation-time sandbox did not allow Gradle.

## Database migrations

- [x] Schema 8 exported to `app/schemas`; upgrades no longer wipe data (only downgrades do). Every future schema change needs a `Migration` and a migration test.

## Reliability (device matrix)

Devices: Pixel (Android 14/15/16), Samsung One UI, Motorola, a low-RAM Android 8–9 device.

- [ ] Set as default home, press Home from apps, reboot: Yoga returns, timebox/focus survive.
- [ ] Midnight rollover and a timezone change: usage resets on the local day; schedules recompute.
- [ ] Battery optimisation on: reminders arrive within ~15–30 min; timebox end arrives (exact and inexact).
- [ ] DND restored only when Yoga changed it.
- [ ] Device-wide greyscale restores the user's previous colour correction.
- [ ] Package install/uninstall/update reflected in drawer, profiles and categories.
- [ ] 24-hour soak: no crash, no runaway wakeups (check `dumpsys alarm` and battery stats).

## Beta

- [ ] Closed testing track with 10–20 people for 2 weeks.
- [ ] Questions: does the pause help or annoy? Is the money indicator motivating or stressful? Do they keep Yoga as home?
- [ ] Revise defaults (pause length, reminder thresholds) from feedback.
