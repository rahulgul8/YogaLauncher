# ADR 0015: A way through with resistance

Date: 2026-11-11. Status: accepted for J1-J4; this worktree implements J1 and J2.

## Context

A hard block invites people to disable the guard entirely when they want Shorts or Reels. An instant bypass is too easy to activate without thinking. The mindful pause and guard need a consistent, calm way to make an intentional choice.

## Decision

Offer a way through by default, free for everyone. Use the shared resistance ladder: growing wait, bounded duration choices, factual local costs, and a hold to continue. Not now remains the primary, immediate action and counts as resisted. Screen readers get a labelled activation after the wait; reduced motion uses plain progress.

Passes cover an app family (native and browser YouTube, or native and browser Instagram), never the other family. The guard continues existing allowance accounting and resumes blocking at expiry. A pass pill takes precedence over the paid value-of-time pill. Persist each family's duration so expiry wording remains accurate when families overlap.

Strict mode is opt-in and hides the way through. Its pending off date is honoured from that local day; J4 supplies controls and tomorrow-effective loosening. Tightening is immediate. J3 strengthens the launcher pause; outside-launcher pauses remain deferred until after Play approval.

## Consequences

The person can keep the guard enabled while choosing an occasional limited pass. More passes require longer waits; saying no does not increase the wait. This adds an activity and a small amount of local state. Gate-owned blocks/time and preference-owned passes/resisted are merged inside the DataStore transaction, so stale gate snapshots cannot erase choices or resurrect yesterday's counters. Counter-only writes do not restart gate accounting.

Seven-day blocks, passes and Not now aggregate rings stay on the phone. No content, URLs or screen structure is stored or transmitted. No new dependencies or permissions are added. Weekly minutes are omitted: the existing seven-day history contains counts, not duration. Android background activity launch, OEM behaviour, accessibility, rotation and large text still require device acceptance checks.
