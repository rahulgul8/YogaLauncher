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

## Data safety before the first public build

- [ ] Replace Room `fallbackToDestructiveMigration()` with real migrations (and export the schema) — today every schema change wipes local data, which is fine only before release.

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
