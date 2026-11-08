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
- [ ] **Name check:** Lenovo's YOGA trademark, before public launch.
- [ ] **Roadmap order** in docs/research/feature-roadmap.md.
- [ ] **Learning videos:** branch `learning-videos`. Check it on the phone, then merge or park it.

## Release and Play

- [~] **First Play review of 1.2 (3):** open testing in 178 countries, store listing, and all 12 declarations, Accessibility included. Sent 8 Nov 2026. *Waiting on Google.*
- [ ] **Trader status declaration** (founder, Play Console). Due 1 Feb 2027.
- [ ] **When 1.3 ships:** redeploy yogaphone.tuwi.in, because the privacy policy now says guard block counts are kept for 7 days (`firebase deploy --only hosting`).
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

## B. Onboarding images (Codex imagegen) [~] merged in edd1436; Reels still to do

- [x] Brief Codex with D1, the brand tokens (docs/design/brand.md) and the image list (brief 13).
- [~] Onboarding images: welcome, awareness, shorts, allowance, nudges and done are done. **Reels:** the draft reads as a speech bubble, so regenerate it.
- [x] Spot images (3): shield, hourglass, moon.
- [x] Transparent WebP in `res/drawable-nodpi/`: heroes 1080 px at 15–37 KB, spots 384 px at 2–4 KB (`scripts/onboarding-images.py`).
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
- [ ] Check on the phone: the Usage card, the home line and the Shorts flow through Android settings and back. Needs Usage Access for Yoga, which is off on the moto g31.
- [ ] Check the block-card line "Also turn off Reels?". Needs Instagram, which isn't installed on the moto g31.

## D. Home usage display styles

- [ ] OpenSpec change `add-home-usage-styles`, ADR 0014.
- [ ] Styles: Line (default), Number, Ring, Week, Off. "Show value of time" (Pro).
- [ ] Settings → Appearance → "Today on home".
- [ ] Screenshot tests for every style in all 3 looks.

## E. Onboarding v3

- [ ] Fewer words: titles of 6 words or fewer, and at most one line of 12 words or fewer under each.
- [ ] New step **Your week**: real numbers, with YouTube and Instagram highlighted.
- [ ] New step **Shorts & Reels**, only when YouTube or Instagram is installed: image, one switch per app, and the real number from Your week.
- [ ] New step **On your home**: live previews of the display styles.
- [ ] Prefill the allowance slider from real usage.
- [ ] Move Value of time out of onboarding; it becomes introduction I10.
- [ ] Existing users get a short What's-new run with only the new steps, not the full replay.
- [ ] Check on the phone in all 3 looks.

## F. Review moments and feedback

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

## I. Guard measure mode (after first approval, D3)

- [ ] Record Shorts and Reels minutes and video count without blocking.
- [ ] Update the disclosure text. Re-check the Play Accessibility declaration and video.
- [ ] Show count or time, whichever lands harder: time if it's 1 h a week or more, otherwise the count if it's 30 or more.

## Phone checks still open

- [x] Sheets opened half-expanded and hid taller sheets' buttons (Timebox Start) below the screen. All sheets now open fully (fde118d).
- [ ] Pause screen, sheets, light mode, and the Stone and Midnight Sage looks on the release build.
- [ ] Turn off `svc power stayon` on the founder's phone. Wait until the founder frees up the phone.
