## Why

People use YouTube to learn as well as to scroll. Today Yoga counts all YouTube time the same way: against the allowance and in the live time-value pill. Someone watching a lecture feels punished, and the burning pill tells them their study hour cost them money. The founder wants learning and productive videos allowed.

Regain (see `docs/research/regain.md`) does this with a user-chosen list of study channels. During focus, only those channels play, and Shorts and the home feed stay blocked. That works because the user decides what counts, not the app. Yoga adopts the same core and makes building the list much easier.

## What Changes

- Add **Learning channels**: a per-user list of YouTube channels whose videos count as learning.
- Make the list easy to build:
  - **Add with one tap** from the video the user is watching. The live pill gets a "Count as learning" action.
  - **Starter chips** of well-known education channels shown during setup. The user picks them; none are pre-selected.
  - The last few channels added are offered as chips again.
- **While a learning video plays:**
  - it doesn't count against the YouTube allowance;
  - the live pill switches to a calm "Learning" state, not burning;
  - the time is tracked as "Learning time" on the dashboard and in insights.
- **During a focus session:** YouTube opens only for learning channels. Anything else is sent Back with a short card. Shorts stay blocked even from learning channels, and the home feed is blocked by default during focus.
- **"Is this learning?" hint:** if a video from an unknown channel has study words in its title, the pill offers to add the channel. Yoga never decides on its own that a video is learning.
- **Later, optional:** on-device AI suggestions on supported phones (Gemini Nano through ML Kit's Prompt API). It only suggests; it never decides.

## Impact

- **Affected specs:** learning-videos (new); it extends yoga-guard (live pill, surface detection) and usage allowances.
- **Affected code:**
  - guard: YouTube watch-page reader, learning state, pill action;
  - usage accounting: subtracting learning time;
  - focus;
  - settings: Learning channels screen;
  - dashboard and insights.
- **Data:** stores only the channel names the user added and daily learning totals per app. Video titles are read in memory and never stored.
- **Policy:** reading the channel name and title is reading screen content. The Accessibility disclosure, privacy policy and Play declaration must say so.
- **Depends on Yoga guard** being enabled.
- **Proposed as a Yoga Pro feature** (see add-yoga-pro).

Status: proposal only. Build after the founder confirms.
