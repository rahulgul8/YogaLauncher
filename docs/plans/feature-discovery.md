# Plan: introduce every feature at the right moment, update seamlessly, ask for reviews when people are happy

Written 8 November 2026. Tracked in [`TODO.md`](../../TODO.md). Each build piece gets its own OpenSpec change before it is built (project rule).

## What exists today (checked in the code)

- **Onboarding** (`ui/OnboardingScreen.kt`, `ONBOARDING_VERSION = 2`): 9 text-only steps. Welcome, Make home, Pick apps, Usage access, Allowance, Value of time, Nudges, Look, Done. No images. Shorts and Reels are not mentioned.
- **Yoga guard** (`guard/`): blocks YouTube Shorts and Instagram Reels, in the apps and in browsers. It counts blocks and minutes per surface only for surfaces that are switched on. Shorts blocking is **free** (`ProGate`).
- **Usage on home** (`ui/HomeUsageIndicator.kt`): one style only. A line of text ("42 min of 90 today") over a thin progress bar, plus an optional value-of-time line. On or off in Settings.
- **Weekly summary**: a Sunday 19:00 notification plus insight cards (`insights/`). **Pro only** (`WEEKLY_REPORTS`).
- **No in-app updates, no in-app review, no "what's new".** Updates reach people only through Play's own auto-update.

## 1. Feature introductions ("moments")

### Principles

1. **Evidence first.** Every introduction leads with one real number from the person's own phone ("6 h 40 m on YouTube this week"). If there's no number to show, there's no introduction, except the few that are based on time since install.
2. **One tap to turn it on.** The button turns the feature on with sensible defaults already filled in from their data. The confirmation offers **Undo**, never "Are you sure?".
3. **One at a time.** At most one introduction per surface, and at most one *new* introduction a day. None in the first 24 hours after onboarding.
4. **Easy to dismiss.** "Not now" snoozes it for 7 days. A second "Not now" retires it for good. Retired introductions stay findable in Settings → Help → **Suggestions**.
5. **Never for something already on**, or for an app that isn't installed.
6. **Pro features:** shown during the trial with the normal button. After the trial they show a small Pro mark, at most once a month in total.
7. **Copy:** a number line plus a verb button. No paragraphs.

### Where introductions appear

| Surface | What it shows | Notes |
|---|---|---|
| **Weekly review** (new in-app screen, opened from the Sunday notification and the Usage screen) | The top introduction, under the week's numbers | The best moment: the person is looking at their own week. |
| **Usage screen** (`UsageDashboardScreen`) | One card at the top | Shown every day the conditions hold. |
| **Home** | One quiet line in the style of `DefaultHomePrompt` (dot · text · action) | Only for the most valuable introductions, at most once a day. |
| **After a block** | A second line on the block card ("Also turn off Reels?") | Only for the sibling feature. |
| **Notification** | Only through the existing weekly summary | No new notification channel. |

### Shorts and Reels (the founder's example)

**Before Yoga guard is on**, the app sees only per-app time from Usage Access, not Shorts specifically:

- **Trigger:** YouTube (or Instagram) at least 3 h in the last 7 days, or at least 30 min a day on average over 3 or more recorded days, and that surface's guard is off.
- **Card:** "**6 h 40 m on YouTube this week.**" Button: **Turn off Shorts**. Small print: "Regular videos still work."
- **Instagram:** the same pattern. "**4 h 10 m on Instagram this week.**" Button: **Turn off Reels**.
- **The enable flow:**
  1. the guard disclosure dialog appears, already pointed at YouTube;
  2. Android's Accessibility settings open;
  3. on return, a confirmation: "Shorts are off. Regular videos still work." with **Undo**.
- If the person backs out of Android settings without switching it on, a calm "Not switched on. Try again" line appears. No nagging.

**After Yoga guard is on**, the app can measure Shorts precisely:

- Turning on one surface introduces its sibling. Shorts on → "Also turn off Reels?"
- Blocks moving to the browser trigger the browser surfaces. If Chrome opens youtube.com/shorts within 5 minutes of a block, show "Shorts blocked, then opened in Chrome 3 times" with the button **Block in browsers too**.
- **Count or time, whichever lands harder.** Show time when it's at least 1 hour a week, otherwise the count when it's at least 30, otherwise nothing. Example: "**212 Shorts this week**" versus "**3 h 05 m of Shorts this week**".
  - Counting Shorts needs a guard "measure" state: for a surface that is not blocked, record minutes and the number of videos (each scroll to a new reel) without blocking.
  - That changes what the accessibility service does. So it needs updated in-app disclosure text, and possibly an updated Play declaration and video.
  - **Do this only after Play approves the first review.**

