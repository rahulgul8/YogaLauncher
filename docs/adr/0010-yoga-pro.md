# ADR 0010: Yoga Pro and the reverse trial

Date: 7 November 2026

## Decision

Yoga is a free download with a 14-day, on-device reverse trial. No payment method or paywall is required during onboarding. When the trial ends, the launcher stays usable and Pro features pause. Google Play sells a yearly subscription (`yoga_pro_yearly`) and a lifetime one-time product (`yoga_pro_lifetime`). The founder approved the scope in `openspec/changes/add-yoga-pro` and the split and honesty rules in `docs/launch/pricing.md`.

The application owns a `ProRepository` exposing entitlement as a `StateFlow`. DataStore holds the original trial start, clock progress, prompt dismissal and last known purchased plan. Trial time uses local calendar days with persisted progress so moving the clock backwards cannot recover previously consumed days. Manual backups retain trial history; restoring a backup must not move the trial start forwards. Purchase entitlement and debug overrides must not be granted by importing a backup.

Play Billing 8.3 uses its Java API from Kotlin, without the KTX artifact or a third-party SDK. Product details supply the displayed prices and subscription base-plan offer token. Only completed purchases grant Pro; pending purchases do not. Purchases are acknowledged and refreshed on start, resume and explicit restore. A successful query of both product types can revoke cached entitlement; an unavailable store or failed query preserves the last known purchased plan for offline use.

## Gates and saved settings

`ProGate.allows` defines the free/Pro boundary for both UI and background work. Trial and Pro unlock every feature. Home, the clock, drawer, search, dock, app launching, ordinary themes, today's screen time, Shorts/Reels blocking, three mindful-pause apps and one app allowance remain free.

Locked entry points carry a quiet Pro label and open `settings/pro`. Effective runtime settings suppress paid functionality without deleting saved configuration. A saved paid look falls back to Obsidian and Brass; the Colon icon falls back to Ten past ten. Purchased access restores the saved choices. Background services, workers and alarms obey the same gates as screens.

No Room table or schema migration is needed. The existing downgrade migration policy is retained.

## Presentation and honesty

Screens use the Yoga component kit, theme tokens and YogaIcons. Yearly is selected initially and labelled “Best value”; lifetime is “Once, forever”. Prices are exactly Google's `formattedPrice` values. The subscription cancellation note appears only for yearly. Owners see their plan without an upsell. Missing products show “Yoga Pro isn't available yet” while the trial and free launcher continue working.

From trial day 10, one dismissible home card can explain the user's results. Its dismissal is persisted. Guard block counts are measured; blocked time is not time saved, so counts must never be converted into invented savings. With no usable personal results, the card states the remaining trial days. There are no countdown timers, crossed-out prices or fabricated discounts.

## Consequences

There is no backend purchase validation or server clock. Google Play is the purchase authority; local cache deliberately favours uninterrupted offline access and can remain stale until a successful refresh. Clearing Android app data or reinstalling without a backup cannot be distinguished from a new installation. Restoring trial metadata prevents a restored installation from starting another full trial.

Debug builds offer an Advanced settings override for trial, Pro and free states. Release builds do not honour it. Play Console product setup, license-tester purchases, pending and cancelled transactions, offline restore, and lifecycle transitions still require device validation before release. Google Play handles payment information; Yoga does not receive card details or send usage measurements to a billing server.
