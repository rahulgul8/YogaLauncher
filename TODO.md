# Yoga Phone: TODO

This is the single tracker. The plan with the details is in [docs/plans/feature-discovery.md](docs/plans/feature-discovery.md).

- Tick an item when it's merged and checked on the phone.
- Add the commit hash when you tick it.
- Status marks: `[ ]` to do, `[~]` in progress, `[x]` done.
- **Owner** is either the founder or Claude/Codex.

## Decisions needed (founder)

- [x] **D1. Onboarding image style.** Decided 8 Nov 2026: monochrome etched line art, transparent, tinted per look (the recommended option).
- [ ] **D2. Weekly review for free users.** Recommended: a free in-app weekly view; the Sunday notification stays Pro. *Unblocks piece G.*
- [ ] **D3. Guard measure mode (Shorts count).** Recommended: build it after the first Play approval. *Unblocks piece I.*
- [x] **D4–D7. A way through with resistance** Decided 11 Nov 2026: the recommended answers. (docs/plans/bypass-with-resistance.md). Recommended: way through by default (D4), free (D5), strict changes apply tomorrow (D6), pause for apps opened outside Yoga after Play approval (D7). *D4 unblocks piece J.*
- [ ] **Name check:** Lenovo's YOGA trademark, before public launch.
- [ ] **Roadmap order** in docs/research/feature-roadmap.md.
- [x] **Learning videos:** merged in d831010 (12 Nov), shipped in 1.6 (8). Phone check of learning itself still open.

## Release and Play

- [~] **First Play review of 1.2 (3):** open testing in 178 countries, store listing, and all 12 declarations, Accessibility included. Sent 8 Nov 2026. *Waiting on Google.*
- [ ] **Trader status declaration** (founder, Play Console). Due 1 Feb 2027.
- [x] **When 1.3 ships:** redeployed yogaphone.tuwi.in on 8 Nov (1.3 on internal testing). The policy also covers the update check and stored suggestions. Was: redeploy yogaphone.tuwi.in, because the privacy policy now says guard block counts are kept for 7 days (`firebase deploy --only hosting`).
- [ ] **After approval:** replace "Coming soon to Google Play" and the email CTA on yogaphone.tuwi.in with the store link.
- [ ] **Production release.** Only when the founder asks for production.
- [ ] **Tink Tonk: Music Puzzle** update is approved and waiting to be published (another app, same account).

## A. Updates: seamless and automatic (ship in 1.3) [~] merged in 0a5421e; phone check left