### Catalogue: every feature and its moment

Impact is estimated from the minutes it would save, and sets the order when more than one is eligible.

| # | Feature | Moment (trigger) | Evidence shown | One-tap action (prefilled) | Tier |
|---|---|---|---|---|---|
| I1 | Shorts block | YouTube ≥ 3 h/week, guard off for YouTube | YouTube time this week | Disclosure → Android settings → confirm | Free |
| I2 | Reels block | Instagram ≥ 3 h/week, guard off for Instagram | Instagram time this week | Same | Free |
| I3 | Browser Shorts/Reels | Opened in a browser within 5 min of a block, twice or more | Count of browser escapes | Turn on browser surfaces | Free |
| I4 | Daily allowance | No allowance, and ≥ 2 h/day of distracting apps over 3 days | Average per day | Allowance = 80% of their average, rounded to 15 min | Free (one) |
| I5 | Lower allowance | Already computed by `suggestedAllowance` | "5 of 7 days under 75 min" | Set the new value | Free |
| I6 | Reminders | Over the allowance on 2 or more days this week, reminders off | Days over | Turn on (+ notification permission) | Pro |
| I7 | Mindful pause | One distracting app opened ≥ 40 times in a day | "Instagram opened 52 times yesterday" | Add a pause for that app | Free (first app) |
| I8 | Grey past limit | Over the limit on 3 or more days this week | Days over | Turn on | Free |
| I9 | Bedtime greyscale | Late-night use ≥ 30 min/day (existing late-night data) | Late-night minutes | Greyscale 23:00–06:00 | Free |
| I10 | Value of time | Allowance set, over the limit this week, value off | Minutes past the limit | Ask for the hourly value (offer recent entries) | Pro |
| I11 | Live time-value pill | Value of time on and guard on | Today's value | Turn on | Pro |
| I12 | Timebox | A single distracting session ≥ 45 min | "One YouTube session ran 1 h 20 m" | Timebox the next session automatically | Pro |
| I13 | Focus schedule | ≥ 80 unlocks a day, or heavy use between 09:00 and 18:00 on weekdays | Unlocks or work-hour minutes | Focus 09:00–12:00 on weekdays | Pro |
| I14 | Do Not Disturb with focus | After the first completed focus session | Session length | Turn on DND during focus | Pro |
| I15 | Quiet notifications | Day 5, if focus or DND is used | (time-based) | Open the digest setup | Pro |
| I16 | Weekly summary | First full week of recorded data | Week total | Turn on | Pro |
| I17 | Hidden apps | A drawer app not opened in 30 days | App name | Hide it | Free |
| I18 | Profiles | Day 7, if work apps (Slack, Teams, Outlook…) are used in the evening | Evening work-app minutes | Create Work and Personal | Pro |
| I19 | Widgets | Day 3 (time-based) | — | Show the long-press hint | Pro |
| I20 | Looks | Day 2, in the evening (time-based) | — | Preview Stone or Midnight Sage | Pro |
| I21 | Backup | Day 7 after setup | Number of settings set | Export a backup | Free |
| I22 | Learning videos (when shipped) | Shorts blocked ≥ 10 times | Block count | Add learning channels | Pro |
| I23 | Home basics (swipe up, long-press, top icons) | First 3 home sessions, one hint each | — | Dismiss the hint | Free |
| I24 | Yoga Pro (end of trial) | Trial day 11 | What they used ("blocked Shorts 42 times, 3 h kept") | Open the plans | — |

### Build shape

- `intro/IntroEngine.kt`: a pure function from (signals, intro state, today, Pro state) to at most one intro per surface. Fully unit-tested, the same way `InsightEngine` is.
- `intro/IntroState`: state kept in DataStore:
  - shown count, last shown, snoozed until, retired;
  - the date of the last *new* introduction;
  - the last review moment (section 4).
- `intro/IntroSignals`: built from the existing usage, guard, focus and insights repositories. It adds two signals: daily open counts per app (`UsageEvents` ACTIVITY_RESUMED) and the longest single session.
- `ui/IntroCard.kt`: one shared kit component, used on every surface.
- **Weekly review screen:** a free, in-app view of the week with its introduction. The Sunday notification and deeper insights stay Pro. Free users open it from the Usage screen. *(Decision D2.)*

## 2. Onboarding v3: less text, images, Shorts and Reels, home display

