You are working in the repo at C:\me\code\mobile\YogaLauncher. Read `YogaLauncher_Product_Specification.md` section 4.12 ("Free digital-wellbeing questionnaire") and section 9 (privacy) first. This task touches only a new `web/questionnaire/` folder (plus one line in the spec and README) — do not touch the Android app.

## Goal: Piece 7 — the free online habits questionnaire (prototype)

Build a single static page `web/questionnaire/index.html` (inline CSS and JS are fine; no frameworks, no external scripts, fonts or trackers) plus `web/questionnaire/scoring.js` (pure functions, also loaded by the page) and `web/questionnaire/scoring.test.mjs` runnable with `node --test web/questionnaire/scoring.test.mjs` (Node's built-in test runner; no npm packages).

Content and rules (decided — follow them):
1. **Framing**: "A 2-minute check-in on your phone habits". Clearly state: this is a self-reflection, **not a medical or psychological assessment, and not a diagnosis of addiction**. No shaming words ("addict", "problem user").
2. **12 original questions** (write your own wording; do not copy any published scale), 3 per theme: *checking* (reaching for the phone without a reason), *sleep* (phone use in bed/late night), *focus* (interruptions while working/studying), *control* (using longer than intended; wanting to cut down). Answers on a 5-point frequency scale: Never / Rarely / Sometimes / Often / Almost always (scored 0–4). One question per screen on mobile, keyboard accessible, progress indicator, back button.
3. **Scoring** (`scoring.js`): total 0–48 → bands *Mostly intentional* (0–14), *Some autopilot* (15–28), *Often on autopilot* (29–48); per-theme scores 0–12; the two highest themes drive suggestions. Export `scoreAnswers(answers)` returning `{ total, band, themes: {checking, sleep, focus, control}, topThemes: [..2] }` and validate input (exactly 12 integers 0–4, else throw).
4. **Results**: the band in plain language, a small bar per theme, and 2–3 practical suggestions per top theme (e.g. sleep → "keep the phone out of reach in bed; try a bedtime focus schedule"), each linked to how Yoga Launcher can help (mindful pause, allowances, focus schedules, greyscale at bedtime, timebox). A neutral "Try Yoga Launcher" button pointing at `#install` (placeholder; the store URL is not decided yet).
5. **Privacy**: answers never leave the browser (no network requests at all, no cookies, no localStorage except optionally remembering in-progress answers in `sessionStorage`). Say so on the page. **No email capture** in this prototype; add a TODO comment that any future opt-in must be explicit and separate.
6. **Design**: calm, monochrome with one muted green accent (#4f6f5a), large readable type, works at 360px wide and on desktop, respects `prefers-color-scheme` and `prefers-reduced-motion`.
7. **Tests**: band boundaries (14/15, 28/29), theme sums, top-theme tie-breaking (stable by theme order checking → sleep → focus → control), invalid input rejection.
8. Docs: add `web/questionnaire/README.md` (what it is, how to open it locally, how to run the tests, the non-clinical framing); tick "Prototype free online questionnaire and scoring interpretation" in the spec's section 13; add one line to the root `README.md` pointing at it.

## Constraints
Plain HTML/CSS/JS only; UTF-8 without BOM; no git commands that write. Verify with `node --test web/questionnaire/scoring.test.mjs` if Node runs in your sandbox, otherwise say so.

## Done means
Page works when opened directly from disk, tests pass, items 1–8 done; finish with a short summary.
