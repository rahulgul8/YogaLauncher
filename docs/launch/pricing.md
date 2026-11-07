# Pricing: free, paid, or trial

Decision paper, 2026-11-07. Status: **recommendation, waiting for the founder's decision.**

Goal (founder): Yoga is a paid, premium product, and people should be able to try it free first.

## What Google Play allows

| Model | Free trial possible? | Notes |
| --- | --- | --- |
| Paid download (pay before install) | **No** | Play has no trial for paid apps; the only safety net is Play's short refund window. Paid downloads convert poorly, especially in India, and the listing can't show the app working first. |
| Free download + subscription | **Yes, Play-managed** | Trial is a subscription offer (e.g. 7 or 14 days). Needs a payment method or UPI Autopay mandate up front; converts automatically; one trial per Google account. |
| Free download + one-time "lifetime" unlock | Only an in-app trial | Play has no trial for one-time products, so Yoga would track its own trial days. |
| Reverse trial | Yes, in-app | Everyone gets everything free for N days, with no card. Then the app drops to a free tier unless the user buys. |

## The constraint that decides it: Yoga is the home screen

When a paid feature expires, the user still has to be able to use their phone. If an unpaid launcher locked up, the user would be stranded on a blank home screen, uninstall Yoga and leave 1-star reviews. So **after any trial ends, the launcher must keep working**: the clock, app drawer, search and dock. A launcher therefore can't be "paid or nothing". What the user pays for is the mindful layer on top.

## Recommendation

**Free download, a 14-day reverse trial of Yoga Pro, then a free tier or Pro.**

1. **Install:** every Pro feature is on for 14 days. No card is needed, and there's no paywall during onboarding.
2. **Days 10–14:** a calm card on home and in Settings shows what the user got from Pro, from their own numbers. For example: "Shorts blocked 41 times. 3 h 20 m back. ₹1,240 of your time." Then "Keep Yoga Pro".
3. **Day 15 without buying:** Pro features pause, nothing is deleted, and the launcher keeps working. Buying later restores everything as it was.
4. **Purchase options (Play Billing, no server):**
   - **Yearly:** the default, pre-selected option.
   - **Lifetime:** a one-time purchase. Premium-segment buyers often prefer it, and some of them dislike subscriptions on principle.
   - **Monthly:** optional. Leave it out at launch to keep the choice simple.

Why not a Play-managed subscription trial that needs a card up front: many Indian users don't have a card, and asking for a UPI mandate on day 0 loses most of them before they've seen the value. A reverse trial shows the value first. Yoga's value is personal (its own numbers about the user), so it sells best after two weeks of use.

### Free tier vs Pro (proposal)

| Free forever | Yoga Pro |
| --- | --- |
| Home screen, clock, drawer, search, dock, home apps | All three looks and both app icons (Free gets Obsidian and Brass only) |
| Light, dark and greyscale | Value of time and the live time-value pill |
| Today's screen time | Unlimited app allowances and reminders (Free: one allowance) |
| Shorts and Reels blocking (the hook people recommend to friends) | Learning videos (YouTube study channels) |
| Mindful pause for up to 3 apps | Focus schedules, timebox, Do Not Disturb |
|  | Insights and weekly reports, history beyond 7 days |
|  | Quiet notifications digest, profiles, widgets |

Rule: anything the user set up during the trial keeps its settings when Pro pauses, and comes back exactly as it was after purchase.

### Price points (starting suggestion, adjust after beta)

| | India | US / rest of world (Play price templates) |
| --- | --- | --- |
| Yearly | ₹999 | US$24.99 |
| Lifetime | ₹2,499 | US$59.99 |
| Monthly (optional) | ₹149 | US$3.99 |

Regain charges ₹199 a month and ₹999 a year, but discounts heavily, as low as ₹84 a month and ₹299 a year. Yoga should **not** run constant fake discounts. One honest launch price for the first months is enough. Play charges a 15% service fee on subscriptions, and on one-time purchases up to the first US$1M of earnings a year.

## Honesty rules (part of the premium feel)

- No paywall on first launch. No countdown timers, no "93% off" anchors.
- One clear trial-ending card, which can be dismissed. No nagging after that, beyond a quiet "Yoga Pro" row in Settings.
- Refunds follow Play's policy, and the policy is written once and consistently. Regain's refund policy and Terms contradict each other.
- Prices shown exactly as Play returns them, including local tax.

## What has to be built (when approved)

See `openspec/changes/add-yoga-pro`. In summary:
- Play Billing Library.
- On-device entitlement and trial clock.
- Feature gates.
- Paywall screen and trial-ending card.
- Restore purchases.
- Play Console products.
- Listing copy ("Free to try · In-app purchases").

Open questions for the founder:
1. Should it be a reverse trial (recommended), or a Play subscription trial that needs a card?
2. Should lifetime be offered? (Recommended: yes.)
3. Is the free/Pro split above right?
4. Are the price points right?
