# Feature roadmap: match the category, then stand apart

Written 2026-11-07 from `competitors.md` (13 apps) and `regain.md`. Audience: anyone busy who wants their time back — students, working professionals, parents — not only exam students. No study rooms or leaderboards.

Status: **proposal, waiting for the founder's go-ahead on order.** Learning videos is already approved and is next for Codex.

## Where Yoga already stands

Yoga already covers most of what the category offers: text-first home, search drawer with recent searches, hide and rename apps, three looks (light, dark, greyscale each), widgets, a dock, categories and profiles, notification digest (Quiet notifications), greyscale, backup and restore, schedules, per-app allowances, a mindful pause with breathing, usage reminders, Shorts/Reels blocking, focus and timebox, screen-time dashboard and insights.

Things nobody else has: **value of time** (the burning pill), the **dock + search pill** home design, **notification digest inside a launcher**, and three complete **premium looks**.

## 1. Parity: the gaps worth closing

Ordered by how often competitors have them and how cheap they are. "Free" or "Pro" follows `docs/launch/pricing.md`: the launcher stays free; the mindful layer is Pro.

| # | Feature | Seen in | Size | Tier |
|---|---|---|---|---|
| P1 | **Auto-launch on a single search match** (setting, on by default) | 7 of 9 launchers | S | Free |
| P2 | **Gestures**: double-tap to lock, swipe left / right opens a chosen app, long-press clock opens alarms | 6 launchers | S–M | Free |
| P3 | **Work profile apps** in the drawer with a small "Work" label; pause work apps in focus | MP, OL, BE | M | Free (pause: Pro) |
| P4 | **Today's three**: up to three intentions/tasks on home, under the clock; tick to clear; last entries offered as chips | PL, IN, RG | S–M | Free (1), Pro (3 + history) |
| P5 | **Next event line**: the next calendar event under the date (on-device, READ_CALENDAR, optional). No weather — it needs location and a server. | MP, PL, NI, IN | S | Free |
| P6 | **Website blocker** in Chrome, Firefox, Samsung Internet via Yoga guard's browser reading; a built-in adult-site list (on/off) plus your own list | MP, OP, SZ, RG, OS | M–L | Pro |
| P7 | **Commitment mode** (our calmer "strict mode"): lock today's allowances and focus until a chosen time; editing asks you to wait 10 minutes, never "no turning back". No device admin, no uninstall blocking — Play scrutiny and user trust both argue against it. | OP, SZ, OS, RG | M | Pro |
| P8 | **Streaks and goals, visible**: Yoga already computes a streak in insights; show a quiet "days under your allowance" line and a goal on the dashboard | OP, SZ, RG | S | Pro |
| P9 | **Focus sounds**: 4–5 bundled loops (rain, café, brown noise) during focus/timebox | OP, RG | M (audio licensing) | Pro |

Deliberately **not** copying: icon packs (Yoga is text-first), leaderboards and study rooms, anti-uninstall/device admin, website "commit forever" blocks, daily wallpapers (the looks are the wallpaper).

## 2. Stand-out features (only Yoga)

| # | Feature | Why it's ours | Size | Tier |
|---|---|---|---|---|
| S1 | **Learning videos** — approved; see `openspec/changes/add-learning-videos`. Starter chips now cover work too (e.g. Google for Developers, Harvard Business Review, Ali Abdaal, TED, Khan Academy, NPTEL). | Productive YouTube without a blunt block | L | Pro |
| S2 | **Intention launch**: opening a distracting app asks "For what?" with your recent answers as chips ("reply to Priya", "15 min break"); the answer sets a timebox and the pill shows it. Combines one sec's pause with Yoga's timebox in one tap. | Turns friction into a plan, not a wall | M | Pro |
| S3 | **Calm first hour**: for the first N minutes after your first unlock of the day, home shows only Today's three and essentials; everything else is one search away. Same for the last hour before bed. | The two moments that set the day | M | Pro |
| S4 | **Weekly letter + share card**: Sunday's insight becomes a short letter ("You took back 3 h 20 m — ₹1,240 of your time") and an optional image card the user can share. Nothing leaves the phone unless they share. | Personal, honest, and a growth loop | M | Pro (card free) |
| S5 | **Accountability buddy, lightweight**: pick one person; once a week Yoga drafts a message with your summary for you to send (WhatsApp/SMS share sheet). No accounts, no server. | Social support without leaderboards | S | Pro |
| S6 | **Time budget for the whole phone**: "2 h a day" across distracting apps, with the value of time per hour — already half-built (TOTAL rule); make it the headline setting in onboarding. | Simple promise people understand | S | Free (one budget) |
| S7 | **Meeting / driving quiet**: when a calendar event marked busy is on, hold notifications and tuck distracting apps; ends automatically. | Uses calendar on-device, no location | M | Pro |

## 3. Suggested order

1. **Now (Codex, approved):** S1 Learning videos.
2. **Next, small parity bundle:** P1 auto-launch, P2 gestures, P5 next event line, P8 streak line — one Codex task, mostly UI.
3. **Then:** P4 Today's three + S3 Calm first hour (they share the home "Today" area).
4. **Then:** P6 Website blocker + adult list (guard work; needs the Accessibility declaration video to show it).
5. **Then:** S2 Intention launch, P7 Commitment mode.
6. **Later:** S4 weekly letter/card, S5 buddy, S7 meeting quiet, P3 work profile, P9 sounds.

Each item gets an OpenSpec change before it is built.

## Pricing check against the category

| | Monthly | Yearly | Lifetime | Trial |
|---|---|---|---|---|
| **Yoga Pro** | **₹149 / $3.99** | **₹999 / $24.99** | **₹2,499 / $59.99** | 14 days, no card |
| minimalist phone | ~$4.99 | ~$20 | $30–60 | 7 days |
| one sec | — | $19.99 | $99.99 | 7 days |
| Opal (iOS list) | $19.99 | $99.99 | $399 | yes |
| Regain | ₹199 (often ₹84) | ₹999 (often ₹299) | — | not published |
| Niagara Pro | — | ₹140 | ₹420 | — |
| Before / UnLauncher | one-time ₹180–999 | | | |

Yoga sits at the premium end of launchers and well below blocker apps like Opal, which matches "premium but fair".
