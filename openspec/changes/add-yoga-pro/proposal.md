## Why

Yoga is a paid, premium product, and people should be able to try it before paying (founder, 2026-11-07). Google Play offers no trial for paid downloads, and a launcher can't lock people out of their own home screen. So Yoga is a free download with a **14-day reverse trial of Yoga Pro**, then either the free tier or Pro. The reasoning is in `docs/launch/pricing.md`.

## What Changes

- **Entitlement:** Trial, Pro (yearly or lifetime) or Free. It is computed on the device from Play Billing purchases and the trial start date.
- **Reverse trial:** every Pro feature is on for 14 days from first launch. No card is needed, and there's no paywall in onboarding.
- **Trial-ending card** from day 10: it shows what Pro did for this user, from their own numbers, offers "Keep Yoga Pro", and can be dismissed.
- **Yoga Pro screen:**
  - plans as Play returns them, with yearly pre-selected and lifetime as the other option;
  - restore purchases;
  - honest terms.
- **Feature gates:**
  - When Pro lapses, Pro features pause. Settings and data are kept and come back on purchase.
  - The launcher (home, drawer, search, dock) is never gated.
- **Settings:** a quiet "Yoga Pro" row showing the current state.
- **Play Console:** a yearly subscription, a lifetime one-time product, and the listing's "In-app purchases" label.

## Impact

- **Affected specs:** yoga-pro (new). Gates touch value of time, the guard pill, allowances, focus, insights, quiet notifications, profiles, widgets, looks and icons, and learning videos.
- **New dependency:** Google Play Billing Library. No server and no third-party billing SDK, so "nothing leaves the phone" stays true apart from Play itself.
- **Decisions waiting on the founder:**
  - reverse trial vs a card-first trial;
  - whether to offer lifetime;
  - the free/Pro split;
  - prices.

Status: proposal only. Build after the founder confirms.
