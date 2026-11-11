# Yoga Phone — Privacy Policy

Last updated: 8 November 2026.

Yoga Phone ("Yoga", "the app", package `com.tuwi.yogaphone`) is a home-screen app from TUWI Technologies Pvt. Ltd. that helps you use your phone more intentionally. This policy explains what the app reads, what it stores and where.

## The short version

- Yoga works **on your phone only**. It has no account, no server and no analytics or advertising SDKs. Nothing Yoga measures is sent to us or anyone else.
- Optional features need Android permissions that **you** grant, one by one, when you turn the feature on. You can revoke them at any time.
- You can see what is stored, delete it, or reset everything from **Settings → Privacy and data**.

## What Yoga reads and why

| Data | Why | Permission | Optional? |
| --- | --- | --- | --- |
| The list of apps installed on your phone | To show and launch your apps — the basic job of a home screen | Query all packages | Required for a launcher |
| Which apps were in the foreground, for how long, and when the phone was unlocked | Daily allowances, reminders, the usage dashboard and insights | Usage Access | Yes |
| Notifications from apps you choose to quiet (title and text only) | To hold them during focus and show them later in a digest | Notification access | Yes |
| Do Not Disturb state | To turn on priority-only mode during focus and restore it afterwards | Do Not Disturb access | Yes |
| Android's colour-correction setting | Device-wide greyscale on your chosen triggers | Granted only by you from a computer (adb) | Yes |
| The open app and, for enabled Shorts/Reels surfaces only, screen structure | Optional live time value and Shorts/Reels blocking | Yoga guard accessibility service | Yes |

Apart from the optional notification and Yoga guard uses described here, Yoga does **not** read app content. It does not access your contacts, location, photos, microphone or camera.

## Optional accessibility use: Yoga guard

Yoga guard is off by default. Before opening Android's accessibility settings, Yoga explains its access and asks you to choose **Agree and open settings** or **Not now**. It is a digital-wellbeing feature, not a tool designed specifically for disability support.

When enabled, it identifies which app is open to show your optional live, illustrative time value. If you enable Shorts or Reels controls, it inspects screen structure only in YouTube, Instagram and selected supported browsers (Chrome, Samsung Internet and Firefox), to recognise those surfaces. Browser detection uses the address bar and is off by default. Screen structure, text, URLs and content descriptions are processed transiently on this phone: none of that content is stored, logged or sent anywhere.

The service can go Back, or Home if Back fails twice, when a selected surface is blocked or its daily allowance runs out. Regular videos remain available. The live pill uses an accessibility overlay; Yoga does not request draw-over-apps permission. It hides on the lock screen, when the screen is off, during calls, and in essential, camera and Finance/payment apps. Its value is illustrative, not money spent or lost.

Only guard settings, per-surface block, pass and "not now" counts for the last 7 days and allowance time are retained locally. These preferences are included in a manual backup export. You can switch off either feature in **Settings → Shorts and live time value**, disable Yoga guard in Android accessibility settings at any time, or remove its local data with **Reset everything**.

## What Yoga stores, and for how long

All data is stored in the app's private storage on your phone:

- Your settings (home apps, themes, allowances, schedules, profiles, categories).
- Daily and hourly usage totals, unlock counts, focus and timebox sessions, and pause/reflection outcomes — kept for 90 days by default (choose 30, 90 or 365 days).
- Held notifications — title and text only, deleted after at most 24 hours.
- The optional "value of time" amount you enter. It is used only to show an illustrative figure and is never treated as income or spending.
- Which in-app suggestions you have seen, put off or acted on, so each one appears only when it is useful.
- When Yoga last checked for an update and which "What's new" notes you have seen.

Android's automatic cloud backup is **turned off** for Yoga, so this data is not copied to Google Drive. If you choose **Export** in Privacy and data, Yoga writes a backup file to a location you pick; what happens to that file is up to you.

## Yoga Pro purchases

Optional monthly, yearly and lifetime purchases are processed by Google Play. Google handles payment details; Yoga does not receive your card or bank details. Yoga queries Play purchase status and acknowledges completed purchases through Play Billing. The app stores the last known purchased plan locally so Pro works offline. No purchase-validation server or third-party billing SDK is used, and Yoga does not send usage measurements to Google for billing.

The original trial start, trial progress and dismissal of the trial-ending card are stored locally and retained in manual backups. A backup does not grant purchased access: purchases are restored from Google Play. Resetting launcher settings and history keeps the original trial record, prompt dismissal and cached purchase status; it does not begin another trial. Uninstalling or clearing Android app storage removes these local records.

## App updates

At most once a day, Yoga asks Google Play whether a newer version is available, using Play's in-app updates service. Google Play downloads and installs the update. Yoga sends no usage data with this check.

## Your choices

- Turn usage measuring and insights off entirely (Privacy and data).
- Revoke any permission in Android settings; the related feature stops and Yoga keeps working.
- Delete usage history, or reset everything, from Privacy and data. Uninstalling Yoga removes all of its data.

## This website

yogaphone.tuwi.in has no cookies, analytics, advertising or third-party requests. If you choose a look or brightness on the site, your browser remembers it in its own local storage; it never reaches us. The site is hosted on Firebase Hosting (Google), which processes standard request data such as IP addresses to deliver the pages.

## Children

Yoga is designed for adults and is not directed at children under 13.

## Changes

We will update this page when the app's data practices change and note the date above.

## Contact

tuwi.developer@gmail.com · TUWI Technologies Pvt. Ltd., India
