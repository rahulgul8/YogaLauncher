# Play Store listing and declarations (draft)

> Draft for review. Recommended title: **Yoga Phone: Mindful Launcher** (28 of 30 characters); the home-screen label stays **Yoga Phone**. Pricing: see `pricing.md`.

## Title (≤ 30 characters) — options

1. Yoga Launcher: Mindful Home
2. Yoga Phone: Minimalist Launcher
3. Yoga – Calm Minimal Launcher

## Short description (≤ 80 characters)

> A calm, minimal home screen that helps you use your phone on purpose.

Contains in-app purchases

## Full description

A calmer phone, without giving up a useful one.

Yoga replaces a busy home screen with a quiet, text-first launcher and gives you gentle tools to use your phone intentionally — awareness first, friction only where you ask for it.

**A calm home screen**
• Your essential apps as clean text, with a large clock and date
• Fast search; swipe up for all apps
• Profiles for work, reading or bedtime, each with its own home
• Light, dark and greyscale themes

**Know where your time goes**
• A daily allowance for distracting apps, with a quiet progress line on home
• Neutral reminders when you're close to or past your limit
• A usage dashboard and weekly insights — late nights, longest breaks, unlocks
• Optional "value of time": see the illustrative value of time past your limit
• Optional live time-value pill with Burning and Gentle styles; illustrative, not money lost

**Use your phone on purpose**
• A short, mindful pause before distracting apps
• Timebox: set a bounded session and get a gentle note when it ends
• Focus sessions and schedules that tuck distracting apps away
• Greyscale on your terms — over the limit, during focus or at bedtime
• Quiet notifications: hold chosen apps' notifications and read them in a digest
• Yoga guard: turn off YouTube Shorts and Instagram Reels, or give each a daily allowance

**Private by design**
Your usage data stays on your phone. No Yoga account, no server, no ads, no tracking. Optional Yoga Pro purchases are processed by Google Play. You choose each permission when you turn a feature on, and you can delete everything at any time.

Yoga measures app foreground time from Android's usage data; it can differ from other screen-time tools. Apps opened from notifications or other apps skip the mindful pause.

Yoga guard optionally uses Android's Accessibility API to identify the open app for the live time-value indicator and recognise Shorts/Reels from screen structure in YouTube, Instagram and supported browsers you enable. It goes Back (or Home if needed) when a selected surface is blocked. Screen content is never stored or sent. You choose whether to enable it after an in-app disclosure and can turn it off at any time.

## Category and tags

Category: Personalization (alternative: Productivity). Tags: launcher, minimalist, digital wellbeing, focus, screen time.

## Permission declarations (Play Console)

| Permission | Declaration / justification |
| --- | --- |
| `QUERY_ALL_PACKAGES` | Core functionality: Yoga is a home-screen launcher and must list and launch every installed app. Eligible use case: launcher. |
| `PACKAGE_USAGE_STATS` | Special app access granted by the user in Settings; used only on device for allowances, reminders and insights. |
| Notification listener (`BIND_NOTIFICATION_LISTENER_SERVICE`) | Optional "Quiet notifications": holds notifications from apps the user selects and shows them later. Only title/text kept, on device, ≤ 24 h. |
| `ACCESS_NOTIFICATION_POLICY` | Optional: turns on Do Not Disturb (priority only) during focus and restores it after. |
| `SCHEDULE_EXACT_ALARM` | Optional, user-granted: timeboxes end on the minute. The app works without it (inexact alarm). Not `USE_EXACT_ALARM`. |
| `WRITE_SECURE_SETTINGS` | Protected; cannot be granted from the store. Only effective if the user grants it over adb for device-wide greyscale. Declared so the advanced path works; no special Play declaration expected, but mention it in the review notes. |
| `REQUEST_DELETE_PACKAGES` | Uninstall action from the app drawer (system confirms). |
| `EXPAND_STATUS_BAR` | Swipe down on home opens the notification shade. |
| `RECEIVE_BOOT_COMPLETED` | Restore timebox and schedule alarms after restart. |
| `POST_NOTIFICATIONS` | Reminders, timebox endings, digests and weekly summary; requested in context. |

**Not used:** Device admin, `SYSTEM_ALERT_WINDOW` (draw-over-apps permission), location, contacts. Yoga guard uses `TYPE_ACCESSIBILITY_OVERLAY` instead.

### Accessibility API declaration — Piece 8

Service: `in.tuwi.yogaphone.guard.YogaGuardService`, protected by `BIND_ACCESSIBILITY_SERVICE`, with `isAccessibilityTool=false`.

**Core-functionality justification:** Yoga is a launcher and digital-wellbeing app. The optional guard implements user-selected limits on short-form feeds while leaving regular video use available, and provides live feedback on the illustrative value of eligible app time. Usage Access alone cannot recognise a Shorts/Reels screen or navigate away from that screen. Accessibility window events identify the open app; bounded node inspection runs only for enabled surfaces in YouTube, Instagram, Chrome, Samsung Internet and Firefox. Deterministic user-configured rules issue Back and, after two unsuccessful attempts, Home. The service does not change settings, prevent disabling/uninstalling, or collect screen content.

**Disclosure and consent:** A dedicated in-app dialog explains the open-app and screen-structure access, both purposes, on-device processing, non-retention/non-transmission of screen content and how to disable the service. Only **Agree and open settings** opens Android accessibility settings; **Not now** cancels. Feature toggles default off. Daily block counts and allowance usage are local aggregates, separate from transient screen content.

Submit the permission declaration and a demonstration video showing disclosure, consent, Android activation, both guard features and disabling before release. Store approval remains a release task. Review against [Google Play's AccessibilityService API policy](https://support.google.com/googleplay/android-developer/answer/10964491?hl=en) and [prominent disclosure guidance](https://support.google.com/googleplay/android-developer/answer/11150561?hl=en).

## Data safety form (answers)

- Yoga usage measurements: processed only on device and not transmitted by Yoga. Google Play processes optional Pro purchases; verify the final purchase-data declarations against the shipped Billing integration before submission.
- Data shared: **None**.
- Security: data is not transferred off the device; the user can request deletion in the app (Privacy and data → Reset everything) and by uninstalling.

## Screenshots to capture (phone, light and dark)

1. Home with clock, usage line and favourites
2. App drawer with search and recent-search chips
3. Mindful pause screen
4. Timebox running on home + notification
5. Focus on (distracting apps tucked away)
6. Usage dashboard
7. Insights (week bars + heatmap)
8. Privacy and data screen
