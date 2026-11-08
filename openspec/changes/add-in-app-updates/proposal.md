## Why

The founder wants updates to reach people seamlessly and automatically, as soon as they ship (2026-11-08). Today Yoga relies only on Play's own auto-update, which can take days and never runs for people who turned it off. An app can only prompt for updates if the version they have installed already contains the update code, so this has to ship in the next release (1.3). Plan: `docs/plans/feature-discovery.md`, section 3.

## What Changes

- **Quiet update check** with the Play In-App Updates API (`app-update-ktx`):
  - runs when home resumes, at most once every 24 hours;
  - no network calls of our own; Play does the work.
- **Normal updates** (priority 0–3):
  - a quiet line on home, "Update ready · Update", starts a flexible download;
  - when the download finishes, Yoga installs it as soon as it is no longer in front (screen off or another app open), so the person never sees a restart;
  - if Yoga is still in front 24 hours after the download, the home line becomes "Restart to update".
- **Priority 4:** the flexible Play dialog on the next home resume.
- **Priority 5:** an immediate update, used only for critical fixes.
- **Settings:** Help → "Check for updates", which shows the current version and checks now.
- **What's new:** after a version change, one home line "New in 1.3 ·" opens a one-screen sheet. Its text comes from a bundled resource built from `docs/play/release-notes.md`. Dismissed for good with one tap.
- **Release tooling:** `scripts/play-upload.mjs --priority N` sets `inAppUpdatePriority` on the release (default 2).

## Impact

- **Affected specs:** app-updates (new).
- **New dependency:** `com.google.android.play:app-update-ktx` (Play Core). It talks only to the Play Store app on the phone.
- **Privacy:** no change. Nothing leaves the phone except through Play itself.
- **Code:**
  - a new `update/` package;
  - a home prompt line next to `DefaultHomePrompt`;
  - a Help row;
  - `scripts/play-upload.mjs`.
- **ADR:** 0013.
