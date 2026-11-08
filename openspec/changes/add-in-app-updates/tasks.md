## 1. Updates
- [x] 1.1 Add the dependency `app-update-ktx` (Claude adds it before Codex starts).
- [x] 1.2 `update/UpdatePolicy.kt`: a pure decision from (availability, priority, staleness days, download state, last check, foreground) to an action, with unit tests.
- [x] 1.3 `update/UpdateController.kt`: thin glue around `AppUpdateManager`, covering:
  - the check throttle (24 h, stored in DataStore);
  - the install-state listener;
  - `completeUpdate()` when Yoga goes to the background.
- [x] 1.4 Home line: "Update ready · Update" / "Restart to update", in the `DefaultHomePrompt` style.
- [x] 1.5 Priority 4 shows the flexible dialog; priority 5 shows the immediate flow.
- [x] 1.6 Help → "Check for updates", with the version and the result.

## 2. What's new
- [x] 2.1 Bundled what's-new text per version (`res/raw/whats_new.json`, from `docs/play/release-notes.md`).
- [x] 2.2 Home line "New in X ·" after a version change, opening a one-screen sheet that is dismissed for good.
- [x] 2.3 Fresh installs never see it; only version changes do.

## 3. Tooling and docs
- [x] 3.1 `scripts/play-upload.mjs --priority N` (0–5, default 2) on upload and promote.
- [x] 3.2 ADR 0013, `docs/launch/release.md` (how to pick a priority), `TODO.md` piece A.
- [ ] 3.3 Tests (policy and What's-new done; controller with FakeAppUpdateManager still to write): `UpdatePolicy` unit tests, `WhatsNew` version logic, and the controller against `FakeAppUpdateManager`.
