# ADR 0012: Feature introductions

Date: 8 November 2026

## Decision

Yoga introduces features at the moment they would help, based on the person's own numbers. Plan: `docs/plans/feature-discovery.md`, section 1. Spec: `openspec/changes/add-feature-introductions`.

**Engine.** `intro/IntroEngine` is pure Kotlin and fully unit-tested. Given signals (usage per app for 7 days, guard surfaces and 7-day block counts, allowance and days over it, grey past limit, installed apps, Pro gates) and the stored state, it returns at most one introduction per surface. The rules:
- Evidence first: there's no introduction without a number.
- Never for a feature that's already on or an app that isn't installed.
- None in the 24 hours after onboarding.
- At most one introduction not shown before per local day.
- Home shows one introduction a day, and the same one for the rest of that day.
- The first "Not now" snoozes for 7 days; the second retires it to Help → Suggestions.
- Higher estimated weekly impact wins; ties go to the declared order.

**State.** `IntroStore` keeps records in the shared DataStore:
- for each introduction: shown count, first shown, snoozed until, dismissals, retired, acted;
- the date of the last new introduction;
- the home day and introduction;
- the onboarding-finished time;
- a pending guard action.

Selecting an introduction and recording the impression happen in one DataStore transaction, so the three surfaces never double-count.

**Signals.** `IntroSignalsReader` reads Room and DataStore on IO and exposes a `StateFlow`; it never reads during home recomposition. Guard block counts are now kept for 7 days as daily aggregates per surface (`guard_blocks_7d`). That's the same kind of data the disclosure already covers ("block counts"), and it's cleared by delete history.

**Surfaces.**
- A card at the top of Usage.
- A quiet line on home, used only by `homeWorthy` introductions: Shorts, Reels, daily allowance.
- On the guard's block card, a tappable sibling line ("Also turn off Reels?"). The guard is already on there, so it enables the surface in place, with Undo.

**Actions.** Every action applies prefilled values and confirms with a self-dismissing Undo notice. Shorts and Reels go through the existing disclosure (`GuardDisclosureDialog`, text unchanged from the Google-reviewed copy) and Android's Accessibility settings. On return:
- if Yoga guard is on, the surface is enabled and confirmed;
- if not, one "Yoga guard isn't on yet · Try again" notice is shown and the pending action is cleared.

## Consequences

- New introductions (pieces H, F) add an `IntroId`, an eligibility rule with tests, and an action. The surfaces and rules come for free.
- The privacy policy now says block counts are kept for 7 days.
- Home, the block card and the Android-settings round trip still need checking on a phone.
