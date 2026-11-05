# Play Store listing and declarations (draft)

> Draft for review. The public name is still open (spec section 12: "Yoga Launcher" vs "Yoga Phone"); the app label in `strings.xml` is currently **Yoga Phone**.

## Title (≤ 30 characters) — options

1. Yoga Launcher: Mindful Home
2. Yoga Phone: Minimalist Launcher
3. Yoga – Calm Minimal Launcher

## Short description (≤ 80 characters)

> A calm, minimal home screen that helps you use your phone on purpose.

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

**Use your phone on purpose**
• A short, mindful pause before distracting apps
• Timebox: set a bounded session and get a gentle note when it ends
• Focus sessions and schedules that tuck distracting apps away
• Greyscale on your terms — over the limit, during focus or at bedtime
• Quiet notifications: hold chosen apps' notifications and read them in a digest

**Private by design**
Everything stays on your phone. No account, no server, no ads, no tracking. You choose each permission when you turn a feature on, and you can delete everything at any time.

Yoga measures app foreground time from Android's usage data; it can differ from other screen-time tools. Apps opened from notifications or other apps skip the mindful pause.

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

**Not used:** Accessibility services, device admin, overlays (draw over other apps), location, contacts.

## Data safety form (answers)

- Data collected: **None** (all processing on device; nothing transmitted).
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
