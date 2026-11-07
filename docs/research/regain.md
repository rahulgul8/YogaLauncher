# Competitor study: Regain (ai.regainapp)

Researched 2026-11-07 from the Play listing (India), regainapp.ai (home, support, about, privacy, refund and terms pages),
AppGoblin and AlternativeTo. The paywall itself was not seen: that needs the app installed on a phone.

## Who and how big

- **Maker:** EpowerX Labs Pvt Ltd, Bengaluru. Its founder previously built "Where Is My Train", which Google acquired.
- **Play listing:** "Regain: Study Timer for Focus", 50 lakh+ downloads, 4.4★ from 2.28 lakh reviews, Productivity, no ads, in-app purchases.
- **Revenue (AppGoblin estimates, not official):** about US$100K+ a month, all from in-app purchases, with about 6.8 lakh monthly active users.
- **Audience:** Indian competitive-exam students (JEE, NEET, Boards, UPSC, CA, CAT, GATE). Its site claims "Trusted by 3M+ students". The Terms nonetheless say users must be 18+.
- **Positioning shift:** it started as a screen-time and app-limit tool and has moved to a study timer with social accountability. One AlternativeTo user wrote "Used to be 100% free, now it try to sells you subscriptions".

## Features

| Area | What it does |
| --- | --- |
| Focus timer | Timer, stopwatch and Pomodoro modes; focus music; themes. Strict Mode blocks leaving early and hides the home screen. |
| Multiplayer focus | Live focus rooms and exam-cohort groups with global leaderboards (one showed "520,203 students focusing"). This is its strongest retention hook. Top reviews complain about people faking 20-hour days to climb the boards. |
| App limits + Strict Mode | Daily limits per app. In strict mode a limit can't be edited until it ends. Streaks. |
| Block Reels & Shorts | Instagram, YouTube, Snapchat Spotlight and Facebook Reels, through the Accessibility API. |
| YouTube Study Mode | Allows only channels the user has added; blocks Shorts and the YouTube home feed (see below). |
| Browser Study Mode | Allows only "productive" websites during focus. |
| Website blocker | Adult sites and distractions. "Strict blocking" can't be undone. |
| Block protection | Uninstall prevention, and blocking of floating windows and split screen. |
| Insights | Productive vs distracted time, daily and weekly stats, tags. |
| Schedule planner | Planned and repeating sessions, a weekly calendar, streaks. |
| "Rega" | A mascot companion that sends nudges. |

## Money

- **Model:** freemium with a subscription, called "Regain Pro". SDKs found in the app: RevenueCat (subscriptions) and Razorpay (Indian payments), plus Mixpanel and Segment analytics.
- **Free:** "the focus timer, app blocking, and screen time insights" (their FAQ).
- **Pro:** "premium themes and additional strict-blocking options". The site doesn't say which headline features are Pro-only.
- **Prices (refund policy, Android):**
  - ₹199 a month, often discounted to as little as ₹84.
  - ₹999 a year, often discounted to as little as ₹299.
  - No lifetime plan.
  - Prices change with "timely offers", meaning frequent discounting.
- **Trial:** no trial length is published.
- **Refunds:** the refund policy offers refunds within 24 hours of purchase or renewal. The Terms say "all sales are final". The two contradict each other.

## How YouTube Study Mode decides what's "study"

**Regain doesn't classify videos itself. The user chooses the channels.** In the site's words: "Choose the channels you study from. Everything else, including Shorts, stays blocked." A screenshot caption reads "block Shorts, block homepage, only study channels".

- During a focus session, the Accessibility service reads the YouTube screen. A video from a channel on the user's list is allowed; anything else is blocked.
- The home feed and Shorts are always blocked in this mode, which removes the two main ways a study session drifts.
- I found no sign of AI, keyword matching or YouTube categories.
- This works because the decision is the user's own, so the app never wrongly calls a video "educational". The cost is setup: students have to build the list.

## What it means for Yoga

1. **Their Accessibility use got through Play review** with a similar description. That's a useful precedent for Yoga guard's declaration.
2. **The channel list is the proven core of study videos.** Yoga can make it easier to build: add the current video's channel with one tap, and offer starter channels as chips. AI can come later and only ever suggests (see `openspec/changes/add-learning-videos`).
3. **Their audience is students; ours is adults who want a calmer phone** (the premium segment). Yoga shouldn't copy leaderboards and strict-mode lock-ins. Yoga's edge is a calm, beautiful home screen, the value of time, and honesty: no dark patterns, no fake discounts.
4. **Pricing anchor in India:** ₹199 a month or ₹999 a year, heavily discounted in practice. A premium product can sit at or a little above this if the trial shows its value (see `docs/launch/pricing.md`).
5. **Watch-outs from their reviews and policies:**
   - Contradictory refund terms.
   - Users who feel a free app turned paid.
   - Strict modes that can't be undone, which feel punitive.