### Flow

Steps marked *new* are new; conditional steps appear only when they apply.

| # | Step | Title (≤ 6 words) | Line (≤ 12 words) | Visual |
|---|---|---|---|---|
| 1 | Welcome | Your phone, on purpose. | A calm home that shows where time goes. | Image + live clock |
| 2 | Home | Make Yoga your home. | Switch back any time. | — |
| 3 | Apps | Apps you reach for on purpose. | Up to 8. The rest is one swipe away. | — |
| 4 | Awareness | See where time goes. | Stays on this phone. Never sent anywhere. | Image |
| 5 | *Your week (new)*, only once access is granted | Your last 7 days. | — | Real numbers: total plus top 3 apps, YouTube/Instagram highlighted |
| 6 | *Shorts & Reels (new)*, only if YouTube or Instagram is installed | Keep videos. Lose the scroll. | Shorts and Reels off. Everything else works. | Image; one switch per installed app, using last step's number |
| 7 | Allowance | How much is enough? | — | Slider prefilled at 80% of their real average |
| 8 | *On your home (new)* | How should today look? | — | Live previews of the display styles below |
| 9 | Nudges | Start gently. | — | Image; the 3 existing switches |
| 10 | Look | Choose your look. | — | The existing picker |
| 11 | Done | Ready. | Everything is unlocked for 14 days. | Image |

- **Value of time** moves out of onboarding. It becomes introduction I10, at the moment someone first goes past their limit, which is when the money metaphor lands.
- **Existing users don't replay the whole walkthrough.** Keep `ONBOARDING_VERSION` and add a short "What's new" run with only the new steps (5, 6, 8) on their first open after updating.
- Copy rules: one title, at most one line, no "you can change this later" lines. The Done step says it once.

### Images (generated by Codex `imagegen`)

- **Style (recommended, decision D1):**
  - monochrome etched line art on a transparent background, with a lot of empty space;
  - tinted at runtime with the current look's accent (brass, saffron or sage), so one image works for all 3 looks and for light and dark;
  - no app logos or lookalikes (YouTube and Instagram are trademarks).
- **Set:**
  - welcome: a sun over still water inside a dial;
  - awareness: an hourglass with a fine sand stream;
  - shorts: an endless film strip cut cleanly;
  - reels: a loop of film, untied;
  - allowance: a bowl being filled to a line;
  - nudges: a single feather or ripple;
  - done: an open doorway to the horizon.
  - Plus 3 small spot images for introduction cards: shield, hourglass, moon.
- **Delivery:** `res/drawable-nodpi/onboarding_*.webp`, about 1080 px wide, each under 80 KB. Codex copies the chosen outputs from `~/.codex/generated_images/` into the project; Claude reviews them on the phone in all 3 looks.

### Home usage display (a choice in onboarding, then in Settings → Appearance)