- [x] OpenSpec change `add-in-app-updates`, ADR 0013.
- [x] Add `app-update-ktx`. Check on home resume, at most once a day.
- [x] **Normal update:** quiet home line, "Update ready · Update". The download is flexible and installs silently while Yoga is in the background.
- [x] **Priority 4:** the flexible Play dialog. **Priority 5:** an immediate update.
- [x] Add `--priority N` to `scripts/play-upload.mjs` (`inAppUpdatePriority`), default 2.
- [x] **What's new:** home line, then a one-screen sheet after a version change.
- [x] Phone, 8 Nov (moto g31, sideloaded): the "New in 1.2" line, the What's new sheet and Done work, and Help → Check now reaches Play and reports "You're up to date". Fixed: the version now sits in the sans eyebrow, because the serif drew "1.2" as "I.2".
- [ ] Controller test with `FakeAppUpdateManager` (policy and What's-new tests done).
- [ ] Check a real download and install through internal app sharing; this needs a Play-installed build.

## B. Onboarding images (Codex imagegen) [x] merged in edd1436 and e249c8e

- [x] Brief Codex with D1, the brand tokens (docs/design/brand.md) and the image list (brief 13).
- [x] Onboarding images: welcome, awareness, shorts, reels, allowance, nudges and done. Reels was regenerated as an open loop of film (brief 13b).
- [x] Spot images (3): shield, hourglass, moon.
- [x] Transparent WebP in `res/drawable-nodpi/`: heroes 1080 px at 15–45 KB, spots 384 px at 2–4 KB (`scripts/onboarding-images.py`).
- [x] Tint test in Obsidian, Stone and Midnight Sage, light and dark (`design/onboarding/contact-sheet.png`).

## C. Feature introductions engine [~] merged in 59da920; phone check left

- [x] OpenSpec change `add-feature-introductions`, ADR 0012.
- [x] `IntroEngine` (pure, unit-tested) with these rules:
  - one per surface and one new a day;
  - none for 24 h after onboarding;
  - "Not now" snoozes for 7 days, and a second "Not now" retires it.
- [x] `IntroState` in DataStore, `IntroSignals` from the existing repositories, and the shared `IntroCard` kit component.
- [x] Surfaces: the Usage screen card, a home line, and the sibling line on the block card.
- [x] Settings → Help → Suggestions: a list of retired introductions.
- [x] **I1 Shorts block:** YouTube ≥ 3 h a week → "6 h 40 m on YouTube this week" → **Turn off Shorts** → disclosure → Android settings → confirmation with Undo.
- [x] **I2 Reels block:** the same for Instagram.
- [x] **I3 Browser surfaces:** at least 5 blocks a week and at least 1 h of browser use.
- [x] **I4 Daily allowance**, prefilled at 80% of their average.
- [x] **I5 Lower allowance**, using `suggestedAllowance`.
- [x] **I8 Grey past limit.**
- [x] Phone, 8 Nov: Suggestions shows "Nothing to suggest right now", and the block card still works with the guard on and Shorts blocked.
- [x] Phone, 8 Nov, with fake evidence (`--es intro_preview "days=7,avg=200"`, debug and profile builds only): the home line "3 h 20 m a day in distracting apps · Set 2 h 30 m a day" and the Usage card both show, and the action sets the allowance.
- [x] Fixed (4a327cd): the action showed a false "Couldn't change that setting", because the card's coroutine was cancelled as the card left the screen. A notice without an action now shows only "OK".
- [x] Re-checked on the phone after 4a327cd (`--es intro_preview "reset=1,instagram=300"`): the Reels introduction shows on home and on Usage. "Turn off Reels" shows "Reels are off. Posts and messages still work." with Undo and no failure notice, and Undo turns Instagram Reels back off. The test allowance was removed, and the preview and introduction history were reset on the moto g31. Also fixed: the allowance evidence now says "outside essential apps" when the allowance counts all apps except essential (868126c).
- [ ] Check the Shorts flow through Android settings and back. This needs the guard turned off first, which is the founder's call on their phone.
- [x] Block-card line checked on the moto g31 (fake evidence `instagram=300`, which also counts Instagram as installed). Opening Shorts shows "Also turn off Reels? · Turn off Reels · Not now", and the card stays up for 8 s. The overlay takes taps: "Turn off Reels" turns Reels on and shows "Reels are off too · Undo", and Undo turns Reels off again. The phone was left as found: Reels off, Shorts blocked, preview cleared.

## D. Home usage display styles [x] merged in a0abda4, shipped in 1.4 (5)

- [ ] OpenSpec change `add-home-usage-styles`, ADR 0014.
- [ ] Styles: Line (default), Number, Ring, Week, Off. "Show value of time" (Pro).
- [ ] Settings → Appearance → "Today on home".
- [ ] Screenshot tests for every style in all 3 looks.

## E. Onboarding v3 [x] merged in b3ccccc, shipped in 1.4 (5); phone checks under "Phone checks still open"

- [ ] Fewer words: titles of 6 words or fewer, and at most one line of 12 words or fewer under each.
- [ ] New step **Your week**: real numbers, with YouTube and Instagram highlighted.
- [ ] New step **Shorts & Reels**, only when YouTube or Instagram is installed: image, one switch per app, and the real number from Your week.
- [ ] New step **On your home**: live previews of the display styles.
- [ ] Prefill the allowance slider from real usage.
- [ ] Move Value of time out of onboarding; it becomes introduction I10.
- [ ] Existing users get a short What's-new run with only the new steps, not the full replay.
- [ ] Check on the phone in all 3 looks.

## F. Review moments and feedback [~] merged in ebd4bc6; phone check left

Built: R1–R4, the guards, Help → Send feedback, and "What didn't work?". R2 runs on leaving Usage → Last 7 days until piece G exists. Queued moments lapse after a day. Still to do: the checks below.


- [ ] Add `review-ktx`. Moments:
  - **R1:** right after turning on Shorts or Reels blocking from usage data;
  - **R2:** a good week;
  - **R3:** the 3rd focus session or timebox;
  - **R4:** the first strong blocking day, asked the next evening.
- [ ] Guards:
  - installed at least 3 days, with at least 5 home sessions;
  - Yoga is the default home;
  - not during onboarding;
  - not within 10 min of a block, pause or limit notice;
  - not over the limit today;
  - at most once per 90 days and 3 times ever.
- [ ] Never ask a question before the review card (Play policy).
- [ ] Help → Send feedback: an email filled in with the version and device.
- [ ] "What didn't work?" with 3 reasons, kept on the phone, shown when someone turns a feature off within 7 days of turning it on.
- [ ] Tests with `FakeReviewManager`, then internal app sharing.

## G. Weekly review screen

- [ ] An in-app weekly view: the week's numbers plus the top introduction. Free or Pro depends on D2.
- [ ] Opens from the Sunday notification (Pro) and from the Usage screen.

## H. Remaining introductions

- [ ] New signals: opens per app per day (`UsageEvents`), and the longest single session.
- [ ] **I6** Reminders, **I7** Mindful pause, **I9** Bedtime greyscale, **I10** Value of time, **I11** Live pill, **I12** Timebox.
- [ ] **I13** Focus schedule, **I14** DND with focus, **I15** Quiet notifications, **I16** Weekly summary.
- [ ] **I17** Hidden apps, **I18** Profiles, **I19** Widgets, **I20** Looks, **I21** Backup.
- [ ] **I22** Learning videos (once shipped), **I23** Home basics hints, **I24** End-of-trial recap.

## K. WhatsApp Status [~] in 1.6 (8); real WhatsApp check left

## J. A way through, with resistance (D4–D7) [~] J1–J4 in 1.5 (7) on internal testing; phone check left

- [x] J1 (522c689) `ResistanceLadder`, Shorts passes in `ShortsGate`, passes and resists in `GuardDaily`.
- [x] J2 (522c689) Resist screen, "Watch anyway" on the block card, pass pill.
- [x] J3 (d1c7c79) Pause screen: growing wait, how-long chips, hold to open, cost line, time-up nudge.
- [x] J4 (aff24c1) Resistance when loosening rules; strict mode.
- [ ] J5 Pause for apps opened outside Yoga (after Play approval, D7).

## I. Guard measure mode (after first approval, D3)

- [ ] Record Shorts and Reels minutes and video count without blocking.
- [ ] Update the disclosure text. Re-check the Play Accessibility declaration and video.
- [ ] Show count or time, whichever lands harder: time if it's 1 h a week or more, otherwise the count if it's 30 or more.

## Phone checks still open

- [x] 1.4 (5) on internal testing, 10 Nov (v1.4-5): onboarding v3, home display styles, work apps.
- [x] 1.4.1 (6) on internal testing, 10 Nov (v1.4.1-6): startup flash and Week bars fixed.
- [x] Onboarding v3 short run for v2 users, checked on the moto g31: "Your last 7 days" and the Shorts step with its image.
- [x] Onboarding v3 short run finished on the moto g31 (10 Nov): the display choice with real numbers, Done, and the chosen style on home.
- [ ] Onboarding v3 still to check: the value-of-time step and the full replay (Help → Replay setup).
- [x] Home display styles checked without an allowance (Number on home, all previews). Fixed: the startup flash of 'Allow Usage Access' and empty '+' dock slots; Week bars readable.
- [ ] Home display styles with an allowance set (Line hairline, Ring fill, Week allowance line).
- [ ] Work apps: needs a phone with a work profile (the moto g31 has none). Check paused, then turn on and open.
- [ ] **Accessibility declaration + video:** learning videos and WhatsApp Status changed what guard reads (disclosure updated in dc4a0e6). Update the Play declaration and video before the next open-testing or production release.

- [x] Home gestures (7f44eec), checked on the moto g31: touch and hold on empty home opens the Home sheet; Home on a calm home opens the drawer with the keyboard; Home with a sheet open only closes it; typing "calc" opens Calculator; Home from an app returns to plain home.
- [ ] Home gestures still to check: the "Add apps to home" picker (check marks, Done, a full-home message) and Appearance → Search → "Open the only match" turned off.

- [x] Sheets opened half-expanded and hid taller sheets' buttons (Timebox Start) below the screen. All sheets now open fully (fde118d).
- [ ] Pause screen, sheets, light mode, and the Stone and Midnight Sage looks on the release build.
- [ ] Usage screen labels (534eef2): "on distracting apps" and "in all apps" lines, Today and Last 7 days.
- [ ] Review moments (ebd4bc6) with `--ez review_fake true`: R1 after turning on Shorts from an introduction; Help → Send feedback; "What didn't work?" after turning the pause off.
- [x] 1.5 (7) on internal testing, 11 Nov (v1.5-7): a way through Shorts/Reels and the pause, resistance when loosening, review moments, feedback. Emulator check: pause wait, chips, hold, pass, Keep-it sheet.
- [ ] 1.5 on a real phone: "Watch anyway" on the block card, the resist screen, a pass on YouTube and on Instagram (pill, expiry, "Your N minutes are up"), strict mode and "ends tomorrow", the pause time-up notification, TalkBack on the hold buttons.
- [ ] Privacy policy now mentions passes and "not now" counts: redeploy yogaphone.tuwi.in before the next open-testing release.
- [x] 1.6 (8) on internal testing, 12 Nov (v1.6-8): WhatsApp Status block, learning videos, plus 1.5's features. Checked on the moto g31: What's new, Status row and loosen sheet, pause (opens, resisted, pass, time-up notification), Send feedback. Fixed on the phone: empty feedback To field; keyboard covering the pause's first hold.
- [ ] WhatsApp Status on a phone with WhatsApp: confirm the Status viewer is blocked and chats, calls and in-chat videos are not; check the real view ids (`ShortsDetector.whatsappStatusIds`).
- [ ] Turn off `svc power stayon` on the founder's phone. Wait until the founder frees up the phone.
