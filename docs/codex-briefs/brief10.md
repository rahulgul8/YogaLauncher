# Brief 10: Yoga Pro (reverse trial, Play Billing, feature gates)

You are working in a git worktree of the Yoga Launcher Android app (Kotlin, Jetpack Compose, package `in.tuwi.yogaphone`), on branch `yoga-pro`. Claude reviews, builds and commits your work. Your sandbox can't run Gradle or fetch packages. The Play Billing dependency is already added: `implementation(libs.play.billing)`, `com.android.billingclient:billing:8.3.0`. That is the **Java** artifact; don't use the `-ktx` one, because it needs Kotlin 2.2 and this project uses Kotlin 2.0.21. Use plain Java APIs from Kotlin, with `suspendCancellableCoroutine` where useful.

Read first:
- `openspec/changes/add-yoga-pro/` (proposal, tasks, spec). This is the approved scope.
- `docs/launch/pricing.md`: the free/Pro split and the honesty rules. The founder approved all of it.
- `docs/design/brand.md` and `app/src/main/java/in/tuwi/yogaphone/ui/theme/YogaComponents.kt`: every screen uses the Yoga kit and YogaIcons. Never hard-code a colour or font; never use Material icons.
- `docs/adr/0009-premium-design-system.md` for the ADR format. Your ADR is **0010**.

## What to build

1. **Entitlement (`in.tuwi.yogaphone.pro` package)**
   - `enum Entitlement { TRIAL, PRO, FREE }` and a `ProState(entitlement, trialDaysLeft, plan: YEARLY|LIFETIME|null)` exposed as a `StateFlow` from a `ProRepository` held in `YogaApplication`.
   - Trial: store `trialStartedAt` (epoch millis) in DataStore on first launch. If it's already set, never reset it. Include it in the existing backup export and import (`privacy/BackupCodec`, `BackupManager`) so reinstall-and-restore doesn't restart the trial.
   - The trial is **14 days**. Compute days left from the local date, so time-zone changes can't extend it by more than a day. Write a pure function and unit-test it.
   - PRO comes from Play purchases: an active subscription `yoga_pro_yearly`, or an owned one-time product `yoga_pro_lifetime`. Cache the last known PRO state in DataStore so the app works offline. Re-query on app start and on resume.
   - Debug builds only: a hidden switch in Advanced settings to force TRIAL, PRO or FREE, for testing. Guard it with `BuildConfig.DEBUG`.

2. **Billing (`pro/BillingClientWrapper.kt`)**
   - `BillingClient.newBuilder(context).setListener(...).enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()).enableAutoServiceReconnection().build()`.
   - Query both products with `queryProductDetailsAsync` (subscription and in-app). The v8 result type is `QueryProductDetailsResult`.
   - Launch purchase flows. For the subscription, use the base plan's offer token.
   - Acknowledge purchases.
   - `queryPurchasesAsync` for restore.
   - Handle a missing Play Store, products not yet configured (show "Yoga Pro isn't available yet" and keep the trial and FREE logic working), user cancel, and pending purchases.
   - No server and no third-party SDK.

3. **Gates**
   - One function, `ProGate.allows(feature: ProFeature, state): Boolean`. TRIAL and PRO allow everything; FREE allows only the free tier.
   - Free forever:
     - home, clock, drawer, search, dock, home apps;
     - light, dark and greyscale themes;
     - today's screen time;
     - Shorts/Reels blocking in Yoga guard;
     - mindful pause for up to **3** apps;
     - **1** app allowance.
   - Pro features (`ProFeature` enum):
     - the Stone and Midnight Sage looks and the Colon icon (FREE keeps Obsidian and Brass and the Ten past ten icon);
     - value of time and the live time-value pill;
     - more than 1 allowance;
     - more than 3 mindful-pause apps;
     - reminders;
     - focus schedules, timebox and Do Not Disturb;
     - insights and weekly reports;
     - history beyond 7 days;
     - quiet notifications;
     - profiles;
     - widgets;
     - learning videos (placeholder entry; the feature comes later).
   - **When FREE:** a Pro entry point shows a small "Pro" tag (a YogaChip-like label) and opens the Yoga Pro screen instead of the feature.
   - **Already-configured Pro features pause; they're never deleted.** For example, the pill stops showing, allowances beyond the first stop being enforced, the selected look falls back to Obsidian and Brass, and the Colon icon alias goes back to Ten past ten. All saved settings stay, and they come back exactly as they were when PRO returns.
   - The guard service and workers must check the gate too: no live pill when FREE, but Shorts blocking still works.
   - **Never gate launching an app.**

4. **UI (Yoga kit)**
   - **Yoga Pro screen** (route `settings/pro`):
     - an eyebrow, a calm title "Yoga Pro", and a short list of what Pro includes;
     - two plan cards with prices **exactly as Play returns them** (`formattedPrice`): yearly pre-selected and marked "Best value", lifetime as "Once, forever";
     - a primary button that buys the selected plan, and a text button "Restore purchases";
     - a small note: "Billed by Google Play. Cancel any time in Play Store → Subscriptions." Show it only for yearly.
     - When PRO: "You have Yoga Pro" and the plan, with no upsell.
     - When products aren't available: plans hidden, a note shown, and the trial info still shown.
   - **Settings:** a "Yoga Pro" row at the top of the main list. Its summary is "Trial · 9 days left", "Yearly", "Lifetime" or "Free · See what Pro adds". It is quiet, not a banner.
   - **Trial-ending card** on home, in the existing status area where `DefaultHomePrompt` lives:
     - It appears from **day 10** of the trial, once only, and can be dismissed. Store the dismissal.
     - Text built from the user's own numbers where available: Shorts blocked (guard daily counts), time back, and the value of time if it's set up. Fall back to a simple "Your Yoga Pro trial ends in N days" when there's nothing to show.
     - Buttons: "Keep Yoga Pro" opens the Pro screen; an X dismisses.
   - **Onboarding:** no paywall. One line on the DONE step: "Everything is unlocked for 14 days."
   - **Honesty:** no countdown timers, no strike-through prices, no fake discounts.

5. **Tests and docs**
   - JUnit tests for the trial clock (day boundaries, time-zone change, clock set back), `ProGate` for every feature in every state, and the restore mapping from purchases to `ProState`.
   - ADR `docs/adr/0010-yoga-pro.md`.
   - Tick the done items in `openspec/changes/add-yoga-pro/tasks.md`. Leave the Play Console product creation item unticked.
   - Add a line to `docs/launch/play-listing.md`: "Contains in-app purchases".

## Constraints

- Make the user's work easy: prefill the yearly plan, use one tap where possible, and no "are you sure?" dialogs.
- Keep `fallbackToDestructiveMigrationOnDowngrade`. If you add a Room table (you shouldn't need one; prefer DataStore), bump the version, add a real `Migration`, and say so.
- Don't touch the guard's detection logic, the drawer or the home performance code (`DrawerController`, `graphicsLayer` use).
- Write plain UTF-8 without a BOM. Avoid mojibake in "→" and "·".
- When finished, list the files you changed and anything you couldn't complete.