| Style | What it shows |
|---|---|
| **Line** (today's default) | "42 min of 90 today" and a hairline |
| **Number** | Large lining figures "1 h 12 m" under the clock, with a small "of 1 h 30 m" |
| **Ring** | A thin arc that fills against the allowance, beside the date; the accent turns tertiary past the limit |
| **Week** | 7 tiny bars, today's bar in the accent, the allowance as a hairline |
| **Off** | Nothing on home |

- Options: "Show value of time" (Pro), and tap anywhere to open Usage.
- All styles use the lining, tabular figures already in the clock.
- Before Usage Access is granted, the onboarding previews use sample numbers, labelled "Sample".

## 3. Updates: seamless and automatic

**What's possible on Android:**

- **Play auto-update.** It is on for most people and installs updates in the background, usually within a day or two. Play waits for Wi-Fi, charging and idle conditions, and we can't make it go faster.
- **The In-App Updates API.** It lets the app check for a new version and download it in the background. Android still needs the person to agree once per update (a small Play dialog), except for updates they already allowed through auto-update.

**Plan:**

1. **Ship this in the very next version (1.3).** Only versions that contain the code can prompt, so every release without it is a release we can't nudge.
2. **Check quietly** on a home resume, at most once a day (`app-update-ktx`).
3. **Normal update** (priority 0–3): a quiet home line "**Update ready** · Update". Tapping it starts a *flexible* download.
   - Calling `completeUpdate()` while Yoga is in the background installs silently without interrupting the person.
   - So we install when the screen goes off or another app is in front, and Android brings the home screen back on its own.
4. **Important update** (priority 4): the flexible Play dialog on the next home resume.
5. **Critical update** (priority 5, for example a crash or policy fix): an *immediate* update, which is full-screen. Use it rarely, because it blocks the home screen.
6. **Priority is set per release** with `scripts/play-upload.mjs --priority N` (the API field `inAppUpdatePriority`). Default 2.
7. **What's new:** after a version change, one home line "New in 1.3 ·" opens a one-screen sheet with 2–3 lines. A new feature's introduction jumps the queue for the next week.
8. **Shipping speed:** managed publishing stays off, so approved builds go live as soon as Google approves them, and small updates roll out at 100% straight away. Release notes come from `docs/play/release-notes.md`. Play's review time (hours to days) is outside our control.
9. **Testing:** `FakeAppUpdateManager` in unit tests, plus Play *internal app sharing* on the phone for the real flow.

## 4. Reviews and feedback at the right moment

Use the **Play In-App Review API** (`review-ktx`).

**Play's policy:**
- We may not ask "Do you like Yoga?" before the review card, and may not show the card only to happy users through a question.
- Play also limits how often the card appears and never tells us whether it was shown.

So the *moment* is how we pick happy people.

**Moments, highest first:**
1. **R1:** right after someone turns on Shorts or Reels blocking from a usage introduction, the weekly review or onboarding's Your week step. The trigger is the confirmation screen after returning from Android settings. They've just made a decision based on their own data. (The founder's example.)
2. **R2:** the weekly review shows a good week, at least 5 of 7 days within the allowance, or distracting time down 20% or more.
3. **R3:** the third completed focus session or timebox.
4. **R4:** the first day the guard blocks Shorts or Reels 5 or more times and the person stays under their allowance. The ask comes at the next evening home resume, never on the block card itself.

**Guards** (all must pass):
- installed at least 3 days ago, with at least 5 home sessions;
- Yoga is the default home;
- not in onboarding;
- not within 10 minutes of a block card, a pause screen or a limit notification;
- not over the limit today;
- our own limit of one ask per 90 days and 3 ever.

**Feedback for everyone else (not tied to the review card):**
- Help → **Send feedback** opens an email to tuwi.developer@gmail.com with the app version and device model filled in. The person sees it before sending.
- When someone turns off a feature they turned on in the last 7 days (guard, allowance, pause), one optional line asks "What didn't work?" with 3 tap reasons plus Other. The answer is kept on the phone and offered for the next feedback email; nothing is sent automatically.

**Before public launch:** reviews from open-testing testers aren't public. Test the flow with `FakeReviewManager` and internal app sharing.

## 5. Build order (Codex pieces)

| Piece | What | Depends on | Codex model and effort |
|---|---|---|---|
| A | **In-app updates + What's new** (section 3), `--priority` in play-upload.mjs | — | Default model, medium |
| B | **Onboarding images** (imagegen, 10 images, tint test) | D1 | Codex imagegen |
| C | **IntroEngine + state + IntroCard + Suggestions list**, with I1–I5 and I8 | — | Frontier model, medium (rules and time maths) |
| D | **Home display styles** (Line, Number, Ring, Week, Off) + setting | — | Default model, medium |
| E | **Onboarding v3** (new steps, copy, images, What's-new run for existing users) | B, C, D | Default model, medium |
| F | **Review moments + feedback** (section 4) | C | Default model, medium |
| G | **Weekly review screen (free)** with introductions | C, D2 | Default model, medium |
| H | **Remaining introductions** I6, I7, I9–I24 (new signals: opens per app, longest session) | C | Default model, medium |
| I | **Guard measure mode + Shorts count** | First Play approval; disclosure and declaration update | Frontier model, high |

- **Starting together:** A, B and C can begin at the same time, because they touch different files.
- **Version 1.3:** A + C + D.
- **Version 1.4:** E + F + G.
- **ADR numbers** (0011 is taken by learning videos):
  - 0012: feature introductions (C);
  - 0013: updates and reviews (A, F);
  - 0014: home display styles (D).

## Decisions (recommended answer first)

- **D1. Image style:** monochrome etched line art, transparent, tinted per look *(recommended)*; or full-colour scenes per look (3× the images, heavier).
- **D2. Weekly review for free users:** an in-app weekly view with introductions is free; the Sunday notification and deeper insights stay Pro *(recommended: introductions need an audience, and free users are the ones to convert)*. Or keep it Pro-only.
- **D3. Guard measure mode:** build it after the first Play approval, with an updated disclosure *(recommended)*; or never, and use per-app time only.
