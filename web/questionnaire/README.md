# Phone habits check-in (prototype)

A single static page for spec section 4.12: a free, 2-minute self-reflection on phone habits that points people to Yoga Launcher's features.

- **Not clinical.** Original questions; it does not use or imitate a validated scale and does not diagnose "addiction". The page says so before and after.
- **Private.** No network requests, cookies, analytics or external assets. In-progress answers sit in `sessionStorage` and vanish when the tab closes. No email capture — any future opt-in must be a separate, explicit choice.
- **Scoring** (`scoring.js`): 12 questions, 3 per theme (checking, sleep, focus, control), each 0–4. Total 0–14 *Mostly intentional*, 15–28 *Some autopilot*, 29–48 *Often on autopilot*. The two highest themes (ties broken checking → sleep → focus → control) choose the suggestions.

Open `index.html` directly in a browser. Run the tests with:

```bash
node --test web/questionnaire/scoring.test.mjs
```

Open items: the store link (`#install` placeholder), hosting, and whether to add an opt-in for launch updates.
