# ADR 0013: In-app updates and what's new

Date: 8 November 2026

## Decision

Yoga uses Google Play's In-App Updates API through `app-update-ktx`. It asks Play for update information when the home
activity resumes, at most once every 24 hours. Yoga makes no update network request of its own. Devices without Play
and sideloaded builds fail quietly on home and direct people to Google Play from Help.

Release priority controls interruption. Priorities 0–3 show a quiet home line and start a flexible download only after
the person taps Update. Priority 4 starts Play's flexible dialog on resume. Priority 5 starts an immediate update and is
reserved for critical fixes. An immediate flow that Play reports as already in progress is resumed.

Flexible downloads are observed with an install-state listener. When a download finishes while Yoga is not in front,
Yoga calls `completeUpdate()` so Play installs it silently. If Yoga remains in front, a Restart line appears after 24
hours. The update line can be hidden for three days for that version. Check time, download time, dismissal, and the
last-run version code are stored in the existing preferences DataStore.

After an installed version increase, bundled JSON can show one "New in" home line and a single Yoga sheet. Fresh
installs store their current version without showing the sheet. Opening or dismissing the sheet marks that version
seen. Release creation sets Play's `inAppUpdatePriority`; the upload script defaults it to 2.

## Consequences

Google Play remains the update authority and owns all download and installation UI. The feature cannot update a
sideloaded installation or a device without Play, and those cases must never affect launcher availability. Priority 5
is intentionally disruptive and requires a release-time decision. Real update flows, background installation, and
Play-unavailable behavior still require validation with internal app sharing on a phone.
