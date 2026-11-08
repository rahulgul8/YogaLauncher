## 1. Engine
- [ ] 1.1 Models in `intro/IntroModels.kt`: `IntroId`, `IntroSurface`, `IntroSignals`, `IntroRecord`, `IntroState`, `Intro` (copy, evidence, action, impact).
- [ ] 1.2 `intro/IntroEngine.kt`: pure selection rules (see proposal), with unit tests for every rule and the day boundary.
- [ ] 1.3 `intro/IntroStore.kt`: DataStore persistence of shown, snoozed, retired and last-new-date, plus the onboarding-finished time.
- [ ] 1.4 `intro/IntroSignalsReader.kt`: builds the signals from the usage, guard, insights and Pro repositories and installed packages.

## 2. Introductions
- [ ] 2.1 I1 Shorts block and I2 Reels block, with time-based evidence from Usage Access.
- [ ] 2.2 I3 browser surfaces: a block followed by the same surface in a browser within 5 minutes, twice or more.
- [ ] 2.3 I4 daily allowance at 80% of the average, rounded down to 15 minutes; I5 lower allowance via `suggestedAllowance`.
- [ ] 2.4 I8 grey past limit.

## 3. Surfaces and actions
- [ ] 3.1 `ui/IntroCard.kt` kit component.
- [ ] 3.2 The Usage screen card, the home line and the block-card sibling line.
- [ ] 3.3 Extract the guard disclosure into a reusable flow, with confirmation and Undo on return.
- [ ] 3.4 Help → Suggestions screen.

## 4. Docs
- [ ] 4.1 ADR 0012; tick piece C in `TODO.md`.
