# Plan: let people through, but make them choose it

Status: decided 11 Nov 2026 with the recommended answers: D4 a way through by default, D5 all free, D6 strict changes apply tomorrow, D7 Part 4 after the first Play approval. Building J1–J4 for 1.5.

## What happens today

| | Shorts and Reels block | App pause (mindful pause) |
|---|---|---|
| **Where** | Guard service (Accessibility), in YouTube, Instagram and browsers | Only when the app is opened from Yoga (home, drawer, search) |
| **What it does** | Presses Back (Home after repeated tries) and shows a card for 2.5 s: "Shorts are off · Regular videos still work · Blocked 3 times today" | Full screen: a breathing clock that counts down for 2–10 s (+5 s during Focus), "What for?" with recent purposes, then **Not now** or **Open app** |
| **Way through** | None in the moment. The only ways round are a daily Shorts allowance (minutes) or turning the guard off in Settings | A plain tap on **Open** once the countdown ends; then no pause for that app for 5 min |
| **Recorded** | Blocks per surface per day (`GuardDaily`) | Each outcome (`recordIntervention`: CONTINUED or WENT_BACK, with purpose) |

So the two ends are wrong in opposite ways. The Shorts block has no way through, so people who really want one turn the whole guard off in Settings, and we lose them. The pause has a way through that costs almost nothing: the same short wait every time, then one tap.

## The idea

Both interrupts get the same shape: **a way through that's always there, but takes a real choice**. Saying no is always the big button and always one tap. Getting through takes waiting, choosing how long, and seeing what it costs. Each extra time through on the same day costs a little more.

Building blocks, used by both:

1. **A wait that grows.** The first time through today waits the base time; every further time through that day doubles it, up to a cap. Saying no never adds to it. It resets at midnight.
2. **Choose how long.** Getting through is never open-ended. The person picks 5, 10 or 15 minutes. The chip from last time is preselected, so it's still one tap. When the time runs out, the block or pause comes back, with no automatic extension.
3. **See the cost, in their own numbers.** One line, for example "3rd time today · 48 min on Shorts this week" or "You're 20 min past today's limit". It shows facts and never scolds.
4. **Hold to continue.** The way-through button is pressed and held (about 1.5 s, with a ring filling), not tapped. That stops reflex taps. Plain tap with a reduced-motion fallback stays available for accessibility (TalkBack double-tap-and-hold works).
5. **Count the wins.** Every "Not now" counts as resisted: "Resisted 4 times today". It shows on the screen and in the Usage screen, and feeds the weekly review and review moment R4.
6. **Strict mode, opt-in.** For people who want no way through (today's Shorts behaviour). Turning strict mode off, or loosening any limit, takes effect **tomorrow** (or after a 10-minute wait, the founder's call), so it really binds.

## Part 1. Shorts and Reels: "Watch anyway"

Flow when someone opens Shorts and the block is on:

1. The guard presses Back as today and shows the block card. The card gains a second line: **"Watch anyway"** (a quiet text button), and stays up for 6 s instead of 2.5 s when that line is shown.
2. Tapping "Watch anyway" opens a Yoga screen (a full-screen activity, because the overlay card can't take text or long presses):
   - eyebrow "Shorts" or "Reels", the breathing clock counting down the wait;
   - the cost line: blocks today, passes today, minutes this week;
   - "For how long?" chips: 5 · 10 · 15 min (last choice preselected);
   - **Not now** (primary, always active), **Hold to watch for 10 min** (secondary, active when the wait ends).
3. Holding opens a **pass**: the guard lets that surface through for the chosen minutes. A small pill shows the time left ("Shorts · 7 min left"), using the existing live-pill overlay.
4. When the pass ends, the block resumes at the next detection. The card says "Your 10 minutes are up".
5. "Not now" returns to YouTube or Instagram on the regular feed and counts a resist.

Numbers (defaults, changeable later):
- Wait: 15 s for the first pass of the day, then 30 s, 60 s, 120 s (cap).
- Passes: unlimited, but each costs more. After the 3rd, the chips drop to 5 min only.
- Passes are per app family: a YouTube pass doesn't open Reels.

Code changes:
- `guard/ShortsGate.kt`: add `passUntil: Map<GuardSurface, Long>`. While a pass is live, `evaluate` returns NONE and still charges time to `usedMillis`. Pure and unit-tested like today.
- `guard/GuardModels.kt`: `GuardDaily` gains `passes` and `resisted` per surface; `GuardPreferences` gains `strict: Boolean`.
- `guard/YogaGuardService.kt`: the card's "Watch anyway" starts `ResistActivity` (a new `ui/ResistScreen.kt` hosted in a small activity with `excludeFromRecents`). The pass comes back to the service through `GuardPrefs`, which it already observes.
- Settings → Shorts and Reels: "When I open Shorts: **Let me through after a pause** (default) · **Never** (strict)".
- Check: an Accessibility service may start an activity from the background; confirm on Android 14 and 15 on the moto g31 and one more phone.

## Part 2. App pause: make "Open" cost something

Keep the pause screen, change the way through:

- **The wait grows** per app per day: the base `delaySeconds` first, then doubling, up to 60 s. During Focus, +5 s as today.
- **For how long?** chips (5 · 10 · 15 min, last choice preselected). When the time is up, a gentle full-screen nudge the next time Yoga sees the app in front: "Your 10 minutes on Instagram are up · Close · 5 more" ("5 more" goes through the same wait). This replaces "Don't ask again for 5 min".
- **The cost line** gets sharper: "4th open today · 52 min in Instagram today" or the allowance line we already have.
- **Hold to open** replaces the plain tap.
- **Past the daily allowance**, the base wait doubles and the chips drop to 5 min only.
- Resisted counts show on the screen and in Usage.

Code changes:
- `focus/FocusLogic.kt`: a pure `ResistanceLadder` shared with Part 1 (wait for the nth pass, chip set, over-limit rule).
- `focus/FocusViewModel.decide`: pass today's count for that app; `LaunchDecision.Pause` carries the wait and chips.
- `ui/MindfulDelayScreen.kt`: chips, the cost line, hold-to-open. Reuse `ResistScreen` pieces so both screens look the same.
- `focus/FocusPrefs.kt`: `lastPassMinutes` per app; drop `dontAskMinutes` (migrate it into the default chip).

## Part 3. Loosening rules takes resistance too

Today anyone can turn off the guard, raise the allowance or turn off the pause in Settings, instantly. That's the real bypass. Under the new model:
- Turning **off** a block, raising an allowance, or leaving strict mode shows the same wait and cost line first ("You've resisted 12 times this week").
- In strict mode the change is scheduled for tomorrow, with a line in Settings: "Shorts block turns off tomorrow · Cancel".
- Tightening rules is always instant.

## Part 4 (later). The pause for apps opened outside Yoga

Opening Instagram from Recents, a notification or another app skips the pause today. The guard service could show the same pause when a paused app comes to the front without going through Yoga. This is the biggest gain, but it changes what we tell Play the Accessibility service does. **Do this after the first Play approval**, with an updated declaration and video, like guard measure mode (D3).

## Decisions for the founder

1. **D4. Default for Shorts and Reels:** way through with resistance (recommended: fewer people turn the guard off completely), or keep strict as the default.
2. **D5. Free or Pro:** the way through and its resistance are free (recommended: it's part of the core block); strict mode's "change applies tomorrow" could be Pro.
3. **D6. Strict mode cooling-off:** changes apply **tomorrow** (recommended, a clear rule) or after a 10-minute wait.
4. **D7. Part 4** (pause for apps opened outside Yoga) after the first Play approval (recommended), or now.

## Order of work (Codex pieces)

| Piece | What | Needs | Model |
|---|---|---|---|
| J1 | `ResistanceLadder` + `ShortsGate` passes + `GuardDaily` fields, with tests | D4 | Frontier model, medium (timing and blocking logic) |
| J2 | `ResistScreen` + activity + block-card "Watch anyway" + pass pill | J1 | Default model, medium |
| J3 | Pause screen: growing wait, chips, hold, cost line, time-up nudge | J1 | Default model, medium |
| J4 | Settings resistance + strict mode | J2, D6 | Default model, medium |
| J5 | Pause for apps opened outside Yoga | D7, Play approval | Frontier model, medium |

Before building: an OpenSpec change `add-bypass-with-resistance` and ADR 0015, and an update to the privacy policy line about what the guard stores (passes and resists, kept 7 days like blocks).

Phone checks: a pass on YouTube Shorts and on Instagram Reels (start, pill, expiry, block resumes), the wait growing across 3 passes, strict mode, TalkBack hold, and the pause time-up nudge.
