## Why

Yoga needs a policy-compliant way to request Play reviews after genuinely useful moments, while giving every person a
direct, private feedback path even when a review request is not appropriate.

## What Changes

- Add a pure review eligibility policy covering install age, home use, launcher role, onboarding, interruptions,
  allowance state, cooldown, lifetime caps, and one-time moments.
- Trigger review moments after an introduction enables Shorts or Reels blocking, after leaving a good seven-day usage
  view, on home after the third completed focus/timebox, and on an evening after a strong block day.
- Persist review counters, pending moments, feature-on times, and the last three feedback answers on-device.
- Add Help → Send feedback with a prefilled email that the person reviews before sending.
- Offer an optional inline "What didn't work?" question when guard, allowance, or mindful pause is disabled within
  seven days of being enabled.

## Impact

- Affected specs: reviews-feedback (new).
- Affected code: application controllers, home resume, introductions, focus completion, guard cards, usage
  notifications/dashboard/settings, mindful pause, Help, and local preferences DataStore.
- Dependency: the already configured `com.google.android.play:review-ktx`; no other dependency is added.
- Privacy: review timing and feedback answers remain on-device; email is opened visibly and is never sent by Yoga.
