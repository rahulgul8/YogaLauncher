## Why

Most of Yoga's features sit behind Settings, so people never find them. The founder wants each feature introduced at the moment it would help, backed by the person's own numbers (2026-11-08). For example, heavy YouTube use leads to an offer to turn off Shorts. Plan: `docs/plans/feature-discovery.md`, section 1.

## What Changes

- **IntroEngine:** a pure, unit-tested function. It picks at most one introduction per surface from the person's signals and the introduction state. Rules:
  - evidence first: no number, no introduction;
  - one at a time per surface, and at most one *new* introduction a day;
  - none for 24 hours after onboarding;
  - never for a feature that is already on or an app that isn't installed;
  - "Not now" snoozes it for 7 days; a second "Not now" retires it;
  - higher estimated impact (minutes a week) wins.
- **Surfaces:**
  - a card at the top of the Usage screen;
  - one quiet home line, only for introductions marked `homeWorthy`;
  - a sibling line on the guard's block card ("Also turn off Reels?").
- **Suggestions:** Help → Suggestions lists retired and snoozed introductions, so people can still turn them on.
- **First introductions:**
  - I1 Shorts block, I2 Reels block, I3 browser surfaces;
  - I4 daily allowance, I5 lower allowance;
  - I8 grey past limit.
- **One-tap enable:**
  - each action turns the feature on with values already filled in from the person's data;
  - a confirmation offers Undo;
  - I1 and I2 go through the existing guard disclosure and Android's Accessibility settings, then confirm on return ("Shorts are off. Regular videos still work.").

## Impact

- **Affected specs:** feature-introductions (new).
- **Code:**
  - a new `intro/` package;
  - `ui/IntroCard.kt` in the kit;
  - Usage screen, home, the guard block card, Help;
  - the guard disclosure is extracted for reuse.
- **No new permissions and no new dependencies.** Everything is computed on the phone from data Yoga already has.
- **ADR:** 0012.
